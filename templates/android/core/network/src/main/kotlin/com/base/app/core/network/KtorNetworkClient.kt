package com.base.app.core.network

import com.base.app.core.common.AppResult
import com.base.app.core.common.network.NetworkMonitor
import com.base.app.core.common.util.AppLogger
import com.base.app.core.coroutines.IoDispatcher
import com.base.app.core.network.di.AuthenticatedClient
// <opt:room>
import com.base.app.core.network.model.CachePolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
// </opt:room>
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
import java.io.IOException
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement

/**
 * The Ktor implementation. The only class in the project that knows what HTTP library is in use.
 *
 * 1. A cache hit that is still fresh short-circuits everything — no socket is opened.
 * 2. Offline, with a stale cache entry and a policy that allows it: serve the stale copy.
 * 3. Offline, with a queueable mutation: persist it and report a failure marked `queued`, so the
 *    caller can say it will be sent later.
 * 4. Otherwise: make the call, and retry it as [RetryPolicy] allows when it gets no answer or a
 *    transient one.
 */
@Singleton
class KtorNetworkClient @Inject constructor(
    @AuthenticatedClient private val client: HttpClient,
    private val config: NetworkConfig,
    private val networkMonitor: NetworkMonitor,
    private val unwrapper: ResponseUnwrapper,
    private val retryPolicy: RetryPolicy,
    private val inFlight: InFlightRequests,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    // <opt:room>
    private val responseCache: ResponseCache,
    private val requestQueue: RequestQueue,
    // </opt:room>
) : NetworkClient {

    override suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse> =
        withContext(ioDispatcher) {
            // Keyed before anything else, so a request queued straight away carries its key too.
            val outgoing = request.withIdempotencyKey()
            // <opt:room>
            val policy = outgoing.cache as? CachePolicy.Enabled
            if (policy != null && !policy.forceRefresh) {
                responseCache.fresh(policy.key, policy.maxAgeMillis)?.let { cached ->
                    return@withContext AppResult.Success(cached)
                }
            }
            // </opt:room>

            if (!networkMonitor.isOnline.first()) {
                return@withContext unreachable(outgoing, deviceIsOffline = true, cause = null)
            }

            if (outgoing.method == HttpMethodType.GET) {
                inFlight.shareOrRun(outgoing.sharingKey()) { sendWithRetries(outgoing) }
            } else {
                sendWithRetries(outgoing)
            }
        }
    // <opt:room>

    override fun stream(request: NetworkRequest): Flow<AppResult<NetworkResponse>> = flow {
        val policy = request.cache as? CachePolicy.Enabled
        val saved = policy?.let { responseCache.any(it.key) }
        if (saved != null) {
            emit(AppResult.Success(saved, fromCache = true, cachedAtEpochMillis = saved.cachedAtEpochMillis))
        }
        // The saved copy is already on screen, so a failure is reported rather than papered over
        // with the same copy a second time.
        val latest = policy?.copy(forceRefresh = true, staleOnFailure = saved == null) ?: request.cache
        emit(execute(request.copy(cache = latest)))
    }.flowOn(ioDispatcher)
    // </opt:room>

    private suspend fun sendWithRetries(outgoing: NetworkRequest): AppResult<NetworkResponse> {
        var attempt = 0
        while (true) {
            val outcome = try {
                send(outgoing)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (io: IOException) {
                Outcome.NoAnswer(io)
            } catch (throwable: Throwable) {
                AppLogger.e("Request failed: ${outgoing.path}", throwable)
                return AppResult.Failure(cause = NetworkException.Unknown(throwable))
            }

            val wait = when (outcome) {
                is Outcome.Answered -> if (outcome.response.status.isSuccess()) {
                    null
                } else {
                    retryPolicy.delayBefore(
                        attempt = attempt,
                        method = outgoing.method,
                        statusCode = outcome.response.status.value,
                        retryAfter = outcome.response.headers[HttpHeaders.RetryAfter],
                    )
                }
                // Offline at this point, another attempt would fail the same way; the queue handles it.
                is Outcome.NoAnswer -> if (networkMonitor.isOnline.first()) {
                    retryPolicy.delayBefore(attempt, outgoing.method, statusCode = null, retryAfter = null)
                } else {
                    null
                }
            }
            if (wait == null) return complete(outgoing, outcome)
            attempt++
            delay(wait)
        }
    }

    private suspend fun send(request: NetworkRequest): Outcome.Answered {
        val response = client.request(request.resolvedUrl(config.resolvedBaseUrl)) {
            method = request.method.toKtor()
            request.query.forEach { (key, value) -> value?.let { parameter(key, it) } }
            // The server's own error messages come back in the language the app is showing.
            header(HttpHeaders.AcceptLanguage, Locale.getDefault().toLanguageTag())
            request.headers.forEach { (key, value) -> header(key, value) }
            if (!request.requiresAuth) attributes.put(SkipAuthAttribute, true)
            applyBody(request.body, request.parts)
        }
        return Outcome.Answered(response, response.bodyAsText())
    }

    private suspend fun complete(request: NetworkRequest, outcome: Outcome): AppResult<NetworkResponse> =
        when (outcome) {
            is Outcome.NoAnswer -> {
                // Online but unreachable: wrong base URL, DNS or a server that is down. Recovery is
                // the same as offline, the message is not.
                AppLogger.w("Network IO failure for ${request.path}", throwable = outcome.cause)
                unreachable(request, deviceIsOffline = !networkMonitor.isOnline.first(), cause = outcome.cause)
            }

            is Outcome.Answered -> if (!outcome.response.status.isSuccess()) {
                failure(outcome.response, outcome.body)
            } else {
                // <opt:room>
                (request.cache as? CachePolicy.Enabled)?.let {
                    responseCache.put(it.key, outcome.body, outcome.response.status.value)
                }
                // </opt:room>
                AppResult.Success(
                    NetworkResponse(
                        statusCode = outcome.response.status.value,
                        body = outcome.body,
                        headers = outcome.response.headers.toMap(),
                    ),
                )
            }
        }

    /**
     * The request did not produce a response, either because the device is offline or because the
     * server could not be reached. [deviceIsOffline] decides which of those the user is told.
     */
    // <opt:!room>    @Suppress("UnusedParameter") // `request` is only read by the cache and queue.
    private suspend fun unreachable(
        request: NetworkRequest,
        deviceIsOffline: Boolean,
        cause: Throwable?,
    ): AppResult<NetworkResponse> {
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
            return AppResult.Failure(cause = exception, isOffline = deviceIsOffline, queued = true)
        }
        // </opt:room>

        return AppResult.Failure(cause = exception, isOffline = deviceIsOffline)
    }

    private fun failure(response: HttpResponse, body: String): AppResult.Failure {
        val root = runCatching { NetworkJson.parseToJsonElement(body) }.getOrNull()
        return AppResult.Failure(
            message = root?.let(unwrapper::errorMessage),
            cause = NetworkException.Http(response.status.value, body),
            code = response.status.value,
            fieldErrors = root?.let(unwrapper::fieldErrors).orEmpty(),
            rawBody = body,
        )
    }
}

