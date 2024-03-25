package de.unistuttgart.iste.sqa.clara.export.graphviz

import arrow.core.Either
import arrow.core.getOrElse
import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.utils.process.readError
import de.unistuttgart.iste.sqa.clara.utils.process.startChecked
import de.unistuttgart.iste.sqa.clara.utils.process.writeOutput
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.time.Duration.Companion.seconds

class GraphVizExporter(private val config: Config) : Exporter {

    data class Config(
        val outputType: String,
        val outputFile: String,
    )

    private val log = KotlinLogging.logger {}

    override fun export(components: Set<Component>, communications: Set<Communication>): Either<GraphVizExportFailure, Unit> {
        log.info { "Export to GraphViz ..." }

        val graphVizVersion = getGraphVizVersion().getOrElse { return Either.Left(it) }
        log.info { "Using GraphViz version: $graphVizVersion" }

        val graphvizCode = GraphVizCodeGenerator.generateDotCode(components, communications)

        log.trace { "Generated GraphViz code:\n$graphvizCode" }

        try {
            Path(config.outputFile).normalize().createParentDirectories()
        } catch (ex: IOException) {
            return Either.Left(GraphVizExportFailure("Cannot create parent directories of output file '${config.outputFile}': ${ex.message}"))
        } catch (ex: FileSystemException) {
            return Either.Left(GraphVizExportFailure("Cannot create parent directories of output file '${config.outputFile}': ${ex.message}"))
        }

        val maybeExportFailure = executeExportProcess(graphvizCode)

        log.info { "Done exporting to GraphViz" }

        return maybeExportFailure
    }

    private fun getGraphVizVersion(): Either<GraphVizExportFailure, String> {
        val versionAskProcess = ProcessBuilder("dot", "--version").startChecked()

        return versionAskProcess
            // for some reason the correct output is sent over the error stream
            .readError(timeout = 5.seconds)
            .mapLeft { GraphVizExportFailure("Cannot get GraphViz version: ${it.description}") }
            .map { it.substringAfter("version").trim() }
    }

    private fun executeExportProcess(code: String): Either<GraphVizExportFailure, Unit> {
        val exportProcess = ProcessBuilder("dot", "-T${config.outputType}", "-o", config.outputFile).startChecked()

        return exportProcess
            .writeOutput(timeout = 30.seconds) {
                write(code)
                write(System.lineSeparator())
                flush()
            }
            .toEither {}
            .swap()
            .mapLeft { GraphVizExportFailure("Cannot export: ${it.description}") }
    }
}
