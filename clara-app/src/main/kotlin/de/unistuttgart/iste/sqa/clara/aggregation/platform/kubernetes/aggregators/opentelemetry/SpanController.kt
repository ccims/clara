package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry


import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.module.Span
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.module.SpanInformation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.api.GlobalOpenTelemetry
import java.math.BigInteger
import java.net.InetAddress
import java.util.*
import java.util.concurrent.Executor
import java.util.function.Function
import java.util.regex.Pattern
import javax.sql.DataSource
import io.opentelemetry.api.trace.Tracer
import java.net.URL


// TODO 95% of the code is copied from https://github.com/ga52can/microlyze and has not properly been modified yet
// Activities are entire business activities meaning the whole span of a trace
// Services are actual Microservices
// Instances are instances of microservices
// Hardware is the used hardware (don't know if necessary)

// TODO the Span Class from open telemetry is more useful for actually generating spans and exporting it and not for working with it
// TODO therefore we should add a custom Implementation that inherits from it and contains all the attributes as properties, we need
// TODO we have to read this then from some sort of data source, where we dumped the spans beforehand

class SpanController() {

    private val log = KotlinLogging.logger {}

    /*private fun initialize() {
        componentRevisionMap = revisionService.getCurrentRevisionsByComponentName()
    }*/

    // Proceeding of all ingoing spans via a stream or the ZipKin-REST API.
    // components and relations of them are discovered and persistently stored here
    @Synchronized // TODO where to get the spannamese from
    fun proceedSpans(spans: List<String>) {
        for (spanName in spans) {

            val tracer: Tracer = GlobalOpenTelemetry.getTracer("example-tracer")

            // FIXME get the spanname from somewhere
            // There probably ust be some method somewhere
            val span: Span = Span("", "", "", "", "", "", mapOf())

            //val tempSpan: Span = tempStorageSpans[span.id]

            //// 0 Update Components based on the endpoints
            // 0.1 Create Components (Service, Instance, Hardware) from endpoint information

            // Get the spanKind: either client, server, consumer or producer
            val spanKind = span.spanKind

            // TODO verify if it really fails
            // 0.2 if one of the (not binary) endpoints is not valid (== no ip address, happens from time to time, probably a sleuth bug), don't process the span
            //if (!allEndpointsAreValid) continue

            //// 1  Create Components
            // 1.1  Generate Services, Instances and Hardware components and relations between them from the binary endpoints

            //updateComponents()


            //// 2  Discover relations between services
            // 2.1  compute the first span of a transaction (usually Zuul's SR-Response to the not instrumented client)
            //      Mapping of the first span's path (name) with a service via the ComponentMapping-collection
            //      and creation of newly discovered relations between activities and services.
            if (span.parentId == null) { // Get parentId from span -> if there is none you know it's parent
                // Get path and method (should be availalbe)
                val path: String = span.name
                var method = "GET"
                for (attribute in span.attributes) {
                    if (attribute.key.lowercase() == "http.method") {
                        method = attribute.value.uppercase()
                    }
                }
            }
        }
    }

    // how does updateComponents look like?
    // If client:
    //      server.address -> Map to server component (probably primary key)
    //      client identifier? -> there might be an instance name based on the instrumentation
    //      http.url / url.full -> describes the server
    //      relation caller / callee -> if caller is possible to be determined
    // If server:
    //      look for some server identifier e.g. entity.name
    //      look for some client identifier e.g. user-agent
    private fun findBasicClientServerInfo(span: Span): SpanInformation = when (span.spanKind) {
        "CLIENT" -> {
            val serverUrl = getServerUrl(span)
            SpanInformation(clientIdentifier = span.serviceName, serverIdentifier = serverUrl.host, endpoint = serverUrl.path)
        }

        "SERVER" -> {
            val serverUrl = getServerUrl(span)
            SpanInformation(clientIdentifier = null, serverIdentifier = span.serviceName,  endpoint = serverUrl.path )
        }

        "CONSUMER" -> {
            log.warn { "Consumer span identified" }
            throw UnsupportedOperationException()
        }

        "PRODUCER" -> {
            log.warn { "Producer span identified" }
            throw UnsupportedOperationException()
        }

        else -> {
            throw UnsupportedOperationException()
        }
    }

    private fun getServerUrl(span: Span): URL {
        val paths = span.attributes.filter { // TODO check if there might be more keys that represent urls
            it.key.lowercase() == "http.uri" || it.key.lowercase() == "http.url"
        }.values
        if (paths.isEmpty()) {
            log.error { "${span.spanKind} span without http url detected, the attributes are ${span.attributes}" }
        }
        return URL(paths.first())
    }
/*

    // extracts information about components (Hardware, Service, Instance) from an endpoint-object
    // and creates new component-objects for them if they are not existing yet.
    // Additionally it creates relations between these components
    private fun updateComponents(): Boolean {
        val serviceName: String = endpoint.serviceName.toUpperCase()
        val hardwareName = getHardwareName(endpoint)
        val instanceName = getInstanceName(endpoint)
        if (instanceName == "unknown") return false
        if (!componentRevisionMap!!.containsKey(serviceName)) {
            val service: Service = serviceService.findByName(serviceName)
            if (service == null) {
                serviceService.createService(serviceName)
            } else revisionService.createRevision(service)
        }
        if (!componentRevisionMap!!.containsKey(hardwareName)) {
            val hardware: Hardware = hardwareService.findByName(hardwareName)
            if (hardware == null) hardwareService.createHardware(hardwareName) else revisionService.createRevision(hardware)
        }
        if (!componentRevisionMap!!.containsKey(instanceName)) {
            var instance: Instance = instanceService.findByName(instanceName)
            if (instance == null) {
                instance = instanceService.createInstance(instanceName)
                instance.setAnnotation("ad.port", endpoint.port.toString())
                instance.setAnnotation("ad.ip", getHardwareName(endpoint))
                instanceService.saveInstance(instance)
            } else revisionService.createRevision(instance)
            val instanceRevision: Revision? = componentRevisionMap!![instanceName]
            val serviceRevision: Revision? = componentRevisionMap!![serviceName]
            val hardwareRevision: Revision? = componentRevisionMap!![hardwareName]
            relationService.setInstanceRevisionRelations(instanceRevision, serviceRevision, hardwareRevision)
        }
        return true
    }*/
}/*
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