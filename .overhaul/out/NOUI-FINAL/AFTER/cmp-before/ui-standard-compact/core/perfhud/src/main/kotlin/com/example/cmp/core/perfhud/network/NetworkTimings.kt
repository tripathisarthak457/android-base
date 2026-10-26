package com.example.cmp.core.perfhud.network

import com.example.cmp.core.devtools.log.NetworkExchange
import kotlinx.serialization.Serializable

/**
 * How long the app's requests took, from the inspector's request log: overall, and for the
 * endpoints called most. Medians and the 90th percentile rather than means, because one request
 * that waited out a timeout drags a mean past every request that actually happened.
 */
@Serializable
internal data class NetworkTimings(
    val requests: Int,
    val failures: Int,
    val medianMillis: Long,
    val p90Millis: Long,
    val slowestMillis: Long,
    val endpoints: List<EndpointTimings>,
) {
    companion object {
        /** The endpoints listed, by how often they were called. */
        const val TOP_ENDPOINTS = 5

        val None = NetworkTimings(0, 0, 0, 0, 0, emptyList())

        fun of(exchanges: List<NetworkExchange>): NetworkTimings {
            if (exchanges.isEmpty()) return None
            val durations = exchanges.map { it.durationMillis }.sorted()
            return NetworkTimings(
                requests = exchanges.size,
                failures = exchanges.count { it.isFailure },
                medianMillis = durations.atFraction(MEDIAN),
                p90Millis = durations.atFraction(P90),
                slowestMillis = durations.last(),
                endpoints = exchanges
                    .groupBy(::endpointOf)
                    .map { (endpoint, calls) ->
                        val sorted = calls.map { it.durationMillis }.sorted()
                        EndpointTimings(
                            endpoint = endpoint,
                            requests = calls.size,
                            failures = calls.count { it.isFailure },
                            medianMillis = sorted.atFraction(MEDIAN),
                            slowestMillis = sorted.last(),
                        )
                    }
                    .sortedByDescending { it.requests }
                    .take(TOP_ENDPOINTS),
            )
        }

        private const val MEDIAN = 0.5
        private const val P90 = 0.9
        private const val ID_SEGMENT = "{id}"
        private val UUID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

        /**
         * `GET /posts/42/comments` as `GET /posts/{id}/comments`: one endpoint whichever item it was
         * called for, or a list of the most called would be the same route once per item.
         */
        private fun endpointOf(exchange: NetworkExchange): String =
            exchange.method + " " + exchange.path.split('/').joinToString("/") { segment ->
                if (segment.isIdentifier()) ID_SEGMENT else segment
            }

        private fun String.isIdentifier(): Boolean =
            isNotEmpty() && (all(Char::isDigit) || UUID.matches(this))

        /** Nearest rank: the smallest value with at least [fraction] of the values at or below it. */
        private fun List<Long>.atFraction(fraction: Double): Long =
            this[(Math.ceil(fraction * size).toInt() - 1).coerceIn(0, lastIndex)]
    }
}

@Serializable
internal data class EndpointTimings(
    /** The method and the path without its query, ids as `{id}`, such as `GET /posts/{id}`. */
    val endpoint: String,
    val requests: Int,
    val failures: Int,
    val medianMillis: Long,
    val slowestMillis: Long,
)
