package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.module

data class SpanInformation(
    val serverIdentifier: String?,
    val clientIdentifier: String?,
    val endpoint: String?,
)
