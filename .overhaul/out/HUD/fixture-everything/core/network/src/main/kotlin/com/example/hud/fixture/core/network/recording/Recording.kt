package com.example.hud.fixture.core.network.recording

import com.example.hud.fixture.core.network.client.CredentialsAttribute
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequest
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.Headers
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.isWebsocket
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CancellationException

/**
 * Reports every answered request to [observers]. A WebSocket upgrade is left alone: splitting its
 * body for a copy makes OkHttp fail the socket milliseconds after it opens.
 */
internal fun HttpClientConfig<*>.installRecording(observers: Set<NetworkObserver>) {
    if (observers.isEmpty()) return
    install(ResponseObserver) {
        filter { call -> !call.request.url.protocol.isWebsocket() }
        onResponse { response ->
            val request = response.request
            val secret = request.carriesCredentials()
            val body = if (secret) null else response.bodyAsText()
            val exchange = HttpExchange(
                method = request.method.value,
                url = request.url.toString(),
                status = response.status.value,
                startedAtMillis = response.requestTime.timestamp,
                durationMillis = response.responseTime.timestamp - response.requestTime.timestamp,
                requestHeaders = request.headers.flatten(),
                responseHeaders = response.headers.flatten(),
                requestBody = if (secret) null else request.content.describe(),
                responseBody = body,
                responseBytes = body?.encodeToByteArray()?.size?.toLong() ?: 0,
                failure = null,
            )
            observers.forEach { it.onExchange(exchange) }
        }
    }
}

/** Reports the requests that never got an answer: the timeouts and refused connections an inspector exists for. */
internal fun HttpClient.recordFailures(observers: Set<NetworkObserver>) {
    if (observers.isEmpty()) return
    plugin(HttpSend).intercept { request ->
        val startedAt = getTimeMillis()
        try {
            execute(request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            val secret = request.attributes.getOrNull(CredentialsAttribute) == true
            val exchange = HttpExchange(
                method = request.method.value,
                url = request.url.buildString(),
                status = null,
                startedAtMillis = startedAt,
                durationMillis = getTimeMillis() - startedAt,
                requestHeaders = request.headers.build().flatten(),
                responseHeaders = emptyMap(),
                requestBody = if (secret) null else (request.body as? OutgoingContent)?.describe(),
                responseBody = null,
                responseBytes = 0,
                failure = failure.message ?: failure::class.java.name,
            )
            observers.forEach { it.onExchange(exchange) }
            throw failure
        }
    }
}

private fun HttpRequest.carriesCredentials(): Boolean = attributes.getOrNull(CredentialsAttribute) == true

/**
 * Text as itself; anything else by its type and size. The content's class name would read as an
 * obfuscated "e71" in a minified build, so it is never used.
 */
private fun OutgoingContent.describe(): String? = when (this) {
    is TextContent -> text
    is OutgoingContent.NoContent -> null
    else -> buildString {
        append(contentType?.toString() ?: "binary")
        contentLength?.let { append(", ").append(it).append(" bytes") }
    }
}

private fun Headers.flatten(): Map<String, String> =
    entries().associate { (name, values) -> name to values.joinToString(", ") }
