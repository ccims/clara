package de.unistuttgart.iste.sqa.clara

import de.unistuttgart.iste.sqa.clara.aggregation.AggregatorManager
import de.unistuttgart.iste.sqa.clara.aggregation.ParallelAggregationExecutor
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationExecutor
import de.unistuttgart.iste.sqa.clara.api.export.ExportExecutor
import de.unistuttgart.iste.sqa.clara.api.merge.Merger
import de.unistuttgart.iste.sqa.clara.config.ClaraConfig
import de.unistuttgart.iste.sqa.clara.export.ExporterManager
import de.unistuttgart.iste.sqa.clara.export.ParallelExportExecutor
import de.unistuttgart.iste.sqa.clara.merge.DynamicMerger
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
        val merger: Merger = DynamicMerger()
        val exportExecutor: ExportExecutor = ParallelExportExecutor(ExporterManager(config.export))

        val aggregationResult = aggregationExecutor.aggregateAll()
        val aggregationFailures = aggregationResult.getLeft()

        if (aggregationFailures.isNotEmpty()) {
            log.error { "Errors while aggregating: \n${aggregationFailures.indentedEnumeration { it.description }}" }
        }

        val aggregations = aggregationResult.getRight().toSet()

        val (mergeFailures, components, communications) = merger.merge(aggregations)

        if (mergeFailures.isNotEmpty()) {
            log.error { "Errors while merging the results of the different aggregators: \n${mergeFailures.indentedEnumeration { it.description }}" }
        }

        log.info { "Found ${components.size} components and ${communications.size} communications" }

        if (communications.isEmpty() && components.isEmpty() && !config.export.onEmpty) {
            log.info { "Skipping export" }
        } else {
            val exportFailures = exportExecutor.exportAll(components, communications)
            if (exportFailures.isNotEmpty()) {
                log.error { "Errors while exporting: \n${exportFailures.indentedEnumeration { it.description }}" }
            }
        }

        log.info { "End application" }

        if (config.app?.blockAfterFinish == true) {
            log.info { "Keeping process alive from now on." }
            while (true) {
            }
        }
    }
}

private fun <T> Iterable<T>.indentedEnumeration(transform: (T) -> CharSequence): String {
    return this.joinToString(prefix = "    - ", separator = "\n    - ", transform = transform)
}
