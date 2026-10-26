package com.example.finalafter.crui2.core.designsystem.foundation.scroll

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui2.core.designsystem.foundation.adaptive.LocalInputTraits
import com.example.finalafter.crui2.core.designsystem.foundation.layout.LocalBottomChrome
import com.example.finalafter.crui2.core.designsystem.foundation.layout.LocalTopChrome
import com.example.finalafter.crui2.core.designsystem.theme.LocalAppColors
import com.example.finalafter.crui2.core.designsystem.theme.LocalAppSprings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * iOS's scroll indicator for a vertical [state]: a thin capsule at the trailing edge that shows
 * while the content scrolls and fades a moment after it stops. Its track keeps clear of the top bar
 * and of whatever floats over the bottom edge, as the content's own padding does. With a mouse or
 * trackpad it is wider, as a desktop scroll bar is.
 *
 * ```
 * val listState = rememberLazyListState()
 * LazyColumn(state = listState, modifier = Modifier.appScrollIndicator(listState)) { … }
 * ```
 *
 * It is drawn over the content from the scroll position alone, so scrolling redraws the capsule
 * and recomposes nothing.
 */
fun Modifier.appScrollIndicator(state: ScrollableState): Modifier = this then ScrollIndicatorElement(state)

private data class ScrollIndicatorElement(val state: ScrollableState) : ModifierNodeElement<ScrollIndicatorNode>() {
    override fun create() = ScrollIndicatorNode(state)

    override fun update(node: ScrollIndicatorNode) = node.update(state)

    override fun InspectorInfo.inspectableProperties() {
        name = "appScrollIndicator"
    }
}

private class ScrollIndicatorNode(private var state: ScrollableState) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val shown = Animatable(0f)
    private var watcher: Job? = null

    override fun onAttach() = watch()

    fun update(state: ScrollableState) {
        if (state === this.state) return
        this.state = state
        if (isAttached) watch()
        invalidateDraw()
    }

    private fun watch() {
        watcher?.cancel()
        watcher = coroutineScope.launch {
            snapshotFlow { state.isScrollInProgress }.collectLatest { scrolling ->
                if (scrolling) {
                    shown.animateTo(1f, currentValueOf(LocalAppSprings).quickFade())
                } else {
                    delay(LingerMillis)
                    shown.animateTo(0f, currentValueOf(LocalAppSprings).slowFade())
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = shown.value
        val indicator = state.scrollIndicatorState ?: return
        if (alpha <= 0f) return
        val content = indicator.contentSize
        val viewport = indicator.viewportSize
        val offset = indicator.scrollOffset
        if (content == Int.MAX_VALUE || offset == Int.MAX_VALUE || content <= viewport || viewport <= 0) return

        val top = currentValueOf(LocalTopChrome).value.toPx() + ScrollIndicatorDefaults.Inset.toPx()
        val bottom = size.height - currentValueOf(LocalBottomChrome).value.toPx() - ScrollIndicatorDefaults.Inset.toPx()
        val track = bottom - top
        if (track <= 0f) return
        val length = (track * viewport / content).coerceIn(ScrollIndicatorDefaults.MinLength.toPx().coerceAtMost(track), track)
        val travel = (offset.toFloat() / (content - viewport)).coerceIn(0f, 1f)
        val width = (if (currentValueOf(LocalInputTraits).finePointer) ScrollIndicatorDefaults.PointerWidth else ScrollIndicatorDefaults.TouchWidth).toPx()
        val x = if (layoutDirection == LayoutDirection.Ltr) size.width - ScrollIndicatorDefaults.Inset.toPx() - width else ScrollIndicatorDefaults.Inset.toPx()
        drawRoundRect(
            color = currentValueOf(LocalAppColors).contentTertiary,
            topLeft = Offset(x, top + (track - length) * travel),
            size = Size(width, length),
            cornerRadius = CornerRadius(width / 2f),
            alpha = alpha,
        )
    }
}

/**
 * The indicator's width for touch and for a pointer, its inset from the edge and its shortest
 * length.
 */
internal object ScrollIndicatorDefaults {
    val TouchWidth = 3.dp
    val PointerWidth = 6.dp
    val Inset = 3.dp
    val MinLength = 36.dp
}

private const val LingerMillis = 500L
