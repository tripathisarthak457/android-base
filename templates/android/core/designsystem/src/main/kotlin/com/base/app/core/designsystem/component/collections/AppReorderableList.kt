package com.base.app.core.designsystem.component.collections

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.foundation.AppHaptic
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A list whose rows can be put in order: press and hold a row, then drag it. It lifts with a tick,
 * the others slide out of its way, and the list scrolls when the row is held near an edge. A
 * screen-reader user moves a row with "Move up" and "Move down" from its actions menu.
 *
 * The lifted row follows the finger in its layer, and the drag moves a plain float, so dragging
 * recomposes only when the order changes.
 */
@Composable
fun <T> AppReorderableList(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable LazyItemScope.(item: T, lifted: Boolean) -> Unit,
) {
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val springs = AppTheme.springs
    val move by rememberUpdatedState(onMove)
    val currentItems by rememberUpdatedState(items)
    val drag = remember(state) { ReorderDrag(state) }
    val moveUp = stringResource(R.string.designsystem_move_up)
    val moveDown = stringResource(R.string.designsystem_move_down)

    LazyColumn(
        state = state,
        contentPadding = contentPadding,
        modifier = modifier.pointerInput(state) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    val hit = state.layoutInfo.visibleItemsInfo.firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
                        ?: return@detectDragGesturesAfterLongPress
                    drag.start(hit.key, hit.offset)
                    haptics.perform(AppHaptic.Threshold)
                    drag.autoScroll = scope.launch {
                        while (true) {
                            val speed = drag.edgeSpeed(EDGE.toPx(), MAX_SCROLL.toPx())
                            withFrameMillis { }
                            if (speed != 0f) state.scrollBy(speed)
                        }
                    }
                },
                onDrag = { change, amount ->
                    change.consume()
                    drag.travelled += amount.y
                    drag.swapTarget(currentItems.map(key))?.let { (from, to) -> move(from, to) }
                },
                onDragEnd = { drag.drop(scope, springs.layout.pixels) },
                onDragCancel = { drag.drop(scope, springs.layout.pixels) },
            )
        },
    ) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            val lifted = drag.key == key(item)
            Box(
                modifier = Modifier
                    .zIndex(if (lifted) 1f else 0f)
                    .then(if (lifted) Modifier else Modifier.animateItem(placementSpec = springs.layout.offset))
                    .graphicsLayer {
                        if (lifted) {
                            translationY = drag.translation()
                            scaleX = LIFT_SCALE
                            scaleY = LIFT_SCALE
                            shadowElevation = LIFT_ELEVATION.toPx()
                        }
                    }
                    .semantics {
                        customActions = buildList {
                            if (index > 0) add(CustomAccessibilityAction(moveUp) { move(index, index - 1); true })
                            if (index < items.lastIndex) add(CustomAccessibilityAction(moveDown) { move(index, index + 1); true })
                        }
                    },
            ) {
                itemContent(item, lifted)
            }
        }
    }
}

/** The lifted row: where it started, how far the finger has moved it, and where it is laid out now. */
@Stable
private class ReorderDrag(private val list: LazyListState) {
    var key by mutableStateOf<Any?>(null)
        private set
    var travelled by mutableFloatStateOf(0f)
    private var startOffset = 0
    private var settle: Job? = null
    var autoScroll: Job? = null

    fun start(key: Any, offset: Int) {
        settle?.cancel()
        this.key = key
        startOffset = offset
        travelled = 0f
    }

    private fun current() = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    /** How far the row is drawn from its laid-out slot: where the finger has it, minus where it sits. */
    fun translation(): Float {
        val item = current() ?: return travelled
        return startOffset + travelled - item.offset
    }

    /** The neighbour the lifted row's middle has passed, as a move from its index to that one's. */
    fun swapTarget(keys: List<Any>): Pair<Int, Int>? {
        val item = current() ?: return null
        val middle = startOffset + travelled + item.size / 2f
        val target = list.layoutInfo.visibleItemsInfo.firstOrNull { other ->
            other.key != key && middle.toInt() in other.offset..(other.offset + other.size) &&
                (if (other.index > item.index) middle > other.offset + other.size / 2f else middle < other.offset + other.size / 2f)
        } ?: return null
        val from = keys.indexOf(key)
        val to = keys.indexOf(target.key)
        return if (from >= 0 && to >= 0) from to to else null
    }

    /** Scroll speed near the viewport's edges, faster the deeper the row is held into the edge. */
    fun edgeSpeed(edge: Float, max: Float): Float {
        val item = current() ?: return 0f
        val info = list.layoutInfo
        val top = startOffset + travelled
        val bottom = top + item.size
        return when {
            top < info.viewportStartOffset + edge -> -max * ((info.viewportStartOffset + edge - top) / edge).coerceIn(0f, 1f)
            bottom > info.viewportEndOffset - edge -> max * ((bottom - (info.viewportEndOffset - edge)) / edge).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    fun drop(scope: CoroutineScope, spec: AnimationSpec<Float>) {
        autoScroll?.cancel()
        autoScroll = null
        val item = current()
        if (item == null) {
            key = null
            return
        }
        settle = scope.launch {
            val from = translation()
            animate(from, 0f, animationSpec = spec) { value, _ -> travelled = item.offset - startOffset + value }
            key = null
        }
    }
}

private val EDGE = 56.dp
private val MAX_SCROLL = 14.dp
private val LIFT_ELEVATION = 8.dp
private const val LIFT_SCALE = 1.02f
