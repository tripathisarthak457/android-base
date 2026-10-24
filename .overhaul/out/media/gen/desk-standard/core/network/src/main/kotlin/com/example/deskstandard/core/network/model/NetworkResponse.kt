package com.example.deskstandard.core.network.model

/** What came back, before it has been decoded into anything. */
data class NetworkResponse(
    val status: Int,
    /** The body as text, read once. Empty for a 204. */
    val body: String,
    /** Every value of every header; a server may repeat one, as it does `Set-Cookie`. */
    val headers: Map<String, List<String>> = emptyMap(),
) {
    /** The first value of [name], matched without regard to case as HTTP requires. */
    fun header(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()
}
