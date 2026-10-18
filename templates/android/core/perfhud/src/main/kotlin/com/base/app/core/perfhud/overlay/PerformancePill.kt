package com.base.app.core.perfhud.overlay

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.base.app.core.designsystem.foundation.appClickable
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.perfhud.R
import com.base.app.core.perfhud.frame.FrameLog
import com.base.app.core.perfhud.frame.LiveStats
import com.base.app.core.perfhud.recording.PerformanceRecorder
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Two lines under the DEV badge while recording: frames per second over the last second of
 * drawing against the display's rate, and the janky share of the recording. Tapping them opens
 * the Performance tab.
 *
 * Four times a second it writes three numbers that only its draw phase reads, so the app never
 * recomposes for it. A number that has not changed is not written, so an idle app stays idle: the
 * pill draws nothing new and asks for no frame. In a debug build the numbers are drawn without a
 * verdict colour, since a debug build's frames say little about the release's.
 */
@Composable
internal fun PerformancePill(
    recorder: PerformanceRecorder,
    debugBuild: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recording by recorder.recording.collectAsState()
    if (!recording) return
    LivePill(frames = recorder.frames, judged = !debugBuild, onOpen = onOpen, modifier = modifier)
}

@Composable
private fun LivePill(frames: FrameLog, judged: Boolean, onOpen: () -> Unit, modifier: Modifier) {
    val numbers = remember { PillNumbers() }
    LaunchedEffect(frames) {
        val live = LiveStats()
        while (true) {
            frames.readLive(System.nanoTime(), live)
            numbers.update(live)
            delay(REFRESH_MILLIS)
        }
    }

    val measurer = rememberTextMeasurer(cacheSize = TEXT_CACHE)
    val typography = AppTheme.typography
    val style = remember(typography) { typography.mono.merge(TextStyle(fontSize = typography.labelSmall.fontSize)) }
    val fpsFormat = stringResource(R.string.perfhud_pill_fps)
    val idleFormat = stringResource(R.string.perfhud_pill_idle)
    val jankyFormat = stringResource(R.string.perfhud_pill_janky)
    val description = stringResource(R.string.perfhud_pill_description)
    val colors = AppTheme.colors
    val neutral = colors.warning.content
    val good = if (judged) colors.success.content else neutral
    val fair = if (judged) colors.warning.content else neutral
    val poor = if (judged) colors.danger.content else neutral

    // The widest the lines can get, measured once, so a changing number never resizes the badge.
    val pillSize = remember(measurer, style, fpsFormat, jankyFormat) {
        val fps = measurer.measure(fpsFormat.inRoot(WIDEST_FPS, WIDEST_FPS), style).size
        val janky = measurer.measure(jankyFormat.inRoot(WIDEST_PERCENT), style).size
        IntSize(width = maxOf(fps.width, janky.width), height = fps.height + janky.height)
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
            .drawBehind {
                val fps = numbers.fps
                val refresh = numbers.refreshHz
                val fpsText = if (fps == LiveStats.IDLE) idleFormat.inRoot(refresh) else fpsFormat.inRoot(fps, refresh)
                val fpsColor = when {
                    fps == LiveStats.IDLE -> neutral
                    fps >= refresh * GOOD_SHARE -> good
                    fps >= refresh * FAIR_SHARE -> fair
                    else -> poor
                }
                val janky = numbers.jankyTenths
                val jankyColor = when {
                    janky <= GOOD_JANKY_TENTHS -> good
                    janky <= FAIR_JANKY_TENTHS -> fair
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

/** What the pill shows, as numbers its draw phase reads; an equal write invalidates nothing. */
@Stable
private class PillNumbers {
    var fps by mutableIntStateOf(LiveStats.IDLE)
    var refreshHz by mutableIntStateOf(0)

    /** The janky share in tenths of a percent, the precision the pill shows. */
    var jankyTenths by mutableIntStateOf(0)

    fun update(live: LiveStats) {
        fps = live.framesPerSecond
        refreshHz = live.refreshHz
        jankyTenths = (live.jankyPercent * TENTHS).roundToInt()
    }
}

private fun tenths(value: Int): String = (value / TENTHS_INT).toString() + "." + (value % TENTHS_INT)

/** Digits as ASCII whatever the locale: these are measurements, read alongside logcat and reports. */
private fun String.inRoot(vararg args: Any): String = String.format(Locale.ROOT, this, *args)

private const val REFRESH_MILLIS = 250L
private const val TEXT_CACHE = 16
private const val WIDEST_FPS = 888
private const val WIDEST_PERCENT = "100.0"
private const val GOOD_SHARE = 0.95f
private const val FAIR_SHARE = 0.75f
private const val GOOD_JANKY_TENTHS = 5
private const val FAIR_JANKY_TENTHS = 20
private const val TENTHS = 10f
private const val TENTHS_INT = 10
