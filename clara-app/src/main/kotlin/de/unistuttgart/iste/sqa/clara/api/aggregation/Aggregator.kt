package de.unistuttgart.iste.sqa.clara.api.aggregation

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent

fun interface Aggregator {

    fun aggregate(): Either<AggregationFailure, Aggregation>
}

data class Aggregation(
    val components: Set<AggregatedComponent>,
    val communications: Set<AggregatedCommunication>,
)
