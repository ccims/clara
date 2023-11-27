package de.unistuttgart.iste.sqa.clara.export.graphviz.dsl

class SubGraph(
    private val name: String,
    private val isDirected: Boolean,
    private val indentationLevel: Int,
) {

    private val nodes = mutableListOf<Node>()
    private val edges = mutableListOf<Edge>()
    private val subGraphs = mutableListOf<SubGraph>()

    var label = ""

    fun node(id: String, label: String, block: Node.() -> Unit = {}) {
        val node = Node(id, label).apply(block)
        nodes.add(node)
    }

    fun node(node: Node) = nodes.add(node)

    fun edge(from: String, to: String, block: Edge.() -> Unit = {}) {
        val edge = Edge(from, to, isDirected).apply(block)
        edges.add(edge)
    }

    fun edge(edge: Edge) = edges.add(edge)

    fun subGraph(name: String, block: SubGraph.() -> Unit) {
        val subGraph = SubGraph(name, isDirected = isDirected, indentationLevel = indentationLevel + 2).apply(block)
        subGraphs.add(subGraph)
    }

    fun subGraph(subGraph: SubGraph) = subGraphs.add(subGraph)

    fun toDot(): String {
        val indentation = " ".repeat(indentationLevel)
        val innerIndentation = " ".repeat(indentationLevel + 2)

        return buildString {
            append("subgraph ")
            appendInQuotes(name)
            appendLine(" {")

            if (label.isNotBlank()) {
                append(innerIndentation)
                append("label = ")
                appendInQuotes(label)
                appendLine()
            }

            if (nodes.isNotEmpty()) appendLine()

            nodes.forEach {
                append(innerIndentation)
                appendLine(it.toDot())
            }

            if (edges.isNotEmpty()) appendLine()

            edges.forEach {
                append(innerIndentation)
                appendLine(it.toDot())
            }

            if (subGraphs.isNotEmpty()) appendLine()

            subGraphs.forEach {
                append(innerIndentation)
                appendLine(it.toDot())
            }

            append(indentation)
            append("}")
        }
    }
}
