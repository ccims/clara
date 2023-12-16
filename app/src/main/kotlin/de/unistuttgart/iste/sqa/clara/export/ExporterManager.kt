package de.unistuttgart.iste.sqa.clara.export

import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.config.ExportConfig
import de.unistuttgart.iste.sqa.clara.config.ifEnabled
import de.unistuttgart.iste.sqa.clara.export.graphviz.GraphVizExporter
import io.github.oshai.kotlinlogging.KotlinLogging

class ExporterManager(config: ExportConfig) {

    private val log = KotlinLogging.logger {}

    val exporters: List<Exporter> = buildList {
        config.exporters?.graphViz?.ifEnabled {
            add(GraphVizExporter(it))
            log.info { "Registered exporter: Graphviz" }
        }

        config.exporters?.gropius?.ifEnabled {
            log.info { "Registered exporter: Gropius (Will be added soon!)" }
        }
    }
}
