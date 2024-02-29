package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client

import de.unistuttgart.iste.sqa.clara.api.model.IpAddress
import de.unistuttgart.iste.sqa.clara.api.model.Namespace
import de.unistuttgart.iste.sqa.clara.api.model.Namespaced

data class KubernetesService(
    val name: Name,
    val ipAddress: IpAddress,
    override val namespace: Namespace,
    val selectedPods: List<KubernetesPod>
) : Namespaced {

    @JvmInline
    value class Name(val value: String)
}
