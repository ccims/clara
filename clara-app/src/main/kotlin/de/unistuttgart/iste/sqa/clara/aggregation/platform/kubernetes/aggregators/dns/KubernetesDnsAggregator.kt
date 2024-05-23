package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesClient
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregation
import de.unistuttgart.iste.sqa.clara.api.aggregation.Aggregator
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Domain
import de.unistuttgart.iste.sqa.clara.api.model.Namespace
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

class KubernetesDnsAggregator(
    private val config: Config,
    private val kubernetesClient: KubernetesClient,
) : Aggregator {

    data class Config(
        val namespaces: List<Namespace>,
        val includeKubeNamespaces: Boolean,
        val sinceTime: String,
        val useLogsFromFile: Boolean = true
    )

    private val log = KotlinLogging.logger {}

    override fun aggregate(): Either<DnsAggregationFailure, Aggregation> {
        log.info { "Aggregate Kubernetes DNS ..." }

        val (dnsLogs, knownPods, knownServices) = kubernetesClient.use { client ->
            val dnsLogs = if (!config.useLogsFromFile)
                client.getDnsLogs(config.sinceTime)
                    .getOrElse { return Either.Left(DnsAggregationFailure(it.description)) }
            else {
                runCatching {
                    val logFile = File("/app/resources/dnslogs")
                    val logLine = logFile.readLines().joinToString("\n" )
                    listOf(logLine)
                }.getOrElse { return Either.Left(DnsAggregationFailure("${it.message}")) }
            }
            val knownPods = client.getPodsFromNamespaces(config.namespaces, config.includeKubeNamespaces)
                .getOrElse { return Either.Left(DnsAggregationFailure(it.description)) }
            val knownServices = client.getServicesFromNamespaces(config.namespaces, config.includeKubeNamespaces)
                .getOrElse { return Either.Left(DnsAggregationFailure(it.description)) }

            Triple(dnsLogs, knownPods, knownServices)
        }

        log.trace { "Got these DNS logs:\n" + dnsLogs.joinToString("\n") }

        val dnsQueries = dnsLogs.flatMap { KubernetesDnsLogAnalyzer.parseLogs(it) }.toSet()
        val queryAnalyzer = KubernetesDnsQueryAnalyzer(knownPods, knownServices)
        val communications = queryAnalyzer.analyze(dnsQueries)

        val allComponentNames = (communications.map { it.source.componentName } + communications.map { it.target.componentName }).toSet()

        val allPodAndServiceNames = knownPods.map { it.name.value } + knownServices.map { it.name.value }

        // components whose name is not a known service or pod are considered external
        val externalComponents = allComponentNames
            .filter { allPodAndServiceNames.contains(it.value).not() }
            .map { AggregatedComponent.External(name = AggregatedComponent.Name(it.value), domain = Domain(it.value), type = null) }
            .toSet()

        log.info { "Found ${externalComponents.size} components and ${communications.size} communications" }
        log.info { "Done aggregating Kubernetes DNS" }

        return Aggregation(
            components = externalComponents,
            communications = communications,
        ).right()
    }
}
