package com.base.app.core.network

import com.base.app.core.common.AppResult
import com.base.app.core.network.model.HttpMethodType
import com.base.app.core.network.model.NetworkRequest
// <opt:room>
import com.base.app.core.network.model.CachePolicy
import com.base.app.core.network.model.NetworkResponse
import kotlinx.coroutines.flow.toList
// </opt:room>
import com.base.app.core.testing.FakeNetworkMonitor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.Locale
import kotlin.random.Random

class KtorNetworkClientTest {

    private val monitor = FakeNetworkMonitor(initiallyOnline = true)
    private val seen = mutableListOf<HttpRequestData>()
    // <opt:room>
    private val queue = RecordingQueue()
    private val cache = MemoryCache()
    // </opt:room>

    private fun TestScope.client(
        vararg replies: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): KtorNetworkClient {
        var call = 0
        val engine = MockEngine { request ->
            seen += request
            replies[minOf(call++, replies.lastIndex)](request)
        }
        return KtorNetworkClient(
            client = HttpClient(engine) { expectSuccess = false },
            config = NetworkConfig(baseUrl = "https://api.example.com/", webSocketUrl = "", isDebug = false),
            networkMonitor = monitor,
            unwrapper = PassthroughUnwrapper(),
            retryPolicy = RetryPolicy(random = Random(1)),
            inFlight = InFlightRequests(backgroundScope),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            // <opt:room>
            responseCache = cache,
            requestQueue = queue,
            // </opt:room>
        )
    }

    @Test
    fun `a get that meets a 503 is sent again and succeeds`() = runTest {
        val client = client({ respond("", HttpStatusCode.ServiceUnavailable) }, { respond("{}", HttpStatusCode.OK) })

        val result = client.execute(get())

        assertTrue(result is AppResult.Success)
        assertEquals(2, seen.size)
    }

    @Test
    fun `a post that meets a 503 is not repeated`() = runTest {
        val client = client({ respond("", HttpStatusCode.ServiceUnavailable) })

        val result = client.execute(NetworkRequest(HttpMethodType.POST, "orders"))

        assertEquals(503, (result as AppResult.Failure).code)
        assertEquals(1, seen.size)
    }

    @Test
    fun `retries stop at the limit and the failure carries no english of its own`() = runTest {
        val client = client({ respond("", HttpStatusCode.BadGateway) })

        val result = client.execute(get()) as AppResult.Failure

        assertEquals(1 + RetryPolicy.DEFAULT_MAX_RETRIES, seen.size)
        assertEquals(502, result.code)
        assertNull(result.message)
    }

    @Test
    fun `retry-after decides the wait`() = runTest {
        val client = client(
            { respond("", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.RetryAfter, "2")) },
            { respond("{}", HttpStatusCode.OK) },
        )

        client.execute(get())

