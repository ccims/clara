package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Span
import kotlinx.coroutines.flow.Flow

interface SpanProvider {

    val spanFlow: Flow<Span>
}
