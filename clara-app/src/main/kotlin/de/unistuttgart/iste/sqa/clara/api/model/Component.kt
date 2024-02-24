package de.unistuttgart.iste.sqa.clara.api.model

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Service

sealed interface Component {

    @JvmInline
    value class External(val domain: Domain) : Component

    sealed interface Internal : Component, Namespaced {

        data class OpenTelemetryService(
            val name: Name,
            val hostIdentifier: Service.HostIdentifier,
            val endpoints: List<Service.Endpoint>,
            override val namespace: Namespace = Namespace(value = "default"),
        ) : Internal {

            @JvmInline
            value class Name(val value: String) {}
        }

        data class Pod(
            val name: Name,
            val ipAddress: IpAddress,
            override val namespace: Namespace,
        ) : Internal {

            @JvmInline
            value class Name(val value: String) {

                override fun toString() = value
            }
        }

        data class KubernetesService(
            val name: Name,
            val ipAddress: IpAddress,
            override val namespace: Namespace,
        ) : Internal {

            @JvmInline
            value class Name(val value: String) {

                override fun toString() = value
            }
        }
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