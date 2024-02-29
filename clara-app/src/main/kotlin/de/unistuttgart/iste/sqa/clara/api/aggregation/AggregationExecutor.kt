package de.unistuttgart.iste.sqa.clara.api.aggregation

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

fun interface AggregationExecutor {

    fun aggregateAll(): Pair<List<Either<AggregationFailure, AggregatedComponent>>, List<Either<AggregationFailure, AggregatedCommunication>>>
}
