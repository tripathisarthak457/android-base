package com.base.app.core.designsystem.component.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.clickableNoIndication
import com.base.app.core.designsystem.foundation.HapticEffect
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * One action revealed by swiping a row.
 *
 * [isDestructive] does two things: it colours the action, and it makes a full swipe past the
 * threshold trigger it directly rather than resting the row open. Delete is the action people
 * swipe hard for; making them swipe and then tap is the interaction everyone complains about.
 */
data class SwipeAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val containerColor: Color,
    val contentColor: Color = Color.White,
    val isDestructive: Boolean = false,
)

/**
 * A row that reveals actions when swiped.
 *
 * ## Resting versus committing
 *
 * A short swipe rests the row open so the actions can be tapped. A swipe past
 * [COMMIT_FRACTION] of the row's width fires the first action directly — but only when it is
 * destructive, because a full-swipe that silently performs a non-destructive action the user
 * cannot see is a surprise, and a full-swipe that performs a *destructive* one they can undo is
 * the interaction they expect.
 *
 * ## The actions are behind, not beside
 *
 * They fill the row's own bounds and the content slides over them, so no measurement pass is
 * needed and the actions cannot be laid out at a different height than the row.
 *
 * ## Resetting
 *
 * [resetKey] snaps the row closed when it changes. Pass the list's own identity: a row left open
 * when the list refreshes underneath it ends up showing another item's actions.
 */
@Composable
fun AppSwipeToAction(
    actions: List<SwipeAction>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actionWidth: Dp = 84.dp,
    resetKey: Any? = null,
    content: @Composable () -> Unit,
) {
    if (actions.isEmpty()) {
        Box(modifier = modifier) { content() }
        return
    }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val motion = AppTheme.motion
    val haptics = rememberAppHaptics()

    val offset = remember { Animatable(0f) }
    var rowWidth by remember { mutableFloatStateOf(0f) }
    val revealPx = with(density) { (actionWidth * actions.size).toPx() }
    val currentActions by rememberUpdatedState(actions)

    LaunchedEffect(resetKey) { offset.snapTo(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidth = it.width.toFloat() },
    ) {
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End,
        ) {
            actions.forEach { action ->
                Column(
                    modifier = Modifier
                        .width(actionWidth)
                        // The Row above uses matchParentSize, so it has a definite height here
                        // and fillMaxHeight resolves against the row rather than against nothing.
                        .fillMaxHeight()
                        .background(action.containerColor)
                        .clickableNoIndication {
                            scope.launch { offset.animateTo(0f, motion.sheet()) }
                            action.onClick()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    AppIcon(action.icon, contentDescription = null, tint = action.contentColor)
                    AppText(
                        text = action.label,
                        style = AppTheme.typography.labelSmall,
                        color = action.contentColor,
                        maxLines = 1,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offset.value }
                .background(AppTheme.colors.surface)
                .draggable(
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            // Clamped so the row cannot be dragged the wrong way or past its
                            // actions — an over-drag exposes the background behind the list.
                            offset.snapTo((offset.value + delta).coerceIn(-revealPx, 0f))
                        }
                    },
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    onDragStopped = { velocity ->
                        val travelled = abs(offset.value)
                        val destructive = currentActions.firstOrNull { it.isDestructive }
                        val committed = rowWidth > 0f && travelled > rowWidth * COMMIT_FRACTION

                        when {
                            destructive != null && committed -> {
                                haptics.perform(HapticEffect.Confirm)
                                offset.animateTo(-rowWidth, tween(motion.quick))
                                destructive.onClick()
                            }

                            travelled > revealPx * REST_FRACTION || velocity < -FLING_VELOCITY -> {
                                // The row has come to rest open. Without a tick here the gesture
                                // has no moment of commitment — the actions simply appear, and
                                // the difference between "resting open" and "still dragging" is
                                // only visible, never felt.
                                haptics.perform(HapticEffect.Threshold)
                                offset.animateTo(-revealPx, motion.sheet())
                            }

                            else -> offset.animateTo(0f, motion.sheet())
                        }
                    },
                )
                // Tapping an open row closes it rather than activating whatever is under the
                // finger, which is what every list with this gesture does and what people expect.
                .then(
                    if (offset.value != 0f) {
                        Modifier.clickableNoIndication {
                            scope.launch { offset.animateTo(0f, motion.sheet()) }
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

private const val COMMIT_FRACTION = 0.55f
private const val REST_FRACTION = 0.5f
private const val FLING_VELOCITY = 800f
