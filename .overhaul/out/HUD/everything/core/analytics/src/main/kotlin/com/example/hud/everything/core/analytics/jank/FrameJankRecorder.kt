package com.example.hud.everything.core.analytics.jank

import android.view.Window
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.StateInfo
import com.example.hud.everything.core.analytics.AnalyticsEvent
import com.example.hud.everything.core.analytics.AnalyticsTracker
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
    private val stateKeys = StateKeys()
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
        record(stateKeys.of(frame.states), frame.isJank)
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

    private class FrameCounts {
        var frames = 0
        var janky = 0
    }

    internal companion object {
        const val EVENT_NAME = "frame_jank"
        const val IDLE_STATE = "idle"
        const val MAX_STATE_LENGTH = 100
    }
}

/**
 * The analytics key of a frame's states, built again only when they change. The states of
 * consecutive frames are the same [StateInfo] instances until something calls `putState`, so a
 * screen that keeps a state for its whole life costs one string, not one per frame.
 *
 * Called on JankStats' thread only.
 */
internal class StateKeys {
    private val last = arrayOfNulls<StateInfo>(MAX_REMEMBERED)
    private var lastCount = -1
    private var lastKey = FrameJankRecorder.IDLE_STATE

    fun of(states: List<StateInfo>): String {
        if (states.isEmpty()) return FrameJankRecorder.IDLE_STATE
        if (states.size == lastCount && sameAsLast(states)) return lastKey

        // Analytics vendors cap a parameter value at about a hundred characters.
        lastKey = states.joinToString(separator = ",") { it.key + "=" + it.value }
            .take(FrameJankRecorder.MAX_STATE_LENGTH)
        // More states than are remembered never match, so their key is built every frame: correct,
        // and no screen sets that many.
        lastCount = if (states.size <= MAX_REMEMBERED) states.size else -1
        for (index in 0 until minOf(states.size, MAX_REMEMBERED)) last[index] = states[index]
        return lastKey
    }

    private fun sameAsLast(states: List<StateInfo>): Boolean {
        for (index in states.indices) {
            if (states[index] !== last[index]) return false
        }
        return true
    }

    private companion object {
        const val MAX_REMEMBERED = 8
    }
}
