package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.spanprovider

import de.unistuttgart.iste.sqa.clara.test_utils.TestData
import de.unistuttgart.iste.sqa.clara.test_utils.awaitBothInParallel
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.opentelemetry.proto.trace.v1.Span
import io.opentelemetry.proto.trace.v1.span
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import io.opentelemetry.proto.trace.v1.Span as OpenTelemetrySpan

class OpenTelemetrySpanProviderIntegrationTest : FreeSpec({

    "An OpenTelemetrySpanProvider" - {

        "should start correctly" {

            Fixture.setup(listenDuration = 5.seconds) {

                awaitBothInParallel(
                    { underTest.getSpans() },
                    { client.checkThatServerRespondsToPings() }
                )
            }
        }

        "should accept correct spans" {

            Fixture.setup(listenDuration = 5.seconds) {

                val serviceName = "test-service"
                val spansToSend = createRandomSpans(
                    numberOfSpans = 500,
                    seed = 9090909,
                    includeUnspecifiedSpanKind = false,
                )

                val (spans, _) = awaitBothInParallel(
                    { underTest.getSpans() },
                    { client.sendSpans(spansToSend, serviceName) }
                )

                spans shouldContainExactly spansToSend.map { it.toClaraSpan(serviceName).getOrNull() }
            }
        }

        "should reject incorrect spans" {

            Fixture.setup(listenDuration = 1.seconds) {

                val serviceName = "test-service"
                val spansToSend = createRandomSpans(
                    numberOfSpans = 50000,
                    seed = 9090909,
                    includeUnspecifiedSpanKind = true,
                )
                val expectedReceivedSpans = spansToSend.mapNotNull { it.toClaraSpan(serviceName).getOrNull() }

                val (actualReceivedSpans, _) = awaitBothInParallel(
                    { underTest.getSpans() },
                    {
                        client.checkThatServerRespondsToPings()
                        client.sendSpans(spansToSend, serviceName)
                    }
                )

                actualReceivedSpans shouldBeSameSizeAs expectedReceivedSpans

                // experimenting because kotests shouldContainExactly is extremely slow
                // TODO: finish

                measureTime {
                    expectedReceivedSpans.forEachIndexed { index, expectedSpan ->
                        actualReceivedSpans[index] shouldBe expectedSpan
                    }
                }.also { println("custom: $it") }

                measureTime {
                    actualReceivedSpans shouldContainExactly expectedReceivedSpans
                }.also { println("kotest shouldContainExactly: $it") }

                measureTime {
                    actualReceivedSpans shouldContainExactlyInAnyOrder expectedReceivedSpans
                }.also { println("kotest shouldContainExactlyInAnyOrder: $it") }
            }
        }
    }
})

fun createRandomSpans(numberOfSpans: Int, includeUnspecifiedSpanKind: Boolean, seed: Int): List<Span> {
    val random = Random(seed)

    return buildList(numberOfSpans) {
        repeat(numberOfSpans) {

            val spanName = TestData.names[random.nextInt(TestData.names.size)] + "-" + TestData.names[random.nextInt(TestData.names.size)]
            val spanKindChoices = if (includeUnspecifiedSpanKind) {
                listOf(
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_CLIENT,
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_SERVER,
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_PRODUCER,
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_CONSUMER,
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_INTERNAL,
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_UNSPECIFIED
                )
            } else {
                listOf(
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_CLIENT,
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_SERVER,
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_PRODUCER,
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_CONSUMER,
                    OpenTelemetrySpan.SpanKind.SPAN_KIND_INTERNAL,
                )
            }
            val spanKind = spanKindChoices.random(random)

            val span = span {
                name = spanName
                kind = spanKind
                // TODO: set other fields
            }

            add(span)
        }
    }
}
