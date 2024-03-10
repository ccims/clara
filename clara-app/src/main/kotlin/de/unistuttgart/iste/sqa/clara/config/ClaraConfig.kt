package de.unistuttgart.iste.sqa.clara.config

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ExperimentalHoplite

data class ClaraConfig(
    val app: AppConfig?,
    val aggregation: AggregationConfig,
    val merge: MergeConfig,
    val filter: FilterConfig?,
    val export: ExportConfig,
) {

    companion object {

        @OptIn(ExperimentalHoplite::class)
        fun loadFrom(vararg source: String): ClaraConfig {
            return ConfigLoader
                .builder()
                .strict()
                .withExplicitSealedTypes()
                .build()
                .loadConfigOrThrow<ClaraConfig>(source.toList())
        }
    }
}
