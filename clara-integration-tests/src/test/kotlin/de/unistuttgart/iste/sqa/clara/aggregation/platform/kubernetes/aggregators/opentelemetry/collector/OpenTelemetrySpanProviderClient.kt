package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.collector

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.grpc.PingServiceGrpcKt
import de.unistuttgart.iste.sqa.clara.grpc.ping
import io.grpc.Deadline
import io.grpc.ManagedChannelBuilder
import io.kotest.matchers.shouldBe
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpcKt
import io.opentelemetry.proto.collector.trace.v1.exportTraceServiceRequest
import io.opentelemetry.proto.common.v1.anyValue
import io.opentelemetry.proto.common.v1.keyValue
import io.opentelemetry.proto.resource.v1.resource
import io.opentelemetry.proto.trace.v1.Span
import io.opentelemetry.proto.trace.v1.resourceSpans
import io.opentelemetry.proto.trace.v1.scopeSpans
import java.io.Closeable
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration

class OpenTelemetrySpanProviderClient(serverPort: Int, deadline: Duration) : Closeable {

    private val channel = ManagedChannelBuilder.forAddress("127.0.0.1", serverPort).usePlaintext().build()

    private val traceServiceStub = TraceServiceGrpcKt.TraceServiceCoroutineStub(channel)
        .withDeadline(Deadline.after(deadline.inWholeMilliseconds, TimeUnit.MILLISECONDS))
        .withWaitForReady()

    private val pingServiceStub = PingServiceGrpcKt.PingServiceCoroutineStub(channel)
        .withDeadline(Deadline.after(deadline.inWholeMilliseconds, TimeUnit.MILLISECONDS))
        .withWaitForReady()

    suspend fun checkThatServerRespondsToPings() {
        val pingMessageToSend = ping {
            id = Random.nextLong()
            message = "hello"
        }

        val response = pingServiceStub.sendPing(pingMessageToSend)

        response.id shouldBe pingMessageToSend.id
        response.message shouldBe pingMessageToSend.message
    }

    suspend fun sendSpans(span: List<Span>, serviceName: String) {
        val request = exportTraceServiceRequest {
            resourceSpans.add(
                resourceSpans {
                    resource = resource {
                        attributes.add(
                            keyValue {
                                key = "service.name"
                                value = anyValue {
                                    stringValue = serviceName
                                }
                            }
                        )
                    }

                    scopeSpans.add(
                        scopeSpans {
                            spans.addAll(span)
                        }
                    )
                }
            )
        }

        val response = traceServiceStub.export(request) // TODO: check response
    }

    override fun close() {
        channel.shutdownNow()
    }
}
