package com.base.app.core.designsystem.component.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.flow.drop
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

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
    val springs = AppTheme.springs
    val flingBehavior = remember(listState, springs) {
        snapFlingBehavior(SnapLayoutInfoProvider(listState), springs.flingFast, springs.thumb.pixels)
    }
    val haptics = rememberAppHaptics()
    val density = LocalDensity.current
    val style = AppTheme.typography.body
    val rowHeight = if (itemHeight != Dp.Unspecified) itemHeight else maxOf(44.dp, with(density) { style.lineHeight.toDp() } + 16.dp)
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
    val visiblePx = rowPx * visibleCount
    // The list is laid out flat over more rows than are visible; bending it onto the drum pulls the
    // outer rows in, so the extra rows are what fills the wheel's top and bottom edges.
    val laidOutRows = visibleCount * Squeeze
    val cameraPx = with(density) { PerspectiveDistance.toPx() }

    Box(
        modifier = modifier
            .height(rowHeight * visibleCount)
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
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
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(unbounded = true)
                .height(rowHeight * laidOutRows),
            contentPadding = PaddingValues(vertical = rowHeight * (laidOutRows - 1) / 2),
        ) {
            items(count = items.size, key = { it }) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .graphicsLayer {
                            // Driven by the row's offset in the flat list, so the drum turns under the finger.
                            val flat = centreOffset(listState, index)
                            val angle = flat / visiblePx * 2f * MaxVisibleAngle / Squeeze
                            if (abs(angle) >= HalfTurn) {
                                alpha = 0f
                                return@graphicsLayer
                            }
                            val radius = visiblePx * DiameterRatio / 2f
                            val depth = radius * (1f - cos(angle))
                            val perspective = 1f / (1f + depth / cameraPx)
                            translationY = radius * sin(angle) * perspective - flat
                            scaleX = perspective
                            scaleY = perspective
                            rotationX = Math.toDegrees(angle.toDouble()).toFloat()
                            cameraDistance = cameraPx / CameraUnitPx
                            alpha = lerp(1f, OffCentreAlpha, (abs(flat) / rowPx).coerceAtMost(1f))
                            // One line of text: fading each draw is exact and needs no offscreen buffer.
                            compositingStrategy = CompositingStrategy.ModulateAlpha
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

// The drum of iOS's picker, as UIKit draws it: a cylinder slightly wider than the wheel is tall,
// seen from a fixed distance, with rows packed closer than a flat list and dimmed off the centre.
private const val DiameterRatio = 1.07f
private const val Squeeze = 1.45f
private const val OffCentreAlpha = 0.447f
private val PerspectiveDistance = 333.dp
private val MaxVisibleAngle = asin(1f / DiameterRatio)
private const val HalfTurn = (PI / 2).toFloat()

/** The unit `graphicsLayer`'s camera distance is given in: 72 pixels, as Skia's camera counts them. */
private const val CameraUnitPx = 72f

@AppComponentPreviews
@Composable
private fun AppWheelPickerPreview() {
    Row(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppWheelPicker(items = (1..12).toList(), selectedIndex = 3, onSelectedChange = {}, modifier = Modifier.weight(1f)) { "$it" }
        AppWheelPicker(
            items = listOf("Small", "Medium", "Large", "Extra large for the whole family"),
            selectedIndex = 1,
            onSelectedChange = {},
            modifier = Modifier.weight(2f),
            visibleCount = 3,
        ) { it }
    }
}
