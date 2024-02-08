package de.unistuttgart.iste.sqa.clara

import de.unistuttgart.iste.sqa.clara.aggregation.AggregatorManager
import de.unistuttgart.iste.sqa.clara.aggregation.ParallelAggregationExecutor
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationExecutor
import de.unistuttgart.iste.sqa.clara.api.export.ExportExecutor
import de.unistuttgart.iste.sqa.clara.config.ClaraConfig
import de.unistuttgart.iste.sqa.clara.export.ExporterManager
import de.unistuttgart.iste.sqa.clara.export.ParallelExportExecutor
import de.unistuttgart.iste.sqa.clara.utils.list.getLeft
import de.unistuttgart.iste.sqa.clara.utils.list.getRight
import io.github.oshai.kotlinlogging.KotlinLogging

class App(private val config: ClaraConfig) {

    val log = KotlinLogging.logger {}

    fun run() {
        AppInfo.printBanner()
        AppInfo.printBuildInformation()

        if (config.app?.logConfig == true) {
            log.info { "Configuration: $config" }
        }

        log.info { "Start application" }

        val aggregationExecutor: AggregationExecutor = ParallelAggregationExecutor(AggregatorManager(config.aggregation))
        val exportExecutor: ExportExecutor = ParallelExportExecutor(ExporterManager(config.export))

        val (componentAggregationResult, communicationAggregationResult) = aggregationExecutor.aggregateAll()
        val aggregationFailures = componentAggregationResult.getLeft().toMutableList().apply { addAll(componentAggregationResult.getLeft()) }

        val components = componentAggregationResult.getRight()
        val communications = communicationAggregationResult.getRight()

        if (aggregationFailures.isNotEmpty()) {
            log.error { "Errors while aggregating: \n${aggregationFailures.joinToString(prefix = "    - ", separator = "\n    - ") { it.description }}" }
        }

        log.info { "Found ${components.size} components and ${communications.size} communications" }

        if (communications.isEmpty() && components.isEmpty() && !config.export.onEmpty) {
            log.info { "Skipping export" }
        } else {
            val exportFailures = exportExecutor.exportAll(components, communications)
            if (exportFailures.isNotEmpty()) {
                log.error { "Errors while exporting: \n${exportFailures.joinToString(prefix = "    - ", separator = "\n    - ") { it.description }}" }
            }
        }

        log.info { "End application" }

        if (config.app?.blockAfterFinish == true) {
            log.info { "Keeping process alive from now on." }
            while (true) {}
        }
    }
}
