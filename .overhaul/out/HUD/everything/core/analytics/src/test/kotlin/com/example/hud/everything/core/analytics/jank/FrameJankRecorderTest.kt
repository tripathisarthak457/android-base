package com.example.hud.everything.core.analytics.jank

import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.StateInfo
import com.example.hud.everything.core.analytics.AnalyticsEvent
import com.example.hud.everything.core.analytics.AnalyticsTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameJankRecorderTest {

    private val tracked = mutableListOf<AnalyticsEvent>()
    private val recorder = FrameJankRecorder(RecordingTracker(tracked))

    @Test
    fun `frames are counted per state and reported once each`() {
        repeat(3) { recorder.record("nav=push:DetailKey", janky = false) }
        recorder.record("nav=push:DetailKey", janky = true)
        recorder.record("idle", janky = false)

        recorder.report()

        assertEquals(
            setOf(
                mapOf("state" to "nav=push:DetailKey", "frames" to 4, "janky" to 1),
                mapOf("state" to "idle", "frames" to 1, "janky" to 0),
            ),
            tracked.map { it.parameters }.toSet(),
        )
        assertTrue(tracked.all { it.name == "frame_jank" })
    }

    @Test
    fun `a report starts the next count from zero`() {
        recorder.record("idle", janky = true)
        recorder.report()
        tracked.clear()

        recorder.report()

        assertTrue(tracked.isEmpty())
    }

    @Test
    fun `a frame is counted under its states and handed to the listener`() {
        val seen = mutableListOf<Long>()
        recorder.frameListener = JankStats.OnFrameListener { seen += it.frameStartNanos }
        val push = StateInfo("nav", "push:DetailKey")

        recorder.onFrame(FrameData(1L, 4_000_000L, false, listOf(push)))
        recorder.onFrame(FrameData(2L, 30_000_000L, true, emptyList()))
        recorder.frameListener = null
        recorder.onFrame(FrameData(3L, 4_000_000L, false, emptyList()))
        recorder.report()

        assertEquals(listOf(1L, 2L), seen)
        assertEquals(
            setOf(
                mapOf("state" to "nav=push:DetailKey", "frames" to 1, "janky" to 0),
                mapOf("state" to "idle", "frames" to 2, "janky" to 1),
            ),
            tracked.map { it.parameters }.toSet(),
        )
    }

    private class RecordingTracker(private val events: MutableList<AnalyticsEvent>) : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) {
            events += event
        }

        override fun setUserId(userId: String?) = Unit

        override fun setUserProperty(name: String, value: String?) = Unit

        override fun setEnabled(enabled: Boolean) = Unit
    }
}
