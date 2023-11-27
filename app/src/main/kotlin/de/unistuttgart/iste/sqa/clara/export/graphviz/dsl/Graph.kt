package de.unistuttgart.iste.sqa.clara.export.graphviz.dsl

class Graph(
    private val name: String,
    private val isDirected: Boolean = false,
    private val indentationLevel: Int = 2,
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
        val subGraph = SubGraph(name, isDirected, indentationLevel).apply(block)
        subGraphs.add(subGraph)
    }

    fun subGraph(subGraph: SubGraph) = subGraphs.add(subGraph)

    fun toDot(): String {
        val graphType = if (isDirected) "digraph " else "graph "
        val indentation = " ".repeat(indentationLevel)

        return buildString {
            append(graphType)
            appendInQuotes(name)
            appendLine(" {")

            if (label.isNotBlank()) {
                append(indentation)
                append("label = ")
                appendInQuotes(label)
                appendLine()
            }

            if (nodes.isNotEmpty()) appendLine()

            nodes.forEach {
                append(indentation)
                appendLine(it.toDot())
            }

            if (edges.isNotEmpty()) appendLine()

            edges.forEach {
                append(indentation)
                appendLine(it.toDot())
            }

            if (subGraphs.isNotEmpty()) appendLine()

            subGraphs.forEach {
                append(indentation)
                appendLine(it.toDot())
            }

            append("}")
        }
    }
}
