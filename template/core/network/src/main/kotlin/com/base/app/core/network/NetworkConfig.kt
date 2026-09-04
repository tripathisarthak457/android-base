package com.base.app.core.network

/**
 * Everything about the backend that varies between environments.
 *
 * Provided by the application module from its own `BuildConfig`, never read from one here. A
 * library module with a `BuildConfig` of its own behaves differently depending on which variant
 * compiled it, which is the kind of difference that only shows up in a release build — and it is
 * also why library modules in this project carry no product flavours at all.
 */
data class NetworkConfig(
    val baseUrl: String,
    val webSocketUrl: String,
    val isDebug: Boolean,
    val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT,
    val connectTimeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT,
    val socketTimeoutMillis: Long = DEFAULT_SOCKET_TIMEOUT,
    /**
     * Endpoint the refresh token is exchanged at, relative to [baseUrl].
     *
     * Blank disables automatic refresh entirely, which is the right setting for an API that
     * issues long-lived tokens — the alternative is a 401 handler that calls an endpoint that
     * does not exist and turns every expiry into a hang.
     */
    val refreshTokenPath: String = "",
) {
    /**
     * [baseUrl] with a guaranteed trailing slash.
     *
     * Every call site joins a relative path onto this, and a base URL typed without the trailing
     * slash silently produces `https://api.example.com/apiposts`. Normalising once here is the
     * difference between that being impossible and it being a config review nobody performs.
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
