package com.example.finalafter.crui2.core.analytics.jank

import android.view.Window
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import com.example.finalafter.crui2.core.analytics.AnalyticsEvent
import com.example.finalafter.crui2.core.analytics.AnalyticsTracker
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Counts janky frames on people's devices, per UI state, and sends the counts as one anonymous
 * [AnalyticsEvent] per state each time the app leaves the foreground.
 *
 * A state is whatever was put into the window's
 * [androidx.metrics.performance.PerformanceMetricsState] while a frame was drawn, such as
 * `nav=push:DetailKey` while a push moves the screen, so a jank regression is attributed to what
 * was happening. Frames drawn with no state count under `idle`. A frame is janky by JankStats'
 * measure: it took more than twice the refresh interval.
 *
 * JankStats hands over each frame on a background thread from API 24, so the cost on the UI thread
 * is nothing beyond the platform's own frame metrics.
 */
@Singleton
class FrameJankRecorder @Inject internal constructor(
    private val analytics: AnalyticsTracker,
) {
    private val counts = HashMap<String, FrameCounts>()
    private var reportingWhenHidden = false

    /**
     * Given every frame as well, on JankStats' thread, while set. Debug tooling reads the frames
     * here rather than putting a second JankStats on the window, which would repeat its per-frame
     * work. The [FrameData] is reused for the next frame once the call returns.
     */
    @Volatile
    var frameListener: JankStats.OnFrameListener? = null

    /** Counts the frames of [window] while [lifecycle] is resumed. Call from `Activity.onCreate`. */
    fun track(window: Window, lifecycle: Lifecycle) {
        if (!reportingWhenHidden) {
            reportingWhenHidden = true
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) = report()
                },
            )
        }
        val stats = JankStats.createAndTrack(window, ::onFrame)
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    stats.isTrackingEnabled = true
                }

                override fun onPause(owner: LifecycleOwner) {
                    stats.isTrackingEnabled = false
                }
            },
        )
    }

    internal fun onFrame(frame: FrameData) {
        record(frame.stateKey(), frame.isJank)
        frameListener?.onFrame(frame)
    }

    internal fun record(state: String, janky: Boolean) {
        synchronized(counts) {
            val forState = counts.getOrPut(state) { FrameCounts() }
            forState.frames++
            if (janky) forState.janky++
        }
    }

    internal fun report() {
        val taken = synchronized(counts) { counts.toMap().also { counts.clear() } }
        taken.forEach { (state, counted) ->
            analytics.track(
                AnalyticsEvent(
                    name = EVENT_NAME,
                    parameters = mapOf("state" to state, "frames" to counted.frames, "janky" to counted.janky),
                ),
            )
        }
    }

    private fun FrameData.stateKey(): String =
        if (states.isEmpty()) {
            IDLE_STATE
        } else {
            // Analytics vendors cap a parameter value at about a hundred characters.
            states.joinToString(separator = ",") { it.key + "=" + it.value }.take(MAX_STATE_LENGTH)
        }

    private class FrameCounts {
        var frames = 0
        var janky = 0
    }

    private companion object {
        const val EVENT_NAME = "frame_jank"
        const val IDLE_STATE = "idle"
        const val MAX_STATE_LENGTH = 100
    }
}
