package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model

import de.unistuttgart.iste.sqa.clara.api.model.IpAddress

data class Service(
    val name: Name?,
    val hostName: HostName?,
    val ipAddress: IpAddress?,
    val port: Port?,
    val hostIdentifier: HostIdentifier?,
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

    // Combination from hostname and port, used to create keys for maps and differentiate different applications on the same host.
    @JvmInline
    value class HostIdentifier(val value: String)

    fun mergeWithOtherServiceObject(other: Service): Service {
        val mergedName = mergeProperty(name, other.name, "name")
        val mergedHostName = mergeProperty(hostName, other.hostName, "hostName")
        val mergedIpAddress = mergeProperty(ipAddress, other.ipAddress, "ipAddress")
        val mergedPort = mergeProperty(port, other.port, "port")
        val mergedHostIdentifier = mergeProperty(hostIdentifier, other.hostIdentifier, "hostIdentifier")
        val mergedEndpoints = mergeEndpoints(endpoints, other.endpoints)

        return Service(
            name = mergedName,
            hostName = mergedHostName,
            ipAddress = mergedIpAddress,
            port = mergedPort,
            endpoints = mergedEndpoints,
            hostIdentifier = mergedHostIdentifier,
        )
    }

    private fun <Property> mergeProperty(value1: Property?, value2: Property?, propertyName: String): Property? {
        return when {
            value1 != null && value2 != null -> {
                if (value1 == value2) {
                    value1
                } else {
                    println("Clash in $propertyName. Choosing: $value1")
                    value1
                }
            }
            value1 != null -> value1
            value2 != null -> value2
            else -> null
        }
    }

    // TODO many clashes take a look here
    private fun mergeEndpoints(endpoints1: List<Endpoint>, endpoints2: List<Endpoint>): List<Endpoint> {
        val mergedEndpoints = mutableListOf<Endpoint>()

        for (endpoint in endpoints1) {
            if (!endpoints2.contains(endpoint)) {
                mergedEndpoints.add(endpoint)
            } else {
                println("Clash in endpoints. Choosing: $endpoint")
                mergedEndpoints.add(endpoint)
            }
        }

        for (endpoint in endpoints2) {
            if (!endpoints1.contains(endpoint)) {
                mergedEndpoints.add(endpoint)
            }
        }

        return mergedEndpoints
    }
}