/** What one attempt produced, before it is decided whether to try again. */
private sealed interface Outcome {
    class Answered(val response: HttpResponse, val body: String) : Outcome
    class NoAnswer(val cause: IOException) : Outcome
}

/**
 * A mutation that may be queued and replayed carries a key the server can deduplicate on, so a
 * request that did arrive before the connection dropped is not applied a second time on replay.
 * Only meaningful when the backend honours `Idempotency-Key`; harmless when it does not.
 */
private fun NetworkRequest.withIdempotencyKey(): NetworkRequest {
    val repeatable = method == HttpMethodType.GET || method == HttpMethodType.PUT || method == HttpMethodType.DELETE
    if (!enqueueOnFailure || repeatable) return this
    if (headers.keys.any { it.equals(IDEMPOTENCY_KEY, ignoreCase = true) }) return this
    return copy(headers = headers + (IDEMPOTENCY_KEY to UUID.randomUUID().toString()))
}

private const val IDEMPOTENCY_KEY = "Idempotency-Key"

/** A path is joined onto the base URL; a full URL, for a third-party API, is used as it is. */
private fun NetworkRequest.resolvedUrl(base: String): String =
    if (path.startsWith("https://") || path.startsWith("http://")) path else base + path.trimStart('/')

/** Two reads are the same read when they would put the same bytes on the wire and in the cache. */
private fun NetworkRequest.sharingKey(): String = buildString {
    append(path).append('?')
    query.entries.sortedBy { it.key }.forEach { (key, value) ->
        append(key).append('=').append(value).append('&')
    }
    headers.entries.sortedBy { it.key.lowercase() }.forEach { (key, value) ->
        append('|').append(key.lowercase()).append(':').append(value)
    }
    append("|auth=").append(requiresAuth)
    // <opt:room>
    (cache as? CachePolicy.Enabled)?.let { append("|cache=").append(it.key) }
    // </opt:room>
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
