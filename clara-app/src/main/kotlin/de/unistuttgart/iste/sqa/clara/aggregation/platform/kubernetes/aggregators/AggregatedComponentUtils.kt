package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesPod
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesService
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Component

internal fun KubernetesPod.asAggregatedComponent(): AggregatedComponent {
    return AggregatedComponent.Internal.KubernetesComponent(
        name = AggregatedComponent.Name(this.name.value),
        type = null,
        version = this.version?.value?.let { AggregatedComponent.Internal.Version(it) },
        ipAddress = this.ipAddress,
        namespace = this.namespace,
        pods = listOf(this),
    )
}

internal fun KubernetesService.asAggregatedComponent(): AggregatedComponent {
    return AggregatedComponent.Internal.KubernetesComponent(
        name = AggregatedComponent.Name(this.name.value),
        type = null,
        version = this.selectedPods.firstOrNull()?.version?.value?.let { AggregatedComponent.Internal.Version(it) },
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

internal fun AggregatedCommunication.MessagingSystem.toSource(): AggregatedCommunication.Source = AggregatedCommunication.Source(componentName)
internal fun AggregatedCommunication.MessagingSystem.toTarget(): AggregatedCommunication.Target = AggregatedCommunication.Target(componentName)

internal fun AggregatedComponent.toComponent(): Component {
    return when (this) {
        is AggregatedComponent.External -> Component.ExternalComponent(
            name = Component.Name(this.name.value),
            domain = this.domain,
            type = this.type,
        )

        is AggregatedComponent.Internal.KubernetesComponent -> Component.InternalComponent(
            name = Component.Name(this.name.value),
            type = this.type,
            version = this.version?.let { Component.InternalComponent.Version(it.value) },
            namespace = this.namespace,
            ipAddress = this.ipAddress,
            endpoints = null,
            libraries = null,
        )

        is AggregatedComponent.Internal.OpenTelemetryComponent -> Component.InternalComponent(
            name = Component.Name(this.name.value),
            type = this.type,
            version = this.version?.let { Component.InternalComponent.Version(it.value) },
            namespace = null,
            ipAddress = null,
            endpoints = Component.InternalComponent.Endpoints(this.domain, this.paths),
            libraries = null,
        )

        is  AggregatedComponent.Internal.SpdxComponent -> Component.InternalComponent(
            name = Component.Name(this.name.value),
            type = this.type,
            version = this.version?.let { Component.InternalComponent.Version(it.value) },
            namespace = null,
            ipAddress = null,
            endpoints = null,
            libraries = this.libraries,
        )
    }
}
