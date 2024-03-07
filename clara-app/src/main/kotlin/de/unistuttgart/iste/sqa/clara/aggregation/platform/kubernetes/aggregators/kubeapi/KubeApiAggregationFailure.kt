package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.kubeapi

import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure

data class KubeApiAggregationFailure(val message: String) : AggregationFailure("Kubernetes: API", message)
