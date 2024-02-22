package de.unistuttgart.iste.sqa.clara.export.graphviz

import de.unistuttgart.iste.sqa.clara.api.model.*
import de.unistuttgart.iste.sqa.clara.export.graphviz.dsl.Graph
import de.unistuttgart.iste.sqa.clara.export.graphviz.dsl.Node
import de.unistuttgart.iste.sqa.clara.export.graphviz.dsl.SubGraph
import de.unistuttgart.iste.sqa.clara.export.graphviz.dsl.graph

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
}
