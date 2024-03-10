package de.unistuttgart.iste.sqa.clara.config

data class FilterConfig(
    val removeComponentEndpoints: Boolean = false,
    val removeComponentsByNames: List<Regex> = emptyList(),
)
