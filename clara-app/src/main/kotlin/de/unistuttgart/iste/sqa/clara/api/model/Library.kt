package de.unistuttgart.iste.sqa.clara.api.model

data class Library(
    val name: AggregatedComponent.Name,
    val version: AggregatedComponent.Internal.Version
)
