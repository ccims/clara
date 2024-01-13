package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

import de.unistuttgart.iste.sqa.clara.api.model.IpAddress

data class SpanInformation(
    val clientServiceName: Service.Name?,
    val serverServiceName: Service.Name?,
    val serverHostname: Service.HostName?,
    val serverHostIdentifier: Service.HostIdentifier?,
    val serverEndpoint: Service.Endpoint?,
    val serverIpAddress: IpAddress?,
    val serverPort: Service.Port?,
    val clientHostName: Service.HostName?,
    val clientIpAddress: IpAddress?,
    val clientPort: Service.Port?,
)
