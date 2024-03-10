package de.unistuttgart.iste.sqa.clara.api.filter

import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

fun interface Rule {

    fun apply(components: Iterable<Component>, communications: Iterable<Communication>): Filtered
}
