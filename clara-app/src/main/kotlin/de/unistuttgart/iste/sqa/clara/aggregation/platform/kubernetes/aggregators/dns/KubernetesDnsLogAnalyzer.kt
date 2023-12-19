package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

import de.unistuttgart.iste.sqa.clara.api.model.Domain
import de.unistuttgart.iste.sqa.clara.api.model.IpAddress

object KubernetesDnsLogAnalyzer : DnsLogAnalyzer {

    override fun parseLogs(logs: String): Set<DnsQuery> {
        return logs
            .lines()
            .mapNotNull { logLine ->
                val match = Regex.successfulDnsQuery.matchEntire(logLine) ?: return@mapNotNull null
                val sourceIpAddress = match.groups["sourceIP"]?.value ?: return@mapNotNull null
                val targetDomain = match.groups["targetDomain"]?.value ?: return@mapNotNull null

                DnsQuery(
                    sourceIpAddress = IpAddress(sourceIpAddress),
                    targetDomain = Domain(targetDomain)
                )
            }
            .toSet()
    }

    private object Regex {

        private const val REGEX_FOR_SUCCESSFUL_DNS_QUERY = """\[INFO]\s(?<sourceIP>\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):\d{1,5}\s-\s\d{1,5}\s"A\sIN\s(?<targetDomain>[a-zA-z0-9-_.]+).*NOERROR.*"""
        val successfulDnsQuery = Regex(REGEX_FOR_SUCCESSFUL_DNS_QUERY)
    }
}
