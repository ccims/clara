package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.spanprovider

import arrow.core.*
import com.google.protobuf.ByteString
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Service
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.model.Span
import io.opentelemetry.proto.collector.trace.v1.*

class OpenTelemetryTraceService(private val processSpans: suspend (List<Span>) -> Unit) : TraceServiceGrpcKt.TraceServiceCoroutineImplBase() {

    override suspend fun export(request: ExportTraceServiceRequest): ExportTraceServiceResponse {
        var numberOfRejectedSpansBecauseOfMissingServiceName = 0L
        var numberOfRejectedSpansBecauseUnsupportedSpanKind = 0L

        val spans: List<Span> = request.resourceSpansList.flatMap { resourceSpans ->
            val serviceName = resourceSpans
                .resource
                .attributesList
                .find { it.key == "service.name" }
                ?.value
                ?.stringValue
                ?: run {
                    numberOfRejectedSpansBecauseOfMissingServiceName += request.resourceSpansList.sumOf { it.scopeSpansCount }
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
                        "$numberOfRejectedSpansBecauseOfMissingServiceName spans were in resourceSpanLists that didn't contain the required attribute 'service.name' and " +
                                "$numberOfRejectedSpansBecauseUnsupportedSpanKind spans had a span kind of 'SPAN_KIND_UNSPECIFIED' or 'UNRECOGNIZED'."
                }
            }
        }
    }
}

fun io.opentelemetry.proto.trace.v1.Span.toClaraSpan(serviceName: String): Option<Span> {
    return Span(
        id = Span.Id(this.spanId.toHexString()),
        attributes = Span.Attributes(
            buildMap(this.attributesCount) {
                this@toClaraSpan.attributesList.forEach { attribute ->
                    put(attribute.key, attribute.value.stringValue)
                }
            }
        ),
        name = Span.Name(this.name),
        parentId = Span.ParentId(this.parentSpanId.toHexString()),
        serviceName = Service.Name(serviceName),
        traceId = Span.TraceId(this.traceId.toHexString()),
        kind = this.kind.toClaraSpanKind().getOrElse { return None }
    ).some()
}

fun ByteString.toHexString(): String {
    val hexChars = "0123456789abcdef"
    val byteArray = this.toByteArray()
    val hexStringBuilder = StringBuilder(2 * this.size())
    for (byte in byteArray) {
        val firstNibble = (byte.toInt() and 0xF0).ushr(4)
        val secondNibble = byte.toInt() and 0x0F
        hexStringBuilder.append(hexChars[firstNibble])
        hexStringBuilder.append(hexChars[secondNibble])
    }
    return hexStringBuilder.toString()
}

fun io.opentelemetry.proto.trace.v1.Span.SpanKind.toClaraSpanKind(): Option<Span.Kind> {
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
