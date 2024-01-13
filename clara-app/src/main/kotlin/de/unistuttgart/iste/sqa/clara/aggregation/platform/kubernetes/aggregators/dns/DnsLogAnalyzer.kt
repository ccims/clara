package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

fun interface DnsLogAnalyzer {

    fun parseLogs(logs: String): Set<DnsQuery>
}
