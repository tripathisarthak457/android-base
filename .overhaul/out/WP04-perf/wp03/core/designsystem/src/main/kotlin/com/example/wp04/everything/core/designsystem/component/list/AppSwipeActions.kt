package com.example.wp04.everything.core.designsystem.component.list

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.wp04.everything.core.designsystem.component.feedback.AppTone
import com.example.wp04.everything.core.designsystem.component.overlay.AppContextMenu
import com.example.wp04.everything.core.designsystem.component.overlay.AppMenuItem
import com.example.wp04.everything.core.designsystem.component.text.AppIcon
import com.example.wp04.everything.core.designsystem.component.text.AppText
import com.example.wp04.everything.core.designsystem.foundation.interaction.AppHaptic
import com.example.wp04.everything.core.designsystem.foundation.interaction.ContainerActions
import com.example.wp04.everything.core.designsystem.foundation.interaction.LocalContainerActions
import com.example.wp04.everything.core.designsystem.foundation.interaction.PressFeedback
import com.example.wp04.everything.core.designsystem.foundation.interaction.pressable
import com.example.wp04.everything.core.designsystem.foundation.interaction.rememberAppHaptics
import com.example.wp04.everything.core.designsystem.theme.AppMotionStyle
import com.example.wp04.everything.core.designsystem.theme.AppTheme
import com.example.wp04.everything.core.designsystem.theme.springs
import com.example.wp04.everything.core.designsystem.theme.tokens.DesignTokens
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

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
 * Whether a swipe row is open, and how far. Hoist it to close the row from outside, for example
 * when the list's data changes under it:
 *
 * ```
 * val swipe = rememberAppSwipeActionsState()
 * LaunchedEffect(items) { swipe.close() }
 * ```
 */
@Stable
class AppSwipeActionsState @RememberInComposition constructor() {
    /**
     * How far the row has moved, in pixels: positive reveals the leading actions, negative the
     * trailing ones, whatever the layout direction. Read it in layout or draw, never in composition.
     */
    @get:FrequentlyChangingValue
    var offset: Float by mutableFloatStateOf(0f)
        internal set

    /** Whether any action shows. Changes only when the row opens or closes, not while it moves. */
    val isOpen: Boolean by derivedStateOf { offset != 0f }

    internal val mutex = MutatorMutex()
    internal var spring: SpringSpec<Float> = AppMotionStyle.Standard.springs().thumb.pixels

    /** Finger travel the offset stands for, while a drag past the last action stretches. */
    internal var travel = 0f

    /** Slides the row closed, unless a finger has it. */
    suspend fun close() = settleTo(0f, 0f)

    internal suspend fun settleTo(target: Float, velocity: Float) = mutex.mutate {
        animate(offset, target, velocity, spring) { value, _ -> offset = value }
    }

    /** Stops a settle so a new drag takes the row from where it is. */
    internal suspend fun stop() = mutex.mutate(MutatePriority.UserInput) {}
}

@Composable
fun rememberAppSwipeActionsState(): AppSwipeActionsState = remember { AppSwipeActionsState() }

/**
 * A row with actions behind it: [leading] revealed by swiping towards the trailing edge, [trailing]
 * by swiping the other way, each list ordered from the outer edge in. A short swipe rests the row
 * open; with [allowFullSwipe], swiping past most of the width performs the outermost action, with a
 * haptic as the finger crosses that point. Without it the row stretches past its last action and
 * springs back. Tapping an open row closes it rather than opening the row.
 *
 * The same actions are in the row's context menu, behind a long press or a right click, for anyone
 * not swiping: a mouse, a keyboard, a screen reader.
 *
 * The row is drawn on [background], which has to be opaque to cover the actions. The finger moves a
 * plain float, so a drag allocates nothing per event; only the settle animates, from the finger's
 * speed, to the stop the swipe would have coasted to.
 */