        assertEquals(2_000L, testScheduler.currentTime)
    }

    @Test
    fun `a dropped connection while online is retried`() = runTest {
        val client = client({ throw IOException("reset") }, { respond("{}", HttpStatusCode.OK) })

        val result = client.execute(get())

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `the server's own message is kept for the user`() = runTest {
        val client = client({ respond("""{"message":"Name taken"}""", HttpStatusCode.UnprocessableEntity) })

        val result = client.execute(get()) as AppResult.Failure

        assertEquals("Name taken", result.message)
    }

    @Test
    fun `identical reads made together share one request`() = runTest {
        val client = client({ respond("{}", HttpStatusCode.OK) })

        val results = List(3) { async { client.execute(get()) } }.awaitAll()

        assertTrue(results.all { it is AppResult.Success })
        assertEquals(1, seen.size)
    }

    @Test
    fun `a path joins the base url and a full url is used as it is`() = runTest {
        val client = client({ respond("{}", HttpStatusCode.OK) })

        client.execute(get())
        client.execute(NetworkRequest(HttpMethodType.GET, "https://demo.example.org/posts", requiresAuth = false))

        assertEquals("https://api.example.com/items", seen[0].url.toString())
        assertEquals("https://demo.example.org/posts", seen[1].url.toString())
    }

    @Test
    fun `every request says which language the app is showing`() = runTest {
        client({ respond("{}", HttpStatusCode.OK) }).execute(get())

        assertEquals(Locale.getDefault().toLanguageTag(), seen.single().headers[HttpHeaders.AcceptLanguage])
    }
    // <opt:room>

    @Test
    fun `a queueable mutation made offline is queued with an idempotency key`() = runTest {
        monitor.setOnline(false)
        val client = client({ respond("{}", HttpStatusCode.OK) })

        val result = client.execute(
            NetworkRequest(
                method = HttpMethodType.POST,
                path = "notes",
                body = buildJsonObject { put("text", JsonPrimitive("hi")) },
                enqueueOnFailure = true,
            ),
        ) as AppResult.Failure

        assertTrue(result.queued)
        assertTrue(result.isOffline)
        assertTrue(queue.requests.single().headers["Idempotency-Key"].orEmpty().isNotBlank())
        assertEquals(0, seen.size)
    }

    @Test
    fun `a fresh saved copy is served without a request`() = runTest {
        cache.put("items", "saved", 200)
        val client = client({ respond("latest", HttpStatusCode.OK) })

        val result = client.execute(cached()) as AppResult.Success

        assertEquals("saved", result.data.body)
        assertEquals(0, seen.size)
    }

    @Test
    fun `force refresh asks the network and saves the answer`() = runTest {
        cache.put("items", "saved", 200)
        val client = client({ respond("latest", HttpStatusCode.OK) })

        val result = client.execute(cached(forceRefresh = true)) as AppResult.Success

        assertEquals("latest", result.data.body)
        assertEquals("latest", cache.any("items")?.body)
    }

    @Test
    fun `force refresh still falls back to the saved copy when the server is down`() = runTest {
        cache.put("items", "saved", 200)
        val client = client({ throw IOException("down") })

        val result = client.execute(cached(forceRefresh = true)) as AppResult.Success

        assertEquals("saved", result.data.body)
    }

    @Test
    fun `stream shows the saved copy, then the latest`() = runTest {
        cache.put("items", "saved", 200)
        val client = client({ respond("latest", HttpStatusCode.OK) })

        val bodies = client.stream(cached()).toList().map { (it as AppResult.Success).data.body }

        assertEquals(listOf("saved", "latest"), bodies)
    }

    @Test
    fun `stream reports a failed refresh instead of repeating the saved copy`() = runTest {
        cache.put("items", "saved", 200)
        val client = client({ respond("", HttpStatusCode.NotFound) })

        val results = client.stream(cached()).toList()

        assertTrue(results[0] is AppResult.Success)
        assertEquals(404, (results[1] as AppResult.Failure).code)
    }

    private fun cached(forceRefresh: Boolean = false) = get().copy(
        cache = CachePolicy.Enabled(key = "items", maxAgeMillis = 60_000, forceRefresh = forceRefresh),
    )

    private class MemoryCache : ResponseCache {
        private val saved = mutableMapOf<String, NetworkResponse>()
        override suspend fun fresh(key: String, maxAgeMillis: Long): NetworkResponse? = saved[key]
        override suspend fun any(key: String): NetworkResponse? = saved[key]
        override suspend fun put(key: String, body: String, statusCode: Int) {
            saved[key] = NetworkResponse(statusCode, body, fromCache = true)
        }
    }

    private class RecordingQueue : RequestQueue {
        val requests = mutableListOf<NetworkRequest>()
        override suspend fun enqueue(request: NetworkRequest) {
            requests += request
        }
        override suspend fun pending(): List<QueuedRequest> = emptyList()
        override suspend fun remove(id: Long) = Unit
        override suspend fun clear() = requests.clear()
    }
    // </opt:room>

    private fun get() = NetworkRequest(HttpMethodType.GET, "items")
}
