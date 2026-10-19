package com.example.hud.everything.core.perfhud.overlay

import android.graphics.Typeface
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hud.everything.core.designsystem.foundation.appClickable
import com.example.hud.everything.core.designsystem.theme.AppTheme
import com.example.hud.everything.core.perfhud.R
import com.example.hud.everything.core.perfhud.frame.FrameLog
import com.example.hud.everything.core.perfhud.frame.LiveStats
import com.example.hud.everything.core.perfhud.recording.PerformanceRecorder
import kotlin.math.roundToInt

/**
 * Two lines under the DEV badge while recording: frames per second over the last second of
 * drawing against the display's rate, and the janky share of the recording. Tapping them opens
 * the Performance tab.
 *
 * The lines are drawn straight from the frame log, at most four times a second and only within
 * frames the app draws anyway ([redrawDuringFrames]): the pill holds no state, never recomposes
 * and never draws a frame of its own, and a redraw allocates nothing ([PillText]). While the app is idle it shows the numbers of its last
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
    val typography = AppTheme.typography
    val style = remember(typography) { typography.mono.merge(TextStyle(fontSize = typography.labelSmall.fontSize)) }
    val fontResolver = LocalFontFamilyResolver.current
    // A downloaded font arrives after the first composition; the text is set up again when it does.
    val typeface by remember(fontResolver, style) {
        fontResolver.resolve(
            fontFamily = style.fontFamily,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
            fontSynthesis = style.fontSynthesis ?: FontSynthesis.All,
        )
    }
    val fpsFormat = stringResource(R.string.perfhud_pill_fps)
    val idleFormat = stringResource(R.string.perfhud_pill_idle)
    val jankyFormat = stringResource(R.string.perfhud_pill_janky)
    val description = stringResource(R.string.perfhud_pill_description)
    val density = LocalDensity.current
    val text = remember(fpsFormat, idleFormat, jankyFormat, style, typeface, density) {
        PillText(fpsFormat, idleFormat, jankyFormat, style, typeface as? Typeface, density)
    }
    val colors = AppTheme.colors
    val neutral = colors.contentSecondary
    val good = if (judged) colors.success.content else neutral
    val fair = if (judged) colors.warning.content else neutral
    val poor = if (judged) colors.danger.content else neutral

    Spacer(
        modifier = modifier
            .size(
                width = with(density) { text.width.toDp() },
                height = with(density) { text.height.toDp() },
            )
            .appClickable(onClick = onOpen, minTouchTarget = 0.dp)
            .semantics { contentDescription = description }
            .redrawDuringFrames(RefreshMillis)
            .drawBehind {
                frames.readLive(System.nanoTime(), live)
                val fps = live.framesPerSecond
                val refresh = live.refreshHz
                val isIdle = fps == LiveStats.IDLE
                val fpsColor = when {
                    isIdle -> neutral
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
                with(text) {
                    drawRate(framesPerSecond = fps, refreshHz = refresh, isIdle = isIdle, color = fpsColor)
                    drawJanky(jankyTenths = janky, color = jankyColor)
                }
            },
    )
}

private const val RefreshMillis = 250L
private const val GoodShare = 0.95f
private const val FairShare = 0.75f
private const val GoodJankyTenths = 5
private const val FairJankyTenths = 20
private const val Tenths = 10f
