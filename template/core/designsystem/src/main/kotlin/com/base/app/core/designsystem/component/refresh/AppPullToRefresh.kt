package com.base.app.core.designsystem.component.refresh

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.feedback.AppCircularProgress
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.foundation.HapticEffect
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * Pull-to-refresh, over any scrollable.
 *
 * ## Why by hand
 *
 * Pull-to-refresh ships in Material 3. The mechanics are a `NestedScrollConnection` plus an
 * indicator, so rebuilding it costs a hundred lines and buys a spinner that matches the rest of
 * this design system rather than one that does not.
 *
 * ## The drag is resisted
 *
 * The indicator moves at 55% of the finger, and its travel is capped. Without resistance the
 * indicator shoots to the threshold in a few millimetres and the gesture has no sense of tension
 * — the user cannot feel how far they have to go, so they either overshoot or give up early.
 *
 * ## Only the overscroll is consumed
 *
 * `onPreScroll` claims upward drags only while the indicator is already extended, so a list that
 * is scrolled down still scrolls normally; `onPostScroll` claims downward drags only once the
 * list itself has reached the top and left the delta unconsumed. That ordering is what stops the
 * gesture fighting the list.
 */
@Composable
fun AppPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val haptics = rememberAppHaptics()

    val thresholdPx = with(density) { THRESHOLD.toPx() }
    val maxPullPx = with(density) { MAX_PULL.toPx() }
    val restingPx = with(density) { RESTING.toPx() }

    val offset = remember { Animatable(0f) }

    val connection = remember(thresholdPx, maxPullPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f || offset.value <= 0f) return Offset.Zero
                // Dragging back up while the indicator is out: retract it before letting the
                // list scroll, so the gesture reverses cleanly instead of the list jumping.
                val consumed = -minOf(offset.value, -available.y)
                scope.launch { offset.snapTo((offset.value + consumed).coerceAtLeast(0f)) }
                return Offset(0f, consumed)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y <= 0f || isRefreshing) return Offset.Zero
                val resisted = available.y * DRAG_RESISTANCE
                scope.launch {
                    offset.snapTo((offset.value + resisted).coerceIn(0f, maxPullPx))
                }
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offset.value <= 0f) return Velocity.Zero
                if (offset.value >= thresholdPx && !isRefreshing) {
                    haptics.perform(HapticEffect.Confirm)
                    currentOnRefresh()
                    offset.animateTo(restingPx)
                } else {
                    offset.animateTo(0f)
                }
                // The fling is not consumed: releasing past the threshold should still let a
                // flick carry the list, which is what makes refresh-then-scroll one gesture.
                return Velocity.Zero
            }
        }
    }

    // Retracting is driven by the caller's flag rather than by the gesture, so the indicator stays
    // out for as long as the refresh actually takes — including when it finishes in 20ms, where
    // snapping away instantly would read as nothing having happened.
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && offset.value > 0f) offset.animateTo(0f)
    }

    Box(modifier = modifier.nestedScroll(connection)) {
        content()

        val progress = (offset.value / thresholdPx).coerceIn(0f, 1f)
        if (offset.value > 0f) {
            AppSurface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = offset.value - size.height
                        alpha = progress
                        scaleX = SCALE_FLOOR + progress * (1f - SCALE_FLOOR)
                        scaleY = scaleX
                    }
                    .size(INDICATOR_SIZE),
                shape = AppTheme.shapes.pill,
                color = AppTheme.colors.surface,
                elevation = AppTheme.elevation.overlay,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isRefreshing) {
                        AppCircularProgress(size = 20.dp, color = AppTheme.colors.accent)
                    } else {
                        // Before the release it is a determinate ring filling towards the
                        // threshold: the user can see exactly how much further to pull.
                        AppCircularProgress(
                            progress = progress,
                            size = 20.dp,
                            color = AppTheme.colors.accent,
                        )
                    }
                }
            }
        }
    }
}

private val THRESHOLD = 80.dp
private val MAX_PULL = 140.dp
private val RESTING = 72.dp
private val INDICATOR_SIZE = 38.dp
private const val DRAG_RESISTANCE = 0.55f
private const val SCALE_FLOOR = 0.6f
