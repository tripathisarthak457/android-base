package com.example.finalafter.crui.core.network.client

import com.example.finalafter.crui.core.common.result.AppResult
import com.example.finalafter.crui.core.network.model.HttpMethodType
import com.example.finalafter.crui.core.network.model.MultipartPart
import com.example.finalafter.crui.core.network.model.NetworkRequest
import com.example.finalafter.crui.core.network.model.NetworkResponse
import com.example.finalafter.crui.core.network.model.RequestBody
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.serializer

/**
 * The whole network surface. Every failure arrives as an `AppError`: `Offline` when the device has
 * no network, `Timeout` when it has one but the server did not answer, `Http` for a refusal (its
 * code, field errors and `Retry-After` already read), `Decode` for a body that is not what the
 * caller asked for. Nothing is thrown except cancellation.
 *
 * A request is always attempted: connectivity decides only which of `Offline` and `Timeout` a
 * failure is, never whether to try.
 */
interface NetworkClient {

    /** Sends [request] and returns the answer as it came, for a caller that needs a status or a header. */
    suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse>

    /** Sends [request] and decodes a successful body through the app's `ResponseUnwrapper`. */
    suspend fun <T> execute(request: NetworkRequest, deserializer: DeserializationStrategy<T>): AppResult<T>
}

suspend inline fun <reified T> NetworkClient.get(
    path: String,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
): AppResult<T> = execute(
    NetworkRequest(HttpMethodType.GET, path, query, headers, requiresAuth = requiresAuth),
    serializer<T>(),
)

/**
 * [carriesCredentials] keeps the body and the answer out of debug logs and the inspector: set it for
 * sign-in, sign-up and anything else that sends a password or receives tokens.
 */
suspend inline fun <reified B, reified T> NetworkClient.post(
    path: String,
    body: B,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    carriesCredentials: Boolean = false,
    enqueueOnFailure: Boolean = false,
): AppResult<T> = execute(
    NetworkRequest(
        method = HttpMethodType.POST,
        path = path,
        query = query,
        headers = headers,
        body = jsonBody(body),
        requiresAuth = requiresAuth,
        carriesCredentials = carriesCredentials,
        enqueueOnFailure = enqueueOnFailure,
    ),
    serializer<T>(),
)

suspend inline fun <reified B, reified T> NetworkClient.put(
    path: String,
    body: B,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    enqueueOnFailure: Boolean = false,
): AppResult<T> = execute(
    NetworkRequest(
        method = HttpMethodType.PUT,
        path = path,
        query = query,
        headers = headers,
        body = jsonBody(body),
        requiresAuth = requiresAuth,
        enqueueOnFailure = enqueueOnFailure,
    ),
    serializer<T>(),
)

suspend inline fun <reified B, reified T> NetworkClient.patch(
    path: String,
    body: B,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    enqueueOnFailure: Boolean = false,
): AppResult<T> = execute(
    NetworkRequest(
        method = HttpMethodType.PATCH,
        path = path,
        query = query,
        headers = headers,
        body = jsonBody(body),
        requiresAuth = requiresAuth,
        enqueueOnFailure = enqueueOnFailure,
    ),
    serializer<T>(),
)

suspend inline fun <reified T> NetworkClient.delete(
    path: String,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    enqueueOnFailure: Boolean = false,
): AppResult<T> = execute(
    NetworkRequest(
        method = HttpMethodType.DELETE,
        path = path,
        query = query,
        headers = headers,
        requiresAuth = requiresAuth,
        enqueueOnFailure = enqueueOnFailure,
    ),
    serializer<T>(),
)

/** A multipart upload. [timeoutMillis] gives a large file longer than the default request timeout. */
suspend inline fun <reified T> NetworkClient.upload(
    path: String,
    parts: List<MultipartPart>,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    timeoutMillis: Long? = null,
): AppResult<T> = execute(
    NetworkRequest(
        method = HttpMethodType.POST,
        path = path,
        query = query,
        headers = headers,
        body = RequestBody.Multipart(parts),
        requiresAuth = requiresAuth,
        timeoutMillis = timeoutMillis,
    ),
    serializer<T>(),
)

@PublishedApi
internal inline fun <reified B> jsonBody(body: B): RequestBody.Json =
    RequestBody.Json(NetworkJson.encodeToString(serializer<B>(), body))
