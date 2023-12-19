package de.unistuttgart.iste.sqa.clara.api.export

import arrow.core.Option
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

interface Exporter {

    fun export(components: Set<Component>, communications: Set<Communication>): Option<ExportFailure>
}
