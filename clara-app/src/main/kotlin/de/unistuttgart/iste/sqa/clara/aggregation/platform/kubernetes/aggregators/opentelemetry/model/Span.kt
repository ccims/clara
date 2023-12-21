package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

data class Span(
    val id: Id,
    val name: Name,
    val traceId: TraceId,
    val parentId: ParentId?,
    val serviceName: Service.Name,
    val kind: Kind,
    val attributes: Attributes,
) {

    @JvmInline
    value class Id(val value: String)

    @JvmInline
    value class Name(val value: String)

    @JvmInline
    value class TraceId(val value: String)

    @JvmInline
    value class ParentId(val value: String)

    enum class Kind {
        Client,
        Server,
        Internal,
        Producer,
        Consumer,
    }

    @JvmInline
    value class Attributes(val value: Map<String, String>) : Map<String, String> by value
}
