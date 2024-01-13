package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

fun interface SpanProvider {

    suspend fun getSpans(): List<Span>
}
