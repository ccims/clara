package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

data class Relation(
    val caller: Service,
    val callee: Service,
    val owner: Service,
    val endpoint: String?,
)
