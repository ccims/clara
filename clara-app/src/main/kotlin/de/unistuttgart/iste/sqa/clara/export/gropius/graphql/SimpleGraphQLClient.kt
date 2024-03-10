package de.unistuttgart.iste.sqa.clara.export.gropius.graphql

import arrow.core.Either
import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import de.unistuttgart.iste.sqa.clara.utils.kotlin.applyIf
import io.netty.channel.ChannelOption
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.net.URL
import java.time.Duration

class SimpleGraphQLClient(graphQLServerUrl: URL, authorizationToken: Token?) : GraphQLClient {

    private val httpClient: HttpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
        .responseTimeout(Duration.ofMillis(10_000))

    private val webClientBuilder = WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(httpClient.wiretap(true)))
        .applyIf(authorizationToken != null) {
            defaultHeader(
                "Authorization",
                "Bearer ${authorizationToken?.value}"
            )
        }

    private val graphQLClient = GraphQLWebClient(
        url = graphQLServerUrl.toString(),
        builder = webClientBuilder
    )

    override suspend fun <T : Any> execute(request: GraphQLClientRequest<T>): Either<GraphQLClient.RequestError, T> {
        val response = runCatching {
            graphQLClient.execute(request)
        }.getOrElse { errors ->
            return Either.Left(GraphQLClient.RequestError.GraphQLRequestFailed(errors.message?.let { listOf(it) } ?: emptyList()))
        }

        val errors = response.errors
        val data = response.data

        if (!errors.isNullOrEmpty()) {
            return Either.Left(GraphQLClient.RequestError.GraphQLRequestFailed(errors.map { it.toString() }))
        }

        if (data == null) {
            throw GraphQLException("Received null data without errors!")
        }

        return Either.Right(data)
    }
}

private class GraphQLException(message: String) : RuntimeException(message)
