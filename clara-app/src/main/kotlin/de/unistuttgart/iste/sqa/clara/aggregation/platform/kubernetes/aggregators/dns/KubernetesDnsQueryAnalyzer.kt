package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.api.model.Component.Internal.Pod
import de.unistuttgart.iste.sqa.clara.api.model.Component.Internal.KubernetesService
import de.unistuttgart.iste.sqa.clara.api.model.Domain
import de.unistuttgart.iste.sqa.clara.utils.regex.Regexes

class KubernetesDnsQueryAnalyzer(
    private val knownPods: List<Pod>,
    private val knownKubernetesServices: List<KubernetesService>,
) : DnsQueryAnalyzer {

    override fun analyze(dnsQueries: Iterable<DnsQuery>): Set<Communication> {
        return dnsQueries
            .mapNotNull { dnsQuery ->
                val sourcePod = knownPods
                    .firstOrNull { it.ipAddress == dnsQuery.sourceIpAddress }
                    ?: return@mapNotNull null

                val communicationTarget = getCommunicationTarget(dnsQuery, knownPods, knownKubernetesServices) ?: return@mapNotNull null

                Communication(
                    source = Communication.Source(sourcePod),
                    target = communicationTarget,
                )
            }
            .toSet()
    }

    private fun getCommunicationTarget(dnsQuery: DnsQuery, knownPods: List<Pod>, knownKubernetesServices: List<KubernetesService>): Communication.Target? {
        return if (dnsQuery.targetDomain.value.endsWith(".svc.cluster.local.")) {
            getCommunicationTargetService(dnsQuery, knownKubernetesServices)
        } else if (dnsQuery.targetDomain.value.endsWith(".pod.cluster.local.")) {
            getCommunicationTargetPod(dnsQuery, knownPods)
        } else {
            val domain = dnsQuery.targetDomain.value.removeSuffix(".")
            Communication.Target(Component.External(Domain(domain), Component.Name(domain)))
        }
    }

    private fun getCommunicationTargetService(dnsQuery: DnsQuery, knownKubernetesServices: List<KubernetesService>): Communication.Target? {
        val serviceName = dnsQuery.targetDomain.value.substringBefore(".")

        val targetService = knownKubernetesServices
            .firstOrNull { it.name.value == serviceName }
            ?: return null

        return Communication.Target(targetService)
    }

    private fun getCommunicationTargetPod(dnsQuery: DnsQuery, knownPods: List<Pod>): Communication.Target? {
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

        return Communication.Target(targetPod)
    }
}
