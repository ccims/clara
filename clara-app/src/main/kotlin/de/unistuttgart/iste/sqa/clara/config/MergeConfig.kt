package de.unistuttgart.iste.sqa.clara.config

data class MergeConfig(
    val comparisonStrategy: ComparisonStrategy,
    val showMessagingCommunicationsDirectly: Boolean,
) {

    enum class ComparisonStrategy {
        Prefix,
        Suffix,
        Contains,
        Equals,
    }
}
