package com.base.app.core.designsystem.component.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.foundation.draw.disabledAlpha
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt

/**
 * A slider. The value follows where the finger is, not how far it moved, so a slow drag across a
 * stepped slider moves step by step; a touch that starts a vertical scroll changes nothing; a tap
 * sets the value where it lands. With [steps], each step plays the selection haptic.
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
    val springs = AppTheme.springs
    val haptics = rememberAppHaptics()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val onChange by rememberUpdatedState(onValueChange)
    val onFinished by rememberUpdatedState(onValueChangeFinished)
    val currentValue by rememberUpdatedState(value)
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span == 0f) 0f else ((value - valueRange.start) / span).coerceIn(0f, 1f)

    // What is drawn: the finger's fraction while dragging, else a spring towards the value, so a tap
    // glides the thumb over rather than teleporting it.
    var dragging by remember { mutableStateOf(false) }
    val shown = remember { Animatable(fraction) }
    LaunchedEffect(fraction, dragging) { if (dragging) shown.snapTo(fraction) else shown.animateTo(fraction, springs.thumb.unit) }

    fun snapped(raw: Float): Float {
        val clamped = raw.coerceIn(0f, 1f)
        if (steps <= 0) return clamped
        val intervals = steps + 1
        return (clamped * intervals).roundToInt() / intervals.toFloat()
    }

    fun emit(raw: Float) {
        val next = valueRange.start + snapped(raw) * span
        if (next == currentValue) return
        if (steps > 0) haptics.perform(AppHaptic.Selection)
        onChange(next)
    }

    val shadowContext = LocalGraphicsContext.current.shadowContext
    val thumbShadow = remember(shadowContext) { shadowContext.createDropShadowPainter(CircleShape, ThumbShadow) }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ThumbSize + 16.dp)
            .disabledAlpha(enabled)
            .progressSemantics(value, valueRange, steps)
            .semantics {
                if (enabled) {
                    setProgress { target ->
                        emit(if (span == 0f) 0f else (target - valueRange.start) / span)
                        true
                    }
                }
            }
            .then(
                if (enabled) {
                    Modifier.pointerInput(steps, rtl, span) {
                        val inset = ThumbSize.toPx() / 2f
                        fun fractionAt(x: Float): Float {
                            val f = (x - inset) / (size.width - inset * 2)
                            return if (rtl) 1f - f else f
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var overSlop = 0f
                            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                                change.consume()
                                overSlop = over
                            }
                            if (drag != null) {
                                dragging = true
                                emit(fractionAt(drag.position.x + overSlop))
                                horizontalDrag(drag.id) { change ->
                                    change.consume()
                                    emit(fractionAt(change.position.x))
                                }
                                dragging = false
                                onFinished?.invoke()
                            } else {
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    up.consume()
                                    emit(fractionAt(down.position.x))
                                    onFinished?.invoke()
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        val thumb = ThumbSize.toPx()
        val track = TrackHeight.toPx()
        val inset = thumb / 2f
        val usable = size.width - inset * 2
        val f = shown.value
        val x = inset + usable * (if (rtl) 1f - f else f)
        val y = size.height / 2f
        val radius = CornerRadius(track / 2f)
        drawRoundRect(colors.fill, topLeft = Offset(inset, y - track / 2f), size = Size(usable, track), cornerRadius = radius)
        val activeStart = if (rtl) x else inset
        drawRoundRect(colors.accent, topLeft = Offset(activeStart, y - track / 2f), size = Size(usable * f, track), cornerRadius = radius)
        translate(left = x - inset, top = y - inset) {
            with(thumbShadow) { draw(Size(thumb, thumb)) }
        }
        drawCircle(Color.White, radius = inset, center = Offset(x, y))
        drawCircle(ThumbRing, radius = inset, center = Offset(x, y), style = Stroke(0.5.dp.toPx()))
    }
}

// iOS's thumb: a soft blur that lifts it off a white page, and a faint ring that still outlines it
// where the blur is too light to see.
private val ThumbShadow = Shadow(radius = 2.dp, alpha = 0.15f, offset = DpOffset(0.dp, 1.dp))
private val ThumbRing = Color.Black.copy(alpha = 0.04f)

private val ThumbSize = 28.dp
private val TrackHeight = 4.dp
