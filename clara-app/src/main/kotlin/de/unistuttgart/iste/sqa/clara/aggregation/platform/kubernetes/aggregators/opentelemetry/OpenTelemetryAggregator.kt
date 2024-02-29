package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.*
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure
import de.unistuttgart.iste.sqa.clara.api.aggregation.CommunicationAggregator
import de.unistuttgart.iste.sqa.clara.api.model.*
import de.unistuttgart.iste.sqa.clara.utils.regex.Regexes
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

// Services are actual Microservices
// Activities are entire business activities meaning the whole span of a trace
// Instances are instances of microservices
// Hardware is the used hardware (don't know if necessary)

class OpenTelemetryAggregator(private val spanProvider: SpanProvider) : CommunicationAggregator {

    private val log = KotlinLogging.logger {}

    private val serviceMap: MutableMap<Service.Name, Service> = mutableMapOf()
    private val relations: MutableList<Relation> = mutableListOf()
    private val unnamedServices: MutableMap<Service.HostIdentifier, Service> = mutableMapOf()
    private val unresolvableServices: MutableList<Service> = mutableListOf()

    override fun aggregate(): Either<AggregationFailure, Set<AggregatedCommunication>> {

        val spans = runBlocking { spanProvider.getSpans() }
        process(spans)

        // TODO the entire mapping needs to be discussed
        val components = serviceMap.values.map {
            AggregatedComponent.Internal.OpenTelemetryComponent(
                name = AggregatedComponent.Name(it.name?.value!!),
                domain = Domain(it.hostIdentifier?.value ?: throw UnsupportedOperationException("TODO")),
                endpoints = it.endpoints.toComponentEndpoints(),
            )
        }

        val unnamedComponents = unnamedServices.values.map {
            AggregatedComponent.Internal.OpenTelemetryComponent(
                name = AggregatedComponent.Name(it.name?.value ?: "not-found-name"),
                domain = Domain(it.hostIdentifier?.value ?: throw UnsupportedOperationException("TODO")),
                endpoints = it.endpoints.toComponentEndpoints(),
            )
        }

        val mergedComponents = components + unnamedComponents

        // TODO make a differentiation to external services
        val communications = relations.map { relation ->
            val caller = mergedComponents.find { component -> component.name.value == relation.caller.name?.value } ?: mergedComponents.find { component -> component.domain.value == relation.caller.hostIdentifier?.value }
            val callee = mergedComponents.find { component -> component.name.value == relation.callee.name?.value } ?: mergedComponents.find { component -> component.domain.value == relation.callee.hostIdentifier?.value }
            if (caller != null && callee != null) {
                AggregatedCommunication(AggregatedCommunication.Source(caller.name), AggregatedCommunication.Target(callee.name))
            } else {
                throw UnsupportedOperationException()
            }
        }.toSet()

        return Either.Right(communications)
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
        unnamedServices.forEach { (hostIdentifier, unnamedService) ->
            val serviceViaIdentifier = serviceMap.filter { (_, service) -> service.hostIdentifier == hostIdentifier }.values.firstOrNull()
            val service = if (serviceViaIdentifier == null && unnamedService.endpoints.isNotEmpty()) {
                // TODO this might be to unsafe
                serviceMap.filter { (_, service) ->
                    service.endpoints.isNotEmpty()
                            && (service.endpoints.containsAll(unnamedService.endpoints) || unnamedService.endpoints.containsAll(service.endpoints))
                }.values.firstOrNull()
            } else {
                serviceViaIdentifier
            }
            if (service != null) {
                val mergedService = service.mergeWithOtherServiceObject(unnamedService)
                serviceMap.replace(service.name!!, mergedService)
                processedUnnamedService.add(unnamedService)
            }
        }
        processedUnnamedService.forEach { unnamedServices.remove(it.hostIdentifier) }
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
            hostIdentifier = null, // For now a client does not have a hostname
            ipAddress = spanInformation.clientIpAddress,
            port = spanInformation.clientPort,
            endpoints = emptyList() // For now a client does not have an endpoint
        )
        updateService(client)

