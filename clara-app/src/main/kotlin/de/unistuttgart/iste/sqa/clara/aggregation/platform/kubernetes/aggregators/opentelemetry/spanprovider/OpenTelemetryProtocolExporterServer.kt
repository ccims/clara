package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.spanprovider

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Span
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ServerBuilder
import java.io.Closeable

class OpenTelemetryProtocolExporterServer(private val config: Config, processSpans: suspend (List<Span>) -> Unit) : Closeable {

    data class Config(val listenPort: Int)

    private val log = KotlinLogging.logger {}

    private val server = ServerBuilder
        .forPort(config.listenPort)
        .addService(OpenTelemetryTraceService(processSpans))
        .addService(PingService())
        .build()

    fun start() {
        log.debug { "Starting ${OpenTelemetryProtocolExporterServer::class.simpleName} server ..." }
        server.start()
        log.info { "Started ${OpenTelemetryProtocolExporterServer::class.simpleName} server listening on port ${server.port}" }
    }

    override fun close() {
        log.debug { "Shutting down ${OpenTelemetryProtocolExporterServer::class.simpleName} server ..." }
        server.shutdownNow()
        log.info { "Shut down of ${OpenTelemetryProtocolExporterServer::class.simpleName} server complete" }
    }
}
