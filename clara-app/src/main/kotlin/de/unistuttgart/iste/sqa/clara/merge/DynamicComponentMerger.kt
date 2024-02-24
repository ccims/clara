package de.unistuttgart.iste.sqa.clara.merge

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.merge.ComponentMerger
import de.unistuttgart.iste.sqa.clara.api.merge.MergeFailure
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import java.lang.UnsupportedOperationException

class DynamicComponentMerger : ComponentMerger {

    // First we need to filter the returned component types (Otel, Dns, etc)
    // Next we define a strict merging hierarchy: 1. DNS 2. Otel, 3. SBOM, 4. ?)
    // We take the one with the highest hierarchy as baseline
    // then we iterate over the first two service group's components and try to merge.
    // thereby, it is important that we somehow keep the reference of the original objects in the merged ones to merge the communications later.
    // then the next component group is merged into the previous result
    // in then end we have one list of components containing all information found

    // next we need to merge communications.
    // communications take generic source, target types and thus should work with the result.
    // we just need to make sure, that the merged component can still be matched, as the communications hold a reference to their object
    override fun merge(components: List<Component>, communications: List<Communication>): Pair<Either<MergeFailure, List<Component>>, Either<MergeFailure, List<Communication>>> {

        if (components.isEmpty()) {
            return Pair(Either.Right(emptyList()), Either.Right(emptyList()))
        }

        // For now, we only have those two service types.
        val mergedComponents = compareAndMergeComponents(
            components,
            Component.Internal.KubernetesService::class.java,
            Component.Internal.OpenTelemetryService::class.java,
        )

        // TODO merge communications
        return Pair(Either.Right(mergedComponents), Either.Right(emptyList()))
    }

    private fun <B, C> compareAndMergeComponents(
        components: List<Component>,
        baseComponentType: B,
        compareComponentType: C,
    ): List<Component> {

        val baseComponents = components.filter { it::class.java == baseComponentType }
        val compareComponents = components.filter { it::class.java == compareComponentType }.toMutableList()

        if (baseComponents.isEmpty() || compareComponents.isEmpty()) {
            return components
        }

        val mergedComponents = mutableListOf<Component>()

        baseComponents.forEach { baseComponent ->
            val compareComponent = compareComponents.find {
                compareComponents(baseComponent, it)
            }
            if (compareComponent != null) {
                mergedComponents.add(mergeComponents(baseComponent, compareComponent))
                compareComponents.remove(compareComponent)
            }
        }
        return mergedComponents
    }

    // TODO more complex comparison based on attributes if names do not match
    private fun compareComponents(baseComponent: Component, compareComponent: Component): Boolean =
        baseComponent.name == compareComponent.name

    private fun mergeComponents(baseComponent: Component, compareComponent: Component): Component {
        return when {
            baseComponent::class.java == Component.Internal.KubernetesService::class.java &&
                    compareComponent::class.java == Component.Internal.OpenTelemetryService::class.java ->
                mergeKubernetesComponentWithOpenTelemetryComponent(
                    baseComponent as Component.Internal.KubernetesService,
                    compareComponent as Component.Internal.OpenTelemetryService
                )

            else -> throw UnsupportedOperationException("We can not merge those components yet.")
        }
    }

    // TODO this is very rudimentary yet
    private fun mergeKubernetesComponentWithOpenTelemetryComponent(
        baseComponent: Component.Internal.KubernetesService,
        compareComponent: Component.Internal.OpenTelemetryService,
    ): Component.Internal.MergedService {
        return Component.Internal.MergedService(
            name = baseComponent.name,
            namespace = baseComponent.namespace,
            ipAddress = baseComponent.ipAddress,
            domain = compareComponent.domain,
            endpoints = compareComponent.endpoints,
        )
    }
}