package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.aggregatedServiceNameFrom
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesPod
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesService
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.utils.regex.Regexes
import io.github.oshai.kotlinlogging.KotlinLogging

class KubernetesDnsQueryAnalyzer(
    private val knownKubernetesPods: List<KubernetesPod>,
    private val knownKubernetesServices: List<KubernetesService>,
) : DnsQueryAnalyzer {

    private val log = KotlinLogging.logger {}

    override fun analyze(dnsQueries: Iterable<DnsQuery>): Set<AggregatedCommunication> {
        return dnsQueries
            .mapNotNull { dnsQuery ->
                val sourcePod = knownKubernetesPods
                    .firstOrNull { it.ipAddress == dnsQuery.sourceIpAddress }
                    ?: return@mapNotNull null.also { log.trace { "No Pod found for source IP: ${dnsQuery.sourceIpAddress}" } }

                val communicationTarget = getCommunicationTarget(dnsQuery, knownKubernetesPods, knownKubernetesServices) ?: return@mapNotNull null

                val sourceServiceName = aggregatedServiceNameFrom(sourcePod, knownKubernetesServices)

                AggregatedCommunication(
                    source = AggregatedCommunication.Source(sourceServiceName),
                    target = communicationTarget,
                )
            }
            .toSet()
    }

    private fun getCommunicationTarget(dnsQuery: DnsQuery, knownKubernetesPods: List<KubernetesPod>, knownKubernetesServices: List<KubernetesService>): AggregatedCommunication.Target? {
        return if (dnsQuery.targetDomain.value.endsWith(".svc.cluster.local.")) {
            getCommunicationTargetService(dnsQuery, knownKubernetesServices)
        } else if (dnsQuery.targetDomain.value.endsWith(".pod.cluster.local.")) {
            getCommunicationTargetPod(dnsQuery, knownKubernetesPods)
        } else {
            val domain = dnsQuery.targetDomain.value.removeSuffix(".")
            AggregatedCommunication.Target(AggregatedComponent.Name(domain))
        }
    }

    private fun getCommunicationTargetService(dnsQuery: DnsQuery, knownKubernetesServices: List<KubernetesService>): AggregatedCommunication.Target? {
        val serviceName = dnsQuery.targetDomain.value.substringBefore(".")

        val targetService = knownKubernetesServices
            .firstOrNull { it.name.value == serviceName }
            ?: return null

        val targetComponentName = aggregatedServiceNameFrom(targetService)

        return AggregatedCommunication.Target(targetComponentName)
    }

    private fun getCommunicationTargetPod(dnsQuery: DnsQuery, knownPods: List<KubernetesPod>): AggregatedCommunication.Target? {
        val podReference = dnsQuery.targetDomain.value.substringBefore(".")
        val podIpAddress = podReference.replace('-', '.')

        val targetPod = knownPods
            .firstOrNull { pod ->
                if (podIpAddress.matches(Regexes.ipAddressV4)) {
                    pod.ipAddress.value == podIpAddress
                } else {
                    pod.name.value == podReference
                }
            }
            ?: return null

        val targetComponentName = aggregatedServiceNameFrom(targetPod, knownKubernetesServices)

        return AggregatedCommunication.Target(targetComponentName)
    }
}
