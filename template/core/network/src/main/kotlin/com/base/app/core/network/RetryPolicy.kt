package com.base.app.core.network

import com.base.app.core.network.model.HttpMethodType
import kotlin.random.Random

/**
 * Whether a request that got no answer, or a transient one, is sent again, and how long to wait.
 *
 * Only methods that are safe to repeat are retried: sending a POST twice can create two orders.
 * The wait doubles each time with random jitter, so a million clients that failed together do not
 * all come back in the same second — which is how a brief outage becomes a long one. A server that
 * says when to come back with `Retry-After` is believed, up to [maxDelayMillis].
 */
class RetryPolicy(
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val baseDelayMillis: Long = 400,
    private val maxDelayMillis: Long = 8_000,
    private val random: Random = Random.Default,
) {

    fun isRepeatable(method: HttpMethodType): Boolean = method in REPEATABLE

    fun isTransient(statusCode: Int): Boolean = statusCode in TRANSIENT_STATUSES

    /** How long to wait before retry number [attempt] (0 for the first), or null to stop. */
    fun delayBefore(attempt: Int, method: HttpMethodType, statusCode: Int?, retryAfter: String?): Long? {
        if (attempt >= maxRetries || !isRepeatable(method)) return null
        if (statusCode != null && !isTransient(statusCode)) return null

        retryAfter?.trim()?.toLongOrNull()?.let { seconds ->
            return (seconds * MILLIS_PER_SECOND).coerceIn(0, maxDelayMillis)
        }
        val ceiling = (baseDelayMillis shl attempt.coerceAtMost(MAX_SHIFT)).coerceAtMost(maxDelayMillis)
        // Half fixed, half random: never a zero wait, never everyone at once.
        return ceiling / 2 + random.nextLong(ceiling / 2 + 1)
    }

    companion object {
        const val DEFAULT_MAX_RETRIES = 2
        private const val MILLIS_PER_SECOND = 1_000L
        private const val MAX_SHIFT = 10
        private val REPEATABLE = setOf(HttpMethodType.GET, HttpMethodType.PUT, HttpMethodType.DELETE)
        private val TRANSIENT_STATUSES = setOf(408, 429, 500, 502, 503, 504)
    }
}
