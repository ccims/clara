package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.module

data class Span(
    val serviceName: String, // get from span resources
    val id: String,
    val parentId: String?,
    val traceId: String,
    val name: String,
    val spanKind: String, // todo make enum
    val attributes: Map<String, String>

)
