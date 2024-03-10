package de.unistuttgart.iste.sqa.clara.api.export

import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

fun interface ExportExecutor {

    fun exportAll(components: Set<Component>, communications: Set<Communication>): List<ExportFailure>
}
