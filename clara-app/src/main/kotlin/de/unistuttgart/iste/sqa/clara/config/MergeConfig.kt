package de.unistuttgart.iste.sqa.clara.config

data class MergeConfig(
    val comparisonStrategy: ComparisonStrategy,
) {

    enum class ComparisonStrategy {
        Prefix,
        Suffix,
        Contains,
        Equals,
    }
}
