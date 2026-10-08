package com.base.app.core.network.auth

import com.base.app.core.network.NetworkConfig
import com.base.app.core.network.NetworkJson
import com.base.app.core.network.di.PlainClient
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TokenRefreshResult {

    data class Success(
        val accessToken: String,
        val refreshToken: String,
        val expiresAtEpochMillis: Long?,
    ) : TokenRefreshResult

    /**
     * [code] is null when the call never reached the server. That distinction decides whether the
     * session survives — see the refresh handler in `HttpClientModule`.
     */
    data class Failure(val code: Int?, val message: String?) : TokenRefreshResult
}

interface TokenRefresher {
    suspend fun refresh(refreshToken: String): TokenRefreshResult
}

/** Exchanges a refresh token for a new pair. */
@Singleton
class KtorTokenRefresher @Inject constructor(
    @PlainClient private val client: HttpClient,
    private val config: NetworkConfig,
) : TokenRefresher {

    override suspend fun refresh(refreshToken: String): TokenRefreshResult = try {
        val response = client.post(config.resolvedBaseUrl + config.refreshTokenPath.trimStart('/')) {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }
        val body = response.bodyAsText()

        if (!response.status.isSuccess()) {
            TokenRefreshResult.Failure(response.status.value, body.takeIf { it.isNotBlank() })
        } else {
            val payload = NetworkJson.decodeFromString<RefreshResponse>(body)
            TokenRefreshResult.Success(
                accessToken = payload.accessToken,
                refreshToken = payload.refreshToken ?: refreshToken,
                expiresAtEpochMillis = payload.expiresInSeconds
                    ?.let { System.currentTimeMillis() + it * MILLIS_PER_SECOND },
            )
        }
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        // No code: the request never got an answer, so the session is still presumed valid.
        TokenRefreshResult.Failure(code = null, message = throwable.message)
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
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

/** Broadcasts "this session is over" from the network layer to whatever is listening. */
@Singleton
class SessionEvents @Inject constructor() {

    private val _expired = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val expired: Flow<Unit> = _expired.asSharedFlow()

    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}
