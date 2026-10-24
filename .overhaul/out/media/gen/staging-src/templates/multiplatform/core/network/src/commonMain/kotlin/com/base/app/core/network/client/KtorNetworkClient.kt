package com.base.app.core.network.client

import com.base.app.core.common.coroutines.AppDispatchers
import com.base.app.core.common.log.Log
import com.base.app.core.common.network.NetworkMonitor
import com.base.app.core.common.result.AppError
import com.base.app.core.common.result.AppResult
import com.base.app.core.network.model.HttpMethodType
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.model.NetworkResponse
import com.base.app.core.network.response.ResponseUnwrapper
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.DeserializationStrategy

/**
 * The Ktor implementation, and the only class that knows which HTTP library is in use. The body
 * of an answer is read once, as text, and decoded once, off the main thread.
 */
internal class KtorNetworkClient(
    private val client: HttpClient,
    private val config: NetworkConfig,
    private val networkMonitor: NetworkMonitor,
    private val unwrapper: ResponseUnwrapper,
    private val dispatchers: AppDispatchers,
) : NetworkClient {

    override suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse> = withContext(dispatchers.io) {
        try {
            val response = send(request)
            if (response.status in SUCCESS) {
                AppResult.Success(response)
            } else {
                AppResult.Failure(unwrapper.error(response.status, response.body))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (io: IOException) {
            // Ktor's timeouts and every engine's connection failures are IOExceptions; which of the
            // two the person sees depends only on whether the device has a network at all.
            val error = if (networkMonitor.isOnline.first()) AppError.Timeout else AppError.Offline
            Log.w("No answer for ${request.path}", io, TAG)
            AppResult.Failure(error)
        } catch (unexpected: Exception) {
            Log.e("Request to ${request.path} failed", unexpected, TAG)
            AppResult.Failure(AppError.Unexpected(unexpected))
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

    private suspend fun send(request: NetworkRequest): NetworkResponse {
        val response = client.request(request.resolvedUrl(config.resolvedBaseUrl)) {
            method = request.method.toKtor()
            request.query.forEach { (key, value) -> value?.let { parameter(key, it) } }
            // The server's own error messages come back in the language the app is showing.
            headers[HttpHeaders.AcceptLanguage] = currentLanguageTag()
            // Set rather than appended: a request's own header replaces the default of that name.
            request.headers.forEach { (name, value) -> headers[name] = value }
            request.jsonBody?.let { setBody(TextContent(it, ContentType.Application.Json)) }
        }
        return NetworkResponse(
            status = response.status.value,
            body = response.bodyAsText(),
            headers = response.headers.entries().associate { (name, values) -> name to values },
        )
    }

    private companion object {
        const val TAG = "Network"
        val SUCCESS = 200..299
    }
}

/** A path is joined onto the base URL; a full URL, for a third-party API, is used as it is. */
private fun NetworkRequest.resolvedUrl(base: String): String =
    if (path.startsWith("https://") || path.startsWith("http://")) path else base + path.trimStart('/')

private fun HttpMethodType.toKtor(): HttpMethod = when (this) {
    HttpMethodType.GET -> HttpMethod.Get
    HttpMethodType.POST -> HttpMethod.Post
    HttpMethodType.PUT -> HttpMethod.Put
    HttpMethodType.PATCH -> HttpMethod.Patch
    HttpMethodType.DELETE -> HttpMethod.Delete
}
