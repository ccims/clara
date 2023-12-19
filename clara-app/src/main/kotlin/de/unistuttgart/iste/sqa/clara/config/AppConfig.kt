package de.unistuttgart.iste.sqa.clara.config

import com.sksamuel.hoplite.ConfigLoader

data class AppConfig(
    val aggregation: AggregationConfig,
    val export: ExportConfig,
) {

    companion object {

        fun loadFrom(vararg source: String): AppConfig {
            return ConfigLoader
                .builder()
                .strict()
                .build()
                .loadConfigOrThrow<AppConfig>(source.toList())
        }
    }
}
