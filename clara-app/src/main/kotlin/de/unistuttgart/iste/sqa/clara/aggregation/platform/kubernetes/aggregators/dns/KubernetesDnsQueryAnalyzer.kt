package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesPod
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesService
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication
import de.unistuttgart.iste.sqa.clara.api.model.AggregatedComponent
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.api.model.Domain
import de.unistuttgart.iste.sqa.clara.utils.regex.Regexes

class KubernetesDnsQueryAnalyzer(
    private val knownPods: List<KubernetesPod>,
    private val knownKubernetesServices: List<KubernetesService>,
) : DnsQueryAnalyzer {

    override fun analyze(dnsQueries: Iterable<DnsQuery>): Set<AggregatedCommunication> {
        return dnsQueries
            .mapNotNull { dnsQuery ->
                val sourcePod = knownPods
                    .firstOrNull { it.ipAddress == dnsQuery.sourceIpAddress }
                    ?: return@mapNotNull null

                val communicationTarget = getCommunicationTarget(dnsQuery, knownPods, knownKubernetesServices) ?: return@mapNotNull null

                AggregatedCommunication(
                    source = AggregatedCommunication.Source(AggregatedComponent.Name("placeholder")),
                    target = communicationTarget,
                )
            }
            .toSet()
    }

    private fun getCommunicationTarget(dnsQuery: DnsQuery, knownPods: List<KubernetesPod>, knownKubernetesServices: List<KubernetesService>): AggregatedCommunication.Target? {
        return if (dnsQuery.targetDomain.value.endsWith(".svc.cluster.local.")) {
            getCommunicationTargetService(dnsQuery, knownKubernetesServices)
        } else if (dnsQuery.targetDomain.value.endsWith(".pod.cluster.local.")) {
            getCommunicationTargetPod(dnsQuery, knownPods)
        } else {
            val domain = dnsQuery.targetDomain.value.removeSuffix(".")
            AggregatedCommunication.Target(AggregatedComponent.Name("external-placeholder"))
        }
    }

    private fun getCommunicationTargetService(dnsQuery: DnsQuery, knownKubernetesServices: List<KubernetesService>): AggregatedCommunication.Target? {
        val serviceName = dnsQuery.targetDomain.value.substringBefore(".")

        val targetService = knownKubernetesServices
            .firstOrNull { it.name.value == serviceName }
            ?: return null

        return AggregatedCommunication.Target(AggregatedComponent.Name("placeholder"))
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

        return AggregatedCommunication.Target(AggregatedComponent.Name("placeholder"))
    }
}
