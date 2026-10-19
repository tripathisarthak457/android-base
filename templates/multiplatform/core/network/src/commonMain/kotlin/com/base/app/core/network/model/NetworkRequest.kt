package com.base.app.core.network.model

/** The verbs this client speaks. An enum rather than a string, so a typo cannot reach the wire. */
enum class HttpMethodType { GET, POST, PUT, PATCH, DELETE }

/** A transport-agnostic description of one call. */
data class NetworkRequest(
    val method: HttpMethodType,
    /** Joined onto the base URL; a full `https://` URL is used as it is. */
    val path: String,
    val query: Map<String, Any?> = emptyMap(),
    /** Replace the client's defaults of the same name rather than adding a second copy. */
    val headers: Map<String, String> = emptyMap(),
    /** JSON, already encoded, so the client sends the text as it is instead of serialising it again. */
    val jsonBody: String? = null,
)
