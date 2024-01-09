package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.collector

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Span
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.SpanProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OpenTelemetryCollectorSpanProvider(private val config: Config) : SpanProvider {

    data class Config(val collectorListenPort: Int, val collectionDuration: Duration) {

        internal fun toServerConfig(): OpenTelemetryCollectorServer.Config {
            return OpenTelemetryCollectorServer.Config(
                listenPort = this.collectorListenPort
            )
        }
    }

    private val log = KotlinLogging.logger {}

    private val spans = mutableListOf<Span>()

    private val collectorServer = OpenTelemetryCollectorServer(config.toServerConfig()) { suspend { spans.addAll(it) } }

    override suspend fun getSpans(): List<Span> {
        log.debug { "Retrieving spans over a time of ${config.collectionDuration} ..." }

        collectorServer.use { server ->
            server.start()
            delay(config.collectionDuration)
        }

        log.debug { "Retrieved ${spans.size} spans" }

        return spans
    }
}

fun main() {
    val config = OpenTelemetryCollectorSpanProvider.Config(collectorListenPort = 7878, collectionDuration = 60.seconds)
    val spanProvider = OpenTelemetryCollectorSpanProvider(config)
    val spans = runBlocking { spanProvider.getSpans() }
    println(spans)
}
