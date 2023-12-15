package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.components

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesClient
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure
import de.unistuttgart.iste.sqa.clara.api.aggregation.ComponentAggregator
import de.unistuttgart.iste.sqa.clara.api.model.Component.Internal.Service
import de.unistuttgart.iste.sqa.clara.api.model.Namespace
import io.github.oshai.kotlinlogging.KotlinLogging

class KubernetesServiceAggregator(
    private val config: Config,
    private val kubernetesClient: KubernetesClient,
) : ComponentAggregator {

    data class Config(
        val namespaces: List<Namespace>,
        val includeKubeNamespaces: Boolean,
    )

    private val log = KotlinLogging.logger {}

    override fun aggregate(): Either<AggregationFailure, Set<Service>> {
        log.info { "Aggregate Kubernetes services ..." }

        return kubernetesClient.use { client ->
            client
                .getServicesFromNamespaces(config.namespaces, config.includeKubeNamespaces)
                .mapLeft { AggregationFailure(it.description) }
                .map { it.toSet() }
        }
    }
}
