package de.unistuttgart.iste.sqa.clara.merge

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.toComponent
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.toSource
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.toTarget
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregation
import de.unistuttgart.iste.sqa.clara.api.merge.Merge
import de.unistuttgart.iste.sqa.clara.api.merge.Merger
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

class DefaultMerger(private val config: Config) : Merger {

    data class Config(
        val comparisonStrategy: ComparisonStrategy,
        val showMessagingCommunicationsDirectly: Boolean,
    ) {

        enum class ComparisonStrategy {
            Prefix,
            Suffix,
            Contains,
            Equals,
        }
    }

    // First we need to filter the returned component types (oTel, Dns, etc.)
    // Next we define a strict merging hierarchy: 1. DNS 2. oTel, 3. SBOM, 4. ?)
    // We take the one with the highest hierarchy as baseline
    // then we iterate over the first two service group's components and try to merge.
    // thereby, it is important that we somehow keep the reference of the original objects in the merged ones to merge the communications later.
    // then the next component group is merged into the previous result
    // in then end we have one list of components containing all information found

    // next we need to merge communications.
    // communications take generic source, target types and thus should work with the result.
    // we just need to make sure, that the merged component can still be matched, as the communications hold a reference to their object
    override fun merge(aggregations: Set<Aggregation>): Merge {

        val components = aggregations.flatMap { it.components }
        val communications = aggregations.flatMap { it.communications }

        val initialFailures = buildList {
            if (components.isEmpty()) {
                add(DefaultMergeFailure("No components to merge!"))
            }
            if (communications.isEmpty()) {
                add(DefaultMergeFailure("No communications to merge!"))
            }
        }

        // we want to continue if there are no communications but components
        if (components.isEmpty()) {
            return Merge(failures = initialFailures, components = emptyList(), communications = emptyList())
        }

        val renamedComponents = mutableMapOf<AggregatedComponent.Name, Component.Name>()

        // For now, we only have those two service types.
        val mergedComponents = compareAndMergeComponents(
            components,
            renamedComponents,
        )

        val adjustedCommunications = adjustMessagingCommunications(communications)

        val renamedCommunications = adjustedCommunications.map { component ->
            Communication(
                source = Communication.Source(renamedComponents[component.source.componentName] ?: Component.Name(component.source.componentName.value)),
                target = Communication.Target(renamedComponents[component.target.componentName] ?: Component.Name(component.target.componentName.value)),
            )
        }

        // TODO filter communications where target/source does not exist anymore / or find a better solution then filtering
        return Merge(failures = initialFailures, components = mergedComponents, communications = renamedCommunications)
    }

    /**
     * Messaging systems can be displayed in two ways in clara.
     *
     * Either, they are the center of communications, which means that communications
     * going through the messaging system will be displayed as such. Then the discovered communications from OpenTelemetry have to be transformed
     * into two communications, one from source towards the messaging system and one from the messaging system to the target.
     *
     * The other option is that communications are displayed directly between source and target, even if they go through the messaging system.
     * Then we need to filter out any communications discovered by the DnsAggregator that go towards the messaging system and rely on the discovery
     * of messaging communications via OpenTelemetry.
     *
     * @param communications the communications to transform.
     */
    private fun adjustMessagingCommunications(communications: List<AggregatedCommunication>): List<AggregatedCommunication> {
        val (messagingCommunications, otherCommunications) = communications.partition { it.messagingSystem != null }
        return if (messagingCommunications.isEmpty()) {
            otherCommunications
        } else {
            if (config.showMessagingCommunicationsDirectly) {
                val messagingSystems = messagingCommunications.map { it.messagingSystem!! }.distinct()
                val filteredCommunications = otherCommunications.filter { communication ->
                    messagingSystems.none { it.componentName == communication.source.componentName || it.componentName == communication.target.componentName }
                }
                filteredCommunications + messagingCommunications
            } else {
                val newCommunications = mutableListOf<AggregatedCommunication>()
                messagingCommunications.forEach {
                    val communicationToMessaging = AggregatedCommunication(source = it.source, target = it.messagingSystem!!.toTarget())
                    val communicationFromMessaging = AggregatedCommunication(source = it.messagingSystem.toSource(), target = it.target)
                    newCommunications.addAll(listOf(communicationFromMessaging, communicationToMessaging))
                }
                otherCommunications + newCommunications
            }
        }
    }

