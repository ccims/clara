package de.unistuttgart.iste.sqa.clara.api.aggregation

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.Component

interface ComponentAggregator {

    fun aggregate(): Either<AggregationFailure, Set<Component>>
}
