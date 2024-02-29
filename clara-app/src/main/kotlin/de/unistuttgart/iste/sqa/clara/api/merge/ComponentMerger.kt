package de.unistuttgart.iste.sqa.clara.api.merge

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

fun interface ComponentMerger {

    fun merge(components: List<AggregatedComponent>, communications: List<AggregatedCommunication>)
            : Pair<List<Either<MergeFailure, Component>>, List<Either<MergeFailure, Communication>>>
}
