package de.unistuttgart.iste.sqa.clara.api.export

import arrow.core.Option

open class ExportFailure(val scope: String, val description: String) {

    fun format() = "$scope: $description"
}

inline fun Option<ExportFailure>.onFailure(func: (Option<ExportFailure>) -> Unit) {
    this.onSome {
        func(this)
    }
}
