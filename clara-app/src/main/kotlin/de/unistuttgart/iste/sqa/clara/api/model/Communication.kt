package de.unistuttgart.iste.sqa.clara.api.model

data class Communication(val source: Source, val target: Target) {

    @JvmInline
    value class Source(val component: Component.Name)

    @JvmInline
    value class Target(val component: Component.Name)
}


data class AggregatedCommunication(val source: Source, val target: Target) {
    @JvmInline
    value class Source(val component: AggregatedComponent.Name)

    @JvmInline
    value class Target(val component: AggregatedComponent.Name)
}