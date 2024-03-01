package de.unistuttgart.iste.sqa.clara.api.aggregation

import arrow.core.Either

fun interface AggregationExecutor {

    fun aggregateAll(): List<Either<AggregationFailure, Aggregation>>
}
