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

/**
 * Aggregator that analyzes OpenTelemetry spans and extracts client, server, producer,
 * and consumer components as well as their communications.
 *
 * @property spanProvider The provider for OpenTelemetry spans.
 */
class OpenTelemetryAggregator(private val spanProvider: SpanProvider) : Aggregator {

    private val log = KotlinLogging.logger {}

    private val serviceMap: MutableMap<Service.Name, Service> = mutableMapOf()
    private val relations: MutableList<Relation> = mutableListOf()
    private val unnamedServices: MutableMap<Service.HostName, Service> = mutableMapOf()
    private val unresolvableServices: MutableList<Service> = mutableListOf()

    /**
     * Collects spans for the configured time, then calls process to process them and merge the found information into an Aggregation.
     *
     * We assume that internal components that are part of any action send a trace, thus after merging all unnamed services, the leftovers have to be external.
     * If there are falsely as external labeled internal components, they might be merged in the [de.unistuttgart.iste.sqa.clara.merge.DynamicMerger].
     */
    override fun aggregate(): Either<AggregationFailure, Aggregation> {
        log.info { "Aggregate OpenTelemetry ..." }

        val spans = runBlocking { spanProvider.getSpans() }
        process(spans)

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

    /**
     * Processing of all ingoing spans, collected via oTel grpc interface.
     *
     * @param spans the collected spans.
     */
    private fun process(spans: List<Span>) {
        spans.forEach { span ->
            runCatching {
                val relationInformation = extractRelationInformationAndUpdateServices(span)
                setRelations(relationInformation)
            }.getOrElse {
                log.trace { "Exception encountered during span extraction: $it" }
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
        Span.Kind.Client, Span.Kind.Server -> {
            val spanInformation = extractInformationFromClientOrServerSpan(span)
            updateServices(spanInformation)
            spanInformation
        }

        Span.Kind.Consumer -> {
            throw UnsupportedOperationException("Consumer span identified")
        }

        Span.Kind.Producer -> {
            throw UnsupportedOperationException("Producer span identified")
        }

        Span.Kind.Internal -> {
            throw UnsupportedOperationException("Internal span identified")
        }
    }

    private fun updateServices(spanInformation: SpanInformation) {
        val client = Service(
            name = spanInformation.client.serviceName,
            hostName = spanInformation.client.hostName, // It is unlikely to find a client hostName in the spans
            ipAddress = spanInformation.client.ipAddress,
            port = spanInformation.client.port,
            paths = emptyList() // Clients don't have paths
        )
        updateService(client)

        val server = Service(
            name = spanInformation.server.serviceName,
            hostName = spanInformation.server.hostName,
            ipAddress = spanInformation.server.ipAddress,
            port = spanInformation.server.port,
            paths = if (spanInformation.server.path != null) {
                listOf(spanInformation.server.path)
            } else {
                emptyList()
            },
        )
        updateService(server)
    }

    private fun updateService(service: Service) {
        if (service.name == null && service.hostName == null) {
            // TODO If they cannot be resolved we log them and might in the future use them
            unresolvableServices.add(service)
            log.trace { "Added service $service to unresolvable Services" }
        } else if (service.name != null) {
            val oldService = serviceMap[service.name]
            if (oldService == null) {
                serviceMap[service.name] = service
            } else {
                val updatedService = service.mergeWithOtherServiceObject(oldService)
                serviceMap[service.name] = updatedService
            }
        } else if (service.hostName != null) {
            val oldService = unnamedServices[service.hostName]
            if (oldService == null) {
                unnamedServices[service.hostName] = service
            } else {
                val updatedService = service.mergeWithOtherServiceObject(oldService)
                unnamedServices[service.hostName] = updatedService
            }
        }
    }

    // Based on https://opentelemetry.io/docs/specs/otel/trace/sdk_exporters/zipkin/ and https://opentelemetry.io/docs/specs/semconv/general/attributes/
    private fun extractInformationFromClientOrServerSpan(span: Span): SpanInformation {

        val (possibleKeyNamesForServerAttributes, possibleKeyNamesForClientAttributes) = when (span.kind) {
            Span.Kind.Server -> {
                listOf(
                    "server.address", "server.port", "http.uri", "http.url", "uri", "url", "http.target", "net.sock.host.addr", "net.sock.host.port",
                ) to listOf(
                    "net.sock.peer.addr", "net.sock.peer.port"
                )
            }

            Span.Kind.Client -> {
                listOf(
                    // Hint: using micrometer apparently htt.url can be in some cases the client address; if so remove it for the time of using micrometer instrumentation.
                    "server.address", "server.port", "network.peer.address", "peer.hostname", "peer.address", "net.peer.name", "net.peer.port", "db.connection_string", "http.uri", "http.url", "http.target", "uri", "url",
                ) to listOf(
                    "client.address", "client.port"
                )
            }

            else -> throw UnsupportedOperationException("This method only handles Client and Server Spans")
        }

        val possibleServerValues = span.attributes.filter {
            it.key.lowercase() in possibleKeyNamesForServerAttributes
        }.values

        val possibleClientValues = span.attributes.filter {
            it.key.lowercase() in possibleKeyNamesForClientAttributes
        }.values

        val (clientServiceName, serverServiceName) = when (span.kind) {
            Span.Kind.Server -> null to span.serviceName
            Span.Kind.Client -> span.serviceName to span.attributes.filter { it.key == "peer.service" || it.key == "network.peer.service" }.values.firstOrNull()?.let { serviceName -> Service.Name(serviceName) }
            else -> throw UnsupportedOperationException("This method only handles Client and Server Spans")
        }

        val serverHostName = possibleServerValues.findHostName()
        val serverIpAddress = possibleServerValues.findIpAddress()
        val serverPort = possibleServerValues.findPort()
        val serverPath = possibleServerValues.findPath()

        val clientHostName = possibleClientValues.findHostName()
        val clientIpAddress = possibleClientValues.findIpAddress()
        val clientPort = possibleClientValues.findPort()

        return SpanInformation(
            SpanInformation.Server(
                serviceName = serverServiceName,
                hostName = serverHostName?.let { Service.HostName(serverHostName) },
                ipAddress = serverIpAddress?.let { IpAddress(serverIpAddress) },
                path = serverPath?.let { Service.Path(serverPath) },
                port = serverPort?.let { Service.Port(serverPort) },
            ),
            SpanInformation.Client(
                serviceName = clientServiceName,
                ipAddress = clientIpAddress?.let { IpAddress(clientIpAddress) },
                hostName = clientHostName?.let { Service.HostName(clientHostName) },
                port = clientPort?.let { Service.Port(clientPort) },
            )
        )
    }

    private fun setRelations(spanInformation: SpanInformation) {
        val caller = serviceMap[spanInformation.client.serviceName] ?: return
        val callee = serviceMap[spanInformation.server.serviceName] ?: unnamedServices[spanInformation.server.hostName] ?: return
        val path = callee.paths.find { it == spanInformation.server.path }
        val relation = Relation(
            owner = caller, // The caller always owns the call
            caller = caller,
            callee = callee,
            path = path,
        )

        relations.add(relation)
    }

    private fun Collection<String>.findHostName(): String? =
        this.firstNotNullOfOrNull { Regexes.hostName.find(it)?.value }?.split("://")?.get(1)?.split("/")?.first()?.split(":")?.first()

    private fun Collection<String>.findIpAddress(): String? =
        this.firstNotNullOfOrNull { Regexes.ipAddressV4.find(it)?.value }

    private fun Collection<String>.findPort(): Int? =
        this.firstNotNullOfOrNull { Regexes.port.find(it)?.value }?.toIntOrNull()

    private fun Collection<String>.findPath(): String? =
        this.firstNotNullOfOrNull { Regexes.urlPath.find(it)?.value }?.split("//")?.last()?.substringAfter("/")
}