@Composable
fun AppSwipeActions(
    modifier: Modifier = Modifier,
    state: AppSwipeActionsState = rememberAppSwipeActionsState(),
    leading: List<SwipeAction> = emptyList(),
    trailing: List<SwipeAction> = emptyList(),
    enabled: Boolean = true,
    allowFullSwipe: Boolean = true,
    shape: Shape = RectangleShape,
    background: Color = AppTheme.colors.surface,
    actionWidth: Dp = 74.dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val currentLeading by rememberUpdatedState(leading)
    val currentTrailing by rememberUpdatedState(trailing)
    val spring = AppTheme.springs.thumb.pixels
    SideEffect { state.spring = spring }

    var width by remember { mutableIntStateOf(0) }
    val actionPx = with(density) { actionWidth.toPx() }
    // Offered to a screen reader on the row itself, beside the context menu that lists them too.
    val spokenActions = remember(leading, trailing) {
        val actions = (leading + trailing).map { action ->
            CustomAccessibilityAction(action.label) {
                action.onClick()
                true
            }
        }
        ContainerActions(menuLabel = null, openMenu = null, actions = actions)
    }

    fun revealOf(actions: List<SwipeAction>) = actionPx * actions.size
    fun fullSwipeAt() = width * FullSwipeFraction

    /** Where the row shows for a finger at [travel]: free up to the actions, then on a rubber band. */
    fun shown(travel: Float): Float {
        val actions = if (travel > 0f) currentLeading else currentTrailing
        if (actions.isEmpty()) return 0f
        val sign = if (travel > 0f) 1f else -1f
        val distance = abs(travel)
        if (allowFullSwipe) return sign * distance.coerceAtMost(width.toFloat())
        val reveal = revealOf(actions)
        if (distance <= reveal) return travel
        val range = (width - reveal).coerceAtLeast(1f)
        val past = (distance - reveal) * RubberBand
        return sign * (reveal + past * range / (range + past))
    }

    /** The finger travel that shows the row at [offset]: [shown] undone, so a drag resumes in place. */
    fun travelFor(offset: Float): Float {
        if (allowFullSwipe || offset == 0f) return offset
        val reveal = revealOf(if (offset > 0f) currentLeading else currentTrailing)
        val distance = abs(offset)
        if (distance <= reveal) return offset
        val range = (width - reveal).coerceAtLeast(1f)
        val stretched = (distance - reveal).coerceAtMost(range * 0.999f)
        val past = stretched * range / (range - stretched)
        return sign(offset) * (reveal + past / RubberBand)
    }

    fun settleTo(target: Float, velocity: Float = 0f) {
        scope.launch { state.settleTo(target, velocity) }
    }

    fun perform(action: SwipeAction) {
        settleTo(0f)
        action.onClick()
    }

    val dragState = rememberDraggableState { delta ->
        val before = state.offset
        state.travel += if (rtl) -delta else delta
        state.offset = shown(state.travel)
        val threshold = fullSwipeAt()
        if (allowFullSwipe && (abs(before) < threshold) != (abs(state.offset) < threshold)) haptics.perform(AppHaptic.Threshold)
    }

    CompositionLocalProvider(LocalContainerActions provides spokenActions) {
        AppContextMenu(
            enabled = enabled && (leading.isNotEmpty() || trailing.isNotEmpty()),
            menu = {
                (leading + trailing).forEach { action ->
                    AppMenuItem(text = action.label, onClick = action.onClick, icon = action.icon, destructive = action.destructive)
                }
            },
            modifier = modifier,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .onSizeChanged { width = it.width },
            ) {
                // The buttons behind the row are its actions, not controls the actions belong to.
                CompositionLocalProvider(LocalContainerActions provides null) {
                    if (leading.isNotEmpty()) ActionPanel(leading, start = true, actionWidth, { state.offset.coerceAtLeast(0f) }, ::perform)
                    if (trailing.isNotEmpty()) ActionPanel(trailing, start = false, actionWidth, { (-state.offset).coerceAtLeast(0f) }, ::perform)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Placed on its own layer rather than translated: a layer that moves is what makes
                        // Compose ask an adaptive-refresh display for its high rate while the row moves.
                        .offset { IntOffset(state.offset.roundToInt(), 0) }
                        .background(background)
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Horizontal,
                            enabled = enabled && (leading.isNotEmpty() || trailing.isNotEmpty()),
                            onDragStarted = {
                                state.stop()
                                state.travel = travelFor(state.offset)
                            },
                            onDragStopped = { velocity ->
                                val logicalVelocity = if (rtl) -velocity else velocity
                                val offset = state.offset
                                val side = if (offset > 0f) currentLeading else currentTrailing
                                val sign = if (offset > 0f) 1f else -1f
                                // Where the row would coast to, as iOS projects a release before it snaps.
                                val coast = offset + logicalVelocity * ProjectionSeconds
                                when {
                                    side.isEmpty() || offset == 0f -> settleTo(0f, logicalVelocity)
                                    allowFullSwipe && abs(offset) > fullSwipeAt() -> {
                                        settleTo(sign * width, logicalVelocity)
                                        side.first().onClick()
                                    }
                                    coast * sign > revealOf(side) * RestFraction -> settleTo(sign * revealOf(side), logicalVelocity)
                                    else -> settleTo(0f, logicalVelocity)
                                }
                            },
                        )
                        .then(
                            if (state.isOpen) {
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

private const val FullSwipeFraction = 0.55f
private const val RestFraction = 0.5f
private val RubberBand = DesignTokens.Motion.rubberBand

// How far ahead of the row a release looks for where it was thrown (iOS's 0.99 projection).
private const val ProjectionSeconds = 0.099f
