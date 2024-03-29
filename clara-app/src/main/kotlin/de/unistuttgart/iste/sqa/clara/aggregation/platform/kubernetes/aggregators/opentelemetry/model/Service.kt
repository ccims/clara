package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.api.model.ComponentType
import de.unistuttgart.iste.sqa.clara.api.model.IpAddress
import de.unistuttgart.iste.sqa.clara.api.model.Path
import io.github.oshai.kotlinlogging.KotlinLogging

data class Service(
    val name: Name?,
    val hostName: HostName?,
    val ipAddress: IpAddress?,
    val port: Port?,
    val paths: List<Path>,
    val type: ComponentType?,
) {

    private val log = KotlinLogging.logger {}

    @JvmInline
    value class Name(val value: String)

    @JvmInline
    value class HostName(val value: String)

    @JvmInline
    value class Port(val value: Int) : Comparable<Int> by value

    @JvmInline
    value class Path(val value: String)

    fun mergeWithOtherServiceObject(other: Service): Service {
        val mergedName = mergeProperty(name, other.name, "name")
        val mergedHostName = mergeProperty(hostName, other.hostName, "hostName")
        val mergedIpAddress = mergeProperty(ipAddress, other.ipAddress, "ipAddress")
        val mergedPort = mergeProperty(port, other.port, "port")
        val mergedComponentType = mergeProperty(type, other.type, "componentType")
        val mergedEndpoints = mergeEndpoints(paths, other.paths)

        return Service(
            name = mergedName,
            hostName = mergedHostName,
            ipAddress = mergedIpAddress,
            port = mergedPort,
            type = mergedComponentType ?: ComponentType.Microservice,
            paths = mergedEndpoints,
        )
    }

    private fun <Property> mergeProperty(value1: Property?, value2: Property?, propertyName: String): Property? {
        return when {
            value1 != null && value2 != null -> {
                if (value1 == value2) {
                    value1
                } else {
                    log.debug { "Clash in $propertyName. Choosing: $value1" }
                    value1
                }
            }

            value1 != null -> value1
            value2 != null -> value2
            else -> null
        }
    }

    private fun mergeEndpoints(endpoints1: List<Path>, endpoints2: List<Path>): List<Path> {
        val mergedEndpoints = mutableListOf<Path>()
        mergedEndpoints.addAll(endpoints1)
        mergedEndpoints.addAll(endpoints2.filter { !endpoints1.contains(it) })
        return mergedEndpoints
    }
}

internal fun List<Service.Path>.toComponentPaths(): List<Path> = this.map { Path(it.value) }
