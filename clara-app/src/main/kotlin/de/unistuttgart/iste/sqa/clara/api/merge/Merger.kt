package de.unistuttgart.iste.sqa.clara.api.merge

import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregation
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

fun interface Merger {

    fun merge(aggregations: Set<Aggregation>): Merge
}

data class Merge(
    val failures: List<MergeFailure>,
    val components: List<Component>,
    val communications: List<Communication>,
)
