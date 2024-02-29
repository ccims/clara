package de.unistuttgart.iste.sqa.clara.api.aggregation

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication
import de.unistuttgart.iste.sqa.clara.api.model.Communication

fun interface CommunicationAggregator {

    fun aggregate(): Either<AggregationFailure, Set<AggregatedCommunication>>
}
