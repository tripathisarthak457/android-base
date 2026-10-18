package com.example.hud.everything.core.perfhud.frame

import android.hardware.display.DisplayManager
import androidx.metrics.performance.FrameDataApi24
import androidx.metrics.performance.FrameDataApi31
import androidx.metrics.performance.StateInfo
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameIntakeTest {

    private val log = FrameLog(capacity = 64)
    private val intake = FrameIntake(log, RefreshRate(mockk<DisplayManager>(relaxed = true)))

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
    fun `each frame counts for the route it was drawn for`() {
        val home = StateInfo(FrameIntake.ROUTE_STATE, "Home")
        val detail = StateInfo(FrameIntake.ROUTE_STATE, "Detail")
        val push = StateInfo("nav", "push")

        repeat(3) { intake.onFrame(api31(states = listOf(home))) }
        // JankStats keeps a key's newest value only, so the frame that spans the navigation carries
        // the new route: it is the new screen's.
        intake.onFrame(api31(states = listOf(push, detail), overrunMillis = 4f))
        repeat(2) { intake.onFrame(api31(states = listOf(detail))) }
        intake.onFrame(api31(states = listOf(push)))

        val screens = log.summary().screens.associate { it.name to it.stats }
        assertEquals(3, screens.getValue("Home").frames)
        assertEquals(0, screens.getValue("Home").janky)
        assertEquals(3, screens.getValue("Detail").frames)
        assertEquals(1, screens.getValue("Detail").janky)
        assertEquals(1, screens.getValue(null).frames)
    }

    private fun api31(
        totalMillis: Float = 6f,
        overrunMillis: Float = -2f,
        states: List<StateInfo> = emptyList(),
    ) = FrameDataApi31(
        // Start, UI time, CPU time, total, overrun past the deadline, JankStats' own verdict, states.
        0L,
        millis(totalMillis / 2),
        millis(totalMillis * 0.8f),
        millis(totalMillis),
        millis(overrunMillis),
        false,
        states,
    )

    private fun millis(value: Float): Long = (value * 1_000_000).toLong()
}
