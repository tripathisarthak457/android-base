package com.base.app.catalog

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppFiguresText
import com.base.app.core.designsystem.theme.AppTheme

/**
 * How long recent frames took, from the platform's frame metrics: the median and the 90th
 * percentile over the last couple of seconds, and the share that missed the display's deadline.
 * A rough check while tuning a component on a device; the numbers that count come from a release
 * build and `perf.sh`.
 *
 * The readout redraws only after several new frames, so it never keeps an idle screen drawing.
 */
@Stable
class FrameTimes(private val activity: Activity) {
    var summary by mutableStateOf<String?>(null)
        private set

    private val main = Handler(Looper.getMainLooper())
    private val durations = LongArray(WINDOW)
    private var count = 0
    private var sinceShown = 0
    private val budgetNanos = (1_000_000_000L / refreshRate(activity)).toLong()
    private val thread = HandlerThread("catalog-frames").apply { start() }
    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ -> record(metrics) }

    init {
        activity.window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
    }

    /** Stops listening; the activity calls this as it is destroyed, so a recreation leaves no thread behind. */
    fun stop() {
        activity.window.removeOnFrameMetricsAvailableListener(listener)
        thread.quitSafely()
    }

    private fun record(metrics: FrameMetrics) {
        if (metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) return
        durations[count % WINDOW] = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
        count++
        sinceShown++
        if (sinceShown < MIN_NEW_FRAMES) return
        sinceShown = 0
        val recent = durations.take(minOf(count, WINDOW)).sorted()
        val p50 = recent[recent.size / 2] / NANOS_PER_MILLI
        val p90 = recent[(recent.size * 9) / 10] / NANOS_PER_MILLI
        val missed = recent.count { it > budgetNanos } * 100f / recent.size
        val text = "%.1f / %.1f ms  %.0f%%".format(p50, p90, missed)
        main.post { summary = text }
    }

    private companion object {
        fun refreshRate(activity: Activity): Float =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display?.refreshRate ?: DEFAULT_RATE
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.refreshRate
            }

        const val DEFAULT_RATE = 60f
        const val WINDOW = 240
        const val MIN_NEW_FRAMES = 20
        const val NANOS_PER_MILLI = 1_000_000.0
    }
}

@Composable
fun FrameTimesReadout(frames: FrameTimes, modifier: Modifier = Modifier) {
    val text = frames.summary ?: return
    AppFiguresText(
        text = text,
        modifier = modifier
            .background(AppTheme.colors.surfaceInverse.copy(alpha = 0.8f), AppTheme.shapes.pill)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = AppTheme.typography.caption2,
        color = AppTheme.colors.contentInverse,
    )
}
