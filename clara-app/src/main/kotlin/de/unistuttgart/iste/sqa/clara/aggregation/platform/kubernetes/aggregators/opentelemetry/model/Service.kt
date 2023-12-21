package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

data class Service(
    val serviceName: String?,
    val hostname: String?,
    val ipAddress: String?,
    val port: String?,
    val endpoints: List<String?>,
)