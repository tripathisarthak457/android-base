package com.base.app.core.perfhud.overlay

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo

/**
 * Redraws the content of this node's layer at most once every [periodMillis], and only inside a
 * frame the window draws anyway.
 *
 * It asks for the redraw from the window's pre-draw pass, which runs only for a frame that is
 * already being drawn, and the layer is recorded again in that same frame; it never schedules a
 * frame of its own. A redraw on a timer would: on the phone a lone frame after idle misses its
 * deadline more often than not, so a performance readout refreshed that way makes the jank it
 * reports, four times a second, and keeps an idle app drawing.
 */
internal fun Modifier.redrawDuringFrames(periodMillis: Long): Modifier =
    this then RedrawDuringFramesElement(periodMillis)

private data class RedrawDuringFramesElement(val periodMillis: Long) : ModifierNodeElement<RedrawDuringFramesNode>() {

    override fun create() = RedrawDuringFramesNode(periodMillis)

    override fun update(node: RedrawDuringFramesNode) {
        node.periodMillis = periodMillis
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "redrawDuringFrames"
        properties["periodMillis"] = periodMillis
    }
}

private class RedrawDuringFramesNode(
    var periodMillis: Long,
) : Modifier.Node(), DrawModifierNode, ViewTreeObserver.OnPreDrawListener {

    private var view: View? = null
    private var lastRedrawNanos = 0L

    override val shouldAutoInvalidate: Boolean get() = false

    override fun onAttach() {
        view = requireView().also { it.viewTreeObserver.addOnPreDrawListener(this) }
    }

    override fun onDetach() {
        view?.viewTreeObserver?.removeOnPreDrawListener(this)
        view = null
    }

    override fun onPreDraw(): Boolean {
        val now = System.nanoTime()
        if (now - lastRedrawNanos >= periodMillis * NANOS_PER_MILLI) {
            lastRedrawNanos = now
            invalidateDraw()
        }
        return true
    }

    override fun ContentDrawScope.draw() {
        drawContent()
    }
}

private const val NANOS_PER_MILLI = 1_000_000L
