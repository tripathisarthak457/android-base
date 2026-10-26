package com.example.finalafter.crui2.core.designsystem.component.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui2.core.designsystem.component.text.AppText
import com.example.finalafter.crui2.core.designsystem.foundation.draw.disabledAlpha
import com.example.finalafter.crui2.core.designsystem.foundation.interaction.AppHaptic
import com.example.finalafter.crui2.core.designsystem.foundation.interaction.collectHeld
import com.example.finalafter.crui2.core.designsystem.foundation.interaction.minimumTouchTarget
import com.example.finalafter.crui2.core.designsystem.foundation.interaction.rememberAppHaptics
import com.example.finalafter.crui2.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * A segmented control, as iOS draws it: a raised thumb with a soft shadow on a sunken track. Tap a
 * segment, or drag the thumb across; it settles on the segment it is let go over. The thumb's
 * position is read where it is placed, so it moves without recomposing anything. A change plays the
 * selection haptic.
 */
@Composable
fun AppSegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (options.isEmpty()) return
    val colors = AppTheme.colors
    val springs = AppTheme.springs
    val haptics = rememberAppHaptics()
    val onSelectNow by rememberUpdatedState(onSelect)
    val position = remember { Animatable(selected.toFloat()) }
    var drag by remember { mutableStateOf<Float?>(null) }
    val pressScale = remember { Animatable(1f) }
    val thumbInteractions = remember { MutableInteractionSource() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    LaunchedEffect(selected) { position.animateTo(selected.toFloat(), springs.thumb.unit) }
    // Held, or dragged from anywhere, the thumb shrinks a little, so it reads as picked up.
    LaunchedEffect(thumbInteractions, springs) {
        thumbInteractions.collectHeld { held ->
            launch {
                pressScale.animateTo(
                    if (held) PressedThumbScale else 1f,
                    if (held) springs.press.unit else springs.release.unit,
                )
            }
        }
    }

    fun choose(index: Int, tick: Boolean = true) {
        if (index == selected) return
        if (tick) haptics.perform(AppHaptic.Selection)
        onSelectNow(index)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .minimumTouchTarget()
            .disabledAlpha(enabled)
            .heightIn(min = AppSegmentedControlDefaults.Height)
            .background(colors.fill, AppSegmentedControlDefaults.TrackShape)
            .padding(AppSegmentedControlDefaults.ThumbInset),
    ) {
        val segmentPx = with(density) { maxWidth.toPx() } / options.size
        // Sized to the segments beside it, which set the height; the thumb is one segment wide and
        // placed across the whole width, so it mirrors with the segments in right-to-left layouts.
        Box(
            Modifier
                .matchParentSize()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(Constraints.fixed(segmentPx.roundToInt(), constraints.maxHeight))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val x = ((drag ?: position.value) * segmentPx).roundToInt()
                        placeable.placeRelative(x, 0)
                    }
                }
                .graphicsLayer {
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                }
                .dropShadow(AppSegmentedControlDefaults.ThumbShape, AppSegmentedControlDefaults.ThumbShadow)
                .background(colors.segmentThumb, AppSegmentedControlDefaults.ThumbShape),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup()
                .then(
                    if (enabled) {
                        Modifier.draggable(
                            state = rememberDraggableState { delta ->
                                val start = drag ?: position.value
                                val step = (if (rtl) -delta else delta) / segmentPx
                                val now = (start + step).coerceIn(0f, (options.size - 1).toFloat())
                                // Each segment the thumb is dragged over ticks, as iOS's does.
                                if (now.roundToInt() != start.roundToInt()) haptics.perform(AppHaptic.Selection)
                                drag = now
                            },
                            orientation = Orientation.Horizontal,
                            interactionSource = thumbInteractions,
                            onDragStopped = { velocity ->
                                val released = drag ?: position.value
                                val speed = (if (rtl) -velocity else velocity) / segmentPx
                                val landed = (released + speed * ProjectionSeconds).roundToInt().coerceIn(0, options.lastIndex)
                                position.snapTo(released)
                                drag = null
                                // The drag already ticked for the segment it ended over.
                                choose(landed, tick = landed != released.roundToInt())
                                position.animateTo(landed.toFloat(), springs.thumb.unit, initialVelocity = speed)
                            },
                        )
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = AppSegmentedControlDefaults.Height - AppSegmentedControlDefaults.ThumbInset * 2)
                        .selectable(
                            selected = isSelected,
                            enabled = enabled,
                            role = Role.Tab,
                            indication = null,
                            interactionSource = if (isSelected) thumbInteractions else null,
                            onClick = { choose(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = option,
                        modifier = Modifier.padding(horizontal = AppTheme.spacing.sm),
                        style = if (isSelected) AppTheme.typography.footnote.copy(fontWeight = FontWeight.SemiBold) else AppTheme.typography.footnote,
                        color = colors.contentPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The segmented control's metrics, iOS's. */
object AppSegmentedControlDefaults {
    val Height = 32.dp
    val ThumbInset = 2.dp
    val TrackShape = RoundedCornerShape(9.dp)
    val ThumbShape = RoundedCornerShape(7.dp)
    val ThumbShadow = Shadow(radius = 4.dp, offset = DpOffset(0.dp, 1.dp), alpha = 0.12f)
}

private const val PressedThumbScale = 0.95f

// How far ahead a release looks for where the thumb was thrown (iOS's 0.99 projection).
private const val ProjectionSeconds = 0.099f

@AppComponentPreviews
@Composable
private fun AppSegmentedControlPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppSegmentedControl(options = listOf("Day", "Week", "Month"), selected = 1, onSelect = {})
        AppSegmentedControl(options = listOf("List", "Map"), selected = 0, onSelect = {}, enabled = false)
        AppSegmentedControl(options = listOf("Everything", "Unread messages", "Mentions"), selected = 2, onSelect = {})
    }
}
