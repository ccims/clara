package de.unistuttgart.iste.sqa.clara

import de.unistuttgart.iste.sqa.clara.aggregation.AggregatorManager
import de.unistuttgart.iste.sqa.clara.aggregation.ParallelAggregationExecutor
import de.unistuttgart.iste.sqa.clara.api.aggregation.AggregationExecutor
import de.unistuttgart.iste.sqa.clara.api.export.ExportExecutor
import de.unistuttgart.iste.sqa.clara.api.filter.Filter
import de.unistuttgart.iste.sqa.clara.api.filter.Rule
import de.unistuttgart.iste.sqa.clara.api.merge.Merger
import de.unistuttgart.iste.sqa.clara.config.ClaraConfig
import de.unistuttgart.iste.sqa.clara.config.FilterConfig
import de.unistuttgart.iste.sqa.clara.config.MergeConfig
import de.unistuttgart.iste.sqa.clara.export.ExporterManager
import de.unistuttgart.iste.sqa.clara.export.ParallelExportExecutor
import de.unistuttgart.iste.sqa.clara.filter.DefaultFilter
import de.unistuttgart.iste.sqa.clara.filter.rules.RemoveComponentEndpoints
import de.unistuttgart.iste.sqa.clara.filter.rules.RemoveComponentsByName
import de.unistuttgart.iste.sqa.clara.filter.rules.RemoveVersions
import de.unistuttgart.iste.sqa.clara.merge.DefaultMerger
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
        val merger: Merger = DefaultMerger(config.merge.toDefaultMergerConfig())
        val filter: Filter = DefaultFilter()
        val exportExecutor: ExportExecutor = ParallelExportExecutor(ExporterManager(config.export))

        val aggregationResult = aggregationExecutor.aggregateAll()
        val aggregationFailures = aggregationResult.getLeft()

        if (aggregationFailures.isNotEmpty()) {
            log.error { "Errors while aggregating: \n${aggregationFailures.indentedEnumeration { it.format() }}" }
        }

        val aggregations = aggregationResult.getRight().toSet()

        val (mergeFailures, components, communications) = merger.merge(aggregations)

        if (mergeFailures.isNotEmpty()) {
            log.error { "Errors while merging the results of the different aggregators: \n${mergeFailures.indentedEnumeration { it.format() }}" }
        }

        log.info { "Found in total ${components.size} components and ${communications.size} communications" }

        val (filteredComponents, filteredCommunications) = filter.filter(components, communications, config.filter.toFilterRules())

        if (filteredComponents.isEmpty() && filteredCommunications.isEmpty() && !config.export.onEmpty) {
            log.info { "Skipping export" }
        } else {
            val exportFailures = exportExecutor.exportAll(filteredComponents, filteredCommunications)
            if (exportFailures.isNotEmpty()) {
                log.error { "Errors while exporting: \n${exportFailures.indentedEnumeration { it.format() }}" }
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

private fun MergeConfig.toDefaultMergerConfig(): DefaultMerger.Config {
    return DefaultMerger.Config(
        comparisonStrategy = when (this.comparisonStrategy) {
            MergeConfig.ComparisonStrategy.Prefix -> DefaultMerger.Config.ComparisonStrategy.Prefix
            MergeConfig.ComparisonStrategy.Suffix -> DefaultMerger.Config.ComparisonStrategy.Suffix
            MergeConfig.ComparisonStrategy.Contains -> DefaultMerger.Config.ComparisonStrategy.Contains
            MergeConfig.ComparisonStrategy.Equals -> DefaultMerger.Config.ComparisonStrategy.Equals
        },
        showMessagingCommunicationsDirectly = this.showMessagingCommunicationsDirectly
    )
}

private fun FilterConfig?.toFilterRules(): List<Rule> {
    if (this == null){
        return emptyList()
    }

    return buildList {

        if (this@toFilterRules.removeComponentEndpoints) {
            add(RemoveComponentEndpoints())
        }

        if (this@toFilterRules.removeVersions) {
            add(RemoveVersions())
        }

        this@toFilterRules.removeComponentsByNames.forEach { nameRegex ->
            add(RemoveComponentsByName(nameRegex))
        }
    }
}

private fun <T> Iterable<T>.indentedEnumeration(transform: (T) -> CharSequence): String {
    return this.joinToString(prefix = "    - ", separator = "\n    - ", transform = transform)
}
