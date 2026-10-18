package com.example.hud.fixture.core.perfhud.frame

import androidx.metrics.performance.FrameData
import androidx.metrics.performance.FrameDataApi24
import androidx.metrics.performance.FrameDataApi31
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.StateInfo

/**
 * Turns each frame JankStats reports into a [FrameLog] entry, on JankStats' thread.
 *
 * A frame is janky when it missed its deadline, which on API 31 and later is what the platform's
 * FrameTimeline counts and so what `perf.sh` and `dumpsys gfxinfo` report; JankStats' own
 * `isJank`, a frame over twice the refresh interval, counts about half as many.
 */
internal class FrameIntake(
    private val log: FrameLog,
    private val refresh: RefreshRate,
) : JankStats.OnFrameListener {

    override fun onFrame(volatileFrameData: FrameData) {
        val frame = volatileFrameData
        val interval = refresh.intervalNanos
        val total: Long
        val missedDeadline: Boolean
        when (frame) {
            is FrameDataApi31 -> {
                total = frame.frameDurationTotalNanos
                missedDeadline = frame.frameOverrunNanos > 0
            }
            is FrameDataApi24 -> {
                total = frame.frameDurationCpuNanos
                missedDeadline = total > interval
            }
            else -> {
                total = frame.frameDurationUiNanos
                missedDeadline = frame.isJank
            }
        }
        log.add(
            startNanos = frame.frameStartNanos,
            uiNanos = frame.frameDurationUiNanos,
            totalNanos = total,
            intervalNanos = interval,
            missedDeadline = missedDeadline,
            screen = screenOf(frame.states),
        )
    }

    /**
     * The route the frame was drawn for. A frame that spans a navigation carries both routes, the
     * old one first; it counts for the new one, so a push's first frames land on the screen it
     * opens.
     */
    private fun screenOf(states: List<StateInfo>): String? {
        for (index in states.lastIndex downTo 0) {
            val state = states[index]
            if (state.key == ROUTE_STATE) return state.value
        }
        return null
    }

    companion object {
        /** The `PerformanceMetricsState` key the current route's name is put under. */
        const val ROUTE_STATE = "route"
    }
}
