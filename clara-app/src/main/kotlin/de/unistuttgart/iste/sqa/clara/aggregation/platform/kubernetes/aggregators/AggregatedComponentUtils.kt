package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesPod
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesService
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.api.model.Domain

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

internal fun AggregatedCommunication.toCommunication(): Communication {
    return Communication(
        source = Communication.Source(Component.Name(this.source.componentName.value)),
        target = Communication.Target(Component.Name(this.target.componentName.value)),
    )
}

internal fun AggregatedComponent.External.toComponent(): Component {
    return Component.ExternalComponent(
        name = Component.Name(name.value),
        domain = Domain(domain.value),
    )
}