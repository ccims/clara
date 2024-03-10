package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

import de.unistuttgart.iste.sqa.clara.api.model.IpAddress

data class SpanInformation(
    val server: Server,
    val client: Client,
) {
    data class Client(
        val serviceName: Service.Name?,
        val hostName: Service.HostName?,
        val ipAddress: IpAddress?,
        val port: Service.Port?,
    )
    data class Server(
        val serviceName: Service.Name?,
        val hostName: Service.HostName?,
        val ipAddress: IpAddress?,
        val port: Service.Port?,
        val path: Service.Path?,
    )
}
