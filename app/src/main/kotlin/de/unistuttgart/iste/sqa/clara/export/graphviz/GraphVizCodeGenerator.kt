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

fun Graph.addSubGraphFromNamespace(index: Int, namespace: Namespace, components: List<Component>) {
    this.subGraph("cluster_$index") {
        label = namespace.value

        components.forEach { component ->
            addNodeFromComponent(component)
        }
    }
}

fun Graph.addEdgeFromCommunication(communications: Communication) {
    this.edge(
        from = communications.source.component.id(),
        to = communications.target.component.id()
    )
}

fun Graph.addNodeFromComponent(component: Component) = this.node(component.toNode())

fun SubGraph.addNodeFromComponent(component: Component) = this.node(component.toNode())

fun Component.toNode() = Node(id = this.id(), label = this.label(), attributes = this.attributes())

@OptIn(ExperimentalStdlibApi::class)
fun Component.id(): String {
    return when (this) {
        is Component.External -> "ext_${this.hashCode().toHexString()}"
        is Component.Internal.Pod -> "pod_${this.hashCode().toHexString()}"
        is Component.Internal.Service -> "svc_${this.hashCode().toHexString()}"
    }
}

fun Component.label(): String {
    return when (this) {
        is Component.External -> this.domain.value
        is Component.Internal.Pod -> "${this.name}\\n(${this.ipAddress})"
        is Component.Internal.Service -> "${this.name}\\n(${this.ipAddress})"
    }
}

fun Component.attributes(): Map<String, String> {
    return when (this) {
        is Component.External -> mapOf("shape" to "oval")
        is Component.Internal.Pod -> mapOf("shape" to "rectangle")
        is Component.Internal.Service -> mapOf("shape" to "octagon")
    }
}
