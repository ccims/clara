package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.collector

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Span
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ServerBuilder
import java.io.Closeable

class OpenTelemetryCollectorServer(config: Config, processSpans: suspend (List<Span>) -> Unit) : Closeable {

    data class Config(val listenPort: Int)

    private val log = KotlinLogging.logger {}

    private val openTelemetrySpanService = OpenTelemetryTraceService(processSpans)
    private val server = ServerBuilder.forPort(config.listenPort).addService(openTelemetrySpanService).build()

    fun start() {
        log.debug { "Starting OpenTelemetryCollector server ..." }
        server.start()
        log.info { "Started OpenTelemetryCollector server listening on port ${server.port}" }
    }

    override fun close() {
        log.debug { "Shutting down OpenTelemetryCollector server ..." }
        server.shutdownNow()
        log.info { "Shut down of OpenTelemetryCollector server complete" }
    }
}
