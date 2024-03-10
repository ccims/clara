package de.unistuttgart.iste.sqa.clara.merge

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.toComponent
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregation
import de.unistuttgart.iste.sqa.clara.api.merge.Merge
import de.unistuttgart.iste.sqa.clara.api.merge.Merger
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

class DefaultMerger(private val config: Config) : Merger {

    data class Config(
        val comparisonStrategy: ComparisonStrategy,
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
            AggregatedComponent.Internal.KubernetesComponent::class.java,
            AggregatedComponent.Internal.OpenTelemetryComponent::class.java,
        )

        val renamedCommunications = communications.map { component ->
            Communication(
                source = Communication.Source(renamedComponents[component.source.componentName] ?: Component.Name(component.source.componentName.value)),
                target = Communication.Target(renamedComponents[component.target.componentName] ?: Component.Name(component.target.componentName.value)),
            )
        }

        // TODO filter communications where target/source does not exist anymore / or find a better solution then filtering
        return Merge(failures = initialFailures, components = mergedComponents, communications = renamedCommunications)
    }

    private fun <B, C> compareAndMergeComponents(
        aggregatedComponents: List<AggregatedComponent>,
        renamedComponents: MutableMap<AggregatedComponent.Name, Component.Name>,
        baseComponentType: B,
        compareComponentType: C,
    ): List<Component> {

        val baseComponents = aggregatedComponents.filter { it::class.java == baseComponentType }
        val compareComponents = aggregatedComponents.filter { it::class.java == compareComponentType }.toMutableList()
        val externalComponents = aggregatedComponents.filter { it::class.java == AggregatedComponent.External::class.java }.toMutableList()

        if (baseComponents.isEmpty() || compareComponents.isEmpty()) {
            return aggregatedComponents.map { it.toComponent() }
        }

        val mergedComponents = mutableListOf<Component>()

        baseComponents.forEach { baseComponent ->

            val compareComponent = compareComponents.find { compareComponents(baseComponent, it) }
            // It is possible that oTel marks components as external when they do not provide traces. If there is a matching k8s component, they will be merged here.
            val externalCompareComponent = externalComponents.find { compareComponents(baseComponent, it) }
            when {
                compareComponent != null -> {
                    val mergedComponent = mergeComponents(baseComponent, compareComponent)
                    mergedComponents.add(mergedComponent)
                    compareComponents.remove(compareComponent)
                    renamedComponents.checkAndAddRenamedComponent(compareComponent, mergedComponent)
                }

                externalCompareComponent != null -> {
                    val mergedComponent = mergeComponents(baseComponent, externalCompareComponent)
                    mergedComponents.add(mergedComponent)
                    externalComponents.remove(externalCompareComponent)
                    renamedComponents.checkAndAddRenamedComponent(externalCompareComponent, mergedComponent)
                }

                else -> {
                    mergedComponents.add(baseComponent.toComponent())
                }
            }
        }
        return mergedComponents + externalComponents.map { it.toComponent() } + compareComponents.map { it.toComponent() }
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

    private fun mergeComponents(baseComponent: AggregatedComponent, compareComponent: AggregatedComponent): Component {
        return when {
            baseComponent::class.java == AggregatedComponent.Internal.KubernetesComponent::class.java &&
                    compareComponent::class.java == AggregatedComponent.Internal.OpenTelemetryComponent::class.java ->
                mergeKubernetesComponentWithOpenTelemetryComponent(
                    baseComponent as AggregatedComponent.Internal.KubernetesComponent,
                    compareComponent as AggregatedComponent.Internal.OpenTelemetryComponent
                )

            baseComponent::class.java == AggregatedComponent.Internal.KubernetesComponent::class.java &&
                    compareComponent::class.java == AggregatedComponent.External::class.java ->
                mergeKubernetesComponentWithExternalComponent(
                    baseComponent as AggregatedComponent.Internal.KubernetesComponent,
                    compareComponent as AggregatedComponent.External
                )

            else -> throw UnsupportedOperationException("We can not merge those components yet (${baseComponent::class.java.name} and ${compareComponent::class.java.name}).")
        }
    }

    private fun mergeKubernetesComponentWithOpenTelemetryComponent(
        baseComponent: AggregatedComponent.Internal.KubernetesComponent,
        compareComponent: AggregatedComponent.Internal.OpenTelemetryComponent,
    ): Component.InternalComponent {
        return Component.InternalComponent(
            name = Component.Name(baseComponent.name.value),
            namespace = baseComponent.namespace,
            ipAddress = baseComponent.ipAddress,
            endpoints = Component.InternalComponent.Endpoints(compareComponent.domain, compareComponent.paths),
        )
    }

    private fun mergeKubernetesComponentWithExternalComponent(
        baseComponent: AggregatedComponent.Internal.KubernetesComponent,
        compareComponent: AggregatedComponent.External,
    ): Component.InternalComponent {
        return Component.InternalComponent(
            name = Component.Name(baseComponent.name.value),
            namespace = baseComponent.namespace,
            ipAddress = baseComponent.ipAddress,
            endpoints = Component.InternalComponent.Endpoints(compareComponent.domain, emptyList()),
        )
    }
}

