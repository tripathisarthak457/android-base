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
    }
}
