package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry

import arrow.core.Either
import arrow.core.right
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.*
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregation
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregator
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Domain
import de.unistuttgart.iste.sqa.clara.api.model.IpAddress
import de.unistuttgart.iste.sqa.clara.utils.regex.Regexes
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

// Services are actual Microservices
// Activities are entire business activities meaning the whole span of a trace
// Instances are instances of microservices
// Hardware is the used hardware (don't know if necessary)

class OpenTelemetryAggregator(private val spanProvider: SpanProvider) : Aggregator {

    private val log = KotlinLogging.logger {}

    private val serviceMap: MutableMap<Service.Name, Service> = mutableMapOf()
    private val relations: MutableList<Relation> = mutableListOf()
    private val unnamedServices: MutableMap<Service.HostName, Service> = mutableMapOf()
    private val unresolvableServices: MutableList<Service> = mutableListOf()

    override fun aggregate(): Either<AggregationFailure, Aggregation> {
        log.info { "Aggregate OpenTelemetry ..." }

        val spans = runBlocking { spanProvider.getSpans() }
        process(spans)

        // We assume that internal components that are part of any action send a trace, thus after merging all unnamed services, the leftovers have to be external
        val internalComponents = serviceMap.values.map {
            AggregatedComponent.Internal.OpenTelemetryComponent(
                name = AggregatedComponent.Name(it.name?.value!!),
                domain = Domain(it.hostName?.value ?: "not-found-domain"),
                paths = it.paths.toComponentPaths(),
            )
        }

        val externalComponents = unnamedServices.values.map {
            AggregatedComponent.External(
                name = AggregatedComponent.Name(it.hostName?.value!!),
                domain = Domain(it.hostName.value),
            )
        }

        val components = (internalComponents + externalComponents).toSet()

        val communications = relations.mapNotNull { relation ->
            val caller = internalComponents.find { component -> component.name.value == relation.caller.name?.value }
                ?: externalComponents.find { component -> component.domain.value == relation.callee.hostName?.value }
            val callee = internalComponents.find { component -> component.name.value == relation.callee.name?.value || component.name.value == relation.callee.hostName?.value }
                ?: externalComponents.find { component -> component.domain.value == relation.callee.hostName?.value }
            if (caller != null && callee != null) {
                AggregatedCommunication(AggregatedCommunication.Source(caller.name), AggregatedCommunication.Target(callee.name))
            } else {
                null
            }
        }.toSet()

        log.info { "Found ${components.size} components and ${communications.size} communications" }
        log.info { "Done aggregating OpenTelemetry" }

        return Aggregation(
            components = components,
            communications = communications,
        ).right()
    }

    // Proceeding of all ingoing spans via otel grpc interface
    // components and relations of them are discovered here
    private fun process(spans: List<Span>) {
        spans.forEach { span ->
            //// 1 update services
            runCatching {
                val relationInformation = extractRelationInformationAndUpdateServices(span)
                //// 2  Discover relations between services
                setRelations(relationInformation)
            }.getOrElse {
                log.error { "Exception encountered during span extraction: $it" }
            }
        }

        mergeServiceMaps()
    }

    private fun mergeServiceMaps() {

        val processedUnnamedService = mutableListOf<Service>()
        unnamedServices.forEach { (hostName, unnamedService) ->
            val serviceViaHostName = serviceMap.filter { (_, service) -> service.hostName == hostName || service.name?.value == hostName.value }.values.firstOrNull()
            val service = if (serviceViaHostName == null && unnamedService.paths.isNotEmpty()) {
                // TODO this might be to unsafe
                serviceMap.filter { (_, service) ->
                    service.paths.isNotEmpty()
                            && (service.paths.containsAll(unnamedService.paths) || unnamedService.paths.containsAll(service.paths))
                }.values.firstOrNull()
            } else {
                serviceViaHostName
            }
            if (service != null) {
                val mergedService = service.mergeWithOtherServiceObject(unnamedService)
                serviceMap.replace(service.name!!, mergedService)
                processedUnnamedService.add(unnamedService)
            }
        }
        processedUnnamedService.forEach { unnamedServices.remove(it.hostName) }
    }

