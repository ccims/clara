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
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class SyftSbomAggregator(
    private val config: Config,
    private val mapper: ObjectMapper,
    private val kubernetesClient: KubernetesClient,
) : Aggregator {

    private val log = KotlinLogging.logger {}

    data class Config(
        val namespaces: List<Namespace>,
        val includeKubeNamespaces: Boolean,
        val sbomFilePath: String,
        val useStoredSbomFiles: Boolean,
    )

    override fun aggregate(): Either<AggregationFailure, Aggregation> {

        val kubernetesServices = kubernetesClient.getServicesFromNamespaces(config.namespaces, config.includeKubeNamespaces)
            .getOrElse { return Either.Left(DnsAggregationFailure(it.description)) }

        if (!config.useStoredSbomFiles) {
            runBlocking {
                // TODO even though async is used this is rather iteratively called
                kubernetesServices.map { async { it.selectedPods.firstOrNull()?.image?.let { generateJsons(it.value) } } }.awaitAll()
            }
        }

        val components = kubernetesServices.mapNotNull { service ->
            val pod = service.selectedPods.firstOrNull()
            if (pod == null) {
                null
            } else {
                val aggregatedSbom = kotlin.runCatching {
                    mapper.readValue(Paths.get("${config.sbomFilePath}${pod.image}.json").toFile(), SPDXDocument::class.java)
                }.getOrElse {
                    null
                }
                if (aggregatedSbom == null) {
                    null
                } else {
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
            }
        }.toSet()

        log.info { "Found ${components.size} components with each on average ${components.map { it.libraries.size }.average()} libraries." }
        log.info { "Done aggregating SBOM API" }

        return Aggregation(
            components = components,
            communications = emptySet()
        ).right()
    }

    private fun String.notContainsLinuxStuff() = !(contains("/usr/share") || contains("/var/lib"))

    // TODO: make sure always most recent syft version binary is fetched OR syft is installed locally
    private fun generateJsons(image: String) {
        log.info { "Generating SPDX.json files for image $image" }
        Runtime.getRuntime().exec("syft $image -o spdx-json=${config.sbomFilePath}$image.json").waitFor(30, TimeUnit.SECONDS)
    }
}