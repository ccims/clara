package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.collector

import arrow.core.*
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Service
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Span
import io.opentelemetry.proto.collector.trace.v1.*

class OpenTelemetryTraceService(private val processSpans: suspend (List<Span>) -> Unit) : TraceServiceGrpcKt.TraceServiceCoroutineImplBase() {

    override suspend fun export(request: ExportTraceServiceRequest): ExportTraceServiceResponse {
        var numberOfRejectedSpansBecauseOfMissingServiceName = 0L
        var numberOfRejectedSpansBecauseUnsupportedSpanKind = 0L

        // TODO Check: Maybe later we should not flatten entirely -> checked, and we should not :D
        val spans: List<Span> = request.resourceSpansList.flatMap { resourceSpans ->
            val serviceName = resourceSpans
                .resource
                .attributesList
                .find { it.key == "service.name" }
                ?.value
                ?.stringValue
                ?: run {
                    numberOfRejectedSpansBecauseOfMissingServiceName += request.resourceSpansCount
                    return@flatMap emptyList<Span>()
                }

            resourceSpans.scopeSpansList.flatMap { scopeSpans ->
                scopeSpans.spansList.mapNotNull { openTelemetrySpan ->
                    openTelemetrySpan.toClaraSpan(serviceName).getOrElse {
                        numberOfRejectedSpansBecauseUnsupportedSpanKind++
                        null
                    }
                }
            }
        }

        processSpans(spans)

        val totalNumberOfRejectedSpans = numberOfRejectedSpansBecauseOfMissingServiceName + numberOfRejectedSpansBecauseUnsupportedSpanKind

        return exportTraceServiceResponse {
            if (totalNumberOfRejectedSpans > 0) {
                partialSuccess = exportTracePartialSuccess {
                    rejectedSpans = totalNumberOfRejectedSpans
                    errorMessage =
                        "$numberOfRejectedSpansBecauseOfMissingServiceName spans were in resourceSpanLists that didn't contain the required attribute 'service.name' and" +
                                "$numberOfRejectedSpansBecauseUnsupportedSpanKind spans had a span kind of 'SPAN_KIND_UNSPECIFIED' or 'UNRECOGNIZED'."
                }
            }
        }
    }
}

private fun io.opentelemetry.proto.trace.v1.Span.toClaraSpan(serviceName: String): Option<Span> {
    return Span(
        id = Span.Id(this.spanId.toStringUtf8()),
        attributes = Span.Attributes(
            buildMap(this.attributesCount) {
                this@toClaraSpan.attributesList.forEach { attribute ->
                    put(attribute.key, attribute.value.stringValue)
                }
            }
        ),
        name = Span.Name(this.name),
        parentId = Span.ParentId(this.parentSpanId.toStringUtf8()),
        serviceName = Service.Name(serviceName),
        traceId = Span.TraceId(this.traceId.toStringUtf8()),
        kind = this.kind.toClaraSpanKind().getOrElse { return None }
    ).some()
}

private fun io.opentelemetry.proto.trace.v1.Span.SpanKind.toClaraSpanKind(): Option<Span.Kind> {
    return when (this) {
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_INTERNAL -> Some(Span.Kind.Internal)
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_SERVER -> Some(Span.Kind.Server)
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_CLIENT -> Some(Span.Kind.Client)
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_PRODUCER -> Some(Span.Kind.Producer)
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_CONSUMER -> Some(Span.Kind.Consumer)
        io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_UNSPECIFIED -> None
        io.opentelemetry.proto.trace.v1.Span.SpanKind.UNRECOGNIZED -> None
    }
}
