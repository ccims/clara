package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.spanprovider

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

    private val server = OpenTelemetryProtocolExporterServer(config.toServerConfig()) {
        spans.addAll(it)
        log.trace { "Received ${it.size} spans. Total spans: ${spans.size}" }
    }

    override suspend fun getSpans(): List<Span> {
        log.debug { "Retrieving spans over a time of ${config.listenDuration} ..." }

        server.use { server ->
            server.start()
            delay(config.listenDuration / 10)
            repeat(9) { iteration ->
                log.info { "Total spans: ${spans.size}. Time remaining: ${config.listenDuration * ((9 - iteration) / 10.0)}" }
                delay(config.listenDuration / 10)
            }
            log.debug { "Total spans: ${spans.size}. Finished" }
        }

        return spans
    }
}
