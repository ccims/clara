package de.unistuttgart.iste.sqa.clara.utils.kotlinx

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.joinAll

suspend fun <A, B> Pair<Deferred<A>, Deferred<B>>.awaitBoth(): Pair<A, B> {
    listOf(this.first, this.second).joinAll()
    return Pair(this.first.await(), this.second.await())
}
