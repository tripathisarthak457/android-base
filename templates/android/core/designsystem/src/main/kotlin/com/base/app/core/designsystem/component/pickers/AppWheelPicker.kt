package com.base.app.core.designsystem.component.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppHaptic
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.abs
import kotlinx.coroutines.flow.drop

/**
 * A wheel that snaps to whichever item is centred, as iOS's picker does: one font throughout, the
 * rows leaning and fading as they leave the middle, a tick as each one passes it. The value is
 * reported once the wheel settles, not on every frame of a fling.
 *
 * The callbacks are read when the wheel settles, not when it was first composed, so wheels that
 * build their value from each other (hours and minutes) never report a stale neighbour.
 */
@Composable
fun <T> AppWheelPicker(
    items: List<T>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = Dp.Unspecified,
    label: (T) -> String,
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex))
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val haptics = rememberAppHaptics()
    val density = LocalDensity.current
    val style = AppTheme.typography.body
    val rowHeight = if (itemHeight != Dp.Unspecified) itemHeight else maxOf(44.dp, with(density) { style.lineHeight.toDp() } + 16.dp)
    val edgeItems = visibleCount / 2
    val current by rememberUpdatedState(selectedIndex)
    val onChange by rememberUpdatedState(onSelectedChange)

    val centred by remember(listState, items) {
        derivedStateOf { centredIndex(listState)?.coerceIn(0, items.lastIndex) ?: current }
    }

    // Silent until the wheel has been placed on its starting value.
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(items) {
        listState.scrollToItem(current.coerceIn(0, items.lastIndex))
        placed = true
    }

    LaunchedEffect(listState, items, placed) {
        if (!placed) return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress to centred }
            .collect { (scrolling, index) -> if (!scrolling && index != current) onChange(index) }
    }

    LaunchedEffect(listState, placed) {
        if (!placed) return@LaunchedEffect
        snapshotFlow { centred }.drop(1).collect { haptics.perform(AppHaptic.Selection) }
    }

    // The caller moving the value (a "now" button, a reset) scrolls the wheel there, so it reads as
    // the same control moving.
    LaunchedEffect(selectedIndex, placed) {
        if (!placed || listState.isScrollInProgress) return@LaunchedEffect
        if (centred != selectedIndex) listState.animateScrollToItem(selectedIndex.coerceIn(0, items.lastIndex))
    }

    val rowPx = with(density) { rowHeight.toPx() }
    val edgeSpan = edgeItems.toFloat().coerceAtLeast(1f)

    Box(modifier = modifier.height(rowHeight * visibleCount), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .clip(AppTheme.shapes.sm)
                .background(AppTheme.colors.fill),
        )
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = rowHeight * edgeItems),
        ) {
            items(count = items.size, key = { it }) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .graphicsLayer {
                            // Driven by the row's fractional offset, so fade and lean follow the finger.
                            val steps = (centreOffset(listState, index) / rowPx).coerceIn(-edgeSpan, edgeSpan)
                            val magnitude = abs(steps)
                            alpha = (1f - magnitude * FADE_PER_STEP).coerceIn(MIN_ALPHA, 1f)
                            val shrink = 1f - magnitude * (1f - SHRUNK_SCALE) / edgeSpan
                            scaleX = shrink
                            scaleY = shrink
                            rotationX = steps * TILT_PER_STEP
                            cameraDistance = CAMERA_DISTANCE.dp.toPx()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = label(items[index]),
                        style = style,
                        color = AppTheme.colors.contentPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun centredIndex(state: LazyListState): Int? {
    val info = state.layoutInfo
    val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - centre) }?.index
}

/** How far this row's centre is from the wheel's centre, in pixels. */
private fun centreOffset(state: LazyListState, index: Int): Float {
    val info = state.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return Float.MAX_VALUE
    val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
    return (item.offset + item.size / 2f) - centre
}

private const val FADE_PER_STEP = 0.30f
private const val MIN_ALPHA = 0.22f
private const val SHRUNK_SCALE = 0.86f

/** Degrees of lean per item away from the centre: enough to curve, not enough to read as broken. */
private const val TILT_PER_STEP = 26f
private const val CAMERA_DISTANCE = 24f
