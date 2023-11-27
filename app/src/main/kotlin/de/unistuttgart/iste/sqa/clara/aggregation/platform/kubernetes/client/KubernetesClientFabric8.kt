package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client

import arrow.core.Either
import arrow.core.right
import de.unistuttgart.iste.sqa.clara.api.model.Component.Internal.Pod
import de.unistuttgart.iste.sqa.clara.api.model.Component.Internal.Service
import de.unistuttgart.iste.sqa.clara.api.model.IpAddress
import de.unistuttgart.iste.sqa.clara.api.model.Namespace
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.KubernetesClientException
import kotlin.jvm.optionals.getOrNull

class KubernetesClientFabric8 : KubernetesClient {

    private val client = KubernetesClientBuilder().build()

    override fun getNamespaces(): Either<KubernetesClientError, List<Namespace>> {
        return try {
            client
                .namespaces()
                .list()
                .items
                .map { Namespace(it.metadata.name) }
                .right()
        } catch (ex: KubernetesClientException) {
            Either.Left(KubernetesClientError("Cannot get namespaces: ${ex.message}"))
        }
    }

    override fun getPodsFromAllNamespace(includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<Pod>> {
        return try {
            client
                .pods()
                .inAnyNamespace()
                .list()
                .items
                .filterNot { it.metadata.namespace.startsWith("kube-") && !includeKubeNamespaces }
                .map(::fromFabric8Pod)
                .right()
        } catch (ex: KubernetesClientException) {
            Either.Left(KubernetesClientError("Cannot get pods from all namespaces: ${ex.message}"))
        }
    }

    override fun getPodsFromNamespaces(namespaces: List<Namespace>, includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<Pod>> {
        return try {
            if (namespaces.any { it.value == "*" }) {
                getPodsFromAllNamespace(includeKubeNamespaces)
            } else {
                namespaces
                    .filterNot { it.value.startsWith("kube-") && !includeKubeNamespaces }
                    .flatMap { namespace: Namespace ->
                        client
                            .pods()
                            .inNamespace(namespace.value)
                            .list()
                            .items
                            .map(::fromFabric8Pod)
                    }
                    .right()
            }
        } catch (ex: KubernetesClientException) {
            Either.Left(KubernetesClientError("Cannot get pods from namespaces $namespaces: ${ex.message}"))
        }
    }

    override fun getServicesFromAllNamespaces(includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<Service>> {
        return try {
            client
                .services()
                .inAnyNamespace()
                .list()
                .items
                .filterNot { it.metadata.namespace.startsWith("kube-") && !includeKubeNamespaces }
                .map(::fromFabric8Service)
                .right()
        } catch (ex: KubernetesClientException) {
            Either.Left(KubernetesClientError("Cannot get services from all namespaces: ${ex.message}"))
        }
    }

    override fun getServicesFromNamespaces(namespaces: List<Namespace>, includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<Service>> {
        return try {
            return if (namespaces.any { it.value == "*" }) {
                getServicesFromAllNamespaces(includeKubeNamespaces)
            } else {
                namespaces
                    .filterNot { it.value.startsWith("kube-") && !includeKubeNamespaces }
                    .flatMap { namespace: Namespace ->
                        client
                            .services()
                            .inNamespace(namespace.value)
                            .list()
                            .items
                            .map(::fromFabric8Service)
                    }
                    .right()
            }
        } catch (ex: KubernetesClientException) {
            Either.Left(KubernetesClientError("Cannot get services from namespaces $namespaces: ${ex.message}"))
        }
    }

    override fun getDnsLogs(): Either<KubernetesClientError, String> {
        return try {
            client
                .pods()
                .inNamespace("kube-system")
                .withLabel("k8s-app=kube-dns")
                .resources()
                .findFirst()
                .getOrNull()
                ?.log
                ?.right()
                ?: Either.Left(KubernetesClientError("Cannot find the Kubernetes DNS server!"))
        } catch (ex: KubernetesClientException) {
            Either.Left(KubernetesClientError("Cannot get Kubernetes DNS logs: ${ex.message}"))
        }
    }

    private fun fromFabric8Pod(service: io.fabric8.kubernetes.api.model.Pod): Pod {
        return Pod(
            name = Pod.Name(service.metadata.name),
            ipAddress = IpAddress(service.status.podIP),
            namespace = Namespace(service.metadata.namespace)
        )
    }

    private fun fromFabric8Service(service: io.fabric8.kubernetes.api.model.Service): Service {
        return Service(
            name = Service.Name(service.metadata.name),
            ipAddress = IpAddress(service.spec.clusterIP),
            namespace = Namespace(service.metadata.namespace)
        )
    }
}
