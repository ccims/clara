package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

import de.unistuttgart.iste.sqa.clara.api.model.ComponentType
import de.unistuttgart.iste.sqa.clara.api.model.IpAddress

data class SpanInformation(
    val server: Server,
    val client: Client,
    val messagingSystem: MessagingSystem? = null
) {
    data class Client(
        val serviceName: Service.Name?,
        val hostName: Service.HostName?,
        val ipAddress: IpAddress?,
        val port: Service.Port?,
        val type: ComponentType?,
    )
    data class Server(
        val serviceName: Service.Name?,
        val hostName: Service.HostName?,
        val ipAddress: IpAddress?,
        val port: Service.Port?,
        val path: Service.Path?,
        val type: ComponentType?,
    )
    data class MessagingSystem(
        val serviceName: Service.Name?,
        val hostName: Service.HostName?,
        val port: Service.Port?,
        val ipAddress: IpAddress?,
        val type: ComponentType?,
    )
}
