package com.base.app.core.network

import com.base.app.core.common.AppResult
import com.base.app.core.network.model.CachePolicy
import com.base.app.core.network.model.HttpMethodType
import com.base.app.core.network.model.MultipartPart
import com.base.app.core.network.model.NetworkException
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.model.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * The whole network surface: one operation, plus typed conveniences.
 *
 * [execute] resolves connectivity, authentication, caching and offline queueing and hands back
 * the raw response. The `get`/`post`/… helpers below decode it. They are extension functions
 * rather than interface members because a generic type parameter cannot be `reified` on an
 * override — which is also why there is a non-reified [request] for use inside `override fun`
 * bodies, where an explicit serializer is passed instead.
 */
interface NetworkClient {
    suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse>
}

/**
 * Decodes a successful response into [T].
 *
 * The decode runs on the IO dispatcher. `execute` already does its work there, but it returns to
 * the *caller's* dispatcher, which for a ViewModel is `Main.immediate` — so without this hop,
 * every list response in the app is parsed on the UI thread and shows up as dropped frames on
 * exactly the screens with the most data.
 */
@PublishedApi
internal suspend inline fun <reified T> NetworkClient.decode(
    request: NetworkRequest,
    unwrapper: ResponseUnwrapper,
): AppResult<T> = when (val result = execute(request)) {
    is AppResult.Success -> withContext(Dispatchers.IO) {
        runCatching {
            val root = NetworkJson.parseToJsonElement(result.data.body)
            val payload = unwrapper.payload(root)
            AppResult.Success(
                data = NetworkJson.decodeFromJsonElement<T>(payload),
                fromCache = result.data.fromCache,
                cachedAtEpochMillis = result.data.cachedAtEpochMillis,
            )
        }.getOrElse { throwable ->
            AppResult.Failure(
                message = "Could not read the server's response.",
                cause = NetworkException.Serialization(throwable),
                code = result.data.statusCode,
                rawBody = result.data.body,
            )
        }
    }

    is AppResult.Failure -> result
}

/** The non-reified form, for use inside an `override fun` where reification is unavailable. */
suspend fun <T> NetworkClient.request(
    request: NetworkRequest,
    deserializer: KSerializer<T>,
    unwrapper: ResponseUnwrapper,
): AppResult<T> = when (val result = execute(request)) {
    is AppResult.Success -> withContext(Dispatchers.IO) {
        runCatching {
            val root = NetworkJson.parseToJsonElement(result.data.body)
            AppResult.Success(
                data = NetworkJson.decodeFromJsonElement(deserializer, unwrapper.payload(root)),
                fromCache = result.data.fromCache,
                cachedAtEpochMillis = result.data.cachedAtEpochMillis,
            )
        }.getOrElse { throwable ->
            AppResult.Failure(
                message = "Could not read the server's response.",
                cause = NetworkException.Serialization(throwable),
                code = result.data.statusCode,
                rawBody = result.data.body,
            )
        }
    }

    is AppResult.Failure -> result
}

suspend inline fun <reified T> NetworkClient.get(
    path: String,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    cache: CachePolicy = CachePolicy.Disabled,
    unwrapper: ResponseUnwrapper = PassthroughUnwrapper(),
): AppResult<T> = decode(
    NetworkRequest(
        method = HttpMethodType.GET,
        path = path,
        query = query,
        headers = headers,
        requiresAuth = requiresAuth,
        cache = cache,
    ),
    unwrapper,
)

suspend inline fun <reified B, reified T> NetworkClient.post(
    path: String,
    body: B? = null,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    enqueueOnFailure: Boolean = false,
    unwrapper: ResponseUnwrapper = PassthroughUnwrapper(),
): AppResult<T> = decode(
    NetworkRequest(
        method = HttpMethodType.POST,
        path = path,
        query = query,
        headers = headers,
        body = body?.let { NetworkJson.encodeToJsonElement(it) },
        requiresAuth = requiresAuth,
        enqueueOnFailure = enqueueOnFailure,
    ),
    unwrapper,
)

suspend inline fun <reified B, reified T> NetworkClient.put(
    path: String,
    body: B? = null,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    enqueueOnFailure: Boolean = false,
    unwrapper: ResponseUnwrapper = PassthroughUnwrapper(),
): AppResult<T> = decode(
    NetworkRequest(
        method = HttpMethodType.PUT,
        path = path,
        query = query,
        headers = headers,
        body = body?.let { NetworkJson.encodeToJsonElement(it) },
        requiresAuth = requiresAuth,
        enqueueOnFailure = enqueueOnFailure,
    ),
    unwrapper,
)

suspend inline fun <reified B, reified T> NetworkClient.patch(
    path: String,
    body: B? = null,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    enqueueOnFailure: Boolean = false,
    unwrapper: ResponseUnwrapper = PassthroughUnwrapper(),
): AppResult<T> = decode(
    NetworkRequest(
        method = HttpMethodType.PATCH,
        path = path,
        query = query,
        headers = headers,
        body = body?.let { NetworkJson.encodeToJsonElement(it) },
        requiresAuth = requiresAuth,
        enqueueOnFailure = enqueueOnFailure,
    ),
    unwrapper,
)

suspend inline fun <reified T> NetworkClient.delete(
    path: String,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    enqueueOnFailure: Boolean = false,
    unwrapper: ResponseUnwrapper = PassthroughUnwrapper(),
): AppResult<T> = decode(
    NetworkRequest(
        method = HttpMethodType.DELETE,
        path = path,
        query = query,
        headers = headers,
        requiresAuth = requiresAuth,
        enqueueOnFailure = enqueueOnFailure,
    ),
    unwrapper,
)

suspend inline fun <reified T> NetworkClient.upload(
    path: String,
    parts: List<MultipartPart>,
    query: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    requiresAuth: Boolean = true,
    unwrapper: ResponseUnwrapper = PassthroughUnwrapper(),
): AppResult<T> = decode(
    NetworkRequest(
        method = HttpMethodType.POST,
        path = path,
        query = query,
        headers = headers,
        parts = parts,
        requiresAuth = requiresAuth,
        // File bytes are never written to the offline queue: the queue is a small database, and
        // a queued 8MB photo is a database that no longer opens.
        enqueueOnFailure = false,
    ),
    unwrapper,
)
