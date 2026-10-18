package com.example.hud.everything.core.perfhud.score

import android.content.res.Resources
import androidx.annotation.StringRes
import com.example.hud.everything.core.perfhud.R
import java.util.Locale

/** How each metric reads to a person: its name, the fix for lost points, and where the docs say more. */
@get:StringRes
internal val Metric.title: Int
    get() = when (this) {
        Metric.JankyFrames -> R.string.perfhud_metric_janky_frames
        Metric.FrameTimeP90 -> R.string.perfhud_metric_frame_p90
        Metric.FrameTimeP99 -> R.string.perfhud_metric_frame_p99
        Metric.SlowFrames -> R.string.perfhud_metric_slow_frames
        Metric.FrozenFrames -> R.string.perfhud_metric_frozen_frames
        Metric.LongMainThreadWork -> R.string.perfhud_metric_long_work
        Metric.ColdStart -> R.string.perfhud_metric_cold_start
        Metric.WarmStart -> R.string.perfhud_metric_warm_start
        Metric.AppMemory -> R.string.perfhud_metric_app_memory
        Metric.BlockingGc -> R.string.perfhud_metric_blocking_gc
        Metric.StrictModeClean -> R.string.perfhud_metric_strict_mode
        Metric.Minified -> R.string.perfhud_metric_minified
        Metric.BaselineProfile -> R.string.perfhud_metric_baseline_profile
        Metric.NotDebuggable -> R.string.perfhud_metric_not_debuggable
    }

@get:StringRes
internal val Metric.fix: Int
    get() = when (this) {
        Metric.JankyFrames -> R.string.perfhud_fix_janky_frames
        Metric.FrameTimeP90 -> R.string.perfhud_fix_frame_p90
        Metric.FrameTimeP99 -> R.string.perfhud_fix_frame_p99
        Metric.SlowFrames -> R.string.perfhud_fix_slow_frames
        Metric.FrozenFrames -> R.string.perfhud_fix_frozen_frames
        Metric.LongMainThreadWork -> R.string.perfhud_fix_long_work
        Metric.ColdStart -> R.string.perfhud_fix_cold_start
        Metric.WarmStart -> R.string.perfhud_fix_warm_start
        Metric.AppMemory -> R.string.perfhud_fix_app_memory
        Metric.BlockingGc -> R.string.perfhud_fix_blocking_gc
        Metric.StrictModeClean -> R.string.perfhud_fix_strict_mode
        Metric.Minified -> R.string.perfhud_fix_minified
        Metric.BaselineProfile -> R.string.perfhud_fix_baseline_profile
        Metric.NotDebuggable -> R.string.perfhud_fix_not_debuggable
    }

/** The section of the project's `docs/PERFORMANCE-OVERLAY.md` about this metric. */
internal val Metric.doc: String
    get() = DOC + "#" + when (this) {
        Metric.JankyFrames -> "janky-frames"
        Metric.FrameTimeP90, Metric.FrameTimeP99 -> "frame-time-percentiles"
        Metric.SlowFrames, Metric.FrozenFrames -> "slow-and-frozen-frames"
        Metric.LongMainThreadWork -> "long-main-thread-work"
        Metric.ColdStart, Metric.WarmStart -> "start-up"
        Metric.AppMemory, Metric.BlockingGc -> "memory"
        Metric.StrictModeClean -> "strictmode"
        Metric.Minified, Metric.BaselineProfile, Metric.NotDebuggable -> "build-checks"
    }

/** [value] in the metric's unit, as the panel and the report print it. */
internal fun Resources.formatValue(metric: Metric, value: Float): String = when (metric.unit) {
    MetricUnit.Percent -> getString(R.string.perfhud_value_percent, decimal(value, 2))
    MetricUnit.BudgetShare -> getString(R.string.perfhud_value_budget, decimal(value, 2))
    MetricUnit.PerThousandFrames -> getString(R.string.perfhud_value_per_thousand, decimal(value, 1))
    MetricUnit.Millis -> getString(R.string.perfhud_value_millis, decimal(value, 0))
    MetricUnit.Megabytes -> getString(R.string.perfhud_value_megabytes, decimal(value, 1))
    MetricUnit.Count -> getString(R.string.perfhud_value_count, decimal(value, 0))
    MetricUnit.Check -> getString(if (value >= 1f) R.string.perfhud_value_pass else R.string.perfhud_value_fail)
}

/** The value an A needs, or a pass. */
internal fun Resources.formatTarget(metric: Metric): String {
    val limits = metric.limits ?: return getString(R.string.perfhud_value_pass)
    return getString(R.string.perfhud_value_target_at_most, formatValue(metric, limits.a))
}

/** A number with [places] decimals, in the format a log or a JSON reader expects whatever the locale. */
internal fun decimal(value: Float, places: Int): String = String.format(Locale.ROOT, "%.${places}f", value)

private const val DOC = "docs/PERFORMANCE-OVERLAY.md"
