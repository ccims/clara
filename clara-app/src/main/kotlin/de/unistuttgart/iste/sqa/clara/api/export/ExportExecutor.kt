package de.unistuttgart.iste.sqa.clara.api.export

import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

interface ExportExecutor {

    fun exportAll(components: List<Component>, communications: List<Communication>): List<ExportFailure>
}
