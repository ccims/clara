package de.unistuttgart.iste.sqa.clara.export.gropius

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.expediagroup.graphql.client.spring.GraphQLWebClient
import de.unistuttgart.iste.sqa.clara.api.export.ExportFailure
import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.utils.kotlin.applyIf
import de.unistuttgart.iste.sqa.gropius.DeleteRelation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelOption
import kotlinx.coroutines.runBlocking
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.net.URL
import java.time.Duration

class GropiusExporter(private val config: Config) : Exporter {

    data class Config(
        val graphQLBackendUrl: URL,
        val graphQLBackendToken: String?,
    )

    private val log = KotlinLogging.logger {}

    override fun export(components: Set<Component>, communications: Set<Communication>): Option<ExportFailure> {
        log.info { "Export to Gropius ..." }

        val httpClient: HttpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(Duration.ofMillis(10_000))
        val webClientBuilder = WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient.wiretap(true)))
            .applyIf(config.graphQLBackendToken != null) {
                defaultHeader(
                    "Authorization",
                    "Bearer ${config.graphQLBackendToken}"
                )
            }

        val graphQLClient = GraphQLWebClient(
            url = config.graphQLBackendUrl.toString(),
            builder = webClientBuilder
        )

        val result = runBlocking {
            // TODO: run the correct queries and mutations
            graphQLClient.execute(DeleteRelation(DeleteRelation.Variables(id = "")))
        }

        val errors = result.errors
        val data = result.data

        if (errors != null) {
            return Some(ExportFailure("Gropius: errors while executing a GraphQL request: $errors"))
        }

        if (data == null) {
            log.warn { "Got no data from a GraphQL request" }
            return None
        }

        println(data) // TODO: handle response data

        log.info { "Done exporting to Gropius" }

        return None
    }
}
