package de.unistuttgart.iste.sqa.clara.api.model

sealed interface Component {

    @JvmInline
    value class External(val domain: Domain) : Component

    sealed interface Internal : Component, Namespaced {

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

        data class Service(
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
