package com.example.videoalone.core.designsystem.component.sheet.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** B-24 to B-26: the rubber band past a sheet's edges, at density 1 on the reference phone. */
class SheetStretchTest {

    /** At Large the band runs to Large's stretch line, 0.008 H above Large's top. */
    private val atLarge = SheetBand(0.008f * 873f)

    @Test
    fun `B-24 it rises with the pull and stays below its asymptote`() {
        var last = 0f
        for (pull in 1..2000 step 50) {
            val shown = atLarge.shown(pull.toFloat())
            assertTrue(shown > last)
            assertTrue(shown < 0.008f * 873f)
            last = shown
        }
    }

    @Test
    fun `B-24 at Large a long over-pull shows a few dp`() {
        assertEquals(6.6f, atLarge.shown(200f), 0.1f)
    }

    @Test
    fun `B-24 a Medium-only sheet stretches toward Large's stretch line`() {
        val mediumOnly = SheetBand(436f - (43f - 0.008f * 873f))
        assertEquals(86f, mediumOnly.shown(200f), 1f)
    }

    @Test
    fun `B-25 below a sheet that may not be dismissed the band runs to hidden`() {
        val blocked = SheetBand(873f - 436f)
        assertEquals(120f, blocked.shown(300f), 1f)
    }

    @Test
    fun `the inverse finds the pull that shows a value, so a caught band resumes in place`() {
        val band = SheetBand(300f)
        for (pull in listOf(10f, 120f, 900f)) {
            assertEquals(pull, band.pull(band.shown(pull)), 0.05f)
        }
    }

    @Test
    fun `B-26 the slope starts at the resistance and falls as the band stretches`() {
        val band = SheetBand(300f)
        assertEquals(0.55f, band.slope(0f), 0.0001f)
        assertTrue(band.slope(400f) < band.slope(100f))
    }
}
