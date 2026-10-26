package com.example.cmp.core.designsystem.foundation.glass

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Display
import android.view.FrameMetrics
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi

/**
 * Counts frames whose GPU work took longer than one refresh period, in windows of
 * [windowFrames], and calls the GPU behind once [badWindowsInARow] consecutive windows had more
 * than [slowShare] of them. One heavy frame (a first draw, an image decode) never decides it; a
 * GPU that cannot hold the frame rate while glass is on screen does within two seconds of
 * scrolling at 120 Hz.
 *
 * The budget is the refresh period, not the frame's deadline: the GPU renders one frame at a time,
 * so a GPU that needs longer than a period per frame falls further behind with every frame, even
 * when the pipeline gives each frame a deadline two periods away.
 */
internal class GpuBudgetWindow(
    private val windowFrames: Int = 120,
    private val slowShare: Float = 0.15f,
    private val badWindowsInARow: Int = 2,
) {
    private var frames = 0
    private var slowFrames = 0
    private var badWindows = 0

    /** True when the next frame recorded starts a new window. */
    val startsWindow: Boolean
        get() = frames == 0

    /** Records one frame and returns true from the frame that completes the last bad window. */
    fun record(gpuNanos: Long, budgetNanos: Long): Boolean {
        frames++
        if (gpuNanos > budgetNanos) slowFrames++
        if (frames < windowFrames) return false
        badWindows = if (slowFrames > windowFrames * slowShare) badWindows + 1 else 0
        frames = 0
        slowFrames = 0
        return badWindows >= badWindowsInARow
    }
}

/**
 * Starts watching the frame metrics of the window [view] is in, and calls [onBehind] on the main
 * thread, once, when [GpuBudgetWindow] decides the GPU cannot keep up. Returns what stops the
 * watch; null below S, which reports no GPU time and does not blur either, and where there is no
 * activity window.
 */
internal fun watchGpuBudget(view: View, onBehind: () -> Unit): (() -> Unit)? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val window = view.context.findActivity()?.window ?: return null
    val display = view.display ?: return null
    return GpuVerdictMonitor(window, display, view, onBehind)::detach
}

/** The frame-metrics listener [watchGpuBudget] starts, on a thread of its own. */
@RequiresApi(Build.VERSION_CODES.S)
private class GpuVerdictMonitor(
    private val window: Window,
    private val display: Display,
    private val view: View,
    private val onBehind: () -> Unit,
) {
    private val thread = HandlerThread("glass-frame-metrics").apply { start() }
    private val budget = GpuBudgetWindow()
    private var periodNanos = 0L
    private var decided = false

    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
        if (!decided) record(metrics)
    }

    init {
        window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
    }

    private fun record(metrics: FrameMetrics) {
        if (metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) return
        // The rate changes with what is on screen (idle at 60 Hz, scrolling at 120 Hz) and with
        // per-app frame rate overrides, so it is read again for every window rather than once.
        if (budget.startsWindow) periodNanos = (NanosPerSecond / display.refreshRate).toLong()
        if (budget.record(metrics.getMetric(FrameMetrics.GPU_DURATION), periodNanos)) {
            decided = true
            view.post(onBehind)
        }
    }

    fun detach() {
        window.removeOnFrameMetricsAvailableListener(listener)
        thread.quitSafely()
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val NanosPerSecond = 1_000_000_000f
