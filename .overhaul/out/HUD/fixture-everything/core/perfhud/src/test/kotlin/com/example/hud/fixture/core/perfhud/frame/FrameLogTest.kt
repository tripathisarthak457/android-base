package com.example.hud.fixture.core.perfhud.frame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FrameLogTest {

    private val log = FrameLog(capacity = 1024)

    @Test
    fun `frames are counted per screen and in total`() {
        draw(frames = 100, screen = "Home", totalMillis = 5f)
        draw(frames = 10, screen = "Detail", totalMillis = 20f, missedDeadline = true)
        draw(frames = 1, screen = "Detail", totalMillis = 800f, missedDeadline = true)

        val summary = log.summary()

        assertEquals(111, summary.overall.frames)
        assertEquals(11, summary.overall.janky)
        assertEquals(11, summary.overall.slow)
        assertEquals(1, summary.overall.frozen)
        val detail = summary.screens.single { it.name == "Detail" }.stats
        assertEquals(11, detail.frames)
        assertEquals(11, detail.janky)
        assertEquals(800f, detail.worstMillis, 0f)
        val home = summary.screens.single { it.name == "Home" }.stats
        assertEquals(0, home.janky)
        assertEquals(5f, home.p50Millis, 0.25f)
    }

    @Test
    fun `frames before the first screen are kept apart`() {
        draw(frames = 3, screen = null)
        draw(frames = 2, screen = "Home")

        val unknown = log.summary().screens.single { it.name == null }

        assertFalse(unknown.isOther)
        assertEquals(3, unknown.stats.frames)
    }

    @Test
    fun `long main-thread work is judged on the UI thread's share of the frame`() {
        draw(frames = 1, screen = "Home", totalMillis = 60f, uiMillis = 20f)
        draw(frames = 1, screen = "Home", totalMillis = 60f, uiMillis = 55f)

        assertEquals(1, log.summary().overall.longWork)
    }

    @Test
    fun `uninterrupted drawing at 120 Hz reads 120 frames per second`() {
        draw(frames = 240, screen = "Home", totalMillis = 6f)
        val live = LiveStats()

        log.readLive(nowNanos = clock, into = live)

        assertEquals(120, live.framesPerSecond)
        assertEquals(120, live.refreshHz)
    }

    @Test
    fun `a frame that takes three intervals costs three intervals of the second`() {
        // Ten frames on time, then one that takes three intervals and holds the next one back:
        // eleven frames in thirteen intervals, about 101 frames a second at 120 Hz.
        repeat(10) {
            draw(frames = 10, screen = "Home", totalMillis = 6f)
            draw(frames = 1, screen = "Home", totalMillis = 22f, missedDeadline = true)
            clock += 2 * INTERVAL
        }
        val live = LiveStats()

        log.readLive(nowNanos = clock, into = live)

        assertEquals(102, live.framesPerSecond, 4)
    }

    @Test
    fun `a second with little drawing reads as idle, not as a low frame rate`() {
        draw(frames = 240, screen = "Home", totalMillis = 6f)
        clock += 2_000_000_000L
        // A blinking cursor: two frames in the last second.
        draw(frames = 1, screen = "Home", totalMillis = 6f)
        clock += 500_000_000L
        draw(frames = 1, screen = "Home", totalMillis = 6f)
        val live = LiveStats()

        log.readLive(nowNanos = clock, into = live)

        assertEquals(LiveStats.IDLE, live.framesPerSecond)
    }

    @Test
    fun `the janky share is the whole recording's`() {
        draw(frames = 199, screen = "Home", totalMillis = 6f)
        draw(frames = 1, screen = "Home", totalMillis = 30f, missedDeadline = true)
        val live = LiveStats()

        log.readLive(nowNanos = clock, into = live)

        assertEquals(0.5f, live.jankyPercent, 0.001f)
    }

    @Test
    fun `the ring keeps the newest frames once it wraps`() {
        val small = FrameLog(capacity = 8)
        repeat(20) { index ->
            small.add(index * INTERVAL, MILLI, (index + 1) * MILLI, INTERVAL, missedDeadline = false, screen = null)
        }
        val series = FrameSeries(capacity = 5)

        small.copyRecent(series)

        assertEquals(5, series.count)
        assertEquals(16f, series.totalMillis[0], 0.001f)
        assertEquals(20f, series.totalMillis[4], 0.001f)
        assertEquals(20, small.summary().overall.frames)
    }

    @Test
    fun `frames per second are binned by the second they started in`() {
        draw(frames = 120, screen = "Home", totalMillis = 6f)
        clock += 3_000_000_000L
        // Every frame takes two intervals: 60 frames a second on a 120 Hz display.
        draw(frames = 60, screen = "Home", totalMillis = 14f, intervals = 2)
        val series = FpsSeries(seconds = 10)

        log.copyFramesPerSecond(nowNanos = clock, into = series)

        val drawn = series.perSecond.filter { it != FpsSeries.NONE }
        assertEquals(120f, drawn.first(), 2f)
        assertEquals(60f, drawn.last(), 2f)
        assertTrue(series.perSecond.count { it == FpsSeries.NONE } >= 6)
    }

    @Test
    fun `clear starts the recording over`() {
        draw(frames = 10, screen = "Home", missedDeadline = true)

        log.clear()

        val summary = log.summary()
        assertEquals(0, summary.overall.frames)
        assertTrue(summary.screens.isEmpty())
        assertEquals(0L, log.count)
    }

    @Test
    fun `the totals carry the busy time between frames`() {
        draw(frames = 121, screen = "Home", totalMillis = 6f)
        val totals = FrameTotals()

        log.copyTotals(totals)

        assertEquals(121, totals.frames)
        assertTrue(abs(totals.busyNanos - 1_000_000_000L) < 1_000L)
        assertEquals(121, totals.histogram.total)
    }

    private var clock = 1_000_000_000L

    /** [frames] frames, each starting [intervals] refresh intervals after the one before. */
    private fun draw(
        frames: Int,
        screen: String?,
        totalMillis: Float = 6f,
        uiMillis: Float = totalMillis / 2,
        missedDeadline: Boolean = false,
        intervals: Int = 1,
    ) {
        repeat(frames) {
            clock += INTERVAL * intervals
            log.add(
                startNanos = clock,
                uiNanos = (uiMillis * MILLI).toLong(),
                totalNanos = (totalMillis * MILLI).toLong(),
                intervalNanos = INTERVAL,
                missedDeadline = missedDeadline,
                screen = screen,
            )
        }
    }

    private fun assertEquals(expected: Int, actual: Int, tolerance: Int) =
        assertTrue("Expected about $expected, was $actual.", abs(expected - actual) <= tolerance)

    private companion object {
        const val MILLI = 1_000_000L
        const val INTERVAL = 8_333_333L
    }
}