    private fun compareAndMergeComponents(
        aggregatedComponents: List<AggregatedComponent>,
        renamedComponents: MutableMap<AggregatedComponent.Name, Component.Name>,
    ): List<Component> {

        val kubernetesComponents = aggregatedComponents.filterIsInstance<AggregatedComponent.Internal.KubernetesComponent>()
        val internalKubernetesComponents = aggregatedComponents.filterIsInstance<AggregatedComponent.Internal.OpenTelemetryComponent>().toMutableList()
        val externalKubernetesComponents = aggregatedComponents.filterIsInstance<AggregatedComponent.External>().toMutableList()
        val spdxComponents = aggregatedComponents.filterIsInstance<AggregatedComponent.Internal.SpdxComponent>().toMutableList()

        if (kubernetesComponents.isEmpty() && internalKubernetesComponents.isEmpty() && spdxComponents.isEmpty()) {
            return aggregatedComponents.map { it.toComponent() }
        }

        val mergedComponents = mutableListOf<Component>()

        kubernetesComponents.forEach { kubernetesComponent ->

            val internalOpenTelemetryComponent = internalKubernetesComponents.find { compareComponents(kubernetesComponent, it) }
            // It is possible that oTel marks components as external when they do not provide traces. If there is a matching k8s component, they will be merged here.
            val externalOpenTelemetryComponent = externalKubernetesComponents.find { compareComponents(kubernetesComponent, it) }
            val spdxComponent = spdxComponents.find { compareComponents(kubernetesComponent, it) }
            when {
                internalOpenTelemetryComponent != null -> {
                    val mergedComponent = mergeKubernetesOpenTelemetrySpdxComponents(kubernetesComponent, internalOpenTelemetryComponent, spdxComponent)
                    mergedComponents.add(mergedComponent)
                    internalKubernetesComponents.remove(internalOpenTelemetryComponent)
                    spdxComponents.remove(spdxComponent)
                    renamedComponents.checkAndAddRenamedComponent(internalOpenTelemetryComponent, mergedComponent)
                }

                externalOpenTelemetryComponent != null -> {
                    val mergedComponent = mergeKubernetesExternalSpdxComponent(kubernetesComponent, externalOpenTelemetryComponent, spdxComponent)
                    mergedComponents.add(mergedComponent)
                    externalKubernetesComponents.remove(externalOpenTelemetryComponent)
                    spdxComponents.remove(spdxComponent)
                    renamedComponents.checkAndAddRenamedComponent(externalOpenTelemetryComponent, mergedComponent)
                }

                spdxComponent != null -> {
                    val mergedComponent = mergeKubernetesSpdxComponent(kubernetesComponent, spdxComponent)
                    mergedComponents.add(mergedComponent)
                    spdxComponents.remove(spdxComponent)
                }

                else -> {
                    mergedComponents.add(kubernetesComponent.toComponent())
                }
            }
        }
        return mergedComponents + externalKubernetesComponents.map { it.toComponent() } + internalKubernetesComponents.map { it.toComponent() }
    }

    private fun MutableMap<AggregatedComponent.Name, Component.Name>.checkAndAddRenamedComponent(
        compareComponent: AggregatedComponent,
        mergedComponent: Component,
    ) {
        if (compareComponent.name.value != mergedComponent.name.value) {
            this.set(key = compareComponent.name, value = mergedComponent.name)
        }
    }

    // TODO more complex comparison based on attributes if names do not match
    private fun compareComponents(baseComponent: AggregatedComponent, compareComponent: AggregatedComponent): Boolean = when (config.comparisonStrategy) {
        DefaultMerger.Config.ComparisonStrategy.Prefix -> baseComponent.name.value.startsWith(compareComponent.name.value) || compareComponent.name.value.startsWith(baseComponent.name.value)
        DefaultMerger.Config.ComparisonStrategy.Suffix -> baseComponent.name.value.endsWith(compareComponent.name.value) || compareComponent.name.value.endsWith(baseComponent.name.value)
        DefaultMerger.Config.ComparisonStrategy.Contains -> baseComponent.name.value.contains(compareComponent.name.value) || compareComponent.name.value.contains(baseComponent.name.value)
        DefaultMerger.Config.ComparisonStrategy.Equals -> baseComponent.name == compareComponent.name
    }

    private fun mergeKubernetesOpenTelemetrySpdxComponents(
        kubernetesComponent: AggregatedComponent.Internal.KubernetesComponent,
        openTelemetryComponent: AggregatedComponent.Internal.OpenTelemetryComponent,
        spdxComponent: AggregatedComponent.Internal.SpdxComponent?,
    ): Component.InternalComponent {
        return Component.InternalComponent(
            name = Component.Name(kubernetesComponent.name.value),
            namespace = kubernetesComponent.namespace,
            ipAddress = kubernetesComponent.ipAddress,
            endpoints = Component.InternalComponent.Endpoints(openTelemetryComponent.domain, openTelemetryComponent.paths),
            type = mergeProperty(kubernetesComponent.type, openTelemetryComponent.type),
            version = kubernetesComponent.version?.value?.let { Component.InternalComponent.Version(it) },
            libraries = spdxComponent?.libraries,
        )
    }

    private fun mergeKubernetesExternalSpdxComponent(
        kubernetesComponent: AggregatedComponent.Internal.KubernetesComponent,
        externalComponent: AggregatedComponent.External,
        spdxComponent: AggregatedComponent.Internal.SpdxComponent?,
    ): Component.InternalComponent {
        return Component.InternalComponent(
            name = Component.Name(kubernetesComponent.name.value),
            namespace = kubernetesComponent.namespace,
            ipAddress = kubernetesComponent.ipAddress,
            endpoints = Component.InternalComponent.Endpoints(externalComponent.domain, emptyList()),
            type = mergeProperty(kubernetesComponent.type, externalComponent.type),
            version = kubernetesComponent.version?.value?.let { Component.InternalComponent.Version(it) },
            libraries = spdxComponent?.libraries,
        )
    }

    private fun mergeKubernetesSpdxComponent(
        kubernetesComponent: AggregatedComponent.Internal.KubernetesComponent,
        spdxComponent: AggregatedComponent.Internal.SpdxComponent,
        ): Component.InternalComponent {
            return Component.InternalComponent(
                name = Component.Name(kubernetesComponent.name.value),
                namespace = kubernetesComponent.namespace,
                ipAddress = kubernetesComponent.ipAddress,
                endpoints = null,
                type = null,
                version = kubernetesComponent.version?.value?.let { Component.InternalComponent.Version(it) },
                libraries = spdxComponent.libraries
            )
    }

    private fun <Property> mergeProperty(value1: Property?, value2: Property?): Property? {
        return when {
            value1 != null && value2 != null -> value2
            value1 != null -> value1
            value2 != null -> value2
            else -> null
        }
    }
}

