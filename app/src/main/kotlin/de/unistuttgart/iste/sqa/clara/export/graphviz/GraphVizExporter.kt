package de.unistuttgart.iste.sqa.clara.export.graphviz

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import de.unistuttgart.iste.sqa.clara.api.export.ExportFailure
import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.config.ExportConfig.Exporters.GraphViz
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories

class GraphVizExporter(private val config: GraphViz) : Exporter {

    private val log = KotlinLogging.logger {}

    override fun export(components: Set<Component>, communications: Set<Communication>): Option<ExportFailure> {
        log.info { "Exporting to GraphViz ..." }

        if (config.outputType !in GraphViz.ALLOWED_TYPES) {
            return Some(ExportFailure("Cannot export with Graphviz because the type '${config.outputType}' is not supported! Supported types are: ${GraphViz.ALLOWED_TYPES}"))
        }

        val graphvizCode = GraphVizCodeGenerator.generateDotCode(components, communications)

        log.trace { "Generated GraphViz code: \n$graphvizCode" }

        try {
            Path(config.outputFile).createParentDirectories()
        } catch (ex: IOException) {
            return Some(ExportFailure("Cannot create parent directories of output file '${config.outputFile}': ${ex.message}"))
        } catch (ex: FileSystemException) {
            return Some(ExportFailure("Cannot create parent directories of output file '${config.outputFile}': ${ex.message}"))
        }

        // TODO: handle errors (timeouts, return code, ...)
        val process = ProcessBuilder()
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .command("dot", "-T${config.outputType}", "-o", config.outputFile)
            .start()

        process.outputStream
            .bufferedWriter()
            .use {
                it.write(graphvizCode)
                it.write(System.lineSeparator())
                it.flush()
            }

        return None
    }
}
