package com.base.app.core.designsystem.component.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppHaptic
import com.base.app.core.designsystem.foundation.disabledAlpha
import com.base.app.core.designsystem.foundation.minimumTouchTarget
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt

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
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    LaunchedEffect(selected) { position.animateTo(selected.toFloat(), springs.selection.unit) }

    fun choose(index: Int) {
        if (index == selected) return
        haptics.perform(AppHaptic.Selection)
        onSelectNow(index)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .minimumTouchTarget()
            .disabledAlpha(enabled)
            .heightIn(min = SegmentHeight)
            .background(colors.fill, TrackShape)
            .padding(ThumbInset),
    ) {
        val segmentPx = with(density) { maxWidth.toPx() } / options.size
        Box(
            Modifier
                .fillMaxHeight()
                .layout { measurable, constraints ->
                    val width = segmentPx.roundToInt()
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(width, placeable.height) {
                        val x = ((drag ?: position.value) * segmentPx).roundToInt()
                        placeable.placeRelative(x, 0)
                    }
                }
                .dropShadow(ThumbShape, ThumbShadow)
                .background(colors.surfaceElevated, ThumbShape),
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
                                drag = (start + step).coerceIn(0f, (options.size - 1).toFloat())
                            },
                            orientation = Orientation.Horizontal,
                            onDragStopped = {
                                val landed = (drag ?: position.value).roundToInt()
                                position.snapTo(drag ?: position.value)
                                drag = null
                                if (landed != selected) choose(landed) else position.animateTo(selected.toFloat(), springs.selection.unit)
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
                        .heightIn(min = SegmentHeight - ThumbInset * 2)
                        .selectable(
                            selected = isSelected,
                            enabled = enabled,
                            role = Role.Tab,
                            indication = null,
                            interactionSource = null,
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

private val SegmentHeight = 32.dp
private val ThumbInset = 2.dp
private val TrackShape = RoundedCornerShape(9.dp)
private val ThumbShape = RoundedCornerShape(7.dp)
private val ThumbShadow = Shadow(radius = 4.dp, offset = DpOffset(0.dp, 1.dp), alpha = 0.12f)
