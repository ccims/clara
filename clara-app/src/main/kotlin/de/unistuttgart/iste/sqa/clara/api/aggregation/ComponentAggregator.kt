package de.unistuttgart.iste.sqa.clara.api.aggregation

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent

fun interface ComponentAggregator {

    fun aggregate(): Either<AggregationFailure, Set<AggregatedComponent>>
}
