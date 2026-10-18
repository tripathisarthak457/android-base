package com.example.hud.fixture.core.perfhud.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hud.fixture.core.designsystem.component.text.AppText
import com.example.hud.fixture.core.designsystem.theme.AppTheme
import com.example.hud.fixture.core.perfhud.R
import com.example.hud.fixture.core.perfhud.frame.FrameStats
import com.example.hud.fixture.core.perfhud.frame.ScreenFrames
import com.example.hud.fixture.core.perfhud.panel.graph.FpsGraph
import com.example.hud.fixture.core.perfhud.panel.graph.FrameTimeGraph
import com.example.hud.fixture.core.perfhud.panel.graph.HistogramGraph
import com.example.hud.fixture.core.perfhud.recording.PerformanceRecorder
import com.example.hud.fixture.core.perfhud.recording.PerformanceSnapshot
import com.example.hud.fixture.core.perfhud.score.decimal

/** The frame-time graph, the numbers, frames per second, the histogram and jank by screen. */
@Composable
internal fun FrameSections(recorder: PerformanceRecorder, snapshot: PerformanceSnapshot, version: () -> Int) {
    val frames = snapshot.frames
    val overall = frames.overall
    Section(title = R.string.perfhud_frame_times, caption = R.string.perfhud_frame_times_caption) {
        FrameTimeGraph(frames = recorder.frames, version = version)
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        ValueRow(R.string.perfhud_frames, overall.frames.toString())
        ValueRow(R.string.perfhud_recorded, duration(snapshot.recordedMillis))
        ValueRow(R.string.perfhud_fps, decimal(frames.framesPerSecond, 1))
        ValueRow(R.string.perfhud_display, stringResource(R.string.perfhud_hz, Math.round(frames.refreshHz)))
        ValueRow(R.string.perfhud_janky, countAndPercent(overall.janky, overall.jankyPercent))
        ValueRow(R.string.perfhud_slow, countAndPercent(overall.slow, overall.slowPercent))
        ValueRow(R.string.perfhud_frozen, overall.frozen.toString())
        ValueRow(R.string.perfhud_long_work, overall.longWork.toString())
        ValueRow(R.string.perfhud_percentiles, percentiles(overall))
        ValueRow(
            R.string.perfhud_ui_percentiles,
            listOf(frames.uiP50Millis, frames.uiP90Millis, frames.uiP99Millis).map { millis(it) }.joinToString(" / "),
        )
        ValueRow(R.string.perfhud_worst, millis(overall.worstMillis))
    }
    Section(title = R.string.perfhud_fps_graph, caption = R.string.perfhud_fps_graph_caption) {
        FpsGraph(frames = recorder.frames, refreshHz = { frames.refreshHz }, version = version)
    }
    Section(title = R.string.perfhud_histogram, caption = R.string.perfhud_histogram_caption) {
        HistogramGraph(
            frames = recorder.frames,
            budgetMillis = { if (frames.refreshHz > 0f) MILLIS_PER_SECOND / frames.refreshHz else 0f },
            version = version,
        )
    }
    Section(title = R.string.perfhud_by_screen) {
        val screens = frames.screens.sortedByDescending { it.stats.jankyPercent }
        val worst = screens.maxOfOrNull { it.stats.jankyPercent } ?: 0f
        screens.forEach { screen -> ScreenRow(screen, scaleTop = maxOf(worst, MIN_BAR_SCALE_PERCENT)) }
    }
}

@Composable
private fun ScreenRow(screen: ScreenFrames, scaleTop: Float) {
    val stats = screen.stats
    val bar = when {
        stats.jankyPercent <= GOOD_JANKY_PERCENT -> AppTheme.colors.success.content
        stats.jankyPercent <= FAIR_JANKY_PERCENT -> AppTheme.colors.warning.content
        else -> AppTheme.colors.danger.content
    }
    val track = AppTheme.colors.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppText(
                text = screenName(screen),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.contentPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            AppText(
                text = stringResource(
                    R.string.perfhud_screen_row,
                    stringResource(R.string.perfhud_value_percent, decimal(stats.jankyPercent, 1)),
                    stats.frames,
                    millis(stats.p90Millis),
                ),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.contentSecondary,
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .drawBehind {
                    drawRect(track)
                    val share = (stats.jankyPercent / scaleTop).coerceIn(0f, 1f)
                    drawRect(bar, size = Size(size.width * share, size.height))
                },
        )
    }
}

@Composable
private fun screenName(screen: ScreenFrames): String = when {
    screen.name != null -> screen.name
    screen.isOther -> stringResource(R.string.perfhud_screen_other)
    else -> stringResource(R.string.perfhud_screen_launch)
}

@Composable
private fun countAndPercent(count: Int, percent: Float): String =
    "$count (" + stringResource(R.string.perfhud_value_percent, decimal(percent, 2)) + ")"

@Composable
private fun percentiles(stats: FrameStats): String =
    listOf(stats.p50Millis, stats.p90Millis, stats.p99Millis).map { millis(it) }.joinToString(" / ")

@Composable
internal fun millis(value: Float): String = stringResource(R.string.perfhud_value_millis, decimal(value, 1))

internal fun duration(millis: Long): String {
    val seconds = millis / MILLIS_PER_SECOND_LONG
    return (seconds / SECONDS_PER_MINUTE).toString() + ":" + (seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')
}

private val BarHeight = 6.dp
private const val MILLIS_PER_SECOND = 1000f
private const val MILLIS_PER_SECOND_LONG = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val GOOD_JANKY_PERCENT = 0.5f
private const val FAIR_JANKY_PERCENT = 2f

/** Below this, a screen's bar is drawn against 5 %, so a 0.1 % screen does not fill its row. */
private const val MIN_BAR_SCALE_PERCENT = 5f
