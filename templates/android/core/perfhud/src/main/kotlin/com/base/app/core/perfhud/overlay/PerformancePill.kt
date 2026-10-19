package com.base.app.core.perfhud.overlay

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.base.app.core.designsystem.foundation.appClickable
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.perfhud.R
import com.base.app.core.perfhud.frame.FrameLog
import com.base.app.core.perfhud.frame.LiveStats
import com.base.app.core.perfhud.recording.PerformanceRecorder
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Two lines under the DEV badge while recording: frames per second over the last second of
 * drawing against the display's rate, and the janky share of the recording. Tapping them opens
 * the Performance tab.
 *
 * The lines are drawn straight from the frame log, at most four times a second and only within
 * frames the app draws anyway ([redrawDuringFrames]): the pill holds no state, never recomposes
 * and never draws a frame of its own. While the app is idle it shows the numbers of its last
 * drawing. In a debug build the numbers are drawn without a verdict colour, since a debug build's
 * frames say little about the release's.
 */
@Composable
internal fun PerformancePill(
    recorder: PerformanceRecorder,
    debugBuild: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recording by recorder.recording.collectAsStateWithLifecycle()
    if (!recording) return
    LivePill(frames = recorder.frames, judged = !debugBuild, onOpen = onOpen, modifier = modifier)
}

@Composable
private fun LivePill(frames: FrameLog, judged: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    // Filled in the draw phase, on the main thread only.
    val live = remember { LiveStats() }
    val measurer = rememberTextMeasurer(cacheSize = TextCache)
    val typography = AppTheme.typography
    val style = remember(typography) { typography.mono.merge(TextStyle(fontSize = typography.labelSmall.fontSize)) }
    val fpsFormat = stringResource(R.string.perfhud_pill_fps)
    val idleFormat = stringResource(R.string.perfhud_pill_idle)
    val jankyFormat = stringResource(R.string.perfhud_pill_janky)
    val description = stringResource(R.string.perfhud_pill_description)
    val colors = AppTheme.colors
    val neutral = colors.contentSecondary
    val good = if (judged) colors.success.content else neutral
    val fair = if (judged) colors.warning.content else neutral
    val poor = if (judged) colors.danger.content else neutral

    // The widest the lines can get, measured once, so a changing number never resizes the badge.
    val pillSize = remember(measurer, style, fpsFormat, idleFormat, jankyFormat) {
        val fps = measurer.measure(fpsFormat.inRoot(WidestFps, WidestFps), style).size
        val idle = measurer.measure(idleFormat.inRoot(WidestFps), style).size
        val janky = measurer.measure(jankyFormat.inRoot(WidestPercent), style).size
        IntSize(width = maxOf(fps.width, idle.width, janky.width), height = fps.height + janky.height)
    }
    val density = LocalDensity.current

    Spacer(
        modifier = modifier
            .size(
                width = with(density) { pillSize.width.toDp() },
                height = with(density) { pillSize.height.toDp() },
            )
            .appClickable(onClick = onOpen, minTouchTarget = 0.dp)
            .semantics { contentDescription = description }
            .redrawDuringFrames(RefreshMillis)
            .drawBehind {
                frames.readLive(System.nanoTime(), live)
                val fps = live.framesPerSecond
                val refresh = live.refreshHz
                val fpsText = if (fps == LiveStats.IDLE) idleFormat.inRoot(refresh) else fpsFormat.inRoot(fps, refresh)
                val fpsColor = when {
                    fps == LiveStats.IDLE -> neutral
                    fps >= refresh * GoodShare -> good
                    fps >= refresh * FairShare -> fair
                    else -> poor
                }
                // Judged as shown, in tenths of a percent.
                val janky = (live.jankyPercent * Tenths).roundToInt()
                val jankyColor = when {
                    janky <= GoodJankyTenths -> good
                    janky <= FairJankyTenths -> fair
                    else -> poor
                }
                drawCentred(measurer, fpsText, style, fpsColor, top = 0f)
                drawCentred(measurer, jankyFormat.inRoot(tenths(janky)), style, jankyColor, top = size.height / 2f)
            },
    )
}

private fun DrawScope.drawCentred(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    color: Color,
    top: Float,
) {
    val layout = measurer.measure(text, style)
    drawText(layout, color = color, topLeft = Offset((size.width - layout.size.width) / 2f, top))
}

private fun tenths(value: Int): String = (value / TenthsInt).toString() + "." + (value % TenthsInt)

/** Digits as ASCII whatever the locale: these are measurements, read alongside logcat and reports. */
private fun String.inRoot(vararg args: Any): String = String.format(Locale.ROOT, this, *args)

private const val RefreshMillis = 250L
private const val TextCache = 16
private const val WidestFps = 888
private const val WidestPercent = "100.0"
private const val GoodShare = 0.95f
private const val FairShare = 0.75f
private const val GoodJankyTenths = 5
private const val FairJankyTenths = 20
private const val Tenths = 10f
private const val TenthsInt = 10
