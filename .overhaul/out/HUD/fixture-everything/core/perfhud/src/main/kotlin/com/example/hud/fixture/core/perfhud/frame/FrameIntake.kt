package com.example.hud.fixture.core.perfhud.frame

import androidx.metrics.performance.FrameData
import androidx.metrics.performance.FrameDataApi24
import androidx.metrics.performance.FrameDataApi31
import androidx.metrics.performance.JankStats

/**
 * Turns each frame JankStats reports into a [FrameLog] entry, on JankStats' thread, counted for the
 * screen [ScreenTimeline] had in front when it was drawn.
 *
 * A frame is janky when it missed its deadline, which on API 31 and later is what the platform's
 * FrameTimeline counts and so what `perf.sh` and `dumpsys gfxinfo` report; JankStats' own
 * `isJank`, a frame over twice the refresh interval, counts about half as many.
 */
internal class FrameIntake(
    private val log: FrameLog,
    private val refresh: RefreshRate,
    private val screens: ScreenTimeline,
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
            // JankStats' own end of a frame for its states: its start plus the main thread's part.
            screen = screens.screenAt(frame.frameStartNanos + frame.frameDurationUiNanos),
        )
    }
}
