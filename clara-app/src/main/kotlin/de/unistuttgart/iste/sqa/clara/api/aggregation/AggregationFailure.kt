package de.unistuttgart.iste.sqa.clara.api.aggregation

import arrow.core.Option

open class AggregationFailure(val scope: String, val description: String) {

    fun format() = "$scope: $description"
}

inline fun Option<AggregationFailure>.onFailure(func: (Option<AggregationFailure>) -> Unit) {
    this.onSome {
        func(this)
    }
}
