package com.base.app.core.network

import com.base.app.core.common.AppResult
import com.base.app.core.common.network.NetworkMonitor
import com.base.app.core.common.util.AppLogger
import com.base.app.core.coroutines.IoDispatcher
import com.base.app.core.network.di.AuthenticatedClient
import com.base.app.core.network.model.CachePolicy
import com.base.app.core.network.model.HttpMethodType
import com.base.app.core.network.model.MultipartPart
import com.base.app.core.network.model.NetworkException
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.model.NetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Ktor implementation. The only class in the project that knows what HTTP library is in use.
 *
 * ## Order of operations
 *
 * 1. A cache hit that is still fresh short-circuits everything — no socket is opened.
 * 2. Offline, with a stale cache entry and a policy that allows it: serve the stale copy.
 * 3. Offline, with a queueable mutation: persist it and report success-shaped failure so the
 *    caller can show "we'll send this when you're back".
 * 4. Otherwise: make the call.
 *
 * ## Failures are classified, not wrapped
 *
 * `isOffline` on the result distinguishes "the request never left the device" from "the server
 * said no", because those need different copy and a different action on every screen — and
 * deciding that at the UI layer would mean the UI layer importing Ktor's exception types.
 */
@Singleton
class KtorNetworkClient @Inject constructor(
    @AuthenticatedClient private val client: HttpClient,
    private val config: NetworkConfig,
    private val networkMonitor: NetworkMonitor,
    private val unwrapper: ResponseUnwrapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    // <opt:room>
    private val responseCache: ResponseCache,
    private val requestQueue: RequestQueue,
    // </opt:room>
) : NetworkClient {

    override suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse> =
        withContext(ioDispatcher) {
            // <opt:room>
            val policy = request.cache as? CachePolicy.Enabled
            if (policy != null) {
                responseCache.fresh(policy.key, policy.maxAgeMillis)?.let { cached ->
                    return@withContext AppResult.Success(cached)
                }
            }
            // </opt:room>

            if (!networkMonitor.isOnline.first()) {
                return@withContext unreachable(request, deviceIsOffline = true, cause = null)
            }

            try {
                val response = client.request(config.resolvedBaseUrl + request.path.trimStart('/')) {
                    method = request.method.toKtor()
                    request.query.forEach { (key, value) -> value?.let { parameter(key, it) } }
                    request.headers.forEach { (key, value) -> header(key, value) }
                    if (!request.requiresAuth) attributes.put(SkipAuthAttribute, true)
                    applyBody(request.body, request.parts)
                }

                val body = response.bodyAsText()
                if (!response.status.isSuccess()) {
                    return@withContext failure(response, body)
                }

                // <opt:room>
                policy?.let { responseCache.put(it.key, body, response.status.value) }
                // </opt:room>

                AppResult.Success(
                    NetworkResponse(
                        statusCode = response.status.value,
                        body = body,
                        headers = response.headers.toMap(),
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (io: IOException) {
                // The recovery is the same as being offline — stale cache, then the queue — but
                // the message is not. The connectivity check above passed, so if the device is
                // still online this is the server being unreachable: a wrong base URL, DNS that
                // does not resolve, a service that is down. Telling that user "you are offline"
                // sends them to check their wifi, which is fine, and is not the problem.
                AppLogger.w("Network IO failure for ${request.path}", throwable = io)
                unreachable(
                    request = request,
                    deviceIsOffline = !networkMonitor.isOnline.first(),
                    cause = io,
                )
            } catch (throwable: Throwable) {
                AppLogger.e("Request failed: ${request.path}", throwable)
                AppResult.Failure(
                    message = throwable.message,
                    cause = NetworkException.Unknown(throwable),
                )
            }
        }

    /**
     * The request did not produce a response, either because the device is offline or because the
     * server could not be reached. [deviceIsOffline] decides which of those the user is told.
     */
    private suspend fun unreachable(
        request: NetworkRequest,
        deviceIsOffline: Boolean,
        cause: Throwable?,
    ): AppResult<NetworkResponse> {
        val reason = if (deviceIsOffline) "You are offline." else "Could not reach the server."
        val exception = if (deviceIsOffline) {
            NetworkException.NoConnectivity()
        } else {
            cause?.let { NetworkException.Unknown(it) } ?: NetworkException.NoConnectivity()
        }

        // <opt:room>
        val policy = request.cache as? CachePolicy.Enabled
        if (policy?.staleOnFailure == true) {
            responseCache.any(policy.key)?.let { stale ->
                return AppResult.Success(stale)
            }
        }

        if (request.enqueueOnFailure && request.parts == null) {
            requestQueue.enqueue(request)
            return AppResult.Failure(
                message = "$reason This will be sent as soon as it goes through.",
                cause = exception,
                isOffline = deviceIsOffline,
            )
        }
        // </opt:room>

        return AppResult.Failure(
            message = reason,
            cause = exception,
            isOffline = deviceIsOffline,
        )
    }

    private fun failure(response: HttpResponse, body: String): AppResult.Failure {
        val root = runCatching { NetworkJson.parseToJsonElement(body) }.getOrNull()
        return AppResult.Failure(
            message = root?.let(unwrapper::errorMessage) ?: "Request failed (${response.status.value}).",
            cause = NetworkException.Http(response.status.value, body),
            code = response.status.value,
            fieldErrors = root?.let(unwrapper::fieldErrors).orEmpty(),
            rawBody = body,
        )
    }
}

private fun HttpMethodType.toKtor(): HttpMethod = when (this) {
    HttpMethodType.GET -> HttpMethod.Get
    HttpMethodType.POST -> HttpMethod.Post
    HttpMethodType.PUT -> HttpMethod.Put
    HttpMethodType.PATCH -> HttpMethod.Patch
    HttpMethodType.DELETE -> HttpMethod.Delete
}

private fun io.ktor.client.request.HttpRequestBuilder.applyBody(
    body: JsonElement?,
    parts: List<MultipartPart>?,
) {
    when {
        parts != null -> setBody(
            MultiPartFormDataContent(
                formData {
                    parts.forEach { part ->
                        when (part) {
                            is MultipartPart.Text -> append(part.name, part.value)
                            is MultipartPart.File -> append(
                                key = part.name,
                                value = part.bytes,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, part.contentType)
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=\"${part.fileName}\"",
                                    )
                                },
                            )
                        }
                    }
                },
            ),
        )

        body != null -> {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }
}

private fun Headers.toMap(): Map<String, String> = buildMap {
    this@toMap.forEach { name, values -> values.firstOrNull()?.let { put(name, it) } }
}
