package com.base.app.core.designsystem.component.sheet.layout

import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Test

class PrepareRampTest {

    private val density = Density(1f)

    private fun next(room: Float, nanos: Long = 4_000_000L, gain: Float = 0f, ended: Boolean = false, full: Float = 2_000f) =
        PrepareRamp.next(room, full, nanos, gain, ended, density)

    @Test
    fun `the first step is a few rows`() {
        assertEquals(160f, next(Float.NaN))
    }

    @Test
    fun `a step takes as much as the last one measured in the frame's budget`() {
        assertEquals(500f, next(room = 300f, nanos = 6_000_000L, gain = 300f))
    }

    @Test
    fun `a slow step still moves on, and a fast one stays bounded`() {
        assertEquals(400f + 64f, next(room = 400f, nanos = 10_000_000L, gain = 100f))
        assertEquals(400f + 320f, next(room = 400f, nanos = 0L, gain = 100f))
    }

    @Test
    fun `content that ended takes its full room, and no step passes it`() {
        assertEquals(2_000f, next(room = 400f, ended = true))
        assertEquals(500f, next(room = 450f, nanos = 0L, gain = 100f, full = 500f))
        assertEquals(100f, next(Float.NaN, full = 100f))
    }
}
