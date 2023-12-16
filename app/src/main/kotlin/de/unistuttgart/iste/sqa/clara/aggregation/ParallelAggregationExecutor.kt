package de.unistuttgart.iste.sqa.clara.aggregation

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationExecutor
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.utils.kotlinx.awaitBoth
import de.unistuttgart.iste.sqa.clara.utils.list.flattenRight
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

class ParallelAggregationExecutor(private val aggregatorManager: AggregatorManager) : AggregationExecutor {

    private val log = KotlinLogging.logger {}

    override fun aggregateAll(): Pair<List<Either<AggregationFailure, Component>>, List<Either<AggregationFailure, Communication>>> {
        if (aggregatorManager.componentAggregators.isEmpty() && aggregatorManager.communicationAggregators.isEmpty()) {
            log.warn { "No aggregators specified and enabled!" }
            return Pair(emptyList(), emptyList())
        }

        log.info { "Start aggregation process ..." }

        val aggregationResult = runBlocking {
            withContext(coroutineContext) {
                val components = async(Dispatchers.IO) { aggregateAllComponents() }
                val communications = async(Dispatchers.IO) { aggregateAllCommunications() }

                Pair(components, communications).awaitBoth()
            }
        }

        log.info { "Finished aggregation process" }

        return aggregationResult
    }

    private suspend fun aggregateAllComponents(): List<Either<AggregationFailure, Component>> {
        return withContext(coroutineContext) {
            aggregatorManager
                .componentAggregators
                .map {
                    async(Dispatchers.IO) {
                        it.aggregate()
                    }
                }
                .awaitAll()
                .flattenRight()
        }
    }

    private suspend fun aggregateAllCommunications(): List<Either<AggregationFailure, Communication>> {
        return withContext(coroutineContext) {
            aggregatorManager
                .communicationAggregators
                .map {
                    async(Dispatchers.IO) {
                        it.aggregate()
                    }
                }
                .awaitAll()
                .flattenRight()
        }
    }
}
