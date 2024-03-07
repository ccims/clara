package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure

data class DnsAggregationFailure(val message: String) : AggregationFailure("Kubernetes: DNS", message)
