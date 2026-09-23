package com.base.app.core.devtools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** The last [CAPACITY] requests the app made, in memory, newest first. */
@Singleton
class DevToolsLog @Inject constructor() {

    private val ids = AtomicLong()

    private val _exchanges = MutableStateFlow<List<NetworkExchange>>(emptyList())
    val exchanges: StateFlow<List<NetworkExchange>> = _exchanges.asStateFlow()

    /** Adds one exchange, assigning it an id and redacting and truncating it on the way in. */
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
