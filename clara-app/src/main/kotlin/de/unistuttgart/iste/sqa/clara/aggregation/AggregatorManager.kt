package de.unistuttgart.iste.sqa.clara.aggregation

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns.KubernetesDnsAggregator
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.OpenTelemetryAggregator
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.spanprovider.OpenTelemetryTraceSpanProvider
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.pod.KubernetesPodAggregator
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.service.KubernetesServiceAggregator
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesClientFabric8
import de.unistuttgart.iste.sqa.clara.api.aggregation.CommunicationAggregator
import de.unistuttgart.iste.sqa.clara.api.aggregation.ComponentAggregator
import de.unistuttgart.iste.sqa.clara.config.AggregationConfig
import de.unistuttgart.iste.sqa.clara.config.ifEnabled
import io.github.oshai.kotlinlogging.KotlinLogging

class AggregatorManager(aggregationConfig: AggregationConfig) {

    private val log = KotlinLogging.logger {}

    val componentAggregators: List<ComponentAggregator> = buildList {
        aggregationConfig.platforms?.kubernetes?.let { kubernetesConfig ->

            kubernetesConfig.aggregators.pod?.ifEnabled {
                val config = KubernetesPodAggregator.Config(kubernetesConfig.namespaces, kubernetesConfig.includeKubeNamespaces)
                add(KubernetesPodAggregator(config, KubernetesClientFabric8()))
                log.info { "Registered aggregator: Kubernetes pod" }
            }

            kubernetesConfig.aggregators.service?.ifEnabled {
                val config = KubernetesServiceAggregator.Config(kubernetesConfig.namespaces, kubernetesConfig.includeKubeNamespaces)
                add(KubernetesServiceAggregator(config, KubernetesClientFabric8()))
                log.info { "Registered aggregator: Kubernetes service" }
            }
        }
    }

    val communicationAggregators: List<CommunicationAggregator> = buildList {
        aggregationConfig.platforms?.kubernetes?.let { kubernetesConfig ->

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
        }
    }
}
