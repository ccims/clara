package de.unistuttgart.iste.sqa.clara.filter.rules

import de.unistuttgart.iste.sqa.clara.api.filter.Filtered
import de.unistuttgart.iste.sqa.clara.api.filter.Rule
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

class RemoveComponentsByName(private val nameRegex: Regex): Rule {

    override fun apply(components: Iterable<Component>, communications: Iterable<Communication>): Filtered {
        return Filtered(
            components = components.filterNot { it.name.value.matches(nameRegex) }.toSet(),
            communications = communications.filterNot { it.source.componentName.value.matches(nameRegex) || it.target.componentName.value.matches(nameRegex) }.toSet(),
        )
    }
}
