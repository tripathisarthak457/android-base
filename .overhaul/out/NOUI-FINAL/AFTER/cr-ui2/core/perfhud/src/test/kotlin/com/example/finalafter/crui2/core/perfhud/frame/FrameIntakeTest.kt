package com.example.finalafter.crui2.core.perfhud.frame

import android.hardware.display.DisplayManager
import androidx.metrics.performance.FrameDataApi24
import androidx.metrics.performance.FrameDataApi31
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameIntakeTest {

    private val log = FrameLog(capacity = 64)
    private val screens = ScreenTimeline()
    private val intake = FrameIntake(log, RefreshRate(mockk<DisplayManager>(relaxed = true)), screens)

    @Test
    fun `a frame past its deadline is janky, however far past`() {
        intake.onFrame(api31(totalMillis = 9f, overrunMillis = 0.5f))
        intake.onFrame(api31(totalMillis = 7f, overrunMillis = -1.3f))

        val overall = log.summary().overall
        assertEquals(2, overall.frames)
        assertEquals(1, overall.janky)
    }

    @Test
    fun `before API 31 a frame is janky when its CPU time passes the refresh interval`() {
        intake.onFrame(FrameDataApi24(0L, millis(10f), millis(18f), false, emptyList()))
        intake.onFrame(FrameDataApi24(1L, millis(4f), millis(9f), false, emptyList()))

        assertEquals(1, log.summary().overall.janky)
    }

    @Test
    fun `each frame counts for the screen in front when its main-thread work ended`() {
        intake.onFrame(api31(startMillis = 0f))
        screens.enter("Home", atNanos = millis(10f))
        repeat(3) { intake.onFrame(api31(startMillis = 10f + 8 * it)) }
        screens.enter("Detail", atNanos = millis(41f))
        // Reported after the navigation, as JankStats reports every frame a little late, but its
        // main-thread part (3 ms) ended before it.
        intake.onFrame(api31(startMillis = 34f))
        // Started before the navigation, ended after it: the frame that opens the new screen.
        intake.onFrame(api31(startMillis = 39f, overrunMillis = 4f))
        repeat(2) { intake.onFrame(api31(startMillis = 50f + 8 * it)) }

        val counted = log.summary().screens.associate { it.name to it.stats }
        assertEquals(1, counted.getValue(null).frames)
        assertEquals(4, counted.getValue("Home").frames)
        assertEquals(0, counted.getValue("Home").janky)
        assertEquals(3, counted.getValue("Detail").frames)
        assertEquals(1, counted.getValue("Detail").janky)
    }

    private fun api31(
        startMillis: Float = 0f,
        totalMillis: Float = 6f,
        overrunMillis: Float = -2f,
    ) = FrameDataApi31(
        // Start, UI time, CPU time, total, overrun past the deadline, JankStats' own verdict, states.
        millis(startMillis),
        millis(totalMillis / 2),
        millis(totalMillis * 0.8f),
        millis(totalMillis),
        millis(overrunMillis),
        false,
        emptyList(),
    )

    private fun millis(value: Float): Long = (value * 1_000_000).toLong()
}
