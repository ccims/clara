package de.unistuttgart.iste.sqa.clara.api.model

data class Communication(val source: Source, val target: Target) {

    @JvmInline
    value class Source(val ref: Reference)

    @JvmInline
    value class Target(val ref: Reference)
}
