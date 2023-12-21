package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

enum class SpanKind {
    CLIENT,
    SERVER,
    PRODUCER,
    CONSUMER,
    INTERNAL,
}