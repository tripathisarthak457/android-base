package com.base.app.core.network

/** Everything about the backend that varies between environments. */
data class NetworkConfig(
    val baseUrl: String,
    val webSocketUrl: String,
    val isDebug: Boolean,
    val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT,
    val connectTimeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT,
    val socketTimeoutMillis: Long = DEFAULT_SOCKET_TIMEOUT,
    /** Endpoint the refresh token is exchanged at, relative to [baseUrl]. */
    val refreshTokenPath: String = "",
    /** How many times a repeatable request is sent again after no answer or a transient one. */
    val maxRetries: Int = RetryPolicy.DEFAULT_MAX_RETRIES,
    /** Sent with every request, so the backend can tell app versions apart. Blank sends Ktor's own. */
    val userAgent: String = "",
    /**
     * Public-key pins per host, as `"sha256/…"` strings: `mapOf("api.example.com" to listOf(primary,
     * backup))`. Empty trusts the system's certificate authorities, which is right for most apps.
     * Pin only with a backup key already issued: a rotated certificate that matches no pin locks
     * every installed copy out until an update ships.
     */
    val certificatePins: Map<String, List<String>> = emptyMap(),
    /** The on-disk HTTP cache, which honours the server's Cache-Control and ETag headers. */
    val httpCacheBytes: Long = DEFAULT_HTTP_CACHE_BYTES,
    // <opt:devtools>
    /** Whether every request and response is kept for the on-device inspector. */
    val recordExchanges: Boolean = false,
    // </opt:devtools>
) {
    /**
     * [baseUrl] with a guaranteed trailing slash. Every call site joins a relative path onto this,
     * and a base URL typed without the trailing slash silently produces
     * `https://api.example.com/apiposts`.
     */
    val resolvedBaseUrl: String get() = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    /** [webSocketUrl] without a trailing slash, so a channel path appends cleanly. */
    val resolvedWebSocketUrl: String get() = webSocketUrl.trimEnd('/')

    companion object {
        const val DEFAULT_REQUEST_TIMEOUT = 30_000L
        const val DEFAULT_CONNECT_TIMEOUT = 15_000L
        const val DEFAULT_SOCKET_TIMEOUT = 30_000L
        const val DEFAULT_HTTP_CACHE_BYTES = 50L * 1024 * 1024
    }
}
