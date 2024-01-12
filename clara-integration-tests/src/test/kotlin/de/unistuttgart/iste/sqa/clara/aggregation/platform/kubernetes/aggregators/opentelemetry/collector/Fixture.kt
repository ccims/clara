package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.collector

import de.unistuttgart.iste.sqa.clara.test_utils.getAvailablePort
import kotlin.time.Duration

class Fixture(listenDuration: Duration) {

    private val serverPort = getAvailablePort()

    val underTest = OpenTelemetryTraceSpanProvider(OpenTelemetryTraceSpanProvider.Config(serverPort, listenDuration))
    val client = OpenTelemetrySpanProviderClient(serverPort, listenDuration)

    companion object {

        suspend fun setup(listenDuration: Duration, testFunction: suspend Fixture.() -> Unit) {
            val fixture = Fixture(listenDuration)
            fixture.client.use {
                testFunction(fixture)
                fixture.client.close()
            }
        }
    }
}
