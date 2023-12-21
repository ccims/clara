package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Span
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class OpenTelemetrySpanProvider : SpanProvider {

    override val spanFlow: Flow<Span> = emptyFlow()
}
