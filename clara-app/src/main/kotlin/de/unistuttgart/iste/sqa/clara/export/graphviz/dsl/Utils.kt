package de.unistuttgart.iste.sqa.clara.export.graphviz.dsl

fun graph(name: String = "G", isDirected: Boolean = true, block: Graph.() -> Unit = {}): Graph {
    return Graph(name, isDirected).apply(block)
}

internal fun StringBuilder.appendInQuotes(value: String): StringBuilder {
    append("\"")
    append(value)
    append("\"")
    return this
}
