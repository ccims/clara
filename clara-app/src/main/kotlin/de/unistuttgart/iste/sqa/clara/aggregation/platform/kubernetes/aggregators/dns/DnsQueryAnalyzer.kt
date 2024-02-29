package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

import de.unistuttgart.iste.sqa.clara.api.model.AggregatedCommunication

fun interface DnsQueryAnalyzer {

    fun analyze(dnsQueries: Iterable<DnsQuery>): Set<AggregatedCommunication>
}
