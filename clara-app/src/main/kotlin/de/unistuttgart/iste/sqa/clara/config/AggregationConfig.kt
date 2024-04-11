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
                val kubeApi: KubeApiAggregator?,
                val dns: DnsAggregator?,
                val openTelemetry: OpenTelemetryAggregator?,
                val syftSbom: SyftSbomAggregator?,
            ) {

                data class KubeApiAggregator(
                    override val enable: Boolean = true,
                ) : Enable

                data class OpenTelemetryAggregator(
                    override val enable: Boolean = true,
                    val listenPort: Int,
                    val listenDuration: Duration,
                ) : Enable

                data class DnsAggregator(
                    override val enable: Boolean = true,
                    val logsSinceTime: String = "",
                ) : Enable

                data class SyftSbomAggregator(
                    override val enable: Boolean = true,
                ) : Enable
            }
        }
    }
}
