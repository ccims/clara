package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client

import de.unistuttgart.iste.sqa.clara.api.model.IpAddress
import de.unistuttgart.iste.sqa.clara.api.model.Namespace
import de.unistuttgart.iste.sqa.clara.api.model.Namespaced

data class KubernetesPod(
    val name: Name,
    val ipAddress: IpAddress,
    override val namespace: Namespace,
) : Namespaced {

    @JvmInline
    value class Name(val value: String)
}
