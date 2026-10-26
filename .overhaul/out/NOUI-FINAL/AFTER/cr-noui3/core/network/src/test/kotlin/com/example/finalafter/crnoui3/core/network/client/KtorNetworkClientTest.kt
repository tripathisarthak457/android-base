package com.example.finalafter.crnoui3.core.network.client

import com.example.finalafter.crnoui3.core.common.coroutines.ApplicationScope
import com.example.finalafter.crnoui3.core.common.result.AppError
import com.example.finalafter.crnoui3.core.common.result.AppResult
import com.example.finalafter.crnoui3.core.common.result.errorOrNull
import com.example.finalafter.crnoui3.core.common.time.AppClock
import com.example.finalafter.crnoui3.core.network.appstatus.ApiSignals
import com.example.finalafter.crnoui3.core.network.appstatus.AppStatus
import com.example.finalafter.crnoui3.core.network.appstatus.AppStatusRules
import com.example.finalafter.crnoui3.core.network.model.HttpMethodType
import com.example.finalafter.crnoui3.core.network.model.NetworkRequest
import com.example.finalafter.crnoui3.core.network.model.RequestBody
import com.example.finalafter.crnoui3.core.network.queue.RequestQueue
import com.example.finalafter.crnoui3.core.network.response.PassthroughUnwrapper
import com.example.finalafter.crnoui3.core.testing.FakeNetworkMonitor
import com.example.finalafter.crnoui3.core.testing.testDispatchers
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
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorNetworkClientTest {

    private val monitor = FakeNetworkMonitor(initiallyOnline = true)
    private val seen = mutableListOf<HttpRequestData>()
    private val config = NetworkConfig(baseUrl = "https://api.example.com/", webSocketUrl = "", isDebug = false)
    private val signals = ApiSignals(AppStatusRules(), config)
    private val queued = mutableListOf<NetworkRequest>()

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
            config = config,
            networkMonitor = monitor,
            unwrapper = PassthroughUnwrapper(),
            retryPolicy = RetryPolicy(random = Random(1)),
            inFlight = InFlightRequests(ApplicationScope(backgroundScope)),
            dispatchers = testDispatchers(),
            clock = AppClock { Instant.fromEpochSeconds(NOW_SECONDS) },
            signals = signals,
            queue = RequestQueue { queued += it },
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
    fun `retry-after in seconds decides the wait and is reported`() = runTest {
        val client = client({ respond("", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.RetryAfter, "2")) })

        val error = client.execute(get()).errorOrNull() as AppError.Http

        assertEquals(2L, error.retryAfterSeconds)
        assertEquals(2 * 2_000L, testScheduler.currentTime)
    }

    @Test
    fun `retry-after as a date is read against the clock`() = runTest {
        val inFiveSeconds = "Thu, 01 Jan 1970 00:00:15 GMT"
        val client = client({ respond("", HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.RetryAfter, inFiveSeconds)) })

        val error = client.execute(get()).errorOrNull() as AppError.Http

        assertEquals(5L, error.retryAfterSeconds)
    }

    @Test
    fun `a dropped connection while online is retried and then a timeout`() = runTest {
        val client = client({ throw IOException("reset") })

        val result = client.execute(get())

        assertEquals(AppError.Timeout, result.errorOrNull())
        assertEquals(1 + RetryPolicy.DEFAULT_MAX_RETRIES, seen.size)
    }

    @Test
    fun `no answer while offline is reported as offline without retrying`() = runTest {
        monitor.setOnline(false)
        val client = client({ throw IOException("unreachable") })

        val result = client.execute(get())

        assertEquals(AppError.Offline, result.errorOrNull())
        assertEquals(1, seen.size)
    }

    @Test
    fun `a request is sent even when the device looks offline`() = runTest {
        monitor.setOnline(false)
        val client = client({ respond("{}", HttpStatusCode.OK) })

        val result = client.execute(get())

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `an empty success body decodes into Unit`() = runTest {
        val client = client({ respond("", HttpStatusCode.NoContent) })

        val result = client.execute(NetworkRequest(HttpMethodType.DELETE, "items/1"), Unit.serializer())

        assertEquals(AppResult.Success(Unit), result)
    }

    @Test
    fun `a body that does not match the type is a decode error`() = runTest {
        val client = client({ respond("""{"id":"not a number"}""", HttpStatusCode.OK) })

        val result = client.execute(get(), Item.serializer())

        assertTrue(result.errorOrNull() is AppError.Decode)
    }

    @Test
    fun `the server's message, code and field errors are read once`() = runTest {
        val body = """{"error":{"code":"name_taken","message":"Name taken","errors":{"name":["Taken"]}}}"""
        val client = client({ respond(body, HttpStatusCode.UnprocessableEntity) })

        val error = client.execute(get()).errorOrNull() as AppError.Http

        assertEquals("Name taken", error.message)
        assertEquals("name_taken", error.code)
        assertEquals(mapOf("name" to listOf("Taken")), error.fieldErrors)
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
    fun `a request's own header replaces the default rather than adding a second`() = runTest {
        val client = client({ respond("{}", HttpStatusCode.OK) })

        client.execute(get())
        client.execute(get().copy(path = "other", headers = mapOf("accept-language" to "fr")))

        assertEquals(Locale.getDefault().toLanguageTag(), seen[0].headers[HttpHeaders.AcceptLanguage])
        assertEquals(listOf("fr"), seen[1].headers.getAll(HttpHeaders.AcceptLanguage))
    }

    @Test
    fun `a json body is sent as it was encoded`() = runTest {
        val client = client({ respond("{}", HttpStatusCode.OK) })

        client.execute(NetworkRequest(HttpMethodType.POST, "items", body = RequestBody.Json("""{"a":1}""")))

        assertEquals("application/json", seen.single().body.contentType?.withoutParameters().toString())
    }

    @Test
    fun `a 426 from any call asks for an update`() = runTest {
        val client = client({ respond("", HttpStatusCode.UpgradeRequired) })

        client.execute(get())

        assertEquals(AppStatus.UpdateRequired, signals.status.value)
    }

    @Test
    fun `a queueable mutation that gets no answer is queued with an idempotency key`() = runTest {
        monitor.setOnline(false)
        val client = client({ throw IOException("offline") })

        val result = client.execute(
            NetworkRequest(HttpMethodType.POST, "notes", body = RequestBody.Json("{}"), enqueueOnFailure = true),
        )

        assertEquals(AppError.Offline, result.errorOrNull())
        assertTrue(queued.single().headers["Idempotency-Key"].orEmpty().isNotBlank())
    }

    @Test
    fun `each queued mutation's idempotency key is a fresh random UUID`() = runTest {
        monitor.setOnline(false)
        val client = client({ throw IOException("offline") })
        val mutation = NetworkRequest(HttpMethodType.POST, "notes", body = RequestBody.Json("{}"), enqueueOnFailure = true)

        client.execute(mutation)
        client.execute(mutation)

        val keys = queued.map { it.headers["Idempotency-Key"].orEmpty() }
        assertEquals(2, keys.size)
        keys.forEach { assertTrue(it, Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}").matches(it)) }
        assertEquals(2, keys.toSet().size)
    }

    private fun get() = NetworkRequest(HttpMethodType.GET, "items")

    @Serializable
    private data class Item(val id: Int)

    private companion object {
        const val NOW_SECONDS = 10L
    }
}
