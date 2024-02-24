package de.unistuttgart.iste.sqa.clara.export.graphviz

import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.api.model.Namespace
import de.unistuttgart.iste.sqa.clara.export.graphviz.dsl.Graph
import de.unistuttgart.iste.sqa.clara.export.graphviz.dsl.Node
import de.unistuttgart.iste.sqa.clara.export.graphviz.dsl.SubGraph
import de.unistuttgart.iste.sqa.clara.export.graphviz.dsl.graph

object GraphVizCodeGenerator {

    fun generateDotCode(components: Iterable<Component>, communications: Iterable<Communication>): String {
        val internalComponentsPerNamespace = buildSet {
            addAll(components.filterIsInstance<Component.Internal>())
            addAll(communications.map { it.source.component }.filterIsInstance<Component.Internal>())
            addAll(communications.map { it.target.component }.filterIsInstance<Component.Internal>())
        }.groupBy { it.namespace }

        val externalComponents = buildSet {
            addAll(components.filterIsInstance<Component.External>())
            addAll(communications.map { it.source.component }.filterIsInstance<Component.External>())
            addAll(communications.map { it.target.component }.filterIsInstance<Component.External>())
        }

        val dotCode = graph {
            internalComponentsPerNamespace.onEachIndexed { index, (namespace, components) ->
                addSubGraphFromNamespace(index, namespace, components)
            }

            externalComponents.forEach { externalComponent ->
                addNodeFromComponent(externalComponent)
            }

            communications.forEach { communication ->
                addEdgeFromCommunication(communication)
            }
        }.toDot()

        return dotCode
    }
}

private fun Graph.addSubGraphFromNamespace(index: Int, namespace: Namespace, components: List<Component>) {
    this.subGraph("cluster_$index") {
        label = namespace.value

        components.forEach { component ->
            addNodeFromComponent(component)
        }
    }
}

private fun Graph.addEdgeFromCommunication(communications: Communication) {
    this.edge(
        from = communications.source.component.id(),
        to = communications.target.component.id()
    )
}

private fun Graph.addNodeFromComponent(component: Component) = this.node(component.toNode())

private fun SubGraph.addNodeFromComponent(component: Component) = this.node(component.toNode())

private fun Component.toNode() = Node(id = this.id(), label = this.label(), attributes = this.attributes())

@OptIn(ExperimentalStdlibApi::class)
private fun Component.id(): String {
    return when (this) {
        is Component.External -> "ext_${this.hashCode().toHexString()}"
        is Component.Internal.Pod -> "pod_${this.hashCode().toHexString()}"
        is Component.Internal.KubernetesService -> "svc_${this.hashCode().toHexString()}"
        is Component.Internal.OpenTelemetryService -> TODO()
    }
}

private fun Component.label(): String {
    return when (this) {
        is Component.External -> this.domain.value
        is Component.Internal.Pod -> "${this.name}\\n(${this.ipAddress})"
        is Component.Internal.KubernetesService -> "${this.name}\\n(${this.ipAddress})"
        is Component.Internal.OpenTelemetryService -> TODO()
    }
}

private fun Component.attributes(): Map<String, String> {
    return when (this) {
        is Component.External -> mapOf("shape" to "oval")
        is Component.Internal.Pod -> mapOf("shape" to "rectangle")
        is Component.Internal.KubernetesService -> mapOf("shape" to "octagon")
        is Component.Internal.OpenTelemetryService -> TODO()
    }
}

/*
object GraphVizCodeGenerator {

    fun generateDotCode(components: Iterable<Component<*>>, communications: Iterable<Communication>): String {
        val internalComponentsPerNamespace = buildSet {
            addAll(components.filterIsInstance<InternalReference>())
            addAll(communications.map { it.source.ref }.filterIsInstance<InternalReference>())
            addAll(communications.map { it.target.ref }.filterIsInstance<InternalReference>())
        }.groupBy { it.namespace }

        val externalComponents = buildSet {
            addAll(components.filterIsInstance<ExternalReference>())
            addAll(communications.map { it.source.ref }.filterIsInstance<ExternalReference>())
            addAll(communications.map { it.target.ref }.filterIsInstance<ExternalReference>())
        }

        val dotCode = graph {
            internalComponentsPerNamespace.onEachIndexed { index, (namespace, components) ->
                addSubGraphFromNamespace(index, namespace, components)
            }

            externalComponents.forEach { externalComponent ->
                addNodeFromComponent(externalComponent)
            }

            communications.forEach { communication ->
                addEdgeFromCommunication(communication)
            }
        }.toDot()

        return dotCode
    }
}

private fun Graph.addSubGraphFromNamespace(index: Int, namespace: Namespace, components: List<Component<*>>) {
    this.subGraph("cluster_$index") {
        label = namespace.value

        components.forEach { component ->
            addNodeFromComponent(component)
        }
    }
}

private fun Graph.addEdgeFromCommunication(communications: Communication) {
    this.edge(
        from = communications.source.ref.id(),
        to = communications.target.ref.id()
    )
}

private fun Graph.addNodeFromComponent(component: Component<*>) = this.node(component.toNode())

private fun SubGraph.addNodeFromComponent(component: Component<*>) = this.node(component.toNode())

private fun Component<*>.toNode() = Node(id = this.ref.id(), label = this.label(), attributes = this.attributes())

@OptIn(ExperimentalStdlibApi::class)
private fun Reference.id(): String {
    return when (this) {
        is ExternalReference -> "ext_${this.hashCode().toHexString()}"
        is InternalReference -> "pod_${this.hashCode().toHexString()}"
    }
}

private fun Component<*>.label(): String {
    return when (this) {
        is Component.External -> this.ref.domain.value
        is Component.Internal.Pod -> "${this.ref.name}\\n(${this.ref.ipAddress})"
        is Component.Internal.Service -> "${this.ref.name}\\n(${this.ref.ipAddress})"
    }
}

private fun Component<*>.attributes(): Map<String, String> {
    return when (this) {
        is Component.External -> mapOf("shape" to "oval")
        is Component.Internal.Pod -> mapOf("shape" to "rectangle")
        is Component.Internal.Service -> mapOf("shape" to "octagon")
    }
}*/