    private fun extractRelationInformationAndUpdateServices(span: Span): SpanInformation = when (span.kind) {
        Span.Kind.Client -> {
            val spanInformation = extractInformationFromClientSpan(span)
            updateServices(spanInformation)
            spanInformation
        }

        Span.Kind.Server -> {
            val spanInformation = extractInformationFromServerSpan(span)
            updateServices(spanInformation)
            spanInformation
        }

        Span.Kind.Consumer -> {
            log.warn { "Consumer span identified" }
            throw UnsupportedOperationException()
        }

        Span.Kind.Producer -> {
            log.warn { "Producer span identified" }
            throw UnsupportedOperationException()
        }

        Span.Kind.Internal -> {
            log.warn { "Internal span identified" }
            throw UnsupportedOperationException()
        }
    }

    private fun updateServices(spanInformation: SpanInformation) {
        val client = Service(
            name = spanInformation.clientServiceName,
            hostName = null, // For now a client does not have a hostname
            ipAddress = spanInformation.clientIpAddress,
            port = spanInformation.clientPort,
            paths = emptyList() // For now a client does not have an path
        )
        updateService(client)

        val server = Service(
            name = spanInformation.serverServiceName,
            hostName = spanInformation.serverHostname,
            ipAddress = spanInformation.serverIpAddress,
            port = spanInformation.serverPort,
            paths = if (spanInformation.serverPath != null) {
                listOf(spanInformation.serverPath)
            } else {
                emptyList()
            },
        )
        updateService(server)
    }

    private fun updateService(service: Service) {
        if (service.name == null && service.hostName == null) {
            // TODO based on opentelemetry specification it is highly likely that we do not have the server's service name here in a server span, but the information is
            // TODO too valuable, therefore we need some sort of hold back list, where we can put the information into, and later on correlate it with the server span
            unresolvableServices.add(service)
        } else if (service.name != null) {
            if (!serviceMap.containsKey(service.name)) {
                serviceMap[service.name] = service
            } else {
                val oldService = serviceMap[service.name]!! // FIXME: save access, this could cause a NullPointerException
                val updatedService = service.mergeWithOtherServiceObject(oldService)
                serviceMap[service.name] = updatedService
            }
        } else if (service.hostName != null) {
            if (!unnamedServices.containsKey(service.hostName)) {
                unnamedServices[service.hostName] = service
            } else {
                val oldService = unnamedServices[service.hostName]!! // FIXME: save access, this could cause a NullPointerException
                val updatedService = service.mergeWithOtherServiceObject(oldService)
                unnamedServices[service.hostName] = updatedService
            }
        }
    }

    // Based on https://opentelemetry.io/docs/specs/otel/trace/sdk_exporters/zipkin/ and https://opentelemetry.io/docs/specs/semconv/general/attributes/
    private fun extractInformationFromClientSpan(clientSpan: Span): SpanInformation {

        // First we look for the exact name of the server (might not be available)
        val serverServiceName = clientSpan.attributes.filter { it.key == "peer.service" || it.key == "network.peer.service" }.values.firstOrNull()

        // filter all values that surely belong to the server side, then try to find more info with reg-exes
        val possibleKeyNamesForServerAttributes = listOf(
            // TODO apparently htt.url can be in some cases the client address
            "server.address", "server.port", "network.peer.address", "peer.hostname", "peer.address", "net.peer.name", "net.peer.port", "db.connection_string", "http.uri", "http.url", "http.target", "uri", "url",
        )

        // filter all values that surely belong to the client side, then try to find more info with reg-exes
        val possibleKeyNamesForClientAttributes = listOf(
            "client.address", "client.port"
        )
        val possibleServerValues = clientSpan.attributes.filter {
            it.key.lowercase() in possibleKeyNamesForServerAttributes
        }.values

        val possibleClientValues = clientSpan.attributes.filter {
            it.key.lowercase() in possibleKeyNamesForClientAttributes
        }.values

        val serverHostName = possibleServerValues.firstNotNullOfOrNull { Regexes.hostName.find(it)?.value }?.split("://")?.get(1)?.split("/")?.first()?.split(":")?.first()
        val serverIpAddress = possibleServerValues.firstNotNullOfOrNull { Regexes.ipAddressV4.find(it)?.value }
        val serverPath = possibleServerValues.firstNotNullOfOrNull { Regexes.urlPath.find(it)?.value } // TODO regex too lazy returns entire url
        val serverPort = possibleServerValues.firstNotNullOfOrNull { Regexes.port.find(it)?.value }?.toIntOrNull()

        val clientHostName = possibleClientValues.firstNotNullOfOrNull { Regexes.hostName.find(it)?.value }?.split("://")?.get(1)?.split("/")?.first()?.split(":")?.first()
        val clientIpAddress = possibleClientValues.firstNotNullOfOrNull { Regexes.ipAddressV4.find(it)?.value }
        val clientPort = possibleClientValues.firstNotNullOfOrNull { Regexes.port.find(it)?.value }?.toIntOrNull()

        return SpanInformation(
            clientServiceName = clientSpan.serviceName,
            serverServiceName = serverServiceName?.let { Service.Name(serverServiceName) },
            serverHostname = serverHostName?.let { Service.HostName(serverHostName) },
            serverIpAddress = serverIpAddress?.let { IpAddress(serverIpAddress) },
            serverPath = serverPath?.let { Service.Path(serverPath) },
            serverPort = serverPort?.let { Service.Port(serverPort) },
            clientIpAddress = clientIpAddress?.let { IpAddress(clientIpAddress) },
            clientHostName = clientHostName?.let { Service.HostName(clientHostName) },
            clientPort = clientPort?.let { Service.Port(clientPort) },
        )
    }

