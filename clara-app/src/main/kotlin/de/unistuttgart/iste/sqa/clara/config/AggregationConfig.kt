package de.unistuttgart.iste.sqa.clara.config

import de.unistuttgart.iste.sqa.clara.api.model.Namespace
import kotlin.time.Duration

data class AggregationConfig(
    val platforms: Platforms?,
) {

    data class Platforms(
        val kubernetes: Kubernetes?,
    ) {

        data class Kubernetes(
            val aggregators: Aggregators,
            val namespaces: List<Namespace>,
            val includeKubeNamespaces: Boolean = false,
        ) {

            data class Aggregators(
                val pod: PodAggregator?,
                val service: ServiceAggregator?,
                val dns: DnsAggregator?,
                val openTelemetry: OpenTelemetryAggregator?,
            ) {

                data class OpenTelemetryAggregator(
                    override val enable: Boolean = true,
                    val listenPort: Int,
                    val listenDuration: Duration,
                ) : Enable

                data class PodAggregator(
                    override val enable: Boolean = true,
                ) : Enable

                data class ServiceAggregator(
                    override val enable: Boolean = true,
                ) : Enable

                data class DnsAggregator(
                    override val enable: Boolean = true,
                    val logsSinceTime: String = "",
                ) : Enable
            }
        }
    }
}
