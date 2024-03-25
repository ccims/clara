package de.unistuttgart.iste.sqa.clara.api.export

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

fun interface Exporter {

    fun export(components: Set<Component>, communications: Set<Communication>): Either<ExportFailure, Unit>
}
