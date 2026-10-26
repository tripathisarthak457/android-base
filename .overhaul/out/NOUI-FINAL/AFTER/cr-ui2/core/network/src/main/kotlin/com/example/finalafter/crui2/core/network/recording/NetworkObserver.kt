package com.example.finalafter.crui2.core.network.recording

/**
 * Told about every HTTP exchange the app's clients make, answered or not. The seam an on-device
 * inspector records through, so this module never depends on one. Bound into a set: empty, nothing
 * is installed and a shipped build pays nothing for it.
 */
fun interface NetworkObserver {
    fun onExchange(exchange: HttpExchange)
}

/** One request and what came of it. Bodies are null when the request carried credentials. */
data class HttpExchange(
    val method: String,
    val url: String,
    /** Null when no answer came back; [failure] then says why. */
    val status: Int?,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val requestHeaders: Map<String, String>,
    val responseHeaders: Map<String, String>,
    val requestBody: String?,
    val responseBody: String?,
    /** The decoded size of the answer, counted from the body read, since a gzip or chunked answer has no length. */
    val responseBytes: Long,
    val failure: String?,
)
