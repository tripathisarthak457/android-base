package com.base.app.core.analytics.jank

import androidx.metrics.performance.StateInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class StateKeysTest {

    @Test
    fun `the key of unchanged states is built once`() {
        val keys = StateKeys()
        val route = StateInfo("route", "Home")
        val nav = StateInfo("nav", "push:DetailKey")

        val first = keys.of(listOf(route, nav))
        val again = keys.of(listOf(route, nav))
        val changed = keys.of(listOf(StateInfo("route", "Detail")))

        assertEquals("route=Home,nav=push:DetailKey", first)
        assertSame(first, again)
        assertEquals("route=Detail", changed)
        assertEquals("idle", keys.of(emptyList()))
    }

    @Test
    fun `a key is capped at the vendors' parameter length`() {
        val keys = StateKeys()
        val long = StateInfo("route", "x".repeat(300))

        assertEquals(StateKeys.MAX_STATE_LENGTH, keys.of(listOf(long)).length)
    }
}
