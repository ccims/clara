package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.spanprovider

import de.unistuttgart.iste.sqa.clara.grpc.Ping
import de.unistuttgart.iste.sqa.clara.grpc.PingServiceGrpcKt
import de.unistuttgart.iste.sqa.clara.grpc.Pong
import de.unistuttgart.iste.sqa.clara.grpc.pong

class PingService : PingServiceGrpcKt.PingServiceCoroutineImplBase() {

    override suspend fun sendPing(request: Ping): Pong {
        return pong {
            id = request.id
            message = request.message
        }
    }
}
