package de.unistuttgart.iste.sqa.clara.api.export

import arrow.core.Either

open class ExportFailure(val scope: String, val description: String) {

    fun format() = "$scope: $description"
}

inline fun <F : ExportFailure, T> Either<F, T>.onFailure(func: (Either<F, T>) -> Unit) {
    this.onLeft {
        func(this)
    }
}
