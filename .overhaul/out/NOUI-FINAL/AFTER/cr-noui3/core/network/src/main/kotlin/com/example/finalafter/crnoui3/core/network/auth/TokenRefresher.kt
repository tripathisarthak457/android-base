package com.example.finalafter.crnoui3.core.network.auth

import com.example.finalafter.crnoui3.core.common.time.AppClock
import com.example.finalafter.crnoui3.core.network.client.CredentialsAttribute
import com.example.finalafter.crnoui3.core.network.client.NetworkConfig
import com.example.finalafter.crnoui3.core.network.client.NetworkJson
import com.example.finalafter.crnoui3.core.network.client.SkipAuthAttribute
import com.example.finalafter.crnoui3.core.network.di.PlainClient
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface TokenRefreshResult {

    data class Success(
        val accessToken: String,
        val refreshToken: String,
        val expiresAtEpochMillis: Long?,
    ) : TokenRefreshResult

    /** The server refused the refresh token, or there is no endpoint to refresh at: the session is over. */
    data object Refused : TokenRefreshResult

    /**
     * No answer, or one that says nothing about the session (a 408, a 429, a 5xx). The session
     * stands and the next request tries again.
     */
    data class Failed(val status: Int?) : TokenRefreshResult
}

/** Exchanges a refresh token for a new pair. */
fun interface TokenRefresher {
    suspend fun refresh(refreshToken: String): TokenRefreshResult
}

/**
 * POSTs `{"refresh_token": …}` to [NetworkConfig.refreshTokenPath] and reads `access_token`,
 * `refresh_token` and `expires_in` back. Only a 401 counts as a refusal; a backend that answers a
 * dead refresh token with another status maps it here.
 */
@Singleton
internal class KtorTokenRefresher @Inject constructor(
    @PlainClient private val client: HttpClient,
    private val config: NetworkConfig,
    private val clock: AppClock,
) : TokenRefresher {

    override suspend fun refresh(refreshToken: String): TokenRefreshResult {
        if (config.refreshTokenPath.isBlank()) return TokenRefreshResult.Refused
        return try {
            val response = client.post(config.resolvedBaseUrl + config.refreshTokenPath.trimStart('/')) {
                attributes.put(SkipAuthAttribute, true)
                attributes.put(CredentialsAttribute, true)
                setBody(TextContent(NetworkJson.encodeToString(RefreshRequest(refreshToken)), ContentType.Application.Json))
            }
            when {
                response.status.value == UNAUTHORIZED -> TokenRefreshResult.Refused
                !response.status.isSuccess() -> TokenRefreshResult.Failed(response.status.value)
                else -> {
                    val payload = NetworkJson.decodeFromString<RefreshResponse>(response.bodyAsText())
                    TokenRefreshResult.Success(
                        accessToken = payload.accessToken,
                        refreshToken = payload.refreshToken ?: refreshToken,
                        expiresAtEpochMillis = payload.expiresInSeconds
                            ?.let { (clock.now() + it.seconds).toEpochMilliseconds() },
                    )
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (noAnswer: IOException) {
            TokenRefreshResult.Failed(status = null)
        } catch (unreadable: IllegalArgumentException) {
            TokenRefreshResult.Failed(status = null)
        }
    }

    private companion object {
        const val UNAUTHORIZED = 401
    }
}

@Serializable
private data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
private data class RefreshResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long? = null,
)
