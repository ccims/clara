package de.unistuttgart.iste.sqa.clara.aggregation.sbom

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns.DnsAggregationFailure
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesClient
import de.unistuttgart.iste.sqa.clara.aggregation.sbom.model.SPDXDocument
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregation
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregator
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Library
import de.unistuttgart.iste.sqa.clara.api.model.Namespace
import kotlinx.coroutines.*
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class SyftSbomAggregator(
    // private val config: Config,
    private val mapper: ObjectMapper,
    private val kubernetesClient: KubernetesClient,
) : Aggregator {
    // Todo always most recent syft version binary is fetched
    // -> get all images of the components from k8s api;
    // -> execute syft for all of them

    data class Config(
        val namespaces: List<Namespace>,
        val includeKubeNamespaces: Boolean,
    )

    companion object {
        const val PATH = "/Users/p371728/master/clara/generated/" //TODO get from config
    }

    override fun aggregate(): Either<AggregationFailure, Aggregation> {

        // TODO namespace configurable
        val kubernetesServices = kubernetesClient.getServicesFromNamespaces(listOf(Namespace("clara")), false)
            .getOrElse { return Either.Left(DnsAggregationFailure(it.description)) }

        // TODO maybe make it possible to load SPDX files if they are already available somewhere
        runBlocking {
            kubernetesServices.map { async { it.selectedPods.firstOrNull()?.image?.let { generateJsons(it.value) }} }.awaitAll()
        }

        val components = kubernetesServices.mapNotNull { service ->
            val pod = service.selectedPods.firstOrNull()
            if (pod == null) {
                null
            } else {
                val aggregatedSbom = mapper.readValue(Paths.get("$PATH${pod.image}.json").toFile(), SPDXDocument::class.java)
                val libraries = aggregatedSbom.packages.filter { pck -> pck.sourceInfo?.notContainsLinuxStuff() ?: false }.map { pck ->
                    Library(name = AggregatedComponent.Name(pck.name), version = AggregatedComponent.Internal.Version(pck.versionInfo))
                }
                AggregatedComponent.Internal.SpdxComponent(
                    name = AggregatedComponent.Name(service.name.value),
                    type = null,
                    version = service.selectedPods.firstOrNull()?.version?.value?.let { AggregatedComponent.Internal.Version(it) },
                    libraries = libraries,
                )
            }
        }.toSet()

        return Aggregation(
            components = components,
            communications = emptySet()
        ).right()
    }

    private fun String.notContainsLinuxStuff() = !(contains("/usr/share") || contains("/var/lib"))

    // TODO call syft from local binary
    private fun generateJsons(image: String) {
        Runtime.getRuntime().exec("syft $image -o spdx-json=$PATH$image.json").waitFor(30, TimeUnit.SECONDS)
    }
}