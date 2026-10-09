package com.base.app.core.designsystem.component.refresh

import androidx.compose.animation.core.animate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.feedback.AppActivityIndicator
import com.base.app.core.designsystem.foundation.AppHaptic
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pull to refresh over any scrolling content, as iOS does it: the content follows the finger down,
 * the spinner's spokes appear as the pull nears the point of no return, a tick plays when it is
 * crossed, and letting go there refreshes. A refresh the user did not pull for shows no spinner
 * unless [showExternalRefresh] asks, so a cached list does not flash one on every load.
 *
 * The pull is a plain float moved on each scroll event and read only while drawing; only the settle
 * animates. A refresh that ends at once still shows its spinner briefly, so the pull is seen to
 * have done something.
 */
@Composable
fun AppPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    showExternalRefresh: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    val refresh by rememberUpdatedState(onRefresh)
    val refreshing by rememberUpdatedState(isRefreshing)
    val spring = AppTheme.springs.layout.pixels
    val acknowledge = AppTheme.springs.fadeSlow.toLong()
    val threshold = with(density) { Threshold.toPx() }
    val maxPull = with(density) { MaxPull.toPx() }
    val resting = with(density) { Resting.toPx() }

    var offset by remember { mutableFloatStateOf(0f) }
    var pulledRefresh by remember { mutableStateOf(false) }
    var settling by remember { mutableStateOf<Job?>(null) }
    val showing by remember { derivedStateOf { offset > 0f } }
    val spinning = pulledRefresh || (isRefreshing && showExternalRefresh)

    fun settleTo(target: Float) {
        settling?.cancel()
        settling = scope.launch { animate(offset, target, animationSpec = spring) { value, _ -> offset = value.coerceAtLeast(0f) } }
    }

    val connection = remember(threshold, maxPull, resting) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || pulledRefresh || available.y >= 0f || offset <= 0f) return Offset.Zero
                settling?.cancel()
                val taken = -minOf(offset / RESISTANCE, -available.y)
                offset = (offset + taken * RESISTANCE).coerceAtLeast(0f)
                return Offset(0f, taken)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f || pulledRefresh || refreshing) return Offset.Zero
                settling?.cancel()
                val before = offset
                offset = (offset + available.y * RESISTANCE).coerceIn(0f, maxPull)
                if (before < threshold && offset >= threshold) haptics.perform(AppHaptic.Threshold)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offset <= 0f || pulledRefresh) return Velocity.Zero
                if (offset >= threshold && !refreshing) {
                    pulledRefresh = true
                    refresh()
                    settling?.cancel()
                    settling = scope.launch {
                        animate(offset, resting, animationSpec = spring) { value, _ -> offset = value }
                        delay(acknowledge)
                        if (!refreshing) {
                            pulledRefresh = false
                            settleTo(0f)
                        }
                    }
                } else {
                    settleTo(0f)
                }
                // The pull belongs to the indicator, not to a fling of the list beneath it.
                return Velocity(0f, available.y)
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            if (showExternalRefresh && !pulledRefresh) settleTo(resting)
        } else if (!pulledRefresh || settling?.isActive != true) {
            // A pulled refresh that is still showing its brief acknowledgement closes itself after it.
            pulledRefresh = false
            if (offset > 0f) settleTo(0f)
        }
    }

    Box(modifier = modifier.nestedScroll(connection)) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = offset }) { content() }
        if (showing || spinning) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(IndicatorArea)
                    .graphicsLayer {
                        translationY = (offset - size.height) / 2f
                        alpha = (offset / threshold).coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (spinning) {
                    AppActivityIndicator(size = SpinnerSize)
                } else {
                    PullSpokes(progress = { (offset / threshold).coerceIn(0f, 1f) })
                }
            }
        }
    }
}

/** The spinner's spokes appearing one by one as the pull nears the threshold, drawn from [progress]. */
@Composable
private fun PullSpokes(progress: () -> Float) {
    val color = AppTheme.colors.contentSecondary
    Canvas(Modifier.size(SpinnerSize)) {
        val shown = (progress() * SPOKES).toInt()
        val outer = size.minDimension / 2f
        val inner = outer * 0.48f
        val width = outer * 0.2f
        val center = Offset(size.width / 2f, size.height / 2f)
        for (i in 0 until shown) {
            rotate(degrees = i * 360f / SPOKES, pivot = center) {
                drawLine(color, Offset(center.x, center.y - inner), Offset(center.x, center.y - outer + width / 2f), width, StrokeCap.Round)
            }
        }
    }
}

private val Threshold = 80.dp
private val MaxPull = 140.dp
private val Resting = 56.dp
private val IndicatorArea = 56.dp
private val SpinnerSize = 24.dp
private const val RESISTANCE = 0.55f
private const val SPOKES = 8
