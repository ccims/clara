package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

interface DnsLogAnalyzer {

    fun parseLogs(logs: String): Set<DnsQuery>
}
