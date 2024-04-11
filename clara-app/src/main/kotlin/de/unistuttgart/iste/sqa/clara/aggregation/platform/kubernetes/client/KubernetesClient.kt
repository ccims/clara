package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.model.Namespace

interface KubernetesClient : AutoCloseable {

    fun getNamespaces(): Either<KubernetesClientError, List<Namespace>>

    fun getPodsFromAllNamespace(includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<KubernetesPod>>

    fun getPodsFromNamespaces(namespaces: List<Namespace>, includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<KubernetesPod>>

    fun getServicesFromAllNamespaces(includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<KubernetesService>>

    fun getServicesFromNamespaces(namespaces: List<Namespace>, includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<KubernetesService>>

    fun getContainerImagesFromPodsFromAllNamespaces(includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<String>>

    fun getContainerImagesFromPodsFromNamespaces(namespaces: List<Namespace>, includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<String>>

    fun getDnsLogs(sinceTime: String): Either<KubernetesClientError, List<String>>
}
