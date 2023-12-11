package de.unistuttgart.iste.sqa.clara.export.graphviz

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import de.unistuttgart.iste.sqa.clara.api.export.ExportFailure
import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.config.ExportConfig.Exporters.GraphViz
import de.unistuttgart.iste.sqa.clara.utils.process.readError
import de.unistuttgart.iste.sqa.clara.utils.process.startChecked
import de.unistuttgart.iste.sqa.clara.utils.process.writeOutput
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class GraphVizExporter(private val config: GraphViz) : Exporter {

    private val log = KotlinLogging.logger {}

    override fun export(components: Set<Component>, communications: Set<Communication>): Option<ExportFailure> {
        log.info { "Exporting to GraphViz ..." }

        if (config.outputType !in GraphViz.ALLOWED_TYPES) {
            return Some(ExportFailure("GraphViz: Cannot export with GraphViz because the type '${config.outputType}' is not supported! Supported types are: ${GraphViz.ALLOWED_TYPES}"))
        }

        val graphVizVersion = getGraphVizVersion().getOrElse { return Some(it) }
        log.info { "Using GraphViz version: $graphVizVersion" }

        val graphvizCode = GraphVizCodeGenerator.generateDotCode(components, communications)

        log.trace { "Generated GraphViz code:\n$graphvizCode" }

        try {
            Path(config.outputFile).createParentDirectories()
        } catch (ex: IOException) {
            return Some(ExportFailure("GraphViz: Cannot create parent directories of output file '${config.outputFile}': ${ex.message}"))
        } catch (ex: FileSystemException) {
            return Some(ExportFailure("GraphViz: Cannot create parent directories of output file '${config.outputFile}': ${ex.message}"))
        }

        return executeExportProcess(graphvizCode)
    }

    private fun getGraphVizVersion(): Either<ExportFailure, String> {
        val versionAskProcess = ProcessBuilder("dot", "--version").startChecked()

        return versionAskProcess
            // for some reason the correct output is sent over the error stream
            .readError(timeout = 2.seconds)
            .mapLeft { ExportFailure("GraphViz: Cannot get GraphViz version: ${it.description}") }
            .map { it.substringAfter("version").trim() }
    }

    private fun executeExportProcess(code: String): Option<ExportFailure> {
        val exportProcess = ProcessBuilder("dot", "-T${config.outputType}", "-o", config.outputFile).startChecked()

        return exportProcess
            .writeOutput(timeout = 1.minutes) {
                write(code)
                write(System.lineSeparator())
                flush()
            }
            .map { ExportFailure("GraphViz: Cannot export using: ${it.description}") }
    }
}
