package de.unistuttgart.iste.sqa.clara.api.merge

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

fun interface ComponentMerger {

    fun merge(components: List<Component>, communications: List<Communication>)
            : Pair<Either<MergeFailure, List<Component>>, Either<MergeFailure, List<Communication>>>
}
