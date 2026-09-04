package com.base.app.core.network.model

import kotlinx.serialization.json.JsonElement

/** The verbs this client speaks. An enum rather than a string, so a typo cannot reach the wire. */
enum class HttpMethodType { GET, POST, PUT, PATCH, DELETE }

/**
 * A transport-agnostic description of one call.
 *
 * The Ktor client translates this into a request. Keeping the description separate from the
 * execution is what lets a failed mutation be *persisted* and replayed later — see the offline
 * queue — which is impossible if the only representation of a request is the builder lambda that
 * issued it.
 */
data class NetworkRequest(
    val method: HttpMethodType,
    val path: String,
    val query: Map<String, Any?> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: JsonElement? = null,
    val parts: List<MultipartPart>? = null,
    val requiresAuth: Boolean = true,
    val cache: CachePolicy = CachePolicy.Disabled,
    /**
     * When true and the request fails purely for lack of connectivity — not because the server
     * refused it — it is persisted and replayed when the network returns. Only meaningful for
     * mutations with a JSON body; multipart bodies carry file bytes and are never queued.
     */
    val enqueueOnFailure: Boolean = false,
)

sealed interface MultipartPart {
    val name: String

    data class Text(override val name: String, val value: String) : MultipartPart

    data class File(
        override val name: String,
        val fileName: String,
        val contentType: String,
        val bytes: ByteArray,
    ) : MultipartPart {
        // Generated equals on a ByteArray compares references, which makes two identical parts
        // unequal and quietly breaks any de-duplication or test assertion built on them.
        override fun equals(other: Any?): Boolean =
            this === other ||
                (
                    other is File &&
                        name == other.name &&
                        fileName == other.fileName &&
                        contentType == other.contentType &&
                        bytes.contentEquals(other.bytes)
                    )

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + contentType.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }
}

/**
 * Whether, and for how long, a successful response may be served from disk.
 *
 * Read-through rather than a plain expiry: [maxAgeMillis] is how long the cached copy is served
 * *without* a network call, and a request that fails after that still falls back to the stale
 * copy rather than showing an error. A user on a train sees yesterday's list, which is almost
 * always better than a retry button.
 */
sealed interface CachePolicy {

    data object Disabled : CachePolicy

    data class Enabled(
        val key: String,
        val maxAgeMillis: Long,
        /** Serve a stale entry when the network call fails. */
        val staleOnFailure: Boolean = true,
    ) : CachePolicy
}

/** What came back, before it has been decoded into anything. */
data class NetworkResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap(),
    val fromCache: Boolean = false,
    val cachedAtEpochMillis: Long? = null,
)

sealed class NetworkException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    class NoConnectivity : NetworkException("No internet connection.")

    class Timeout(cause: Throwable? = null) : NetworkException("The request timed out.", cause)

    class Serialization(cause: Throwable) :
        NetworkException("Could not read the server's response.", cause)

    class Http(val statusCode: Int, val body: String?) :
        NetworkException("HTTP $statusCode")

    class Unknown(cause: Throwable) : NetworkException(cause.message ?: "Unknown error.", cause)
}
