package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.kubeapi

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesService
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent

class KubernetesApiServicePodMerger {

    fun getAggregatedComponents(services: Set<KubernetesService>): Set<AggregatedComponent> {
        return services.map { service ->
            AggregatedComponent.Internal.KubernetesComponent(
                name = AggregatedComponent.Name(service.name.value),
                ipAddress = service.ipAddress,
                namespace = service.namespace
            )
        }.toSet()
    }
}
