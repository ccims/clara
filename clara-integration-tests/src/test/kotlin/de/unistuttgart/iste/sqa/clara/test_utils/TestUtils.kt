package de.unistuttgart.iste.sqa.clara.test_utils

import de.unistuttgart.iste.sqa.clara.utils.kotlinx.awaitBothInParallel
import kotlinx.coroutines.*
import java.net.ServerSocket
import kotlin.coroutines.coroutineContext

fun getAvailablePort(): Int {
    val serverSocket = ServerSocket(0)

    serverSocket.use {
        serverSocket.reuseAddress = true
        serverSocket.close()
    }

    return serverSocket.localPort
}

suspend fun <A, B> awaitBothInParallel(first: suspend CoroutineScope.() -> A, second: suspend CoroutineScope.() -> B, dispatcher: CoroutineDispatcher = Dispatchers.IO): Pair<A, B> {
    return withContext(coroutineContext) {
        Pair(
            async(dispatcher) { first() },
            async(dispatcher) { second() }
        ).awaitBothInParallel()
    }
}
