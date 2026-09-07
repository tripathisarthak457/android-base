package com.base.app.core.devtools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The last [CAPACITY] requests the app made, in memory, newest first.
 *
 * ## Bounded on purpose, twice
 *
 * A log that keeps everything is a memory leak with a friendly name: a screen that polls once a
 * second fills a device overnight. The list is capped at [CAPACITY] exchanges, and each body is
 * capped at [MAX_BODY_CHARS] — a single image or file download is otherwise megabytes of base64
 * held for as long as the app runs, to be looked at by nobody.
 *
 * ## Headers are redacted on the way in
 *
 * Not at render time. A bearer token that reaches this object is a bearer token in a heap dump, in
 * a screenshot of the panel, and in whatever somebody pastes into a bug report. Redacting on entry
 * means the value never exists here to leak.
 *
 * ## Why this is not behind an interface
 *
 * Nothing swaps it out. The whole module is absent from a build that did not ask for it, which is
 * a stronger guarantee than a no-op implementation and needs no binding to be got right.
 */
@Singleton
class DevToolsLog @Inject constructor() {

    private val ids = AtomicLong()

    private val _exchanges = MutableStateFlow<List<NetworkExchange>>(emptyList())
    val exchanges: StateFlow<List<NetworkExchange>> = _exchanges.asStateFlow()

    /**
     * Adds one exchange, assigning it an id and redacting and truncating it on the way in.
     *
     * Takes the whole object rather than eleven parameters: a call site with eleven positional
     * arguments is one transposition away from logging the response headers as the request's.
     */
    fun record(exchange: NetworkExchange) {
        _exchanges.update {
            val recorded = exchange.copy(
                id = ids.incrementAndGet(),
                requestHeaders = exchange.requestHeaders.redacted(),
                responseHeaders = exchange.responseHeaders.redacted(),
                requestBody = exchange.requestBody?.truncated(),
                responseBody = exchange.responseBody?.truncated(),
            )
            (listOf(recorded) + it).take(CAPACITY)
        }
    }

    fun clear() = _exchanges.update { emptyList() }

    fun find(id: Long): NetworkExchange? = _exchanges.value.firstOrNull { it.id == id }

    private fun Map<String, String>.redacted(): Map<String, String> = mapValues { (name, value) ->
        if (name.lowercase() in SECRET_HEADERS) REDACTED else value
    }

    private fun String.truncated(): String =
        if (length <= MAX_BODY_CHARS) this else take(MAX_BODY_CHARS) + TRUNCATION_NOTE

    private companion object {
        const val CAPACITY = 200
        const val MAX_BODY_CHARS = 128 * 1024
        const val TRUNCATION_NOTE = "\n\n… truncated by the inspector."
        const val REDACTED = "… redacted by the inspector"

        val SECRET_HEADERS = setOf(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
        )
    }
}
