package de.unistuttgart.iste.sqa.clara.config

import com.sksamuel.hoplite.ConfigAlias
import com.sksamuel.hoplite.Masked
import de.unistuttgart.iste.sqa.clara.config.ExportConfig.Exporters.GraphViz.Companion.ALLOWED_TYPES
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
         * @property outputType The type of the output, e.g. 'png' or 'svg'. Specified in the list [ALLOWED_TYPES] from the [GraphViz Docs](https://graphviz.org/docs/outputs/).
         */
        data class GraphViz(
            override val enable: Boolean = true,
            val outputFile: String,
            val outputType: String,
        ) : Enable {

            companion object {

                val ALLOWED_TYPES = listOf(
                    "bmp",
                    "cgimage",
                    "canon",
                    "dot",
                    "gv",
                    "xdot",
                    "xdot1.2",
                    "xdot1.4",
                    "eps",
                    "exr",
                    "fig",
                    "gd",
                    "gd2",
                    "gif",
                    "gtk",
                    "ico",
                    "imap",
                    "imap_np",
                    "ismap",
                    "cmap",
                    "cmapx",
                    "cmapx_np",
                    "jpg",
                    "jpeg",
                    "jpe",
                    "jp2",
                    "json",
                    "json0",
                    "dot_json",
                    "xdot_json",
                    "pdf",
                    "pic",
                    "pct",
                    "pict",
                    "plain",
                    "plain-text",
                    "png",
                    "pov",
                    "ps",
                    "ps2",
                    "psd",
                    "sgi",
                    "svg",
                    "svgz",
                    "tga",
                    "tiff",
                    "tk",
                    "vml",
                    "vrml",
                    "wbmp",
                    "webp",
                    "xlib",
                    "x11"
                )
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
        ) : Enable {

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
