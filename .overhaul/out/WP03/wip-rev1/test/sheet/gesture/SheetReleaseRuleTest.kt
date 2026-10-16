package com.base.app.core.designsystem.component.sheet.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** B-22's worked table on the reference phone (Large 43 dp, Medium 436 dp, hidden 873 dp), at density 1. */
class SheetReleaseRuleTest {

    private val thresholds = ReleaseThresholds.of(density = 1f, boundsHeight = 873f)
    private val mediumAndLarge = floatArrayOf(43f, 436f)
    private val largeOnly = floatArrayOf(43f)

    private fun release(offset: Float, velocity: Float, offsets: FloatArray = mediumAndLarge, dismissible: Boolean = true) =
        releaseTarget(offset, velocity, offsets, hidden = 873f, dismissAllowed = dismissible, thresholds = thresholds)

    private val large = SheetRelease.Stop(0)
    private val medium = SheetRelease.Stop(1)

    @Test
    fun `a slow release lands on the projected stop`() {
        assertEquals(medium, release(436f, -300f).target)
        assertEquals(medium, release(376f, 200f).target)
    }

    @Test
    fun `a flick moves exactly one stop`() {
        assertEquals(large, release(436f, -600f).target)
        assertEquals(medium, release(43f, 1200f).target)
        assertEquals(medium, release(43f, 3000f).target)
    }

    @Test
    fun `dismissal needs two window heights a second or the projection past 52 percent`() {
        assertEquals(SheetRelease.Dismiss, release(43f, 1800f, offsets = largeOnly).target)
        assertEquals(medium, release(436f, 1200f).target)
        assertEquals(SheetRelease.Dismiss, release(436f, 1800f).target)
        assertEquals(SheetRelease.Dismiss, release(436f + 250f, 0f).target)
    }

    @Test
    fun `an over-pulled sheet returns to its tallest stop whatever the velocity`() {
        assertEquals(large, release(30f, 2000f).target)
    }

    @Test
    fun `an upward flick below the lowest stop returns to it`() {
        assertEquals(medium, release(700f, -500f).target)
    }

    @Test
    fun `boundaries are inclusive`() {
        assertEquals(large, release(436f, -400f).target)
        assertEquals(SheetRelease.Dismiss, release(436f, 1746f).target)
        assertEquals(medium, release(436f + 0.51f * 437f, 0f).target)
    }

    @Test
    fun `a single-stop sheet flicked down tests dismissal`() {
        assertEquals(SheetRelease.Stop(0), release(43f, 500f, offsets = largeOnly).target)
    }

    @Test
    fun `a blocked dismissal stays and reports one attempt`() {
        val blocked = release(436f + 300f, 0f, dismissible = false)
        assertEquals(medium, blocked.target)
        assertTrue(blocked.attempted)
        val gentle = release(436f + 20f, 0f, dismissible = false)
        assertEquals(medium, gentle.target)
        assertFalse(gentle.attempted)
    }
}
