package de.unistuttgart.iste.sqa.clara.filter.rules

import de.unistuttgart.iste.sqa.clara.api.filter.Filtered
import de.unistuttgart.iste.sqa.clara.api.filter.Rule
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

class RemoveComponentVersions : Rule {

    override fun apply(components: Iterable<Component>, communications: Iterable<Communication>): Filtered {
        return Filtered(
            components = components.filter().toSet(),
            communications = communications.toSet(),
        )
    }

    private fun Iterable<Component>.filter(): List<Component> {
        return this.map {
            when(it) {
                is Component.InternalComponent -> it.copy(version = null)
                is Component.ExternalComponent -> it
            }
        }
    }
}