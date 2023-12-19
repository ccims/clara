package de.unistuttgart.iste.sqa.clara.export.graphviz.dsl

class Edge(
    private val from: String,
    private val to: String,
    private val isDirected: Boolean,
    attributes: Map<String, String> = emptyMap(),
) {

    private val attributes = attributes.toMutableMap()

    fun attr(key: String, value: String) {
        attributes[key] = value
    }

    fun toDot(): String {
        val attrs = attributes.entries.joinToString(", ") { "${it.key}=\"${it.value}\"" }
        val arrow = if (isDirected) " -> " else " -- "

        return buildString {
            appendInQuotes(from)
            append(arrow)
            appendInQuotes(to)

            if (attrs.isNotBlank()) {
                append(" [")
                append(attrs)
                append("]")
            }
        }
    }
}
