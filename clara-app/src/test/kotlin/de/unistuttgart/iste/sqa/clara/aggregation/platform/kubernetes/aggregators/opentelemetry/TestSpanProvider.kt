package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.module.Span
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class TestSpanProvider : SpanProvider {

    override val spanFlow: Flow<Span> = flow {
        val spanProvider = TestSpanProvider()
        val spans = spanProvider.createTrace(false, false, false, 8)

        emitAll(spans.asFlow())
    }

    private fun createTrace(isDiverting: Boolean, isOverlapping: Boolean, returnToPrevious: Boolean, length: Int?): List<Span> {
        val traceLength = length ?: (5..10).random()
        val spans: MutableList<Span> = mutableListOf()

        val services = mockServices(traceLength / 2)

        var oldSpan: Span? = null

        for (i in 1..traceLength) {
            oldSpan = createNextSpan(oldSpan, services)
            spans.add(oldSpan)
        }

        return spans
    }

    private fun createNextSpan(oldSpan: Span?, serviceList: List<String>): Span {
        val traceId = oldSpan?.traceId ?: (100000000..999999999).random().toString()
        // TODO replace with enums from Span class
        val spanKind = listOf("spanKindEnum1", "spanKindEnum2", "spanKindEnum3", "spanKindEnum4", "spanKindEnum5").random()
        // TODO nextSpan can originate from same service as parent
        val serviceName = serviceList.random()
        val serviceSpanName = "$serviceName-${(1..100).random()}"
        val spanId = (oldSpan?.id?.toInt()?.plus(1)) ?: 0
        // TODO create attributes

        return Span(serviceName, spanId.toString(), oldSpan?.id, traceId, serviceSpanName, spanKind, emptyMap())
    }

    private fun mockServices(size: Int): List<String> {
        val adjectives = listOf("crazy", "dumb", "interesting", "boring", "smart", "fast", "relaxed", "ambiguous")
        val animals = listOf("horse", "elephant", "cow", "chicken", "snake", "penguin", "kangaroo", "otter")

        val serviceNames: MutableList<String> = mutableListOf()

        for (i in 0..size) {
            var randomName = ""
            do {
                val randAdjective = adjectives.random()
                val randAnimal = animals.random()
                randomName = "$randAdjective-$randAnimal-service"
            } while (serviceNames.contains(randomName))

            serviceNames.add(randomName)
        }

        return serviceNames
    }
}
