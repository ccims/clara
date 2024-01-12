package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.collector

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Span
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.SpanProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlin.time.Duration

class OpenTelemetryTraceSpanProvider(private val config: Config) : SpanProvider {

    data class Config(val listenPort: Int, val listenDuration: Duration) {

        internal fun toServerConfig(): OpenTelemetryProtocolExporterServer.Config {
            return OpenTelemetryProtocolExporterServer.Config(
                listenPort = this.listenPort
            )
        }
    }

    private val log = KotlinLogging.logger {}

    private val spans = mutableListOf<Span>()

    private val server = OpenTelemetryProtocolExporterServer(config.toServerConfig()) { spans.addAll(it) }

    override suspend fun getSpans(): List<Span> {
        log.debug { "Retrieving spans over a time of ${config.listenDuration} ..." }

        server.use { server ->
            server.start()
            delay(config.listenDuration)
            server.close()
        }

        log.debug { "Retrieved ${spans.size} spans" }

        return spans
    }
}
