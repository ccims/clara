package de.unistuttgart.iste.sqa.clara.api.model

data class Communication(val source: Source, val target: Target) {

    @JvmInline
    value class Source(val componentName: Component.Name)

    @JvmInline
    value class Target(val componentName: Component.Name)
}

data class AggregatedCommunication(val source: Source, val target: Target, val messagingSystem: MessagingSystem? = null) {

    @JvmInline
    value class Source(val componentName: AggregatedComponent.Name)

    @JvmInline
    value class Target(val componentName: AggregatedComponent.Name)

    @JvmInline
    value class MessagingSystem(val componentName: AggregatedComponent.Name)
}
