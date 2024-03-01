package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Service
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Span
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.SpanProvider

class TestSpanProvider : SpanProvider {

    override suspend fun getSpans(): List<Span> {
        val spanProvider = TestSpanProvider()
        val spans = spanProvider.createTrace(false, false, false, 8)

        return spans
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
        val traceId = oldSpan?.traceId?.value ?: (100000000..999999999).random().toString()
        val spanKind = Span.Kind.entries.random()
        // TODO nextSpan can originate from same service as parent
        val serviceName = serviceList.random()
        val serviceSpanName = "$serviceName-${(1..100).random()}"
        val spanId = (oldSpan?.id?.value?.toIntOrNull() ?: -1) + 1
        // TODO create attributes

        return Span(
            id = Span.Id(spanId.toString()),
            name = Span.Name(serviceSpanName),
            traceId = Span.TraceId(traceId),
            parentId = oldSpan?.id?.value?.let { Span.ParentId(it) },
            serviceName = Service.Name(serviceName),
            attributes = Span.Attributes(emptyMap()),
            kind = Span.Kind.Producer
        )
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
