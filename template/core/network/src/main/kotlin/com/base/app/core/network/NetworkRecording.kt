package com.base.app.core.network

import com.base.app.core.devtools.DevToolsLog
import com.base.app.core.devtools.NetworkExchange
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.Headers
import io.ktor.http.content.TextContent

/**
 * Feeds every request and response into the on-device inspector.
 *
 * ## Why `ResponseObserver` rather than a plugin of our own
 *
 * A response body can be read once. A hand-rolled interceptor that reads it to log it hands the
 * caller an empty channel, and the symptom is a repository that parses fine in release and
 * returns nothing in debug — the worst possible place for a difference. `ResponseObserver` exists
 * for exactly this: Ktor saves the response when one is installed, so both the observer and the
 * caller get a complete body.
 *
 * ## Off means absent
 *
 * [NetworkConfig.recordExchanges] is false in a production build, so nothing is installed and no
 * body is ever held. That is a stronger statement than a flag checked at render time: there is no
 * point in the process where a response body and an auth token are sitting in a list waiting for
 * somebody to decide not to show them.
 */
internal fun HttpClientConfig<*>.installRecording(config: NetworkConfig, log: DevToolsLog) {
    if (!config.recordExchanges) return

    install(ResponseObserver) {
        onResponse { response ->
            val request = response.request
            log.record(
                NetworkExchange(
                    method = request.method.value,
                    url = request.url.toString(),
                    status = response.status.value,
                    startedAtMillis = response.requestTime.timestamp,
                    durationMillis = response.responseTime.timestamp -
                        response.requestTime.timestamp,
                    requestHeaders = request.headers.flatten(),
                    responseHeaders = response.headers.flatten(),
                    // Only a text body can be shown as one. A multipart upload or a raw stream is
                    // reported by its type instead, which is the useful half of it anyway.
                    requestBody = when (val content = request.content) {
                        is TextContent -> content.text
                        else -> content::class.simpleName
                    },
                    responseBody = runCatching { response.bodyAsText() }.getOrNull(),
                    responseBytes = response.contentLengthOrZero(),
                    failure = null,
                ),
            )
        }
    }
}

private fun Headers.flatten(): Map<String, String> =
    entries().associate { (name, values) -> name to values.joinToString(", ") }

private fun io.ktor.client.statement.HttpResponse.contentLengthOrZero(): Long =
    headers[io.ktor.http.HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
