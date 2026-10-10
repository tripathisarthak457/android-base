package com.base.app.core.network.client

/** Everything about the backend that varies between environments. */
data class NetworkConfig(
    val baseUrl: String,
    val isDebug: Boolean,
    val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT,
    val connectTimeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT,
) {
    /**
     * [baseUrl] with a guaranteed trailing slash. Every call joins a relative path onto this, and a
     * base URL typed without the slash silently produces `https://api.example.com/apiposts`.
     */
    val resolvedBaseUrl: String get() = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    companion object {
        const val DEFAULT_REQUEST_TIMEOUT = 30_000L
        const val DEFAULT_CONNECT_TIMEOUT = 15_000L
    }
}
