package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.api.model.Namespace

interface KubernetesClient : AutoCloseable {

    fun getNamespaces(): Either<KubernetesClientError, List<Namespace>>

    fun getPodsFromAllNamespace(includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<Component.Internal.Pod>>

    fun getPodsFromNamespaces(namespaces: List<Namespace>, includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<Component.Internal.Pod>>

    fun getServicesFromAllNamespaces(includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<Component.Internal.KubernetesService>>

    fun getServicesFromNamespaces(namespaces: List<Namespace>, includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<Component.Internal.KubernetesService>>

    fun getDnsLogs(sinceTime: String): Either<KubernetesClientError, List<String>>
}
