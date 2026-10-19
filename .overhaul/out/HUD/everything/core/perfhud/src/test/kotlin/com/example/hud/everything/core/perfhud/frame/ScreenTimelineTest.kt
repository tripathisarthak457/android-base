package com.example.hud.everything.core.perfhud.frame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenTimelineTest {

    private val timeline = ScreenTimeline()

    @Test
    fun `a frame before any screen was entered has none`() {
        assertNull(timeline.screenAt(endNanos = 100L))

        timeline.enter("Home", atNanos = 200L)

        assertNull(timeline.screenAt(endNanos = 150L))
    }

    @Test
    fun `a frame counts for the screen in front when its main-thread work ended`() {
        timeline.enter("Home", atNanos = 100L)
        timeline.enter("Detail", atNanos = 500L)

        assertEquals("Home", timeline.screenAt(endNanos = 400L))
        // Reported after the navigation, drawn before it: still the old screen's, as JankStats
        // would have it.
        assertEquals("Home", timeline.screenAt(endNanos = 500L))
        assertEquals("Detail", timeline.screenAt(endNanos = 501L))
    }

    @Test
    fun `a restart covers the frames drawn before it`() {
        timeline.enter("Home", atNanos = 100L)

        timeline.restart("Detail")

        assertEquals("Detail", timeline.screenAt(endNanos = 50L))
        assertEquals("Detail", timeline.screenAt(endNanos = 1_000L))
    }

    @Test
    fun `a restart with no screen forgets every screen`() {
        timeline.enter("Home", atNanos = 100L)

        timeline.restart(null)

        assertNull(timeline.screenAt(endNanos = 1_000L))
    }

    @Test
    fun `entering the screen already in front keeps when it was entered`() {
        timeline.restart("Home")

        timeline.enter("Home", atNanos = 500L)

        assertEquals("Home", timeline.screenAt(endNanos = 50L))
    }

    @Test
    fun `the recent screens are kept and the oldest dropped`() {
        repeat(12) { index -> timeline.enter("Screen$index", atNanos = 100L * (index + 1)) }

        // Eight are kept, so Screen4, entered at 500, is the oldest left.
        assertEquals("Screen11", timeline.screenAt(endNanos = Long.MAX_VALUE))
        assertEquals("Screen4", timeline.screenAt(endNanos = 501L))
        assertNull(timeline.screenAt(endNanos = 500L))
    }
}
