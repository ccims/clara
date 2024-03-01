package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesPod
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesService
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent

internal fun KubernetesPod.asAggregatedComponent(): AggregatedComponent {
    return AggregatedComponent.Internal.KubernetesComponent(
        name = AggregatedComponent.Name(this.name.value),
        ipAddress = this.ipAddress,
        namespace = this.namespace,
        pods = listOf(this)
    )
}

internal fun KubernetesService.asAggregatedComponent(): AggregatedComponent {
    return AggregatedComponent.Internal.KubernetesComponent(
        name = AggregatedComponent.Name(this.name.value),
        ipAddress = this.ipAddress,
        namespace = this.namespace,
        pods = this.selectedPods
    )
}

internal fun aggregatedServiceNameFrom(pod: KubernetesPod, knownServices: Iterable<KubernetesService>): AggregatedComponent.Name {
    return knownServices.find { service -> service.selectedPods.contains(pod) }
        ?.asAggregatedComponent()?.name
        ?: pod.asAggregatedComponent().name
}

internal fun aggregatedServiceNameFrom(service: KubernetesService): AggregatedComponent.Name {
    return service.asAggregatedComponent().name
}
