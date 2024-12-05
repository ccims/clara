package de.unistuttgart.iste.sqa.clara.export.gropius.graphql

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.ConnectException
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

class GropiusGraphQLClient(
    private val backendUrl: URL,
    private val authenticationUrl: URL,
    private val clientId: String,
    private val clientSecret: String,
) : GraphQLClient {

    private val log = KotlinLogging.logger {}

    private lateinit var currentToken: Token
    private lateinit var graphQLClient: GraphQLClient

    override suspend fun <T : Any> execute(request: GraphQLClientRequest<T>): Either<GraphQLClient.RequestError, T> {
        if (!this::graphQLClient.isInitialized || !this::currentToken.isInitialized || currentToken.isExpired()) {
            log.debug { "Requesting a new Gropius-GraphQL authentication token ${if (this::currentToken.isInitialized && currentToken.isExpired()) "because the old one is expired " else ""}..." }
            currentToken = getAuthenticationToken().getOrElse { return it.left() }
            log.debug { "Got a new authentication token which expires in ${Duration.between(Instant.now(), currentToken.expiresAt).toKotlinDuration()}" }

            graphQLClient = SimpleGraphQLClient(backendUrl, currentToken)
        }

        return graphQLClient.execute(request)
    }

    private fun getAuthenticationToken(): Either<GraphQLClient.RequestError, Token> {
        val requestData = mapOf(
            "grant_type" to "client_credentials",
            "client_id" to clientId,
            "client_secret" to clientSecret,
        )

        val requestBody = requestData
            .map { (key, value) -> "$key=$value" }
            .reduce { param1: Any, param2: Any -> "$param1&$param2" }

        val request = HttpRequest.newBuilder()
            .uri(authenticationUrl.toURI())
            .header("content-type", "application/x-www-form-urlencoded")
            .POST(BodyPublishers.ofString(requestBody))
            .build()

        val httpClient = HttpClient.newHttpClient()

        val response = try {
            httpClient.send(request, BodyHandlers.ofString())
        } catch (ex: ConnectException) {
            return Either.Left(GraphQLClient.RequestError.ConnectionFailed(message = "Cannot connect to server to get an authentication token"))
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return Either.Left(GraphQLClient.RequestError.TokenError.InvalidAuthenticationData)
        }

        val parsedResponse = parseJson(response.body())

        val token = parsedResponse
            ?.getChecked("access_token")
            ?.asText()
            ?: return Either.Left(GraphQLClient.RequestError.TokenError.InvalidTokenResponse)

        val expiresInSeconds = parsedResponse
            .getChecked("expires_in")
            ?.asText()
            ?.toLongOrNull()
            ?: return Either.Left(GraphQLClient.RequestError.TokenError.InvalidTokenResponse)

        val expiresAt = Instant.now().plusSeconds(expiresInSeconds)

        return Token(token, expiresAt).right()
    }

    private fun parseJson(input: String): JsonNode? {
        return try {
            ObjectMapper().readTree(input)
        } catch (ex: JsonProcessingException) {
            null
        }
    }

    private fun JsonNode?.getChecked(key: String): JsonNode? {
        return try {
            this?.get(key)
        } catch (ex: JsonProcessingException) {
            null
        }
    }
}
