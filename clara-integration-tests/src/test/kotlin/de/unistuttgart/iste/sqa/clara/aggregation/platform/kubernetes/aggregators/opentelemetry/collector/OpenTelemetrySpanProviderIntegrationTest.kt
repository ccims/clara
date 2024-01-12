package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.collector

import de.unistuttgart.iste.sqa.clara.test_utils.awaitBothInParallel
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.opentelemetry.proto.trace.v1.span
import kotlin.time.Duration.Companion.seconds
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

                // TODO: create more spans

                val serviceName = "test-service"
                val spanToSend = span {
                    name = "nice-span"
                    kind = OpenTelemetrySpan.SpanKind.SPAN_KIND_CLIENT
                }

                val (spans, _) = awaitBothInParallel(
                    { underTest.getSpans() },
                    { client.sendSpan(spanToSend, serviceName) }
                )

                spans shouldContainExactly listOf(spanToSend.toClaraSpan(serviceName).getOrNull())
            }
        }

        "should reject incorrect spans" {

            Fixture.setup(listenDuration = 5.seconds) {

                // TODO
            }
        }
    }
})
