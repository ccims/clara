package de.unistuttgart.iste.sqa.clara.api.model

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Service

sealed interface Component {

    val name: Name

    @JvmInline
    value class Name(val value: String) {}

    data class External(val domain: Domain, override val name: Name) : Component

    sealed interface Internal : Component, Namespaced {

        data class OpenTelemetryService(
            override val name: Name,
            val domain: Domain,
            val endpoints: List<Endpoint>,
            override val namespace: Namespace = Namespace(value = "default"),
        ) : Internal

        // TODO the pod should not be visible on this level anymore.
        // TODO The KubernetesAggregator should already merge pods and services into one service component
        data class Pod(
            override val name: Name,
            val ipAddress: IpAddress,
            override val namespace: Namespace,
        ) : Internal

        data class KubernetesService(
            override val name: Name,
            val ipAddress: IpAddress,
            override val namespace: Namespace,
        ) : Internal

        data class MergedService(
            override val name: Name,
            override val namespace: Namespace,
            val ipAddress: IpAddress?,
            val domain: Domain?,
            val endpoints: List<Endpoint>?,
        ) : Internal
    }
}

/*sealed interface Reference

interface Referencable<T : Reference> {

    val ref: T
}

data class ExternalReference(
    val domain: Domain,
) : Reference

data class InternalReference(
    val name: Name,
    val ipAddress: IpAddress,
    val namespace: Namespace,
) : Reference {

    @JvmInline
    value class Name(val value: String) {

        override fun toString() = value
    }
}

sealed interface Component<R : Reference> : Referencable<R> {

    data class External(override val ref: ExternalReference) : Component<ExternalReference>

    sealed interface Internal : Component<InternalReference> {

        data class Pod(override val ref: InternalReference) : Internal

        data class Service(override val ref: InternalReference, val podRefs: List<InternalReference>) : Internal
    }
}*/