package io.example.fitcnetdb.core.network.client

import io.example.fitcnetdb.core.common.coroutines.AppDispatchers
import io.example.fitcnetdb.core.common.log.Log
import io.example.fitcnetdb.core.common.network.NetworkMonitor
import io.example.fitcnetdb.core.common.result.AppError
import io.example.fitcnetdb.core.common.result.AppResult
import io.example.fitcnetdb.core.common.time.AppClock
import io.example.fitcnetdb.core.network.di.AuthenticatedClient
import io.example.fitcnetdb.core.network.model.HttpMethodType
import io.example.fitcnetdb.core.network.model.MultipartPart
import io.example.fitcnetdb.core.network.model.NetworkRequest
import io.example.fitcnetdb.core.network.model.NetworkResponse
import io.example.fitcnetdb.core.network.model.RequestBody
import io.example.fitcnetdb.core.network.response.ResponseUnwrapper
import io.example.fitcnetdb.core.network.response.retryAfterSeconds
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy

/**
 * The Ktor implementation, and the only class that knows which HTTP library is in use.
 *
 * A request is sent, and sent again as [RetryPolicy] allows when it gets no answer or a transient
 * one. Identical reads in flight together share one request. The answer's body is read once, as
 * text, and decoded once.
 */
@Singleton
internal class KtorNetworkClient @Inject constructor(
    @AuthenticatedClient private val client: HttpClient,
    private val config: NetworkConfig,
    private val networkMonitor: NetworkMonitor,
    private val unwrapper: ResponseUnwrapper,
    private val retryPolicy: RetryPolicy,
    private val inFlight: InFlightRequests,
    private val dispatchers: AppDispatchers,
    private val clock: AppClock,
) : NetworkClient {

    override suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse> = withContext(dispatchers.io) {
        val outgoing = request
        if (outgoing.method == HttpMethodType.GET) {
            inFlight.shareOrRun(outgoing.sharingKey()) { sendWithRetries(outgoing) }
        } else {
            sendWithRetries(outgoing)
        }
    }

    override suspend fun <T> execute(
        request: NetworkRequest,
        deserializer: DeserializationStrategy<T>,
    ): AppResult<T> = when (val result = execute(request)) {
        is AppResult.Success -> withContext(dispatchers.default) { decode(result.value.body, deserializer) }
        is AppResult.Failure -> result
    }

    private fun <T> decode(body: String, deserializer: DeserializationStrategy<T>): AppResult<T> = try {
        AppResult.Success(unwrapper.payload(body, deserializer))
    } catch (mismatch: IllegalArgumentException) {
        AppResult.Failure(AppError.Decode(mismatch))
    }

    private suspend fun sendWithRetries(request: NetworkRequest): AppResult<NetworkResponse> {
        var attempt = 0
        while (true) {
            val wait = try {
                val response = send(request)
                if (response.status in SUCCESS) return AppResult.Success(response)
                val error = refusal(response)
                retryPolicy.delayBefore(attempt, request.method, response.status, error.retryAfterSeconds)
                    ?: return AppResult.Failure(error)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (io: IOException) {
                val error = if (networkMonitor.isOnline.first()) AppError.Timeout else AppError.Offline
                // Offline, another attempt would fail the same way; the monitor says when to try again.
                val retry = if (error == AppError.Timeout) {
                    retryPolicy.delayBefore(attempt, request.method, status = null, retryAfterSeconds = null)
                } else {
                    null
                }
                retry ?: return noAnswer(request, error, io)
            } catch (unexpected: Exception) {
                Log.e("Request to ${request.path} failed", unexpected, TAG)
                return AppResult.Failure(AppError.Unexpected(unexpected))
            }
            attempt++
            delay(wait)
        }
    }

    private suspend fun send(request: NetworkRequest): NetworkResponse {
        val response = client.request(request.resolvedUrl(config.resolvedBaseUrl)) {
            method = request.method.toKtor()
            request.query.forEach { (key, value) -> value?.let { parameter(key, it) } }
            // The server's own error messages come back in the language the app is showing.
            headers[HttpHeaders.AcceptLanguage] = Locale.getDefault().toLanguageTag()
            // Set rather than appended: a request's own header replaces the default of that name.
            request.headers.forEach { (name, value) -> headers[name] = value }
            if (!request.requiresAuth) attributes.put(SkipAuthAttribute, true)
            if (request.carriesCredentials) attributes.put(CredentialsAttribute, true)
            request.timeoutMillis?.let { millis -> timeout { requestTimeoutMillis = millis } }
            request.body?.let(::applyBody)
        }
        val answer = NetworkResponse(
            status = response.status.value,
            body = response.bodyAsText(),
            headers = response.headers.entries().associate { (name, values) -> name to values },
        )
        return answer
    }

    private fun refusal(response: NetworkResponse): AppError.Http {
        val error = unwrapper.error(response.status, response.body)
            .copy(retryAfterSeconds = retryAfterSeconds(response.header(HttpHeaders.RetryAfter), clock.now()))
        return error
    }

    private suspend fun noAnswer(request: NetworkRequest, error: AppError, cause: IOException): AppResult.Failure {
        Log.w("No answer for ${request.path}", cause, TAG)
        return AppResult.Failure(error)
    }

    private companion object {
        const val TAG = "Network"
        val SUCCESS = 200..299
    }
}

/** A path is joined onto the base URL; a full URL, for a third-party API, is used as it is. */
private fun NetworkRequest.resolvedUrl(base: String): String =
    if (path.startsWith("https://") || path.startsWith("http://")) path else base + path.trimStart('/')

/** Two reads are the same read when they would put the same bytes on the wire. */
private fun NetworkRequest.sharingKey(): String = buildString {
    append(path).append('?')
    query.entries.sortedBy { it.key }.forEach { (key, value) -> append(key).append('=').append(value).append('&') }
    headers.entries.sortedBy { it.key.lowercase() }.forEach { (key, value) ->
        append('|').append(key.lowercase()).append(':').append(value)
    }
    append("|auth=").append(requiresAuth)
}

private fun HttpMethodType.toKtor(): HttpMethod = when (this) {
    HttpMethodType.GET -> HttpMethod.Get
    HttpMethodType.POST -> HttpMethod.Post
    HttpMethodType.PUT -> HttpMethod.Put
    HttpMethodType.PATCH -> HttpMethod.Patch
    HttpMethodType.DELETE -> HttpMethod.Delete
}

private fun HttpRequestBuilder.applyBody(body: RequestBody) {
    when (body) {
        is RequestBody.Json -> setBody(TextContent(body.text, ContentType.Application.Json))
        is RequestBody.Raw -> setBody(ByteArrayContent(body.bytes, ContentType.parse(body.contentType)))
        is RequestBody.Multipart -> setBody(
            MultiPartFormDataContent(
                formData {
                    body.parts.forEach { part ->
                        when (part) {
                            is MultipartPart.Text -> append(part.name, part.value)
                            is MultipartPart.File -> append(
                                key = part.name,
                                value = part.bytes,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, part.contentType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"${part.fileName}\"")
                                },
                            )
                        }
                    }
                },
            ),
        )
    }
}
