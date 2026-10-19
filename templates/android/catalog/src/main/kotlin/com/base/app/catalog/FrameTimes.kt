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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
    private val durations = LongArray(WindowFrames)
    private val late = BooleanArray(WindowFrames)
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
        val total = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
        durations[count % WindowFrames] = total
        late[count % WindowFrames] = total > deadline(metrics)
        count++
        sinceShown++
        if (sinceShown < MinNewFrames) return
        sinceShown = 0
        val frames = minOf(count, WindowFrames)
        val recent = durations.take(frames).sorted()
        val p50 = recent[recent.size / 2] / NanosPerMilli
        val p90 = recent[(recent.size * 9) / 10] / NanosPerMilli
        val missed = late.take(frames).count { it } * 100f / frames
        val text = "%.1f / %.1f ms  %.0f%%".format(p50, p90, missed)
        main.post { summary = text }
    }

    /**
     * The time the system gave this frame. A frame may take longer than one refresh and still be
     * shown on time when the pipeline has a buffer queued, so one refresh is only the fallback where
     * the platform does not report the deadline.
     */
    private fun deadline(metrics: FrameMetrics): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) metrics.getMetric(FrameMetrics.DEADLINE) else budgetNanos

    private companion object {
        fun refreshRate(activity: Activity): Float =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display?.refreshRate ?: DefaultRate
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.refreshRate
            }

        const val DefaultRate = 60f
        const val WindowFrames = 240
        const val MinNewFrames = 20
        const val NanosPerMilli = 1_000_000.0
    }
}

@Composable
fun FrameTimesReadout(frames: FrameTimes, modifier: Modifier = Modifier) {
    val text = frames.summary ?: return
    AppFiguresText(
        text = text,
        modifier = modifier
            .padding(bottom = ReadoutMargin)
            .background(AppTheme.colors.surfaceInverse.copy(alpha = 0.8f), AppTheme.shapes.pill)
            .padding(horizontal = 8.dp, vertical = ReadoutPadding),
        style = AppTheme.typography.caption2,
        color = AppTheme.colors.contentInverse,
    )
}

/**
 * How far the readout reaches above the navigation bar, from its text's line height rather than a
 * measurement, so a page that pads its end by it is laid out right before the readout first shows.
 */
@Composable
@ReadOnlyComposable
fun frameTimesReadoutReach(): Dp =
    with(LocalDensity.current) { AppTheme.typography.caption2.lineHeight.toDp() } + ReadoutPadding * 2 + ReadoutMargin

private val ReadoutPadding = 3.dp
private val ReadoutMargin = 4.dp
