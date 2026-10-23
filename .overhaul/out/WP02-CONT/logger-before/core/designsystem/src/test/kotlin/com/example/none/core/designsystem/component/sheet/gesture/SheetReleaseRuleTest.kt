package com.example.none.core.designsystem.component.sheet.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The release rule worked at density 1 on an 873 dp tall phone window: Large's top at 43, Medium's at 436,
 * hidden at 873, so 2 H/s is 1746 px/s and the detent flick 400 px/s.
 */
class SheetReleaseRuleTest {

    private val both = floatArrayOf(43f, 436f)
    private val largeOnly = floatArrayOf(43f)
    private val thresholds = ReleaseThresholds.of(density = 1f, travelLength = 873f)

    private fun release(offset: Float, velocity: Float, offsets: FloatArray = both, dismissAllowed: Boolean = true) =
        releaseTarget(offset, velocity, offsets, hidden = 873f, dismissAllowed = dismissAllowed, thresholds = thresholds)

    @Test
    fun `B-22 a slow release lands where it would coast`() {
        assertEquals(SheetRelease.Stop(1), release(436f, -300f).target)
        assertEquals(SheetRelease.Stop(1), release(376f, 200f).target)
    }

    @Test
    fun `B-22 a flick moves exactly one stop, never to hidden`() {
        assertEquals(SheetRelease.Stop(0), release(436f, -600f).target)
        assertEquals(SheetRelease.Stop(1), release(43f, 1200f).target)
        assertEquals(SheetRelease.Stop(1), release(43f, 3000f).target)
    }

    @Test
    fun `B-22 dismissal takes two heights a second, or a release most of the way out`() {
        assertEquals(SheetRelease.Dismiss, release(43f, 1800f, largeOnly).target)
        assertEquals(SheetRelease.Stop(1), release(436f, 1200f).target)
        assertEquals(SheetRelease.Dismiss, release(436f, 1800f).target)
        assertEquals(SheetRelease.Stop(1), release(586f, 900f).target)
        assertEquals(SheetRelease.Dismiss, release(686f, 0f).target)
    }

    @Test
    fun `B-22 an upward flick below the lowest stop returns to it`() {
        assertEquals(SheetRelease.Stop(1), release(700f, -500f).target)
    }

    @Test
    fun `B-22 an over-pull above the tallest stop returns to it`() {
        assertEquals(SheetRelease.Stop(0), release(30f, 2000f).target)
    }

    @Test
    fun `B-22 boundaries are inclusive`() {
        assertEquals(SheetRelease.Stop(0), release(436f, -400f).target)
        assertEquals(SheetRelease.Dismiss, release(436f, 1746f).target)
        val atShare = 436f + 0.48f * (873f - 436f)
        assertEquals(SheetRelease.Dismiss, release(atShare, 0f).target)
        assertEquals(SheetRelease.Stop(1), release(atShare - 1f, 0f).target)
    }

    @Test
    fun `B-22 a single-detent sheet settles back or closes`() {
        assertEquals(SheetRelease.Stop(0), release(100f, 300f, largeOnly).target)
        assertEquals(SheetRelease.Dismiss, release(500f, 0f, largeOnly).target)
    }

    @Test
    fun `B-22 a sheet that may not close settles back and the guard hears one attempt`() {
        val blocked = release(686f, 0f, dismissAllowed = false)
        assertEquals(SheetRelease.Stop(1), blocked.target)
        assertTrue(blocked.attempted)
        assertFalse(release(500f, 0f, dismissAllowed = false).attempted)
    }

    @Test
    fun `B-22 a modal side panel is judged along its width`() {
        val panel = ReleaseThresholds.of(density = 1f, travelLength = 1280f)
        val out = releaseTarget(0f, 2600f, floatArrayOf(0f), hidden = 376f, dismissAllowed = true, thresholds = panel)
        assertEquals(SheetRelease.Dismiss, out.target)
        val stays = releaseTarget(100f, 900f, floatArrayOf(0f), hidden = 376f, dismissAllowed = true, thresholds = panel)
        assertEquals(SheetRelease.Stop(0), stays.target)
    }
}