    private fun extractInformationFromServerSpan(serverSpan: Span): SpanInformation {
        // extract server information: especially service name and the paths if available.
        // best case would be some FQDN that one could later merge with the hostNames of the unnamed services
        // if not available merging via other attributes such as paths could be tried

        // filter all values that surely belong to the server side, then try to find more info with reg-exes
        val possibleKeyNamesForServerAttributes = listOf(
            "server.address", "server.port", "http.uri", "http.url", "uri", "url", "http.target", "net.sock.host.addr", "net.sock.host.port",
        )

        val possibleServerValues = serverSpan.attributes.filter {
            it.key.lowercase() in possibleKeyNamesForServerAttributes
        }.values

        val serverHostName = possibleServerValues.firstNotNullOfOrNull { Regexes.hostName.find(it)?.value }?.split("://")?.get(1)?.split("/")?.first()?.split(":")?.first()
        val serverIpAddress = possibleServerValues.firstNotNullOfOrNull { Regexes.ipAddressV4.find(it)?.value }
        val serverPath = possibleServerValues.firstNotNullOfOrNull { Regexes.urlPath.find(it)?.value } // TODO regex too lazy returns entire url
        val serverPort = possibleServerValues.firstNotNullOfOrNull { Regexes.port.find(it)?.value }?.toIntOrNull()

        // TODO check for possible client attributes "net.sock.peer.addr"
        // TODO ensure it works properly
        return SpanInformation(
            clientServiceName = null,
            serverServiceName = serverSpan.serviceName,
            serverHostname = serverHostName?.let { Service.HostName(serverHostName) },
            serverIpAddress = serverIpAddress?.let { IpAddress(serverIpAddress) },
            serverPath = serverPath?.let { Service.Path(serverPath) },
            serverPort = serverPort?.let { Service.Port(serverPort) },
            clientIpAddress = null,
            clientHostName = null,
            clientPort = null,
        )
    }

    // For now, we simply add every relation even if it duplicates. In the end a filtering could be done.
    private fun setRelations(spanInformation: SpanInformation) {
        val caller = serviceMap[spanInformation.clientServiceName] ?: throw UnsupportedOperationException()
        val callee = serviceMap[spanInformation.serverServiceName] ?: unnamedServices[spanInformation.serverHostname] ?: throw UnsupportedOperationException() // Todo its unlikely we have the serverServiceName often, we need resilience here
        val path = callee.paths.find { it == spanInformation.serverPath }
        val relation = Relation(
            owner = caller, // The caller always owns the call
            caller = caller,
            callee = callee,
            path = path,
        )

        relations.add(relation)
    }
}
