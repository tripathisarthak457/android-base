package com.example.wp04.everything.core.navigation.back

import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Renders the screen on top into the back card's texture as soon as a finger lands at the window's
 * back edge, before the system decides the touch is a back gesture and hands it over: the gesture's
 * first frame then moves a texture that already exists, rather than rendering it on a GPU still at
 * its idle clock. The system's own back animation starts the same way, from a buffer it already has.
 *
 * It only watches; nothing is consumed. The texture is let go as soon as the touch turns out to be
 * something else: a vertical move (a scroll), a lift no gesture follows, or a second finger. One
 * coroutine per touch-down and per lift, none per move.
 */
internal fun Modifier.backEdgeWarm(host: BackCardHost, edges: BackEdges = SystemGestureEdges): Modifier =
    this then BackEdgeWarmElement(host, edges)

/** Whether a touch at [windowX], in the window of [view], lands where the system's back gesture starts. */
internal fun interface BackEdges {
    fun contains(view: View, windowX: Float): Boolean
}

/** The window's left and right system gesture insets. The card itself needs API 34. */
internal val SystemGestureEdges = BackEdges { view, windowX ->
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val insets = view.rootWindowInsets?.getInsets(WindowInsets.Type.systemGestures())
        insets != null && (windowX < insets.left || windowX >= view.rootView.width - insets.right)
    } else {
        false
    }
}

private data class BackEdgeWarmElement(val host: BackCardHost, val edges: BackEdges) :
    ModifierNodeElement<BackEdgeWarmNode>() {
    override fun create() = BackEdgeWarmNode(host, edges)

    override fun update(node: BackEdgeWarmNode) {
        node.host = host
        node.edges = edges
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "backEdgeWarm"
    }
}

private class BackEdgeWarmNode(var host: BackCardHost, var edges: BackEdges) :
    Modifier.Node(), PointerInputModifierNode, CompositionLocalConsumerModifierNode {

    /** The touch that warmed the card, while it may still become a back gesture. */
    private var touch: PointerId? = null
    private var touchY = 0f

    /** Lets the texture go if no gesture takes it over. */
    private var letGo: Job? = null

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial) return
        val changes = pointerEvent.changes
        when (pointerEvent.type) {
            PointerEventType.Press -> when {
                touch == null && changes.size == 1 -> press(changes[0])
                // A second finger: the system's back gesture is a one-finger swipe.
                touch != null -> cool()
            }
            PointerEventType.Move -> {
                val change = changes.firstOrNull { it.id == touch } ?: return
                // A back swipe is sideways; a vertical move is a scroll.
                if (abs(change.position.y - touchY) > currentValueOf(LocalViewConfiguration).touchSlop) cool()
            }
            PointerEventType.Release -> if (changes.any { it.id == touch && !it.pressed }) lift()
        }
    }

    override fun onCancelPointerInput() {
        if (touch != null) lift()
    }

    override fun onDetach() {
        if (touch != null || letGo != null) host.card.cool()
        touch = null
        letGo = null
    }

    private fun press(change: PointerInputChange) {
        val windowX = requireLayoutCoordinates().localToWindow(change.position).x
        if (!edges.contains(currentValueOf(LocalView), windowX) || !host.warmCard()) return
        touch = change.id
        touchY = change.position.y
        coolAfter(HOLD_MILLIS)
    }

    private fun lift() {
        touch = null
        // The system takes a back gesture's pointer away from the window first and reports the
        // gesture's start after it, a frame or two later: the texture is kept for that long.
        coolAfter(GRACE_MILLIS)
    }

    private fun cool() {
        touch = null
        letGo?.cancel()
        letGo = null
        host.card.cool()
    }

    private fun coolAfter(millis: Long) {
        letGo?.cancel()
        letGo = coroutineScope.launch {
            delay(millis)
            touch = null
            letGo = null
            host.card.cool()
        }
    }

    private companion object {
        const val GRACE_MILLIS = 120L

        /** A finger resting at the edge this long is not starting a swipe. */
        const val HOLD_MILLIS = 1_000L
    }
}
