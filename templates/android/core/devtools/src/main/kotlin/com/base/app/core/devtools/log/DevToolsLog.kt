package com.base.app.core.devtools.log

import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The last [CAPACITY] requests the app made, in memory, newest first. Credentials are redacted on
 * the way in: headers that carry them, and token and password fields in JSON bodies.
 */
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
                requestBody = exchange.requestBody?.withoutSecrets()?.truncated(),
                responseBody = exchange.responseBody?.withoutSecrets()?.truncated(),
            )
            (listOf(recorded) + it).take(CAPACITY)
        }
    }

    fun clear() = _exchanges.update { emptyList() }

    private fun Map<String, String>.redacted(): Map<String, String> = mapValues { (name, value) ->
        if (name.lowercase() in SECRET_HEADERS) REDACTED else value
    }

    private fun String.withoutSecrets(): String =
        SECRET_FIELD.replace(this) { match -> match.groupValues[1] + "\"" + REDACTED + "\"" }

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

        /** `"access_token": "…"`, `"new_password": "…"` and their kin, wherever they sit in a JSON body. */
        val SECRET_FIELD = Regex(
            "(\"[a-z_]*(?:token|password|secret|api_?key)\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"",
            RegexOption.IGNORE_CASE,
        )
    }
}
