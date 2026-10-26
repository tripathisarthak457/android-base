package com.example.finalafter.crnoui.core.network.di

import android.content.Context
import com.example.finalafter.crnoui.core.common.coroutines.AppDispatchers
import com.example.finalafter.crnoui.core.common.session.SessionScopedStore
import com.example.finalafter.crnoui.core.network.auth.SessionTokens
import com.example.finalafter.crnoui.core.network.auth.authenticate
import com.example.finalafter.crnoui.core.network.client.CredentialsAttribute
import com.example.finalafter.crnoui.core.network.client.NetworkConfig
import com.example.finalafter.crnoui.core.network.recording.NetworkObserver
import com.example.finalafter.crnoui.core.network.recording.installRecording
import com.example.finalafter.crnoui.core.network.recording.recordFailures
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

/** Sends the session's token and renews it on a 401. What every repository's calls go through. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

/** Sends no token. Used by the token refresh itself, which would otherwise recurse on its own 401. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlainClient

/** Opens WebSockets: no request timeout, which would cut a healthy idle socket off, and no recorder. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RealtimeClient

@Module
@InstallIn(SingletonComponent::class)
internal object HttpClientModule {

    /**
     * The one OkHttp client. Every Ktor client and the image loader are built on it, so they share
     * one connection pool, one set of TLS sessions and HTTP/2 connections to the API host, one thread
     * pool, and the certificate pins.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(config: NetworkConfig, cache: Cache): OkHttpClient = OkHttpClient.Builder()
        .cache(cache)
        .apply { if (config.certificatePins.isNotEmpty()) certificatePinner(config.certificatePinner()) }
        .build()

    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedClient(
        okHttp: OkHttpClient,
        config: NetworkConfig,
        tokens: SessionTokens,
        observers: Set<@JvmSuppressWildcards NetworkObserver>,
    ): HttpClient = HttpClient(OkHttp) {
        installCommon(okHttp, config)
        installRecording(observers)
    }.apply {
        recordFailures(observers)
        authenticate(apiHost = Url(config.resolvedBaseUrl).host, tokens = tokens)
    }

    @Provides
    @Singleton
    @PlainClient
    fun providePlainClient(
        okHttp: OkHttpClient,
        config: NetworkConfig,
        observers: Set<@JvmSuppressWildcards NetworkObserver>,
    ): HttpClient = HttpClient(OkHttp) {
        installCommon(okHttp, config)
        installRecording(observers)
    }.apply { recordFailures(observers) }

    @Provides
    @Singleton
    @RealtimeClient
    fun provideRealtimeClient(okHttp: OkHttpClient, config: NetworkConfig): HttpClient = HttpClient(OkHttp) {
        engine { preconfigured = okHttp }
        // Without pings a connection a NAT dropped in silence is never noticed, and the socket waits
        // for frames that cannot arrive.
        install(WebSockets) { pingInterval = PING_INTERVAL }
        installUserAgent(config)
    }

    /**
     * Responses the server marks cacheable, kept on disk and revalidated with ETag or Last-Modified,
     * so an unchanged resource costs a 304 and no body. In the cache directory, which the system may
     * empty under storage pressure: it is a cache, not a store.
     */
    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context, config: NetworkConfig): Cache =
        Cache(File(context.cacheDir, HTTP_CACHE_DIRECTORY), config.httpCacheBytes)

    /** Responses to signed-in requests belong to that person, so they go with the rest on sign-out. */
    @Provides
    @IntoSet
    fun provideHttpCacheClearing(cache: Cache, dispatchers: AppDispatchers): SessionScopedStore =
        SessionScopedStore { withContext(dispatchers.io) { cache.evictAll() } }

    private fun HttpClientConfig<OkHttpConfig>.installCommon(okHttp: OkHttpClient, config: NetworkConfig) {
        // Ktor's default throws on any non-2xx, which turns an ordinary 404 or 422 into an exception
        // that has to be caught to recover the status and the body.
        expectSuccess = false
        engine { preconfigured = okHttp }

        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMillis
            connectTimeoutMillis = config.connectTimeoutMillis
            socketTimeoutMillis = config.socketTimeoutMillis
        }
        installUserAgent(config)
        if (config.appVersionCode > 0) {
            install(DefaultRequest) { header(APP_VERSION_HEADER, config.appVersionCode.toString()) }
        }
        if (config.isDebug) {
            install(Logging) {
                logger = LogcatLogger
                level = LogLevel.BODY
                // Debug builds still end up in bug reports and shared screen recordings.
                sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
                filter { request -> request.attributes.getOrNull(CredentialsAttribute) != true }
            }
        }
    }

    /**
     * OkHttp refuses a header value outside printable ASCII, and some phones' model names have
     * characters beyond it: with them in the User-Agent, every request on that phone would fail.
     */
    private fun HttpClientConfig<*>.installUserAgent(config: NetworkConfig) {
        val agent = config.userAgent.filter { it in ' '..'~' }.trim()
        if (agent.isNotEmpty()) install(UserAgent) { this.agent = agent }
    }

    private fun NetworkConfig.certificatePinner(): CertificatePinner = CertificatePinner.Builder().apply {
        certificatePins.forEach { (host, pins) -> add(host, *pins.toTypedArray()) }
    }.build()

    private const val HTTP_CACHE_DIRECTORY = "http"
    private const val APP_VERSION_HEADER = "X-App-Version"
    private val PING_INTERVAL = 20.seconds
}

/** Debug-only request logging. Filter Logcat by `KtorApi`, or run `adb logcat -s KtorApi`. */
private object LogcatLogger : Logger {

    override fun log(message: String) {
        message.chunked(CHUNK).forEach { android.util.Log.i(TAG, it) }
    }

    private const val TAG = "KtorApi"

    // Logcat truncates a line at about 4 KB; a long body is split rather than cut off.
    private const val CHUNK = 4000
}
