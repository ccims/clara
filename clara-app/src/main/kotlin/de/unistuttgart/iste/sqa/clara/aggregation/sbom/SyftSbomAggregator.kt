package de.unistuttgart.iste.sqa.clara.aggregation.sbom

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns.DnsAggregationFailure
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesClient
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregation
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregator
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Domain
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

        val knownPods = kubernetesClient.getContainerImagesFromPodsFromNamespaces(listOf(Namespace("clara")), false)
            .getOrElse { return Either.Left(DnsAggregationFailure(it.description)) }

        // TODO maybe make it possible to load SPDX files if they are already available somewhere
        runBlocking {
            knownPods.map { async { generateJsons(it) } }.awaitAll()
        }

        knownPods.forEach {
            val aggregatedSbom = mapper.readValue(Paths.get("$PATH$it.json").toFile(), LinkedHashMap::class.java)
        }

        return Aggregation(
            components = knownPods.map { AggregatedComponent.External(name = AggregatedComponent.Name(""), type = null, domain = Domain("")) }.toSet(),
            communications = emptySet()
        ).right()
    }

    private fun generateJsons(image: String) {
        Runtime.getRuntime().exec("syft $image -o spdx-json=$PATH$image.json").waitFor(30, TimeUnit.SECONDS)
    }
}