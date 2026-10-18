package com.example.hud.everything.core.perfhud.network

import com.example.hud.everything.core.devtools.log.NetworkExchange
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkTimingsTest {

    @Test
    fun `the median and p90 are requests that happened, by nearest rank`() {
        val timings = NetworkTimings.of((1..10).map { exchange("/posts", millis = it * 100L) })

        assertEquals(10, timings.requests)
        assertEquals(500L, timings.medianMillis)
        assertEquals(900L, timings.p90Millis)
        assertEquals(1000L, timings.slowestMillis)
    }

    @Test
    fun `endpoints are grouped by method and path, the most called first`() {
        val timings = NetworkTimings.of(
            listOf(
                exchange("/posts?page=1", 100),
                exchange("/posts?page=2", 300),
                exchange("/users/1", 50, status = 500),
                exchange("/posts", 200, method = "POST"),
            ),
        )

        assertEquals(listOf("GET /posts", "GET /users/1", "POST /posts"), timings.endpoints.map { it.endpoint })
        assertEquals(2, timings.endpoints.first().requests)
        assertEquals(1, timings.failures)
        assertEquals(1, timings.endpoints.single { it.endpoint == "GET /users/1" }.failures)
    }

    @Test
    fun `no requests read as none`() {
        assertEquals(NetworkTimings.None, NetworkTimings.of(emptyList()))
    }

    private fun exchange(path: String, millis: Long, status: Int = 200, method: String = "GET") = NetworkExchange(
        method = method,
        url = "https://api.example.com$path",
        status = status,
        startedAtMillis = 0L,
        durationMillis = millis,
        requestHeaders = emptyMap(),
        responseHeaders = emptyMap(),
        requestBody = null,
        responseBody = null,
        responseBytes = 0L,
        failure = null,
    )
}
