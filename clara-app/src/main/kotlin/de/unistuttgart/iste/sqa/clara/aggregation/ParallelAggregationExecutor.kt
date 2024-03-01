package de.unistuttgart.iste.sqa.clara.aggregation

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregation
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationExecutor
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ParallelAggregationExecutor(private val aggregatorManager: AggregatorManager) : AggregationExecutor {

    private val log = KotlinLogging.logger {}

    override fun aggregateAll(): List<Either<AggregationFailure, Aggregation>> {
        if (aggregatorManager.aggregators.isEmpty()) {
            log.warn { "No aggregators specified and enabled!" }
            return emptyList()
        }

        log.info { "Start aggregation process ..." }

        val aggregationResult = runBlocking {
            withContext(coroutineContext) {
                aggregatorManager
                    .aggregators
                    .map { async { it.aggregate() } }
                    .awaitAll()
            }
        }

        log.info { "Finished aggregation process" }

        return aggregationResult
    }
}
