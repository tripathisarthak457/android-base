package com.base.app.core.designsystem.component.formfields

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.foundation.draw.disabledAlpha
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Two thumbs on one track, for a range: a price band, an age range, opening hours. A touch moves
 * whichever thumb is nearer, so the range can be set roughly from anywhere on the track, and the
 * thumbs stop at each other rather than crossing.
 *
 * Each thumb is its own control for TalkBack, adjustable with the volume keys like a single slider.
 */
@Composable
fun AppRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val thumbInsetPx = with(LocalDensity.current) { ThumbRadiusDragged.dp.toPx() }
    val touchTargetPx = with(LocalDensity.current) { AppTheme.sizes.minTouchTarget.toPx() }

    var trackWidth by remember { mutableFloatStateOf(0f) }
    // Which thumb a finger is on: StartThumb, EndThumb, or NoThumb between gestures.
    var dragging by remember { mutableIntStateOf(NoThumb) }
    var lastSnapped by remember { mutableFloatStateOf(Float.NaN) }
    val current by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)

    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    fun fractionOf(v: Float) = ((v - valueRange.start) / span).coerceIn(0f, 1f)

    fun move(thumb: Int, raw: Float) {
        val snapped = if (steps > 0) {
            val stepSize = 1f / (steps + 1)
            (Math.round(raw / stepSize) * stepSize).coerceIn(0f, 1f)
        } else {
            raw.coerceIn(0f, 1f)
        }
        if (steps > 0 && snapped != lastSnapped) {
            lastSnapped = snapped
            haptics.perform(AppHaptic.Selection)
        }
        val moved = valueRange.start + snapped * span
        val now = current
        currentOnValueChange(
            if (thumb == StartThumb) {
                moved.coerceAtMost(now.endInclusive)..now.endInclusive
            } else {
                now.start..moved.coerceAtLeast(now.start)
            },
        )
    }

    fun travel() = (trackWidth - 2 * thumbInsetPx).takeIf { it > 0f } ?: trackWidth

    fun fractionAt(x: Float): Float = ((if (rtl) trackWidth - x else x) - thumbInsetPx) / travel()

    fun thumbX(fraction: Float): Float {
        val fromStart = thumbInsetPx + travel() * fraction
        return if (rtl) trackWidth - fromStart else fromStart
    }

    val startFraction = fractionOf(value.start)
    val endFraction = fractionOf(value.endInclusive)
    val startRadius by animateFloatAsState(
        targetValue = if (dragging == StartThumb) ThumbRadiusDragged else ThumbRadius,
        animationSpec = AppTheme.springs.press.unit,
        label = "startThumb",
    )
    val endRadius by animateFloatAsState(
        targetValue = if (dragging == EndThumb) ThumbRadiusDragged else ThumbRadius,
        animationSpec = AppTheme.springs.press.unit,
        label = "endThumb",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTheme.sizes.minTouchTarget)
            .disabledAlpha(enabled)
            .onSizeChanged { trackWidth = it.width.toFloat() }
            .pointerInput(enabled, rtl, steps, valueRange) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val at = fractionAt(down.position.x)
                    val start = fractionOf(current.start)
                    val end = fractionOf(current.endInclusive)
                    val thumb = when {
                        at <= start -> StartThumb
                        at >= end -> EndThumb
                        at - start < end - at -> StartThumb
                        else -> EndThumb
                    }
                    dragging = thumb
                    move(thumb, at)
                    drag(down.id) { change ->
                        move(thumb, fractionAt(change.position.x))
                        change.consume()
                    }
                    dragging = NoThumb
                    currentOnFinished?.invoke()
                }
            }
            .drawBehind {
                val centerY = size.height / 2f
                val trackHeight = AppRangeSliderDefaults.TrackHeight.toPx()
                val radius = CornerRadius(trackHeight / 2f)
                val startX = thumbX(startFraction)
                val endX = thumbX(endFraction)

                drawRoundRect(
                    color = colors.fill,
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = colors.accent,
                    topLeft = Offset(minOf(startX, endX), centerY - trackHeight / 2f),
                    size = Size(abs(endX - startX), trackHeight),
                    cornerRadius = radius,
                )
                drawThumb(Offset(startX, centerY), startRadius, Color.White, colors.outline)
                drawThumb(Offset(endX, centerY), endRadius, Color.White, colors.outline)
            },
    ) {
        ThumbControl(
            label = stringResource(R.string.designsystem_range_start),
            value = value.start,
            range = valueRange.start..value.endInclusive,
            steps = steps,
            enabled = enabled,
            centerX = { thumbX(startFraction) },
            touchTargetPx = touchTargetPx,
            onSet = { target ->
                move(StartThumb, fractionOf(target))
                currentOnFinished?.invoke()
            },
        )
        ThumbControl(
            label = stringResource(R.string.designsystem_range_end),
            value = value.endInclusive,
            range = value.start..valueRange.endInclusive,
            steps = steps,
            enabled = enabled,
            centerX = { thumbX(endFraction) },
            touchTargetPx = touchTargetPx,
            onSet = { target ->
                move(EndThumb, fractionOf(target))
                currentOnFinished?.invoke()
            },
        )
    }
}

/** An invisible box over a thumb, so each end of the range is a separate accessible control. */
@Composable
private fun ThumbControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean,
    centerX: () -> Float,
    touchTargetPx: Float,
    onSet: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .offset { IntOffset((centerX() - touchTargetPx / 2f).roundToInt(), 0) }
            .size(AppTheme.sizes.minTouchTarget)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value, range, steps)
                if (enabled) {
                    setProgress { target ->
                        onSet(target.coerceIn(range.start, range.endInclusive))
                        true
                    }
                }
            },
    )
}

private fun DrawScope.drawThumb(center: Offset, radius: Float, fill: Color, ring: Color) {
    drawCircle(color = fill, radius = radius.dp.toPx(), center = center)
    drawCircle(color = ring, radius = radius.dp.toPx(), center = center, style = Stroke(width = AppRangeSliderDefaults.ThumbRingWidth.toPx()))
}

private const val NoThumb = -1
private const val StartThumb = 0
private const val EndThumb = 1
private const val ThumbRadius = 10f
private const val ThumbRadiusDragged = 12f

/** The range slider's track and the ring around its thumbs. */
object AppRangeSliderDefaults {
    val ThumbRingWidth = 2.5.dp
    val TrackHeight = 6.dp
}

@AppComponentPreviews
@Composable
private fun AppRangeSliderPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppRangeSlider(value = 0.2f..0.7f, onValueChange = {})
        AppRangeSlider(value = 20f..60f, onValueChange = {}, valueRange = 0f..100f, steps = 9)
        AppRangeSlider(value = 0.4f..0.5f, onValueChange = {}, enabled = false)
    }
}
