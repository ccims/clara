package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.api.model.Component.Internal.Pod
import de.unistuttgart.iste.sqa.clara.api.model.Component.Internal.Service
import de.unistuttgart.iste.sqa.clara.api.model.Domain

class KubernetesDnsQueryAnalyzer(
    private val knownPods: List<Pod>,
    private val knownServices: List<Service>,
) : DnsQueryAnalyzer {

    override fun analyze(dnsQueries: Iterable<DnsQuery>): Set<Communication> {
        return dnsQueries
            .mapNotNull { dnsQuery ->
                val sourcePod = knownPods
                    .firstOrNull { it.ipAddress == dnsQuery.sourceIpAddress }
                    ?: return@mapNotNull null

                val communicationTarget = getCommunicationTarget(dnsQuery, knownPods, knownServices) ?: return@mapNotNull null

                Communication(
                    source = Communication.Source(sourcePod),
                    target = communicationTarget,
                )
            }
            .toSet()
    }

    private fun getCommunicationTarget(dnsQuery: DnsQuery, knownPods: List<Pod>, knownServices: List<Service>): Communication.Target? {
        return if (dnsQuery.targetDomain.value.endsWith(".svc.cluster.local.")) {
            getCommunicationTargetService(dnsQuery, knownServices)
        } else if (dnsQuery.targetDomain.value.endsWith(".pod.cluster.local.")) {
            getCommunicationTargetPod(dnsQuery, knownPods)
        } else {
            Communication.Target(Component.External(Domain(dnsQuery.targetDomain.value.removeSuffix("."))))
        }
    }

    private fun getCommunicationTargetService(dnsQuery: DnsQuery, knownServices: List<Service>): Communication.Target? {
        val serviceName = dnsQuery.targetDomain.value.substringBefore(".")

        val targetService = knownServices
            .firstOrNull { it.name.value == serviceName }
            ?: return null

        return Communication.Target(targetService)
    }

    private fun getCommunicationTargetPod(dnsQuery: DnsQuery, knownPods: List<Pod>): Communication.Target? {
        val podReference = dnsQuery.targetDomain.value.substringBefore(".")
        val podIpAddress = podReference.replace('-', '.')

        val targetPod = knownPods
            .firstOrNull { pod ->
                if (podIpAddress.matches(Regex.ipAddress)) {
                    pod.ipAddress.value == podIpAddress
                } else {
                    pod.name.value == podReference
                }
            }
            ?: return null

        return Communication.Target(targetPod)
    }

    private object Regex {

        private const val REGEX_FOR_IP_ADDRESS = """\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""
        val ipAddress = Regex(REGEX_FOR_IP_ADDRESS)
    }
}
