package com.example.modulescompact.core.network

import com.example.modulescompact.core.common.coroutines.AppDispatchers
import com.example.modulescompact.core.common.coroutines.ApplicationScope
import com.example.modulescompact.core.common.result.AppError
import com.example.modulescompact.core.common.result.AppResult
import com.example.modulescompact.core.common.result.errorOrNull
import com.example.modulescompact.core.network.model.HttpMethodType
import com.example.modulescompact.core.network.model.NetworkRequest
import com.example.modulescompact.core.testing.FakeNetworkMonitor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import java.util.Locale
import kotlin.random.Random
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

class KtorNetworkClientTest {

    private val monitor = FakeNetworkMonitor(initiallyOnline = true)
    private val seen = mutableListOf<HttpRequestData>()

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
            inFlight = InFlightRequests(ApplicationScope(backgroundScope)),
            dispatchers = StandardTestDispatcher(testScheduler).let { AppDispatchers(io = it, default = it, main = it) },
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

        assertEquals(503, (result.errorOrNull() as AppError.Http).status)
        assertEquals(1, seen.size)
    }

    @Test
    fun `retries stop at the limit and the failure carries no english of its own`() = runTest {
        val client = client({ respond("", HttpStatusCode.BadGateway) })

        val error = client.execute(get()).errorOrNull() as AppError.Http

        assertEquals(1 + RetryPolicy.DEFAULT_MAX_RETRIES, seen.size)
        assertEquals(502, error.status)
        assertNull(error.message)
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

        val error = client.execute(get()).errorOrNull() as AppError.Http

        assertEquals("Name taken", error.message)
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

    private fun get() = NetworkRequest(HttpMethodType.GET, "items")
}
