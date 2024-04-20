package de.unistuttgart.iste.sqa.clara.api.model

data class Library(
    val name: AggregatedComponent.Name,
    val version: AggregatedComponent.Internal.Version,
)

fun Library.toComponent(): Component {
    return Component.ExternalComponent(
        name = Component.Name(this.name.value),
        version = Component.Version(this.version.value),
        type = ComponentType.Library,
        domain = Domain(""),
    )
}