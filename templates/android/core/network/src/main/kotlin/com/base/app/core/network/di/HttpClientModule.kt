package com.base.app.core.network.di

import android.content.Context
import android.util.Log
import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.coroutines.IoDispatcher
import com.base.app.core.datastore.AuthTokenStore
// <opt:devtools>
import com.base.app.core.devtools.DevToolsLog
import com.base.app.core.network.installRecording
// </opt:devtools>
import com.base.app.core.network.NetworkConfig
import com.base.app.core.network.NetworkJson
import com.base.app.core.network.RetryPolicy
import com.base.app.core.network.SkipAuthAttribute
import com.base.app.core.network.auth.SessionEvents
import com.base.app.core.network.auth.TokenRefreshResult
import com.base.app.core.network.auth.TokenRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CertificatePinner
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HttpClientModule {

    @Provides
    @Singleton
    @PlainClient
    fun providePlainClient(
        config: NetworkConfig,
        // <opt:devtools>
        devToolsLog: DevToolsLog,
        // </opt:devtools>
    ): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        installCommon(config)
        // <opt:devtools>
        installRecording(config, devToolsLog)
        // </opt:devtools>
    }

    /**
     * The client every repository uses. Ktor's default throws on any non-2xx, which turns a
     * perfectly ordinary 404 or 422 into an exception that has to be caught and re-inspected to
     * recover the status and body.
     */
    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedClient(
        config: NetworkConfig,
        httpCache: Cache,
        tokenStore: AuthTokenStore,
        tokenRefresher: TokenRefresher,
        sessionEvents: SessionEvents,
        // <opt:devtools>
        devToolsLog: DevToolsLog,
        // </opt:devtools>
    ): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        engine {
            config {
                cache(httpCache)
                if (config.certificatePins.isNotEmpty()) certificatePinner(config.certificatePinner())
            }
        }
        installCommon(config)
        // <opt:devtools>
        installRecording(config, devToolsLog)
        // </opt:devtools>

        install(Auth) {
            bearer {
                loadTokens {
                    val access = tokenStore.accessToken() ?: return@loadTokens null
                    BearerTokens(access, tokenStore.refreshToken())
                }

                refreshTokens {
                    // A 401 on a request sent without a token, such as a wrong password at sign-in,
                    // says nothing about the session.
                    if (response.call.request.attributes.getOrNull(SkipAuthAttribute) == true) {
                        return@refreshTokens null
                    }
                    val refresh = tokenStore.refreshToken()
                    if (config.refreshTokenPath.isBlank() || refresh == null) {
                        // Nothing to refresh with, so the server has refused the only token there
                        // is. Only a session that had one can end.
                        if (oldTokens != null) endSession(tokenStore, sessionEvents)
                        return@refreshTokens null
                    }
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
                            if (result.code in REFUSED_RANGE) endSession(tokenStore, sessionEvents)
                            null
                        }
                    }
                }

                // A request marked requiresAuth=false never receives a token, which is what keeps
                // sign-in and refresh from carrying a stale one; nor does any host but the app's own.
                val apiHost = Url(config.resolvedBaseUrl).host
                sendWithoutRequest { request ->
                    request.attributes.getOrNull(SkipAuthAttribute) != true && request.url.host == apiHost
                }
            }
        }
    }

    private suspend fun endSession(tokenStore: AuthTokenStore, sessionEvents: SessionEvents) {
        tokenStore.clear()
        sessionEvents.notifyExpired()
    }

    @Provides
    @Singleton
    fun provideRetryPolicy(config: NetworkConfig): RetryPolicy = RetryPolicy(maxRetries = config.maxRetries)

    /**
     * Responses the server marks cacheable, kept on disk and revalidated with ETag or
     * Last-Modified, so an unchanged resource costs a 304 and no body. In the cache directory,
     * which the system may empty under storage pressure — it is a cache, not a store.
     */
    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context, config: NetworkConfig): Cache =
        Cache(File(context.cacheDir, HTTP_CACHE_DIRECTORY), config.httpCacheBytes)

    /** Responses to signed-in requests belong to that user, so they go with the rest on sign-out. */
    @Provides
    @IntoSet
    fun provideHttpCacheClearing(
        httpCache: Cache,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): SessionScopedStore = SessionScopedStore { withContext(ioDispatcher) { httpCache.evictAll() } }

    private fun NetworkConfig.certificatePinner(): CertificatePinner = CertificatePinner.Builder().apply {
        certificatePins.forEach { (host, pins) -> add(host, *pins.toTypedArray()) }
    }.build()

    private fun HttpClientConfig<*>.installCommon(config: NetworkConfig) {
        install(ContentNegotiation) { json(NetworkJson) }

        if (config.userAgent.isNotBlank()) {
            install(UserAgent) { agent = config.userAgent }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMillis
            connectTimeoutMillis = config.connectTimeoutMillis
            socketTimeoutMillis = config.socketTimeoutMillis
        }

        if (config.isDebug) {
            install(Logging) {
                logger = LogcatLogger
                level = LogLevel.BODY
                // Debug builds still end up in bug reports and shared screen recordings.
                sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
            }
        }
    }

    private val REFUSED_RANGE = 400..499
    private const val HTTP_CACHE_DIRECTORY = "http"
}

/** Debug-only request logging. Filter Logcat by `KtorApi`, or run `adb logcat -s KtorApi`. */
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
