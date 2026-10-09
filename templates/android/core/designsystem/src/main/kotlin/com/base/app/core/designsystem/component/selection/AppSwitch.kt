package com.base.app.core.designsystem.component.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.foundation.draw.disabledAlpha
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.minimumTouchTarget
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A switch, as iOS draws it. The thumb widens while it is held, and can be dragged across; where it
 * is let go decides the value. With no [onCheckedChange] it only shows a state and takes no
 * touches, so a row around it keeps its own tap. Inside a settings row, use `AppToggleRow`, which
 * makes the whole row the target.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val haptics = rememberAppHaptics()
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val onChange by rememberUpdatedState(onCheckedChange)
    val motion = remember { SwitchMotion(checked) }
    val springs = AppTheme.springs
    val travel = with(LocalDensity.current) { (TrackWidth - ThumbSize - ThumbInset * 2).toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    fun toggle(on: Boolean) {
        haptics.perform(if (on) AppHaptic.ToggleOn else AppHaptic.ToggleOff)
        onChange?.invoke(on)
    }

    val interactive = enabled && onCheckedChange != null
    SwitchTrack(
        checked = checked,
        interactionSource = source,
        motion = motion,
        modifier = modifier
            .minimumTouchTarget()
            .then(
                if (interactive) {
                    Modifier
                        .toggleable(
                            value = checked,
                            interactionSource = source,
                            indication = null,
                            role = Role.Switch,
                            onValueChange = ::toggle,
                        )
                        .draggable(
                            state = rememberDraggableState { delta ->
                                val start = motion.drag ?: motion.position.value
                                motion.drag = (start + (if (rtl) -delta else delta) / travel).coerceIn(0f, 1f)
                            },
                            orientation = Orientation.Horizontal,
                            interactionSource = source,
                            onDragStopped = {
                                val released = motion.drag ?: motion.position.value
                                motion.position.snapTo(released)
                                motion.drag = null
                                val landed = released >= 0.5f
                                if (landed != checked) {
                                    toggle(landed)
                                } else {
                                    motion.position.animateTo(if (checked) 1f else 0f, springs.thumb.unit)
                                }
                            },
                        )
                } else {
                    Modifier.semantics { toggleableState = ToggleableState(checked) }
                },
            )
            .disabledAlpha(enabled),
    )
}

/** Where the thumb is: settled by [position], or under the finger while [drag] is set. */
@Stable
internal class SwitchMotion(checked: Boolean) {
    val position = Animatable(if (checked) 1f else 0f)
    var drag by mutableStateOf<Float?>(null)
}

/**
 * The switch's drawing alone: track, thumb and their motion, with no input. A toggle row draws this
 * and handles the touch itself; [interactionSource] lets the thumb widen while the row is held.
 */
@Composable
internal fun SwitchTrack(
    checked: Boolean,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    motion: SwitchMotion = remember { SwitchMotion(checked) },
) {
    val colors = AppTheme.colors
    val springs = AppTheme.springs
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val widen = animateFloatAsState(if (pressed || dragged) 1f else 0f, springs.press.unit, label = "switchWiden")
    LaunchedEffect(checked) { motion.position.animateTo(if (checked) 1f else 0f, springs.thumb.unit) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Canvas(modifier = modifier.size(width = TrackWidth, height = TrackHeight)) {
        val fraction = motion.drag ?: motion.position.value
        val visual = if (rtl) 1f - fraction else fraction
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = lerp(colors.fill, colors.accent, fraction), size = size, cornerRadius = radius)
        if (fraction < 1f) {
            drawRoundRect(
                color = colors.outline.copy(alpha = colors.outline.alpha * (1f - fraction)),
                size = size,
                cornerRadius = radius,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        val inset = ThumbInset.toPx()
        val thumbHeight = ThumbSize.toPx()
        val thumbWidth = thumbHeight + ThumbStretch.toPx() * widen.value
        val travel = size.width - inset * 2 - thumbWidth
        val left = inset + travel * visual
        val top = (size.height - thumbHeight) / 2f
        val thumbRadius = CornerRadius(thumbHeight / 2f)
        drawRoundRect(
            color = Color.Black.copy(alpha = ThumbShadowAlpha),
            topLeft = Offset(left, top + 1.dp.toPx()),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = thumbRadius,
        )
        drawRoundRect(color = Color.White, topLeft = Offset(left, top), size = Size(thumbWidth, thumbHeight), cornerRadius = thumbRadius)
    }
}

private val TrackWidth = 51.dp
private val TrackHeight = 31.dp
private val ThumbSize = 27.dp
private val ThumbInset = 2.dp

// How much the thumb grows towards the middle while held, as iOS's does.
private val ThumbStretch = 7.dp
private const val ThumbShadowAlpha = 0.12f
