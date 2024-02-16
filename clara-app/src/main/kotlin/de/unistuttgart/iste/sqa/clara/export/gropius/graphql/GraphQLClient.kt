package de.unistuttgart.iste.sqa.clara.export.gropius.graphql

import arrow.core.Either
import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientRequest

interface GraphQLClient {

    sealed interface RequestError {

        sealed interface TokenError : RequestError {

            data object InvalidAuthenticationData : TokenError

            data object InvalidTokenResponse : TokenError
        }

        @JvmInline
        value class GraphQLRequestFailed(val errors: List<GraphQLClientError>) : RequestError
    }

    suspend fun <T : Any> execute(request: GraphQLClientRequest<T>): Either<RequestError, T>
}
