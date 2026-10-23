package com.example.everything.core.network.client

import com.example.everything.core.network.model.HttpMethodType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {

    private val policy = RetryPolicy(maxRetries = 2, baseDelayMillis = 400, maxDelayMillis = 8_000, random = Random(7))

    @Test
    fun `a repeatable request is retried on a transient status`() {
        listOf(408, 429, 500, 502, 503, 504).forEach { status ->
            assertTrue("$status", policy.delayBefore(0, HttpMethodType.GET, status, retryAfterSeconds = null) != null)
        }
    }

    @Test
    fun `a status that will not change is not retried`() {
        listOf(400, 401, 403, 404, 409, 422, 501).forEach { status ->
            assertNull("$status", policy.delayBefore(0, HttpMethodType.GET, status, retryAfterSeconds = null))
        }
    }

    @Test
    fun `a post or patch is never repeated, because it might be applied twice`() {
        assertNull(policy.delayBefore(0, HttpMethodType.POST, 503, retryAfterSeconds = null))
        assertNull(policy.delayBefore(0, HttpMethodType.PATCH, status = null, retryAfterSeconds = null))
    }

    @Test
    fun `no answer at all counts as transient`() {
        assertTrue(policy.delayBefore(0, HttpMethodType.PUT, status = null, retryAfterSeconds = null) != null)
    }

    @Test
    fun `it stops after the configured number of retries`() {
        assertTrue(policy.delayBefore(1, HttpMethodType.GET, 503, null) != null)
        assertNull(policy.delayBefore(2, HttpMethodType.GET, 503, null))
    }

    @Test
    fun `the wait doubles and stays between half and all of its ceiling`() {
        repeat(50) {
            val first = requireNotNull(policy.delayBefore(0, HttpMethodType.GET, 503, null))
            val second = requireNotNull(policy.delayBefore(1, HttpMethodType.GET, 503, null))
            assertTrue(first in 200..400)
            assertTrue(second in 400..800)
        }
    }

    @Test
    fun `retry-after is believed, up to the cap`() {
        assertEquals(3_000L, policy.delayBefore(0, HttpMethodType.GET, 429, retryAfterSeconds = 3))
        assertEquals(8_000L, policy.delayBefore(0, HttpMethodType.GET, 503, retryAfterSeconds = 600))
    }
}
