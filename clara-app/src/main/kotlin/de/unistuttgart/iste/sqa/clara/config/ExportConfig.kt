package de.unistuttgart.iste.sqa.clara.config

import com.sksamuel.hoplite.ConfigAlias
import com.sksamuel.hoplite.Masked
import java.net.URL

/**
 * Config for the different exporters.
 *
 * @property onEmpty If true, export even when no data was aggregated.
 * @property exporters The different exporters.
 */
data class ExportConfig(
    val onEmpty: Boolean = false,
    val exporters: Exporters?,
) {

    data class Exporters(
        val graphviz: GraphViz?,
        val gropius: Gropius?,
    ) {

        /**
         * Config for exporting with GraphViz.
         *
         * @property enable Whether to enable this exporter.
         * @property outputFile The path and filename of the generated output by GraphViz.
         * @property outputType The type of the output, e.g. 'PNG' or 'SVG'. See [GraphViz Docs](https://graphviz.org/docs/outputs/).
         */
        data class GraphViz(
            override val enable: Boolean = true,
            val outputFile: String,
            val outputType: FileType,
        ) : Enable {

            @Suppress("unused")
            enum class FileType {

                BMP,
                DOT,
                GIF,
                JPG,
                JPEG,
                JSON,
                PDF,
                PNG,
                SVG,
                TIFF,
            }
        }

        /**
         * Config for exporting to Gropius.
         *
         * @property enable Whether to enable this exporter.
         * @property projectId ID of the gropius project to work on.
         * @property graphQLBackendUrl The URL of the Gropius GraphQL backend, like http://localhost:8080/graphql
         * @property graphQLBackendAuthentication The authentication details for the Gropius GraphQL backend.
         */
        data class Gropius(
            override val enable: Boolean = true,
            val projectId: String,
            @ConfigAlias("graphql-backend-url")
            val graphQLBackendUrl: URL,
            @ConfigAlias("graphql-backend-authentication")
            val graphQLBackendAuthentication: Authentication,
            @ConfigAlias("component-handling")
            val gropiusComponentHandling: ComponentHandling,
            @ConfigAlias("export-libraries")
            val exportLibraries: Boolean = true,
        ) : Enable {

            enum class ComponentHandling {
                Modify,
                Delete
            }

            data class Authentication(
                @ConfigAlias("authentication-url")
                val authenticationUrl: URL,
                @ConfigAlias("username")
                val userName: Masked,
                val password: Masked,
                val clientId: Masked,
            )
        }
    }
}
