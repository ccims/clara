package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

data class SpanInformation (
    val clientServiceName: String?,
    val serverServiceName: String?,
    val serverHostname: String?,
    val serverPath: String?,
    val serverIpAddress: String?,
    val serverPort: String?,
    val clientHostName: String?,
    val clientIpAddress: String?,
    val clientPort: String?,
)