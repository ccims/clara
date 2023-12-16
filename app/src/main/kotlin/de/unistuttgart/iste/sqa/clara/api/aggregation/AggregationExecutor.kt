package de.unistuttgart.iste.sqa.clara.api.aggregation

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

interface AggregationExecutor {

    fun aggregateAll(): Pair<List<Either<AggregationFailure, Component>>, List<Either<AggregationFailure, Communication>>>
}
