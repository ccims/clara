package de.unistuttgart.iste.sqa.clara.export.graphviz.dsl

class Node(
    private val id: String,
    label: String,
    attributes: Map<String, String> = emptyMap(),
) {

    private val attributes = attributes.toMutableMap().apply { put("label", label) }

    fun attr(key: String, value: String) {
        attributes[key] = value
    }

    fun toDot(): String {
        val attrs = attributes.entries.joinToString(", ") { "${it.key}=\"${it.value}\"" }

        return buildString {
            appendInQuotes(id)

            if (attrs.isNotBlank()) {
                append(" [")
                append(attrs)
                append("]")
            }
        }
    }
}
