package com.base.app.core.perfhud.log

import android.util.Log
import com.base.app.core.perfhud.frame.FrameHistogram
import com.base.app.core.perfhud.frame.FrameLog
import com.base.app.core.perfhud.frame.FrameStats
import com.base.app.core.perfhud.frame.FrameTotals
import com.base.app.core.perfhud.memory.MemoryLog
import com.base.app.core.perfhud.memory.MemorySummary
import java.util.Locale
import javax.inject.Inject

/**
 * The overlay's log: a line for every few seconds in which the app drew, and a line for each screen
 * it leaves, under the one tag [TAG] and never per frame. Each line is `key=value` pairs, so
 * `adb logcat -s PerfHud` reads as a table and a script can split it. A window line's numbers,
 * garbage collections included, are that window's; the memory sizes are the latest sample's. A
 * screen line sums every visit to the screen since recording began, not only the one just left.
 *
 * `frameP*` are percentiles of each frame's full duration, vsync to GPU done, which on-time frames
 * routinely run past one interval; `uiP*` of its main-thread time, the part the score grades.
 *
 * Written straight to logcat, not through the app's `Log` seam: that drops informational lines from
 * release builds, and devRelease is where these numbers are real. Called from the sampler's thread.
 */
internal class PerfLogcat(private val write: (String) -> Unit) {

    @Inject
    constructor() : this({ line -> Log.i(TAG, line) })

    private val previous = FrameTotals()
    private val current = FrameTotals()
    private val window = FrameHistogram()
    private val windowUi = FrameHistogram()
    private var lastScreen: String? = null
    private var seconds = 0
    private var gcBefore = 0L

    /**
     * Starts counting windows from what [frames] and [memory] hold now, which an earlier recording
     * may have left.
     */
    fun begin(frames: FrameLog, memory: MemoryLog) {
        frames.copyTotals(previous)
        gcBefore = memory.summary()?.gcCount ?: 0L
        lastScreen = null
        seconds = 0
    }

    fun note(message: String) = write(message)

    /** Called once a second while recording, with the screen in front at that moment. */
    fun onSecond(frames: FrameLog, memory: MemoryLog, screen: String?) {
        seconds++
        if (seconds % WINDOW_SECONDS == 0) {
            frames.copyTotals(current)
            val memoryNow = memory.summary()
            if (current.frames > previous.frames) write(windowLine(memoryNow, screen))
            current.copyInto(previous)
            // Moved on whether or not the window was written, so a line never counts the
            // collections of a quiet window before it.
            if (memoryNow != null) gcBefore = memoryNow.gcCount
        }
        if (screen != lastScreen) {
            val left = lastScreen
            lastScreen = screen
            if (left != null) {
                val stats = frames.summary().screens.firstOrNull { it.name == left }?.stats
                if (stats != null) write(screenLine(left, stats))
            }
        }
    }

    private fun windowLine(memory: MemorySummary?, screen: String?): String {
        current.histogram.subtract(previous.histogram, into = window)
        current.uiHistogram.subtract(previous.uiHistogram, into = windowUi)
        val frames = current.frames - previous.frames
        val busy = current.busyNanos - previous.busyNanos
        // A frame's busy time is known once the next one starts, so the totals hold one fewer than
        // their frame count.
        val timedFrames = (current.frames - 1).coerceAtLeast(0) - (previous.frames - 1).coerceAtLeast(0)
        return buildString {
            append("window=").append(WINDOW_SECONDS).append('s')
            append(" frames=").append(frames)
            append(" fps=").append(if (busy > 0) format(timedFrames * NANOS_PER_SECOND / busy) else "-")
            append(" janky=").append(format(percent(current.janky - previous.janky, frames))).append('%')
            append(" frameP50=").append(format(window.percentile(P50)))
            append(" frameP90=").append(format(window.percentile(P90)))
            append(" frameP99=").append(format(window.percentile(P99)))
            append(" uiP50=").append(format(windowUi.percentile(P50)))
            append(" uiP90=").append(format(windowUi.percentile(P90)))
            append(" uiP99=").append(format(windowUi.percentile(P99)))
            append(" slow=").append(current.slow - previous.slow)
            append(" frozen=").append(current.frozen - previous.frozen)
            append(" long=").append(current.longWork - previous.longWork)
            if (memory != null) {
                append(" heapMb=").append(format(memory.javaHeapMb))
                append(" nativeMb=").append(format(memory.nativeHeapMb))
                memory.pssMb?.let { append(" pssMb=").append(format(it)) }
                append(" gc=").append(memory.gcCount - gcBefore)
            }
            append(" screen=").append(screen ?: "-")
        }
    }

    private fun screenLine(screen: String, stats: FrameStats): String = buildString {
        append("screen=").append(screen)
        append(" frames=").append(stats.frames)
        append(" janky=").append(format(stats.jankyPercent)).append('%')
        append(" frameP50=").append(format(stats.p50Millis))
        append(" frameP90=").append(format(stats.p90Millis))
        append(" frameP99=").append(format(stats.p99Millis))
        append(" worst=").append(format(stats.worstMillis))
        append(" slow=").append(stats.slow)
        append(" frozen=").append(stats.frozen)
    }

    private fun FrameTotals.copyInto(other: FrameTotals) {
        other.frames = frames
        other.janky = janky
        other.slow = slow
        other.frozen = frozen
        other.longWork = longWork
        other.busyNanos = busyNanos
        histogram.copyInto(other.histogram)
        uiHistogram.copyInto(other.uiHistogram)
    }

    companion object {
        const val TAG = "PerfHud"

        /** Seconds between window lines. */
        const val WINDOW_SECONDS = 5

        private const val P50 = 0.5f
        private const val P90 = 0.9f
        private const val P99 = 0.99f
        private const val NANOS_PER_SECOND = 1_000_000_000f

        private fun percent(part: Int, whole: Int): Float = if (whole == 0) 0f else part * 100f / whole

        private fun format(value: Float): String = String.format(Locale.ROOT, "%.1f", value)
    }
}
