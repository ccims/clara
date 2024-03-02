package de.unistuttgart.iste.sqa.clara.export

import de.unistuttgart.iste.sqa.clara.api.export.ExportExecutor
import de.unistuttgart.iste.sqa.clara.api.export.ExportFailure
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*

class ParallelExportExecutor(
    private val exporterManager: ExporterManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ExportExecutor {

    private val log = KotlinLogging.logger {}

    override fun exportAll(components: List<Component>, communications: List<Communication>): List<ExportFailure> {
        if (exporterManager.exporters.isEmpty()) {
            log.warn { "No exporters specified and enabled!" }
            return emptyList()
        }

        log.info { "Start export process ..." }

        if (components.isEmpty() && communications.isEmpty()) {
            log.warn { "Exporting without data! Consider removing the config property 'export.onEmpty' or setting it to false." }
        }

        val uniqueComponents = components.toSet()
        val uniqueCommunications = communications.toSet()

        val exportFailures = runBlocking {
            exporterManager
                .exporters
                .map { exporter ->
                    async(dispatcher) {
                        exporter
                            .export(uniqueComponents, uniqueCommunications)
                            .getOrNull()
                    }
                }
                .awaitAll()
                .filterNotNull()
        }

        log.info { "Finished export process" }

        return exportFailures
    }
}
