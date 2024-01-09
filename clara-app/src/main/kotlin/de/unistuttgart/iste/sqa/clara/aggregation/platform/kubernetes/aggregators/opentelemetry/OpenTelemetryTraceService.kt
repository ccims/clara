package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Service
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Span
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ServerBuilder
import io.opentelemetry.proto.collector.trace.v1.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OpenTelemetryTraceService(private val processSpans: (List<Span>) -> Unit): TraceServiceGrpcKt.TraceServiceCoroutineImplBase() {

    override suspend fun export(request: ExportTraceServiceRequest): ExportTraceServiceResponse {
        // TODO Check: Maybe later we should not flatten entirely -> checked, and we should not :D
        val spans = request.resourceSpansList.flatMap { resourceSpans ->
            val serviceName = resourceSpans.resource.attributesList.find { it.key == "service.name" }?.value?.stringValue
                ?: throw UnsupportedOperationException() // Todo proper exception
            
            resourceSpans.scopeSpansList.flatMap { scopeSpans ->
                scopeSpans.spansList.map { openTelemetrySpan ->
                    openTelemetrySpan.toClaraSpan(serviceName)
                }
            }
        }

        processSpans(spans)

        return ExportTraceServiceResponse.getDefaultInstance()
    }
}

private fun io.opentelemetry.proto.trace.v1.Span.toClaraSpan(serviceName: String): Span {
    return Span(
        id = Span.Id(this.spanId.toStringUtf8()),
        attributes = Span.Attributes(
            buildMap(this.attributesCount) {
                this@toClaraSpan.attributesList.forEach { attribute ->
                    put(attribute.key, attribute.value.stringValue)
                }
            }
        ),
        name = Span.Name(this.name),
        parentId = Span.ParentId(this.parentSpanId.toStringUtf8()),
        serviceName = Service.Name(serviceName),
        traceId = Span.TraceId(this.traceId.toStringUtf8()),
        kind = this.kind.toClaraSpanKind()
    )
}

private fun io.opentelemetry.proto.trace.v1.Span.SpanKind.toClaraSpanKind(): Span.Kind {
    return when (this) {
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_UNSPECIFIED -> TODO()
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_INTERNAL -> Span.Kind.Internal
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_SERVER -> Span.Kind.Server
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_CLIENT -> Span.Kind.Client
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_PRODUCER -> Span.Kind.Producer
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_CONSUMER -> Span.Kind.Consumer
        io.opentelemetry.proto.trace.v1.Span.SpanKind.UNRECOGNIZED -> TODO()
    }
}

class OpenTelemetryCollectorServer(config: Config, processSpans: (List<Span>) -> Unit) : Closeable {

    data class Config(val listenPort: Int)

    private val log = KotlinLogging.logger {}

    private val openTelemetrySpanService = OpenTelemetryTraceService(processSpans)
    private val server = ServerBuilder.forPort(config.listenPort).addService(openTelemetrySpanService).build()

    fun start() {
        log.info { "Staring " }
        server.start()
    }

    override fun close() {
        server.shutdownNow()
    }
}

class OpenTelemetrySpanProvider(private val config: Config) : SpanProvider {

    data class Config(val collectorListenPort: Int, val listeningDuration: Duration) {

        internal fun toServerConfig(): OpenTelemetryCollectorServer.Config {
            return OpenTelemetryCollectorServer.Config(
                listenPort = this.collectorListenPort
            )
        }
    }

    private val spans = mutableListOf<Span>()

    private val collectorServer = OpenTelemetryCollectorServer(config.toServerConfig()) { spans.addAll(it) }

    override fun getSpans(): List<Span> {
        collectorServer.use { server ->
            runBlocking {
                server.start()
                delay(config.listeningDuration)
            }
        }

        return spans
    }
}

fun main() {
    val config = OpenTelemetrySpanProvider.Config(collectorListenPort = 7878, listeningDuration = 60.seconds)
    val spanProvider = OpenTelemetrySpanProvider(config)
    val spans = spanProvider.getSpans()
    println(spans)
}
