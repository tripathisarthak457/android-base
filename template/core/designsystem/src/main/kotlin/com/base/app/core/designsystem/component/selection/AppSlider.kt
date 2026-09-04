package com.base.app.core.designsystem.component.selection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.foundation.HapticEffect
import com.base.app.core.designsystem.foundation.disabledAlpha
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A slider.
 *
 * ## Tapping the track jumps to that point
 *
 * A slider that only responds to a drag on the thumb is a slider that is fiddly to set roughly —
 * which is what most sliders are for. The tap handler and the drag handler share one conversion
 * from x-position to value, so they cannot disagree about where a position maps to.
 *
 * ## The thumb grows while dragged
 *
 * Under a finger the thumb is completely hidden, so its size is not what the growth is for: the
 * halo that appears around it is visible past the fingertip, and it is the only confirmation the
 * user has that the control is tracking them rather than the page scrolling.
 */
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val dragged by interactionSource.collectIsDraggedAsState()

    var trackWidth by remember { mutableFloatStateOf(0f) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val haptics = rememberAppHaptics()
    var lastSnapped by remember { mutableFloatStateOf(Float.NaN) }
    // The largest the thumb ever gets, so the travel does not change as it grows under a finger.
    val thumbInsetPx = with(LocalDensity.current) { THUMB_RADIUS_DRAGGED.dp.toPx() }

    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    val thumbRadius by animateFloatAsState(
        targetValue = if (dragged) THUMB_RADIUS_DRAGGED else THUMB_RADIUS,
        animationSpec = AppTheme.motion.press(),
        label = "thumbRadius",
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (dragged) 0.18f else 0f,
        animationSpec = AppTheme.motion.press(),
        label = "haloAlpha",
    )

    fun emit(positionX: Float) {
        if (trackWidth <= 0f) return
        // Mapped through the same inset the thumb is drawn with, so tapping the very left edge
        // still reaches the minimum. Without it the last few pixels at each end are unreachable
        // and the slider feels like it will not quite go to zero.
        val travel = (trackWidth - 2 * thumbInsetPx).takeIf { it > 0f } ?: trackWidth
        val raw = ((positionX - thumbInsetPx) / travel).coerceIn(0f, 1f)
        // Snapping happens on the *fraction*, before mapping back to the range, so the steps are
        // evenly spaced regardless of what the range happens to be.
        val snapped = if (steps > 0) {
            val stepSize = 1f / (steps + 1)
            (Math.round(raw / stepSize) * stepSize).coerceIn(0f, 1f)
        } else {
            raw
        }

        // A tick per detent crossed, and nothing at all on a continuous slider. Buzzing on every
        // pixel of a smooth drag is a vibration, not feedback, and it drains the battery of
        // whoever is scrubbing.
        if (steps > 0 && snapped != lastSnapped) {
            lastSnapped = snapped
            haptics.perform(HapticEffect.Tick)
        }

        currentOnValueChange(valueRange.start + snapped * span)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTheme.sizes.minTouchTarget)
            .disabledAlpha(enabled)
            .onSizeChanged { trackWidth = it.width.toFloat() }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps)
            }
            .pointerInput(enabled, trackWidth, steps) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = { offset -> emit(offset.x) },
                    onTap = { onValueChangeFinished?.invoke() },
                )
            }
            .draggable(
                state = rememberDraggableState { delta ->
                    if (trackWidth > 0f) {
                        val travel = (trackWidth - 2 * thumbInsetPx).takeIf { it > 0f } ?: trackWidth
                        emit(thumbInsetPx + fraction * travel + delta)
                    }
                },
                orientation = Orientation.Horizontal,
                enabled = enabled,
                interactionSource = interactionSource,
                onDragStopped = { onValueChangeFinished?.invoke() },
            )
            .drawBehind {
                val centerY = size.height / 2f
                val trackHeight = TRACK_HEIGHT.toPx()
                val radius = CornerRadius(trackHeight / 2f)

                drawRoundRect(
                    color = colors.surfaceVariant,
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius,
                )

                // The thumb travels between two insets rather than edge to edge. At zero and at
                // one, an un-inset thumb is drawn half outside its own bounds — clipped by
                // whatever is beside it, and visibly not lining up with the end of the track.
                val travel = (size.width - 2 * thumbInsetPx).coerceAtLeast(0f)
                val thumbX = thumbInsetPx + travel * fraction

                drawRoundRect(
                    color = colors.accent,
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(thumbX, trackHeight),
                    cornerRadius = radius,
                )

                if (haloAlpha > 0f) {
                    drawCircle(
                        color = colors.accent.copy(alpha = haloAlpha),
                        radius = HALO_RADIUS.toPx(),
                        center = Offset(thumbX, centerY),
                    )
                }
                drawCircle(
                    color = colors.surface,
                    radius = thumbRadius.dp.toPx(),
                    center = Offset(thumbX, centerY),
                )
                drawCircle(
                    color = colors.accent,
                    radius = thumbRadius.dp.toPx(),
                    center = Offset(thumbX, centerY),
                    style = Stroke(width = 2.5.dp.toPx()),
                )
            },
    )
}

private const val THUMB_RADIUS = 10f
private const val THUMB_RADIUS_DRAGGED = 12f
private val TRACK_HEIGHT = 6.dp
private val HALO_RADIUS = 22.dp
