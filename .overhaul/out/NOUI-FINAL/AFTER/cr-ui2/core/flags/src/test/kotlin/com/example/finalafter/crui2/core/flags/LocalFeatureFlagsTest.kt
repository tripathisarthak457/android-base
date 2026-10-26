package com.example.finalafter.crui2.core.flags

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalFeatureFlagsTest {

    private val overrides = FlagOverrides()
    private val flags: FeatureFlags = LocalFeatureFlags(overrides)

    private val banner = Flag.Bool("banner", default = false)
    private val greeting = Flag.Text("greeting", default = "Hello")
    private val pageSize = Flag.Number("page_size", default = 20L)

    @Test
    fun `a flag nobody has overridden reads its declared default`() {
        assertEquals(false, flags[banner])
        assertEquals("Hello", flags[greeting])
        assertEquals(20L, flags[pageSize])
    }

    @Test
    fun `an override wins until it is reset`() {
        overrides.set(greeting, "Hi")
        assertEquals("Hi", flags[greeting])

        overrides.reset(greeting)

        assertEquals("Hello", flags[greeting])
    }

    @Test
    fun `an override of one flag leaves the others alone`() {
        overrides.set(pageSize, 50L)

        assertEquals(50L, flags[pageSize])
        assertEquals(false, flags[banner])
    }

    @Test
    fun `observing follows each change of that flag and nothing else`() = runTest {
        flags.observe(banner).test {
            assertEquals(false, awaitItem())

            overrides.set(banner, true)
            assertEquals(true, awaitItem())

            overrides.set(pageSize, 50L)
            overrides.set(banner, true)
            overrides.reset(banner)
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `signing out drops every override`() = runTest {
        overrides.set(banner, true)
        overrides.set(greeting, "Hi")

        overrides.clear()

        assertEquals(false, flags[banner])
        assertEquals("Hello", flags[greeting])
    }
}
