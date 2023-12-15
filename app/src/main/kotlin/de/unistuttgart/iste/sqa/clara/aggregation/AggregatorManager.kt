package de.unistuttgart.iste.sqa.clara.aggregation

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.components.KubernetesPodAggregator
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.components.KubernetesServiceAggregator
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns.KubernetesDnsAggregator
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesClientFabric8
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure
import de.unistuttgart.iste.sqa.clara.api.aggregation.CommunicationAggregator
import de.unistuttgart.iste.sqa.clara.api.aggregation.ComponentAggregator
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.config.AggregationConfig
import de.unistuttgart.iste.sqa.clara.config.ifEnabled
import de.unistuttgart.iste.sqa.clara.utils.kotlinx.awaitBoth
import de.unistuttgart.iste.sqa.clara.utils.list.flattenRight
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

class AggregatorManager(aggregationConfig: AggregationConfig) {

    private val log = KotlinLogging.logger {}

    private val componentAggregators: List<ComponentAggregator> = buildList {
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

    private val communicationAggregators: List<CommunicationAggregator> = buildList {
        aggregationConfig.platforms?.kubernetes?.let { kubernetesConfig ->
            kubernetesConfig.aggregators.dns?.ifEnabled {
                val config = KubernetesDnsAggregator.Config(kubernetesConfig.namespaces, kubernetesConfig.includeKubeNamespaces)
                add(KubernetesDnsAggregator(config, KubernetesClientFabric8()))
                log.info { "Registered aggregator: Kubernetes DNS" }
            }
        }
    }

    fun aggregateUsingAllAggregators(): Pair<List<Either<AggregationFailure, Component>>, List<Either<AggregationFailure, Communication>>> {
        if (componentAggregators.isEmpty() && communicationAggregators.isEmpty()) {
            log.warn { "No aggregators specified and enabled!" }
            return Pair(emptyList(), emptyList())
        }

        log.info { "Start aggregation process ..." }

        val aggregationResult = runBlocking {
            val components = async(Dispatchers.IO) { aggregateAllComponents() }
            val communications = async(Dispatchers.IO) { aggregateAllCommunications() }

            Pair(components, communications).awaitBoth()
        }

        log.info { "Finished aggregation process" }

        return aggregationResult
    }

    private suspend fun aggregateAllComponents(): List<Either<AggregationFailure, Component>> {
        return withContext(coroutineContext) {
            componentAggregators
                .map {
                    async(Dispatchers.IO) {
                        it.aggregate()
                    }
                }
                .awaitAll()
                .flattenRight()
        }
    }

    private suspend fun aggregateAllCommunications(): List<Either<AggregationFailure, Communication>> {
        return withContext(coroutineContext) {
            communicationAggregators
                .map {
                    async(Dispatchers.IO) {
                        it.aggregate()
                    }
                }
                .awaitAll()
                .flattenRight()
        }
    }
}
