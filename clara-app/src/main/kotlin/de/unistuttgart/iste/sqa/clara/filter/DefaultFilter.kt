package de.unistuttgart.iste.sqa.clara.filter

import de.unistuttgart.iste.sqa.clara.api.filter.Filter
import de.unistuttgart.iste.sqa.clara.api.filter.Filtered
import de.unistuttgart.iste.sqa.clara.api.filter.Rule
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

class DefaultFilter: Filter {

    override fun filter(components: Iterable<Component>, communications: Iterable<Communication>, rules: Iterable<Rule>): Filtered {
        var filtered = Filtered(components.toSet(), communications.toSet())

        rules.forEach { rule ->
            filtered = rule.apply(filtered.components, filtered.communications)
        }

        return filtered
    }
}
