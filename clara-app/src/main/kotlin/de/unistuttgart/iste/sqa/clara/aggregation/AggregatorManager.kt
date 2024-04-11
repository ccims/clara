package de.unistuttgart.iste.sqa.clara.aggregation

import com.fasterxml.jackson.databind.ObjectMapper
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns.KubernetesDnsAggregator
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.kubeapi.KubeApiAggregator
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.OpenTelemetryAggregator
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.spanprovider.OpenTelemetryTraceSpanProvider
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesClientFabric8
import de.unistuttgart.iste.sqa.clara.aggregation.sbom.SyftSbomAggregator
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregator
import de.unistuttgart.iste.sqa.clara.config.AggregationConfig
import de.unistuttgart.iste.sqa.clara.config.ifEnabled
import io.github.oshai.kotlinlogging.KotlinLogging

class AggregatorManager(aggregationConfig: AggregationConfig) {

    private val log = KotlinLogging.logger {}

    val aggregators: List<Aggregator> = buildList {
        aggregationConfig.platforms?.kubernetes?.let { kubernetesConfig ->

            kubernetesConfig.aggregators.kubeApi?.ifEnabled {
                val config = KubeApiAggregator.Config(kubernetesConfig.namespaces, kubernetesConfig.includeKubeNamespaces)
                add(KubeApiAggregator(config, KubernetesClientFabric8()))
                log.info { "Registered aggregator: Kubernetes API" }
            }

            kubernetesConfig.aggregators.dns?.ifEnabled { dnsAggregatorConfig ->
                val config = KubernetesDnsAggregator.Config(kubernetesConfig.namespaces, kubernetesConfig.includeKubeNamespaces, dnsAggregatorConfig.logsSinceTime)
                add(KubernetesDnsAggregator(config, KubernetesClientFabric8()))
                log.info { "Registered aggregator: Kubernetes DNS" }
            }

            kubernetesConfig.aggregators.openTelemetry?.ifEnabled { openTelemetryAggregatorConfig ->
                val config = OpenTelemetryTraceSpanProvider.Config(openTelemetryAggregatorConfig.listenPort, openTelemetryAggregatorConfig.listenDuration)
                add(OpenTelemetryAggregator(OpenTelemetryTraceSpanProvider(config)))
                log.info { "Registered aggregator: OpenTelemetry tracing spans" }
            }

            kubernetesConfig.aggregators.syftSbom?.ifEnabled {
                add(SyftSbomAggregator(ObjectMapper(), KubernetesClientFabric8()))
                log.info { "Registered aggregator: Syft SBOM"}
            }
        }
    }
}
