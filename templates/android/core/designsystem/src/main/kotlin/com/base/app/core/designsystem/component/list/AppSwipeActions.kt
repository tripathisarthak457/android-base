package com.base.app.core.designsystem.component.list

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * One action a swipe reveals. A [destructive] action is red; otherwise [tone] picks the colour, the
 * accent by default.
 */
@Immutable
class SwipeAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
    val tone: AppTone? = null,
)

/**
 * A row with actions behind it: [leading] revealed by swiping towards the trailing edge, [trailing]
 * by swiping the other way, each list ordered from the outer edge in. A short swipe rests the row
 * open; with [allowFullSwipe], swiping past most of the width performs the outermost action, with a
 * haptic as the finger crosses that point. Tapping an open row closes it rather than opening the
 * row. Screen-reader users get every action in the row's actions menu.
 *
 * The row is drawn on [background], which has to be opaque to cover the actions. The finger moves a
 * plain float, so a drag allocates nothing per event; only the settle animates.
 */
@Composable
fun AppSwipeActions(
    modifier: Modifier = Modifier,
    leading: List<SwipeAction> = emptyList(),
    trailing: List<SwipeAction> = emptyList(),
    enabled: Boolean = true,
    allowFullSwipe: Boolean = true,
    shape: Shape = RectangleShape,
    background: Color = AppTheme.colors.surface,
    actionWidth: Dp = 74.dp,
    resetKey: Any? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val spring = AppTheme.springs.sheet.pixels
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val currentLeading by rememberUpdatedState(leading)
    val currentTrailing by rememberUpdatedState(trailing)

    // Logical: positive reveals the leading actions, whatever the layout direction.
    var offset by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    var settling by remember { mutableStateOf<Job?>(null) }
    val open by remember { derivedStateOf { offset != 0f } }
    val actionPx = with(density) { actionWidth.toPx() }

    fun revealOf(actions: List<SwipeAction>) = actionPx * actions.size
    fun fullSwipeAt() = width * FULL_SWIPE_FRACTION
    fun bound(actions: List<SwipeAction>) =
        if (actions.isEmpty()) 0f else if (allowFullSwipe) width.toFloat() else revealOf(actions)

    fun settleTo(target: Float, velocity: Float = 0f) {
        settling?.cancel()
        settling = scope.launch {
            animate(offset, target, velocity, spring) { value, _ -> offset = value }
        }
    }

    fun perform(action: SwipeAction) {
        settleTo(0f)
        action.onClick()
    }

    LaunchedEffect(resetKey) { offset = 0f }

    val dragState = rememberDraggableState { delta ->
        val before = offset
        val logical = if (rtl) -delta else delta
        offset = (offset + logical).coerceIn(-bound(currentTrailing), bound(currentLeading))
        val threshold = fullSwipeAt()
        if (allowFullSwipe && (abs(before) < threshold) != (abs(offset) < threshold)) haptics.perform(AppHaptic.Threshold)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .onSizeChanged { width = it.width }
            .semantics {
                customActions = (leading + trailing).map { action ->
                    CustomAccessibilityAction(action.label) {
                        action.onClick()
                        true
                    }
                }
            },
    ) {
        if (leading.isNotEmpty()) ActionPanel(leading, start = true, actionWidth, { offset.coerceAtLeast(0f) }, ::perform)
        if (trailing.isNotEmpty()) ActionPanel(trailing, start = false, actionWidth, { (-offset).coerceAtLeast(0f) }, ::perform)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = if (rtl) -offset else offset }
                .background(background)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled && (leading.isNotEmpty() || trailing.isNotEmpty()),
                    onDragStarted = { settling?.cancel() },
                    onDragStopped = { velocity ->
                        val logicalVelocity = if (rtl) -velocity else velocity
                        val revealed = abs(offset)
                        val side = if (offset > 0f) currentLeading else currentTrailing
                        val sign = if (offset > 0f) 1f else -1f
                        when {
                            side.isEmpty() || offset == 0f -> settleTo(0f, logicalVelocity)
                            allowFullSwipe && revealed > fullSwipeAt() -> {
                                settleTo(sign * width, logicalVelocity)
                                side.first().onClick()
                            }
                            revealed > revealOf(side) * REST_FRACTION || abs(logicalVelocity) > FLING_VELOCITY && logicalVelocity * sign > 0 ->
                                settleTo(sign * revealOf(side), logicalVelocity)
                            else -> settleTo(0f, logicalVelocity)
                        }
                    },
                )
                .then(
                    if (open) {
                        // Taken before the row's own click sees it: a tap on an open row closes it.
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                var moved = 0f
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    moved += abs(change.position.x - change.previousPosition.x)
                                    if (!change.pressed) {
                                        if (moved < touchSlop) {
                                            change.consume()
                                            settleTo(0f)
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            content()
        }
    }
}

/**
 * The actions on one side. The panel is as wide as the row has moved, in the outermost action's
 * colour, so a full swipe reads as that action taking over the row; [revealed] is read while it is
 * laid out, so dragging re-places it and recomposes nothing. It takes the row's height after the row
 * is measured: filling the height instead would stretch the row to whatever its parent allows.
 */
@Composable
private fun BoxScope.ActionPanel(
    actions: List<SwipeAction>,
    start: Boolean,
    actionWidth: Dp,
    revealed: () -> Float,
    onAction: (SwipeAction) -> Unit,
) {
    val outer = actionColors(actions.first())
    Row(
        modifier = Modifier
            .matchParentSize()
            .layout { measurable, constraints ->
                val visible = revealed().toInt().coerceIn(0, constraints.maxWidth)
                val placeable = measurable.measure(constraints.copy(minWidth = visible, maxWidth = visible))
                layout(constraints.maxWidth, placeable.height) {
                    placeable.placeRelative(if (start) 0 else constraints.maxWidth - visible, 0)
                }
            }
            .background(outer.first),
        horizontalArrangement = if (start) Arrangement.Start else Arrangement.End,
    ) {
        val ordered = if (start) actions else actions.reversed()
        ordered.forEach { action ->
            val (container, content) = actionColors(action)
            Column(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(container)
                    .pressable(onClick = { onAction(action) }, feedback = PressFeedback.Dim),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                AppIcon(action.icon, contentDescription = null, tint = content)
                AppText(text = action.label, style = AppTheme.typography.caption1, color = content, maxLines = 1)
            }
        }
    }
}

@Composable
private fun actionColors(action: SwipeAction): Pair<Color, Color> {
    val colors = AppTheme.colors
    return when {
        action.destructive -> colors.danger.base to colors.danger.on
        action.tone != null -> action.tone.colors.base to action.tone.colors.on
        else -> colors.accent to colors.onAccent
    }
}

private const val FULL_SWIPE_FRACTION = 0.55f
private const val REST_FRACTION = 0.5f
private const val FLING_VELOCITY = 800f
