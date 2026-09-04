package com.base.app.core.network.di

import android.util.Log
import com.base.app.core.datastore.AuthTokenStore
import com.base.app.core.network.NetworkConfig
import com.base.app.core.network.NetworkJson
import com.base.app.core.network.SkipAuthAttribute
import com.base.app.core.network.auth.SessionEvents
import com.base.app.core.network.auth.TokenRefreshResult
import com.base.app.core.network.auth.TokenRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HttpClientModule {

    @Provides
    @Singleton
    @PlainClient
    fun providePlainClient(config: NetworkConfig): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        installCommon(config)
    }

    /**
     * The client every repository uses.
     *
     * ## `expectSuccess = false`
     *
     * Ktor's default throws on any non-2xx, which turns a perfectly ordinary 404 or 422 into an
     * exception that has to be caught and re-inspected to recover the status and body. The client
     * layer here classifies failures deliberately (see `KtorNetworkClient.failure`), and it needs
     * the response, not a throwable.
     *
     * ## Refresh failure is classified before the session is ended
     *
     * A refresh that never reached the server — offline, timeout — leaves the session alone, so a
     * user in a tunnel is not signed out. Only a 4xx, which is the server explicitly refusing the
     * refresh token, ends it: that session is unrecoverable, and leaving the user signed in
     * against a token nothing will accept produces an app where every screen fails silently.
     */
    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedClient(
        config: NetworkConfig,
        tokenStore: AuthTokenStore,
        tokenRefresher: TokenRefresher,
        sessionEvents: SessionEvents,
    ): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        installCommon(config)

        if (config.refreshTokenPath.isNotBlank()) {
            install(Auth) {
                bearer {
                    loadTokens {
                        val access = tokenStore.accessToken() ?: return@loadTokens null
                        val refresh = tokenStore.refreshToken() ?: return@loadTokens null
                        BearerTokens(access, refresh)
                    }

                    refreshTokens {
                        val refresh = tokenStore.refreshToken() ?: return@refreshTokens null
                        when (val result = tokenRefresher.refresh(refresh)) {
                            is TokenRefreshResult.Success -> {
                                tokenStore.save(
                                    accessToken = result.accessToken,
                                    refreshToken = result.refreshToken,
                                    expiresAtEpochMillis = result.expiresAtEpochMillis,
                                )
                                BearerTokens(result.accessToken, result.refreshToken)
                            }

                            is TokenRefreshResult.Failure -> {
                                if (result.code in REFUSED_RANGE) {
                                    tokenStore.clear()
                                    sessionEvents.notifyExpired()
                                }
                                null
                            }
                        }
                    }

                    // A request marked requiresAuth=false never receives a token, which is what
                    // keeps sign-in and refresh from carrying a stale one. Everything else gets
                    // it proactively rather than waiting for a 401, so the common case costs one
                    // round trip instead of two.
                    sendWithoutRequest { request ->
                        request.attributes.getOrNull(SkipAuthAttribute) != true
                    }
                }
            }
        }
    }

    private fun HttpClientConfig<*>.installCommon(config: NetworkConfig) {
        install(ContentNegotiation) { json(NetworkJson) }

        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMillis
            connectTimeoutMillis = config.connectTimeoutMillis
            socketTimeoutMillis = config.socketTimeoutMillis
        }

        if (config.isDebug) {
            install(Logging) {
                logger = LogcatLogger
                level = LogLevel.BODY
            }
        }
    }

    private val REFUSED_RANGE = 400..499
}

/**
 * Debug-only request logging. Filter Logcat by `KtorApi`, or run `adb logcat -s KtorApi`.
 *
 * Split on Logcat's ~4000-character per-entry limit, because a JSON body longer than that is
 * silently truncated otherwise — and the part that gets cut is always the end, which is where the
 * interesting field turns out to be.
 */
private object LogcatLogger : Logger {

    override fun log(message: String) {
        if (message.length <= CHUNK) {
            Log.i(TAG, message)
            return
        }
        var start = 0
        while (start < message.length) {
            val end = minOf(start + CHUNK, message.length)
            Log.i(TAG, message.substring(start, end))
            start = end
        }
    }

    private const val TAG = "KtorApi"
    private const val CHUNK = 4000
}
