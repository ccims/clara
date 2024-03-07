package de.unistuttgart.iste.sqa.clara.api.merge

import arrow.core.Option

open class MergeFailure(val scope: String, val description: String) {

    fun format() = "$scope: $description"
}

inline fun Option<MergeFailure>.onFailure(func: (Option<MergeFailure>) -> Unit) {
    this.onSome {
        func(this)
    }
}
