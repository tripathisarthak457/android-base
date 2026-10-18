package com.example.hud.everything.core.network.model

/** The verbs this client speaks. An enum rather than a string, so a typo cannot reach the wire. */
enum class HttpMethodType { GET, POST, PUT, PATCH, DELETE }

/** A transport-agnostic description of one call. */
data class NetworkRequest(
    val method: HttpMethodType,
    /** Joined onto the base URL; a full `https://` URL is used as it is and never gets the session's token. */
    val path: String,
    val query: Map<String, Any?> = emptyMap(),
    /** Replace the client's defaults of the same name rather than adding a second copy. */
    val headers: Map<String, String> = emptyMap(),
    val body: RequestBody? = null,
    /** False for calls made before there is a session to send, such as signing in. */
    val requiresAuth: Boolean = true,
    /** The body or the answer holds a password or tokens: left out of debug logs and the inspector. */
    val carriesCredentials: Boolean = false,
    /** Replaces `NetworkConfig.requestTimeoutMillis` for this call only, for a large upload. */
    val timeoutMillis: Long? = null,
    /**
     * Kept and sent later when the call gets no answer, rather than failing for good. Only a JSON
     * body is kept; an upload carries file bytes and never is. The failure the caller sees is then
     * `Offline` or `Timeout`, and the request goes out again once the network is back.
     */
    val enqueueOnFailure: Boolean = false,
)

/** What a request sends. */
sealed interface RequestBody {

    /** Already encoded, so the client sends the text as it is instead of serialising it again. */
    data class Json(val text: String) : RequestBody

    /** Bytes sent as they are, such as a photo `PUT` to a pre-signed storage URL. */
    class Raw(val contentType: String, val bytes: ByteArray) : RequestBody {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Raw && contentType == other.contentType && bytes.contentEquals(other.bytes))

        override fun hashCode(): Int = 31 * contentType.hashCode() + bytes.contentHashCode()
    }

    data class Multipart(val parts: List<MultipartPart>) : RequestBody
}

sealed interface MultipartPart {
    val name: String

    data class Text(override val name: String, val value: String) : MultipartPart

    class File(
        override val name: String,
        val fileName: String,
        val contentType: String,
        val bytes: ByteArray,
    ) : MultipartPart {
        // A data class would compare the bytes by reference, so two identical parts would differ.
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
