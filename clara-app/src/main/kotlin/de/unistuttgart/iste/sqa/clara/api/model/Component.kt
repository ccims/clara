package de.unistuttgart.iste.sqa.clara.api.model

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesPod

sealed interface AggregatedComponent {

    val name: Name

    @JvmInline
    value class Name(val value: String)

    data class External(override val name: Name, val domain: Domain) : AggregatedComponent

    sealed interface Internal : AggregatedComponent {

        data class OpenTelemetryComponent(
            override val name: Name,
            val domain: Domain,
            val paths: List<Path>,
        ) : Internal

        data class KubernetesComponent(
            override val name: Name,
            val ipAddress: IpAddress,
            override val namespace: Namespace,
            val pods: List<KubernetesPod>,
        ) : Internal, Namespaced
    }
}

sealed interface Component {

    val name: Name

    @JvmInline
    value class Name(val value: String) {

        override fun toString() = value
    }

    data class InternalComponent(
        override val name: Name,
        val namespace: Namespace?,
        val ipAddress: IpAddress?,
        val endpoints: Endpoints?,
    ) : Component {

        data class Endpoints(val domain: Domain, val paths: List<Path>)
    }

    data class ExternalComponent(
        override val name: Name,
        val domain: Domain,
    ) : Component
}
