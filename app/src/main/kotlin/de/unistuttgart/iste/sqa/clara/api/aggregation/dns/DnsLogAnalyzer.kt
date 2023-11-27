package de.unistuttgart.iste.sqa.clara.api.aggregation.dns

interface DnsLogAnalyzer {

    fun parseLogs(logs: String): Set<DnsQuery>
}
