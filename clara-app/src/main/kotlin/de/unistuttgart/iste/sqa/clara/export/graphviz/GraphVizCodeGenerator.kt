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
        val internalComponentsPerNamespace = components
            .filterIsInstance<Component.InternalComponent>()
            .toSet()
            .groupBy { it.namespace }

        val externalComponents = components
            .filterIsInstance<Component.ExternalComponent>()
            .toSet()

        val dotCode = graph {
            internalComponentsPerNamespace.onEachIndexed { index, (namespace, components) ->
                addSubGraphFromNamespace(index, namespace ?: Namespace("<unknown namespace>"), components)
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
        from = communications.source.componentName.id(),
        to = communications.target.componentName.id()
    )
}

private fun Graph.addNodeFromComponent(component: Component) = this.node(component.toNode())

private fun SubGraph.addNodeFromComponent(component: Component) = this.node(component.toNode())

private fun Component.toNode() = Node(id = this.name.id(), label = this.label(), attributes = this.attributes())

private fun Component.Name.id(): String = this.value.lowercase().filter { it.isLetter() || it.isDigit() || it == '_' }

private fun Component.label(): String {
    return when (this) {
        is Component.ExternalComponent -> "${this.name}\\n(${this.domain})"
        is Component.InternalComponent -> "${this.name}\\nIP-address(${this.ipAddress})\\nendpoints(${this.endpoints})"
    }
}

private fun Component.attributes(): Map<String, String> {
    return when (this) {
        is Component.ExternalComponent -> mapOf("shape" to "oval")
        is Component.InternalComponent -> mapOf("shape" to "octagon")
    }
}
