package de.unistuttgart.iste.sqa.clara.filter.rules

import de.unistuttgart.iste.sqa.clara.api.filter.Filtered
import de.unistuttgart.iste.sqa.clara.api.filter.Rule
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

class RemoveComponentEndpoints : Rule {

    override fun apply(components: Iterable<Component>, communications: Iterable<Communication>): Filtered {
        return Filtered(
            components = components.map {
                when (it) {
                    is Component.ExternalComponent -> it
                    is Component.InternalComponent -> it.copy(endpoints = null)
                }
            }.toSet(),
            communications = communications.toSet(),
        )
    }
}
