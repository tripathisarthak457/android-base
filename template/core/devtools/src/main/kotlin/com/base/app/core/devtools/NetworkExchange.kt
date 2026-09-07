package com.base.app.core.devtools

/**
 * One request and what came back, as the inspector shows it.
 *
 * Headers are stored already redacted — see [DevToolsLog.record]. Keeping the raw values and
 * hiding them at render time would mean a bearer token living in memory for the lifetime of the
 * log and appearing in any screenshot of a stack trace, which is not a trade worth making for a
 * value nobody needs to read.
 */
data class NetworkExchange(
    /** Assigned by [DevToolsLog.record]; whatever a caller passes is replaced. */
    val id: Long = UNASSIGNED_ID,
    val method: String,
    val url: String,
    val status: Int?,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val requestHeaders: Map<String, String>,
    val responseHeaders: Map<String, String>,
    val requestBody: String?,
    val responseBody: String?,
    val responseBytes: Long,
    val failure: String?,
) {
    val path: String
        get() = runCatching { url.substringAfter("://").substringAfter('/').substringBefore('?') }
            .getOrDefault(url)
            .let { if (it.isBlank()) "/" else "/$it" }

    val isFailure: Boolean get() = failure != null || (status ?: 0) >= HTTP_ERROR_FLOOR

    companion object {
        const val UNASSIGNED_ID = 0L
        private const val HTTP_ERROR_FLOOR = 400
    }
}

/** What the Stats tab shows. Derived rather than accumulated, so clearing the log resets it. */
data class NetworkStats(
    val total: Int,
    val failures: Int,
    val medianMillis: Long,
    val slowestMillis: Long,
    val bytesReceived: Long,
) {
    val failureRate: Float get() = if (total == 0) 0f else failures.toFloat() / total

    companion object {
        fun of(exchanges: List<NetworkExchange>): NetworkStats {
            if (exchanges.isEmpty()) return NetworkStats(0, 0, 0, 0, 0)
            val durations = exchanges.map { it.durationMillis }.sorted()
            return NetworkStats(
                total = exchanges.size,
                failures = exchanges.count { it.isFailure },
                // The median rather than the mean: one 30-second timeout drags an average far
                // enough that the number stops describing any request that actually happened.
                medianMillis = durations[durations.size / 2],
                slowestMillis = durations.last(),
                bytesReceived = exchanges.sumOf { it.responseBytes },
            )
        }
    }
}
