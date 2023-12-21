package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

import de.unistuttgart.iste.sqa.clara.api.model.IpAddress

data class Service(
    val name: Name?,
    val hostName: HostName?,
    val ipAddress: IpAddress?,
    val port: Port?,
    val endpoints: List<Endpoint>,
) {

    @JvmInline
    value class Name(val value: String)

    @JvmInline
    value class HostName(val value: String)

    @JvmInline
    value class Port(val value: Int) : Comparable<Int> by value

    @JvmInline
    value class Endpoint(val value: String)
}
