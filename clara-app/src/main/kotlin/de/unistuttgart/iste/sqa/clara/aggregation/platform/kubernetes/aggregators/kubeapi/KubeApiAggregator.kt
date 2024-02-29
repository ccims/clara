package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.kubeapi

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesClient
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure
import de.unistuttgart.iste.sqa.clara.api.aggregation.ComponentAggregator
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Namespace

class KubeApiAggregator(
    private val config: Config,
    private val kubernetesClient: KubernetesClient,
) : ComponentAggregator {

    data class Config(
        val namespaces: List<Namespace>,
        val includeKubeNamespaces: Boolean,
    )

    override fun aggregate(): Either<AggregationFailure, Set<AggregatedComponent>> {

        val (getPodsResult, getServicesResult) = kubernetesClient.use { client ->
            Pair(
                client
                    .getPodsFromNamespaces(config.namespaces, config.includeKubeNamespaces)
                    .mapLeft { AggregationFailure(it.description) }
                    .map { it.toSet() },
                client
                    .getServicesFromNamespaces(config.namespaces, config.includeKubeNamespaces)
                    .mapLeft { AggregationFailure(it.description) }
                    .map { it.toSet() }
            )
        }

        val pods = getPodsResult.getOrElse { return it.left() }
        val services = getServicesResult.getOrElse { return it.left() }

        return KubernetesApiServicePodMerger().getAggregatedComponents(pods, services).right()
    }
}
