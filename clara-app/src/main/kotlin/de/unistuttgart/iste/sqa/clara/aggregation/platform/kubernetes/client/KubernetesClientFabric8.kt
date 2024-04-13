package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import de.unistuttgart.iste.sqa.clara.api.model.IpAddress
import de.unistuttgart.iste.sqa.clara.api.model.Namespace
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.KubernetesClientException
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.stream.Collectors

class KubernetesClientFabric8 : KubernetesClient {

    private val log = KotlinLogging.logger {}

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

    override fun getPodsFromAllNamespace(includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<KubernetesPod>> {
        return try {
            client
                .pods()
                .inAnyNamespace()
                .list()
                .items
                .filterNot { it.metadata.namespace.startsWith("kube-") && !includeKubeNamespaces }
                .filter { it.status.phase == "Running" }
                .mapNotNull(::fromFabric8Pod)
                .right()
        } catch (ex: KubernetesClientException) {
            Either.Left(KubernetesClientError("Cannot get pods from all namespaces: ${ex.message}"))
        }
    }

    override fun getPodsFromNamespaces(namespaces: List<Namespace>, includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<KubernetesPod>> {
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
                            .filter { it.status.phase == "Running" }
                            .mapNotNull(::fromFabric8Pod)
                    }
                    .right()
            }
        } catch (ex: KubernetesClientException) {
            Either.Left(KubernetesClientError("Cannot get pods from namespaces $namespaces: ${ex.message}"))
        }
    }

    override fun getServicesFromAllNamespaces(includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<KubernetesService>> {
        return try {
            client
                .services()
                .inAnyNamespace()
                .list()
                .items
                .filterNot { it.metadata.namespace.startsWith("kube-") && !includeKubeNamespaces }
                .mapNotNull(::fromFabric8Service)
                .right()
        } catch (ex: KubernetesClientException) {
            Either.Left(KubernetesClientError("Cannot get services from all namespaces: ${ex.message}"))
        }
    }

    override fun getServicesFromNamespaces(namespaces: List<Namespace>, includeKubeNamespaces: Boolean): Either<KubernetesClientError, List<KubernetesService>> {
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
                            .mapNotNull(::fromFabric8Service)
                    }
                    .right()
            }
        } catch (ex: KubernetesClientException) {
            Either.Left(KubernetesClientError("Cannot get services from namespaces $namespaces: ${ex.message}"))
        }
    }

    override fun getDnsLogs(sinceTime: String): Either<KubernetesClientError, List<String>> {
        val kubeDnsLogs = getLogs("kube-system", "k8s-app=kube-dns", sinceTime).getOrElse { return it.left() }
        val nodeLocalDnsLos = getLogs("kube-system", "k8s-app=node-local-dns", sinceTime).getOrElse { return it.left() }

        return Either.Right(kubeDnsLogs + nodeLocalDnsLos)
    }

    private fun getLogs(namespace: String, podSelector: String, sinceTime: String): Either<KubernetesClientError, List<String>> {
        return try {
            client
                .pods()
                .inNamespace(namespace)
                .withLabel(podSelector)
                .resources()
                .map { it.sinceTime(sinceTime).log }
                .collect(Collectors.toList())
                .right()
        } catch (ex: KubernetesClientException) {
            log.debug(ex) { "exception when getting the logs" } // TODO: remove this log
            Either.Left(KubernetesClientError("Cannot get logs from pods with the label '$podSelector' in namespace '$namespace': ${ex.message}"))
        }
    }

    override fun close() {
        log.debug { "Close Kubernetes client ..." }
        client.close()
        log.debug { "Done closing Kubernetes client" }
    }

    private fun fromFabric8Pod(pod: io.fabric8.kubernetes.api.model.Pod): KubernetesPod? {
        return KubernetesPod(
            name = KubernetesPod.Name(pod.metadata.name ?: return null),
            ipAddress = IpAddress(pod.status.podIP ?: return null),
            namespace = Namespace(pod.metadata.namespace ?: return null),
            version = KubernetesPod.Version(pod.spec.containers.firstOrNull()?.image?.substringAfter(":") ?: return null),
            image = KubernetesPod.Image(pod.spec.containers.firstOrNull()?.image ?: return null)
        )
    }

    private fun fromFabric8Service(service: io.fabric8.kubernetes.api.model.Service): KubernetesService? {
        return KubernetesService(
            name = KubernetesService.Name(service.metadata.name ?: return null),
            ipAddress = IpAddress(service.spec.clusterIP ?: return null),
            namespace = Namespace(service.metadata.namespace ?: return null),
            selectedPods = client
                .pods()
                .inNamespace(service.metadata.namespace ?: return null)
                .withLabels(service.spec.selector ?: return null)
                .list()
                .items
                .mapNotNull(::fromFabric8Pod)
        )
    }
}
