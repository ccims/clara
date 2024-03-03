package de.unistuttgart.iste.sqa.clara.export.gropius

import de.unistuttgart.iste.sqa.clara.api.model.Component

data class GropiusComponent(
    val component: Component,
    val componentId: ComponentId,
    val componentVersionId: ComponentVersionId,
) {

    @JvmInline
    value class ComponentId(val value: String)

    @JvmInline
    value class ComponentVersionId(val value: String)
}
