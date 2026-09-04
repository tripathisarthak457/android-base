package com.base.app.core.designsystem.component.datetime

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
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
import com.base.app.core.designsystem.foundation.HapticEffect
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.flow.drop
import kotlin.math.abs

/**
 * A scrolling wheel that snaps to whichever item is centred.
 *
 * The building block for the time picker and for any "pick one of many" where a dropdown would be
 * a long list of near-identical rows.
 *
 * ## Finding the selection
 *
 * The centred item is found by measuring, not by arithmetic on the scroll offset. With
 * content padding in play, `firstVisibleItemIndex` plus a computed offset gets the answer right
 * in the middle of the list and wrong at both ends, which shows up as the first and last options
 * being unselectable. Asking `layoutInfo` which visible item's centre is closest to the
 * viewport's centre is exact everywhere, including while the list is still settling.
 *
 * ## Items fade and shrink with distance
 *
 * Purely functional: it makes the centre unambiguous when several adjacent values look alike —
 * which for a list of two-digit numbers is always.
 */
@Composable
fun <T> AppWheelPicker(
    items: List<T>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 40.dp,
    label: (T) -> String,
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 0)
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val edgeItems = visibleCount / 2
    val haptics = rememberAppHaptics()
    val motion = AppTheme.motion

    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf selectedIndex
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index
                ?.coerceIn(0, items.lastIndex)
                ?: selectedIndex
        }
    }

    // Nothing is reported until the wheel has been placed at its starting value.
    //
    // Without this the first layout pass sees the list at index 0 while the caller's value is
    // still, say, 9 — and reports 9 -> 0 before the initial scroll has had a chance to run. On the
    // time picker that reads as opening at 09:30 and immediately snapping to 12:00.
    var placed by remember { mutableStateOf(false) }

    LaunchedEffect(items) {
        listState.scrollToItem(selectedIndex.coerceIn(0, items.lastIndex))
        placed = true
    }

    // Only report a change once the wheel has come to rest. Emitting on every frame of a fling
    // would fire the callback thirty times for one gesture, and anything it drives — a network
    // call, a validation — thirty times with it.
    LaunchedEffect(listState, items, placed) {
        if (!placed) return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress to centeredIndex }
            .collect { (scrolling, index) ->
                if (!scrolling && index != selectedIndex) onSelectedChange(index)
            }
    }

    // The tick, on the other hand, fires on every item that passes the centre — that is the whole
    // point of it. A wheel that only buzzes when it stops feels like a list; one that ticks as it
    // spins feels like a wheel, and it is the single thing that most separates this control from
    // a scrolling column of text.
    LaunchedEffect(listState, placed) {
        if (!placed) return@LaunchedEffect
        snapshotFlow { centeredIndex }
            .drop(1)
            .collect { haptics.perform(HapticEffect.Tick) }
    }

    // The caller changing the value out from under the wheel — a "now" button, a reset — animates
    // rather than jumps, so it reads as the same control moving.
    LaunchedEffect(selectedIndex, placed) {
        if (!placed || listState.isScrollInProgress) return@LaunchedEffect
        if (centeredIndex != selectedIndex) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, items.lastIndex))
        }
    }

    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val edgeSpan = edgeItems.toFloat().coerceAtLeast(1f)

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(AppTheme.shapes.xs)
                .background(AppTheme.colors.surfaceVariant),
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                vertical = itemHeight * edgeItems,
            ),
        ) {
            items(items.size) { index ->
                val selected = index == centeredIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            // Driven by where the row actually is, not by how many rows away it
                            // is from the selection. An integer distance can only change one
                            // whole step at a time, so the fade and the tilt jump between two
                            // states as the wheel turns; a fraction of the item's own height
                            // moves with the finger, which is what makes it read as a cylinder
                            // rather than a list that is highlighting a row.
                            val offset = itemCenterOffset(listState, index)
                            val steps = (offset / itemHeightPx).coerceIn(-edgeSpan, edgeSpan)
                            val magnitude = abs(steps)

                            alpha = (1f - magnitude * FADE_PER_STEP).coerceIn(MIN_ALPHA, 1f)
                            val shrink = 1f - magnitude * (1f - SHRUNK_SCALE) / edgeSpan
                            scaleX = shrink
                            scaleY = shrink

                            // The rows at the ends lean away, so the strip reads as curving over
                            // the horizon instead of sliding flat behind a window.
                            rotationX = steps * TILT_PER_STEP
                            cameraDistance = CAMERA_DISTANCE.dp.toPx()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = label(items[index]),
                        style = if (selected) {
                            AppTheme.typography.headingSmall
                        } else {
                            AppTheme.typography.bodyMedium
                        },
                        color = if (selected) {
                            AppTheme.colors.contentPrimary
                        } else {
                            AppTheme.colors.contentTertiary
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * How far this row's centre is from the wheel's centre, in pixels.
 *
 * Read from `layoutInfo` rather than computed, for the same reason the selected index is: with
 * content padding in play, arithmetic on the scroll offset is right in the middle of the list and
 * wrong at both ends. A row that is not currently laid out is parked at the far edge so it
 * arrives already faded rather than popping in at full strength.
 */
private fun itemCenterOffset(state: LazyListState, index: Int): Float {
    val info = state.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return Float.MAX_VALUE
    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
    return (item.offset + item.size / 2f) - viewportCenter
}

private const val FADE_PER_STEP = 0.30f
private const val MIN_ALPHA = 0.22f
private const val SHRUNK_SCALE = 0.86f

/** Degrees of lean per item away from the centre. Enough to curve, not enough to read as broken. */
private const val TILT_PER_STEP = 26f

/**
 * How far the "camera" sits from the strip, in dp.
 *
 * The default is 8dp, which at this rotation makes the far rows fan out with a violently wrong
 * perspective. Pushing it back flattens the projection to something that looks like a physical
 * drum rather than a fisheye.
 */
private const val CAMERA_DISTANCE = 24f