        val server = Service(
            name = spanInformation.serverServiceName,
            hostName = spanInformation.serverHostname,
            hostIdentifier = spanInformation.serverHostIdentifier,
            ipAddress = spanInformation.serverIpAddress,
            port = spanInformation.serverPort,
            endpoints = if (spanInformation.serverEndpoint != null) {
                listOf(spanInformation.serverEndpoint)
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
                val oldService = serviceMap[service.name]!!
                val updatedService = service.mergeWithOtherServiceObject(oldService)
                serviceMap[service.name] = updatedService
            }
        } else if (service.hostIdentifier != null) {
            if (!unnamedServices.containsKey(service.hostIdentifier)) {
                unnamedServices[service.hostIdentifier] = service
            } else {
                val oldService = unnamedServices[service.hostIdentifier]!!
                val updatedService = service.mergeWithOtherServiceObject(oldService)
                unnamedServices[service.hostIdentifier] = updatedService
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
            "server.address", "server.port", "network.peer.address", "peer.hostname", "peer.address", "db.name", "http.uri", "http.url", "http.target", "uri", "url",
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

        // TODO especially take a look for kubernetes internal names
        val serverHostName = possibleServerValues.firstNotNullOfOrNull { Regexes.hostName.find(it)?.value }?.removePrefix("https://")?.removePrefix("http://")
        val serverIpAddress = possibleServerValues.firstNotNullOfOrNull { Regexes.ipAddressV4.find(it)?.value }
        val serverPath = possibleServerValues.firstNotNullOfOrNull { Regexes.urlEndpoint.find(it)?.value } // TODO not found, regex might not work for fqdns
        val serverPort = possibleServerValues.firstNotNullOfOrNull { Regexes.port.find(it)?.value }?.toIntOrNull()

        val serverHostIdentifier = if (serverPort != null) {
            "${serverHostName}:${serverPort}"
        } else {
            serverHostName
        }

        val clientHostName = possibleClientValues.firstNotNullOfOrNull { Regexes.hostName.find(it)?.value }?.removePrefix("https://")?.removePrefix("http://")
        val clientIpAddress = possibleClientValues.firstNotNullOfOrNull { Regexes.ipAddressV4.find(it)?.value }
        val clientPort = possibleClientValues.firstNotNullOfOrNull { Regexes.port.find(it)?.value }?.toIntOrNull()

        return SpanInformation(
            clientServiceName = clientSpan.serviceName,
            serverServiceName = serverServiceName?.let { Service.Name(serverServiceName) },
            serverHostname = serverHostName?.let { Service.HostName(serverHostName) },
            serverHostIdentifier = serverHostIdentifier?.let { Service.HostIdentifier(serverHostIdentifier) },
            serverIpAddress = serverIpAddress?.let { IpAddress(serverIpAddress) },
            serverEndpoint = serverPath?.let { Service.Endpoint(serverPath) },
            serverPort = serverPort?.let { Service.Port(serverPort) },
            clientIpAddress = clientIpAddress?.let { IpAddress(clientIpAddress) },
            clientHostName = clientHostName?.let { Service.HostName(clientHostName) },
            clientPort = clientPort?.let { Service.Port(clientPort) },
        )
    }

    private fun extractInformationFromServerSpan(serverSpan: Span): SpanInformation {
        // extract server information: especially service name and the endpoints if available.
        // best case would be some FQDN that one could later merge with the hostIdentifiers of the unnamed services
        // if not available merging via other attributes such as endpoints could be tried

        // filter all values that surely belong to the server side, then try to find more info with reg-exes
        val possibleKeyNamesForServerAttributes = listOf(
            "server.address", "server.port", "http.uri", "http.url", "uri", "url",
        )

        val possibleServerValues = serverSpan.attributes.filter {
            it.key.lowercase() in possibleKeyNamesForServerAttributes
        }.values

        val serverHostName = possibleServerValues.firstNotNullOfOrNull { Regexes.hostName.find(it)?.value }?.removePrefix("https://")?.removePrefix("http://")
        val serverIpAddress = possibleServerValues.firstNotNullOfOrNull { Regexes.ipAddressV4.find(it)?.value }
        val serverPath = possibleServerValues.firstNotNullOfOrNull { Regexes.urlEndpoint.find(it)?.value } // TODO not found, regex might not work for fqdns
        val serverPort = possibleServerValues.firstNotNullOfOrNull { Regexes.port.find(it)?.value }?.toIntOrNull()

        val serverHostIdentifier = if (serverPort != null) {
            "${serverHostName}:${serverPort}"
        } else {
            serverHostName
        }

        // TODO check for possible client attributes
        // TODO ensure it works properly
        return SpanInformation(
            clientServiceName = null,
            serverServiceName = serverSpan.serviceName,
            serverHostname = serverHostName?.let { Service.HostName(serverHostName) },
            serverHostIdentifier = serverHostIdentifier?.let { Service.HostIdentifier(serverHostIdentifier) },
            serverIpAddress = serverIpAddress?.let { IpAddress(serverIpAddress) },
            serverEndpoint = serverPath?.let { Service.Endpoint(serverPath) },
            serverPort = serverPort?.let { Service.Port(serverPort) },
            clientIpAddress = null,
            clientHostName = null,
            clientPort = null,
        )
    }

    // For now, we simply add every relation even if it duplicates. In the end a filtering could be done.
    private fun setRelations(spanInformation: SpanInformation) {
        val caller = serviceMap[spanInformation.clientServiceName] ?: throw UnsupportedOperationException()
        val callee = serviceMap[spanInformation.serverServiceName] ?: unnamedServices[spanInformation.serverHostIdentifier] ?: throw UnsupportedOperationException() // Todo its unlikely we have the serverServiceName often, we need resilience here
        val endpoint = callee.endpoints.find { it == spanInformation.serverEndpoint }
        val relation = Relation(
            owner = caller, // The caller always owns the call
            caller = caller,
            callee = callee,
            endpoint = endpoint,
        )

        relations.add(relation)
    }
}/*
                // 2.1  compute the first span of a transaction
                //      Mapping of the first span's path (name) with a service via the ComponentMapping-collection
                //      and creation of newly discovered relations between activities and services.
                //if (span.parentId == null) {
                // }
                // 2.1.1 Create discovered relations between service <-> activities
                var mappingFound = false
                val deprecatedMappings: MutableList<ComponentMapping> = ArrayList<ComponentMapping>()
                for (componentMapping in componentMappingService.findAll()) {
                    if (componentMapping.getHttpMethods() and HttpMethod.valueOf(method).getValue() !== 0 && Pattern.compile(
                            componentMapping.getHttpPathRegex()
                        ).matcher(path).find()
                    ) {
                        val activityRevision: Revision =
                            revisionService.getCurrentRevisionsByComponentId().get(componentMapping.getComponent().getId())
                        if (activityRevision != null) {
                            val activityRelation = Relation()
                            activityRelation.setCaller(activityRevision)
                            activityRelation.setCallee(getServiceRevision(srAnnotation.endpoint))
                            activityRelation.setOwner(activityRevision)
                            addTransactionRelation(activityRelation, span)
                            val serviceRevision: Revision = getServiceRevision(srAnnotation.endpoint)
                            val instanceRevision: Revision = getInstanceRevision(srAnnotation.endpoint)
                            val hardwareRevision: Revision = getHardwareRevision(srAnnotation.endpoint)
                            addTransactionRelation(
                                relationService.findByOwnerAndCallerAndCallee(
                                    serviceRevision,
                                    serviceRevision,
                                    instanceRevision
                                ), span
                            )
                            addTransactionRelation(
                                relationService.findByOwnerAndCallerAndCallee(
                                    instanceRevision,
                                    instanceRevision,
                                    hardwareRevision
                                ), span
                            )
                            mappingFound = true
                        } else deprecatedMappings.add(componentMapping)
                    }
                }

                // 2.1.2 if a mapping of activity <-> service exists for a activity without current revision (means, the activity was removed from the modeled processes), remove the mapping
                if (deprecatedMappings.size > 0) componentMappingService.delete(deprecatedMappings)

                // 2.1.3 if a trace could not be mapped, add it to the list of unmappedTraces (only if no other trace with same path was already added)
                if (!mappingFound && unmappedTraceService.findByHttpPathAndMethod(path, HttpMethod.valueOf(method)) == null) {
                    val unmappedTrace = UnmappedTrace()
                    unmappedTrace.setHttpMethod(HttpMethod.valueOf(method))
                    unmappedTrace.setHttpPath(path)
                    unmappedTrace.setTraceId(span.traceId)
                    unmappedTraceService.saveUnmappedTrace(unmappedTrace)
                }
            } else if (csAnnotation != null || srAnnotation != null) {
                val annotation = csAnnotation ?: srAnnotation!!
                val serviceRevision: Revision = getServiceRevision(annotation.endpoint)
                val instanceRevision: Revision = getInstanceRevision(annotation.endpoint)
                val hardwareRevision: Revision = getHardwareRevision(annotation.endpoint)
                addTransactionRelation(
                    relationService.findByOwnerAndCallerAndCallee(serviceRevision, serviceRevision, instanceRevision),
                    span
                )
                addTransactionRelation(
                    relationService.findByOwnerAndCallerAndCallee(instanceRevision, instanceRevision, hardwareRevision),
                    span
                )
                var relation: Relation? =
                    incompleteRelations[span.id] // if relation is null, the equivalent Server- or Client-span was not yet processed
                if (relation == null) {
                    relation = Relation()
                    incompleteRelations[span.id] = relation
                }
                if (csAnnotation != null) relation.setCaller(serviceRevision) else relation.setCallee(serviceRevision)

                // add all annotations to the new discovered relation
                relation.setAnnotationsFromBinaryAnnotations(span.binaryAnnotations)

                //if the relation is complete (has caller and callee), save it persistently and generate transitive relations (S1 -> S2 and S2-> S3 => S1 -> S3)
                if (relation.getCaller() != null && relation.getCallee() != null) {
                    incompleteRelations.remove(span.id)
                    if (span.parentId != null) {
                        if (!latestRelationsByParent.containsKey(span.parentId)) latestRelationsByParent[span.parentId] =
                            LinkedList<Relation>()
                        latestRelationsByParent[span.parentId]!!.add(relation)
                        proceedLocalComponentForRelation(span.parentId, relation)
                    }
                    relation.setOwner(relation.getCaller())
                    addTransactionRelation(relation, span)
                } // Store local components
            } else {
                for (annotation in span.binaryAnnotations) {
                    if (annotation.key.toLowerCase().equals("lc")) {
                        localComponentSpans[span.id] = span
                        if (latestRelationsByParent.containsKey(span.id)) {
                            for (relation in latestRelationsByParent[span.id]) {
                                proceedLocalComponentForRelation(span.id, relation)
                            }
                        }
                        break
                    }
                }
            }


            //// 3  Discover relations between services through parent - child ids
            // 3.1  compute the first span of a transaction (usually Zuul's SR-Response to the not instrumented client)
            //      Mapping of the first span's path (name) with a service via the ComponentMapping-collection
            //      and creation of newly discovered relations between activities and services.
            val annotation = csAnnotation ?: srAnnotation!!
            val serviceRevision: Revision = getServiceRevision(annotation.endpoint)
            val instanceRevision: Revision = getInstanceRevision(annotation.endpoint)
            val hardwareRevision: Revision = getHardwareRevision(annotation.endpoint)
            addTransactionRelation(checkForExistingRelation(serviceRevision, serviceRevision, instanceRevision), span)
            addTransactionRelation(checkForExistingRelation(instanceRevision, instanceRevision, hardwareRevision), span)
            if (span.parentId != null) {
                val parentSpan: Span? = tempStorageSpans[span.parentId]
                if (parentSpan != null) {
                    val callerRevision: Revision = getServiceRevision(parentSpan.annotations.get(0).endpoint)
                    val r: Relation = relationService.findByOwnerAndCallerAndCallee(callerRevision, callerRevision, serviceRevision)
                    if (r == null) {
                        val relation = Relation()
                        relation.setOwner(callerRevision)
                        relation.setCaller(callerRevision)
                        relation.setCallee(serviceRevision)

                        // add all annotations to the new discovered relation
                        relation.setAnnotationsFromBinaryAnnotations(span.binaryAnnotations)

                        //if the relation is complete (has caller and callee), save it persistently and generate transitive relations (S1 -> S2 and S2-> S3 => S1 -> S3)
                        if (relation.getCaller() != null && relation.getCallee() != null) {
                            addTransactionRelation(relation, span)
                        }
                    }
                }

                // Store local components
            } else {
                for (bannotation in span.binaryAnnotations) {
                    if (bannotation.key.toLowerCase().equals("lc")) {
                        localComponentSpans[span.id] = span
                        if (latestRelationsByParent.containsKey(span.id)) {
                            for (relation in latestRelationsByParent[span.id]) {
                                proceedLocalComponentForRelation(span.id, relation)
                            }
                        }
                        break
                    }
                }
            }
        }
    }

    private fun proceedLocalComponentForRelation(spanId: String?, relation: Relation) {
        if (spanId != null) {
            val span: Span? = localComponentSpans[spanId]
            if (span != null) {
                for (lcAnnotation in span.binaryAnnotations) {
                    if (lcAnnotation.key.toLowerCase().equals("thread")) {
                        relation.setAnnotation("ad.async", "true")
                    }
                }
            }
        }
    }

    // Add newly discovered relation to a transaction and create transitive relations from the collection of transaction-related relations
    private fun addTransactionRelation(relation: Relation?, span: Span) {
        var relation: Relation = relation
        val relations: MutableList<Relation?> = transactionRelations.computeIfAbsent(
            span.traceId,
            Function<Long, MutableList<Relation?>> { k: Long? -> ArrayList<Relation?>() })
        val relationsToCheck: MutableList<Relation> = ArrayList<Relation>()
        relationsToCheck.add(relation)
        if (relation != null) {
            while (relationsToCheck.size > 0) {
                relation = relationsToCheck[0]

                // if the id is null, its uncertainly, if the relation already exists, checking against the object-repository required
                if (relation.getId() == null) {
                    val existingRelation: Relation =
                        relationService.findByOwnerAndCallerAndCallee(relation.getOwner(), relation.getCaller(), relation.getCallee())

                    // the relation exists already and is not new discovered. So use the found relation-object for further processing and add newly discovered annotations
                    if (existingRelation != null) { // Todo: Check if there are really new annotations and if not, dont update/save the object! (many wrong update-Changelogs because of useless object-updates without changes)
                        existingRelation.setAnnotations(relation.getAnnotations())
                        relation = existingRelation
                    }
                    relation = relationService.saveRelation(relation)
                } else if (relation.annotationsRequireSave()) relation = relationService.saveRelation(relation)
                relations.add(relation) // get all new transitive relations of the set of transaction-relations and the current relation
                relationsToCheck.addAll(getTransitiveRelations(relation, relations))
                relationsToCheck.remove(relation)
            }
        }
    }

    // check if relation exists, if not create one, but do not save
    private fun checkForExistingRelation(owner: Revision, caller: Revision, callee: Revision): Relation? {
        var relation: Relation? = null
        val checkRelation: Relation = relationService.findByOwnerAndCallerAndCallee(owner, caller, callee)
        if (checkRelation == null) {
            relation = Relation()
            relation.setOwner(owner)
            relation.setCaller(caller)
            relation.setCallee(callee)
        } else {
            relation = checkRelation
        }
        return relation
    }

    // Finds and returns transitive relations between a relation and a list of relations
    // returns only relations, which are not already in the submitted list of relations
    private fun getTransitiveRelations(relation: Relation, relations: List<Relation?>): List<Relation> {
        val transitiveRelations: MutableList<Relation> = ArrayList<Relation>()
        for (currentRelation in relations) {
            var topRelation: Relation
            var bottomRelation: Relation
            if (currentRelation.getCaller().getId().equals(relation.getCallee().getId())) {
                topRelation = relation
                bottomRelation = currentRelation
            } else if (relation.getCaller().getId().equals(currentRelation.getCallee().getId())) {
                topRelation = currentRelation
                bottomRelation = relation
            } else continue
            val newRelation = Relation()
            newRelation.setCaller(bottomRelation.getCaller())
            newRelation.setCallee(bottomRelation.getCallee())
            newRelation.setOwner(topRelation.getOwner())
            newRelation.setAnnotations(bottomRelation.getAnnotations())
            if (!relations.contains(newRelation)) transitiveRelations.add(newRelation)
        }
        return transitiveRelations
    }

    private fun getServiceRevision(endpoint: Endpoint): Revision {
        val serviceName: String = endpoint.serviceName.toUpperCase()
        if (!revisionService.getCurrentRevisionsByComponentName().containsKey(serviceName)) updateComponents(endpoint)
        return revisionService.getCurrentRevisionsByComponentName().get(serviceName)
    }

    private fun getInstanceRevision(endpoint: Endpoint): Revision {
        val instanceName = getInstanceName(endpoint)
        if (!revisionService.getCurrentRevisionsByComponentName().containsKey(instanceName)) updateComponents(endpoint)
        return revisionService.getCurrentRevisionsByComponentName().get(instanceName)
    }

    private fun getHardwareRevision(endpoint: Endpoint): Revision {
        val hardwareName = getHardwareName(endpoint)
        if (!revisionService.getCurrentRevisionsByComponentName().containsKey(hardwareName)) updateComponents(endpoint)
        return revisionService.getCurrentRevisionsByComponentName().get(hardwareName)
    }

    private fun getInstanceName(endpoint: Endpoint): String {
        val serviceName: String = endpoint.serviceName.toUpperCase()
        val ip = getHardwareName(endpoint)
        return ip + ":" + serviceName + ":" + endpoint.port
    }

    private fun getHardwareName(endpoint: Endpoint): String {
        var ip = ""
        try {
            if (endpoint.ipv4 !== 0) ip =
                InetAddress.getByAddress(BigInteger.valueOf(endpoint.ipv4).toByteArray()).hostAddress else if (endpoint.ipv6 != null) ip =
                InetAddress.getByAddress(endpoint.ipv6).hostAddress
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        if (ip.isEmpty()) {
            ip = "unknown"
        }
        return ip
    }

    private fun mapSpansAndSave(spans: List<Span>) {
        spanMappingService.mapAndSaveSpans(spans)
    }

    // Storage-class which overrides the default storage behaviour of the ZipKin server.
    // Behaves like a extended Zipkin-Mysql-Storage by being a proxy.
    // It forwards any spans to the standard mysql-storage. IF they could be validated and stored by the standard-storage,
    // they are additionally forwarded to a custom span-progressing method for architecture discovery
    @Suppress("unused")
    internal class ZipkinStorage @Autowired constructor(context: ApplicationContext) {
        private val context: ApplicationContext

        init {
            this.context = context
        }

        @Bean
        fun storage(executor: Executor?, dataSource: DataSource?): StorageComponent {
            val mysqlStorage: MySQLStorage = MySQLStorage.builder().executor(executor).datasource(dataSource).build()
            val consumer = AsyncSpanConsumer { spans, callback ->
                val myCallback: Callback<Void> = object : Callback<Void?>() {
                    fun onSuccess(value: Void?) {
                        callback.onSuccess(value)
                        println("SPANS SAVED, START PROCESSING")
                        context.getBean(SpanController::class.java).proceedSpans(spans)
                        println("SPANS PROCESSED")
                        context.getBean(SpanController::class.java).mapSpansAndSave(spans)
                    }

                    fun onError(t: Throwable?) {
                        callback.onError(t)
                    }
                }
                println("SPANS INCOMING")
                mysqlStorage.asyncSpanConsumer().accept(spans, myCallback)
            }
            val storageComponent: StorageComponent = object : StorageComponent() {
                fun spanStore(): SpanStore {
                    return mysqlStorage.spanStore()
                }

                fun asyncSpanStore(): AsyncSpanStore {
                    return mysqlStorage.asyncSpanStore()
                }

                fun asyncSpanConsumer(): AsyncSpanConsumer {
                    return consumer
                }

                fun check(): CheckResult {
                    return mysqlStorage.check()
                }

                fun close() {
                    mysqlStorage.close()
                }
            }
            context.getBean(SpanController::class.java).setZipkinStorageComponent(storageComponent)
            return storageComponent
        }
    }
}*/
