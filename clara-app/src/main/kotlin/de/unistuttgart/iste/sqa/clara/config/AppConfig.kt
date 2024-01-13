package de.unistuttgart.iste.sqa.clara.config

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ExperimentalHoplite

data class AppConfig(
    val aggregation: AggregationConfig,
    val export: ExportConfig,
) {

    companion object {

        @OptIn(ExperimentalHoplite::class)
        fun loadFrom(vararg source: String): AppConfig {
            return ConfigLoader
                .builder()
                .strict()
                .withExplicitSealedTypes()
                .build()
                .loadConfigOrThrow<AppConfig>(source.toList())
        }
    }
}
