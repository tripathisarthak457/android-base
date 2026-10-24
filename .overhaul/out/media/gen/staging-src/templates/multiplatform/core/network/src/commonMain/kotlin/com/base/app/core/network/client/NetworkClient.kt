package com.base.app.core.network.client

import com.base.app.core.common.result.AppResult
import com.base.app.core.network.model.HttpMethodType
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.model.NetworkResponse
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.serializer

/**
 * The whole network surface. Every failure arrives as an `AppError`: `Offline` when the device has
 * no network, `Timeout` when it has one but the server did not answer, `Http` for a refusal (its
 * code and field errors already read), `Decode` for a body that is not what the caller asked for.
 * Nothing is thrown except cancellation.
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
): AppResult<T> = execute(NetworkRequest(HttpMethodType.GET, path, query, headers), serializer<T>())

suspend inline fun <reified B, reified T> NetworkClient.post(
    path: String,
    body: B,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
): AppResult<T> = execute(
    NetworkRequest(
        method = HttpMethodType.POST,
        path = path,
        query = query,
        headers = headers,
        jsonBody = NetworkJson.encodeToString(serializer<B>(), body),
    ),
    serializer<T>(),
)
