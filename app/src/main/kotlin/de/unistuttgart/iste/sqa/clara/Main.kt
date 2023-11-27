package de.unistuttgart.iste.sqa.clara

import com.sksamuel.hoplite.ConfigLoader
import de.unistuttgart.iste.sqa.clara.aggregation.AggregatorManager
import de.unistuttgart.iste.sqa.clara.config.AppConfig
import de.unistuttgart.iste.sqa.clara.export.ExporterManager
import de.unistuttgart.iste.sqa.clara.utils.list.getLeft
import de.unistuttgart.iste.sqa.clara.utils.list.getRight
import io.github.oshai.kotlinlogging.KotlinLogging

fun main() {
    val log = KotlinLogging.logger {}

    log.info { "Start application" }

    val config = ConfigLoader().loadConfigOrThrow<AppConfig>("/config.yml")

    val aggregatorManager = AggregatorManager(config.aggregation)
    val exporterManager = ExporterManager(config.export)

    val (componentAggregationResult, communicationAggregationResult) = aggregatorManager.aggregateUsingAllAggregators()
    val aggregationFailures = componentAggregationResult.getLeft().toMutableList().apply { addAll(componentAggregationResult.getLeft()) }

    val components = componentAggregationResult.getRight()
    val communications = communicationAggregationResult.getRight()

    if (aggregationFailures.isNotEmpty()) {
        log.warn { "Errors while aggregating: \n${aggregationFailures.joinToString(prefix = "    - ", separator = "\n    - ")}" }
    }

    log.info { "Found ${components.size} components and ${communications.size} communications" }

    if (communications.isEmpty() && components.isEmpty() && !config.export.onEmpty) {
        log.info { "Skipping export" }
    } else {
        val exportFailures = exporterManager.exportUsingAllExporters(components, communications)
        if (exportFailures.isNotEmpty()) {
            log.warn { "Errors while exporting: \n${exportFailures.joinToString(prefix = "    - ", separator = "\n    - ")}" }
        }
    }
}
