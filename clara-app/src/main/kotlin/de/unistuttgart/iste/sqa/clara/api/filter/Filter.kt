package de.unistuttgart.iste.sqa.clara.api.filter

import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

fun interface Filter {

    fun filter(components: Iterable<Component>, communications: Iterable<Communication>, rules: Iterable<Rule>): Filtered
}

data class Filtered(
    val components: Set<Component>,
    val communications: Set<Communication>,
)
