package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

data class Span(
    val serviceName: String, // get from span resources
    val id: String,
    val parentId: String?,
    val traceId: String,
    val name: String,
    val spanKind: SpanKind,
    val attributes: Map<String, String>

)
