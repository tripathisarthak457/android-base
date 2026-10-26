package com.example.finalafter.crui.core.devtools.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevToolsLogTest {

    private val log = DevToolsLog()

    @Test
    fun `credentials in headers never reach the log`() {
        log.record(exchange(requestHeaders = mapOf("Authorization" to "Bearer abc", "Accept" to "application/json")))

        val recorded = log.exchanges.value.single()
        assertFalse(recorded.requestHeaders.getValue("Authorization").contains("abc"))
        assertEquals("application/json", recorded.requestHeaders.getValue("Accept"))
    }

    @Test
    fun `token and password fields in a body are redacted, the rest is kept`() {
        log.record(
            exchange(
                requestBody = """{"email":"a@b.c","password":"hunter2"}""",
                responseBody = """{"accessToken":"abc","refresh_token" : "d\"ef","name":"Ada"}""",
            ),
        )

        val recorded = log.exchanges.value.single()
        val request = checkNotNull(recorded.requestBody)
        val response = checkNotNull(recorded.responseBody)
        assertFalse(request.contains("hunter2"))
        assertTrue(request.contains("a@b.c"))
        assertFalse(response.contains("abc"))
        assertFalse(response.contains("d\\\"ef"))
        assertTrue(response.contains("\"name\":\"Ada\""))
    }

    @Test
    fun `the newest exchange comes first and each gets its own id`() {
        log.record(exchange(url = "https://api.example.com/first"))
        log.record(exchange(url = "https://api.example.com/second"))

        val recorded = log.exchanges.value
        assertEquals("/second", recorded.first().path)
        assertTrue(recorded[0].id != recorded[1].id)
    }

    private fun exchange(
        url: String = "https://api.example.com/items",
        requestHeaders: Map<String, String> = emptyMap(),
        requestBody: String? = null,
        responseBody: String? = null,
    ) = NetworkExchange(
        method = "POST",
        url = url,
        status = 200,
        startedAtMillis = 0,
        durationMillis = 12,
        requestHeaders = requestHeaders,
        responseHeaders = emptyMap(),
        requestBody = requestBody,
        responseBody = responseBody,
        responseBytes = responseBody?.length?.toLong() ?: 0,
        failure = null,
    )
}
