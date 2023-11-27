package de.unistuttgart.iste.sqa.clara.export

import de.unistuttgart.iste.sqa.clara.api.export.ExportFailure
import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.config.ExportConfig
import de.unistuttgart.iste.sqa.clara.config.ifEnabled
import de.unistuttgart.iste.sqa.clara.export.graphviz.GraphVizExporter
import io.github.oshai.kotlinlogging.KotlinLogging

class ExporterManager(config: ExportConfig) {

    private val log = KotlinLogging.logger {}

    private val exporters: List<Exporter> = buildList {
        config.exporters?.graphViz?.ifEnabled {
            add(GraphVizExporter(it))
            log.info { "Registered exporter: Graphviz" }
        }

        config.exporters?.gropius?.ifEnabled {
            log.info { "Registered exporter: Gropius (Will be added soon!)" }
        }
    }

    fun exportUsingAllExporters(components: List<Component>, communications: List<Communication>): List<ExportFailure> {
        if (exporters.isEmpty()) {
            log.warn { "No exporters specified and enabled!" }
            return emptyList()
        }

        log.info { "Start export process ..." }

        if (components.isEmpty() && communications.isEmpty()) {
            log.warn { "Exporting without data! Consider removing the config property 'export.onEmpty' or setting it to false." }
        }

        val uniqueComponents = components.toSet()
        val uniqueCommunications = communications.toSet()

        return exporters
            .mapNotNull { it.export(uniqueComponents, uniqueCommunications).getOrNull() }
            .also {
                log.info { "Finished export process" }
            }
    }
}
