package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry

import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationFailure

data class OpenTelemetryAggregationFailure(val message: String) : AggregationFailure("OpenTelemetry", message)
