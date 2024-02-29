package de.unistuttgart.iste.sqa.clara.api.model

sealed interface AggregatedComponent {

    val name: Name

    @JvmInline
    value class Name(val value: String)

    data class External(val domain: Domain, override val name: Name) : AggregatedComponent

    sealed interface Internal : AggregatedComponent {

        data class OpenTelemetryComponent(
            override val name: Name,
            val domain: Domain,
            val endpoints: List<Endpoint>,
        ) : Internal

        data class KubernetesComponent(
            override val name: Name,
            val ipAddress: IpAddress,
            override val namespace: Namespace,
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
        val namespace: Namespace,
        val ipAddress: IpAddress?,
        val domain: Domain?,
        val endpoints: List<Endpoint>?,
    ) : Component

    data class ExternalComponent(
        override val name: Name,
        val domain: Domain?,
    ) : Component
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