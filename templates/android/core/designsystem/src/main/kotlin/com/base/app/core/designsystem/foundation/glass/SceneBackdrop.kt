package com.base.app.core.designsystem.foundation.glass

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop

/**
 * The scene behind a [GlassStage], recorded once per frame into [layer] and sampled from it by
 * reference.
 *
 * Kyant's own `layerBackdrop` draws its content and then records it a second time into the
 * backdrop; here the content is recorded once and the screen draws the recording, so a frame of
 * scrolling costs one recording however many surfaces sample it. A sample holds the layer itself,
 * not a copy, so it re-renders when the scene changes without being recorded again: only a
 * sampling surface that moves records anew.
 */
@Stable
internal class SceneBackdrop(
    val layer: GraphicsLayer,
    private val background: () -> Color,
) : Backdrop {

    // Read only while a sample is drawn; the position below is what makes samples redraw. The
    // scene box is placed again whenever its parent lays out (the bar minimizing, the keyboard),
    // and that alone must not record every sample anew.
    private var scene: LayoutCoordinates? = null
    private var sceneOrigin by mutableStateOf(Offset.Unspecified)
    private val layerScope = GraphicsLayerScope()

    override val isCoordinatesDependent: Boolean
        get() = true

    fun placed(coordinates: LayoutCoordinates) {
        scene = coordinates
        sceneOrigin = coordinates.positionInRoot()
    }

    fun removed() {
        scene = null
        sceneOrigin = Offset.Unspecified
    }

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        // Painted first, so a blur reaching past the scene's edge pulls in the window's colour,
        // not transparency. It fills the sample's own layer, which is far smaller than a screen.
        drawContext.canvas.nativeCanvas.drawColor(background().toArgb())
        if (sceneOrigin.isUnspecified) return
        val scene = scene?.takeIf { it.isAttached } ?: return
        val surface = coordinates?.takeIf { it.isAttached } ?: return
        if (!scene.canBeSampledBy(surface)) return
        val offset = scene.localPositionOf(surface, Offset.Zero)
        withTransform({
            if (layerBlock != null) undoScale(layerBlock)
            translate(-offset.x, -offset.y)
        }) {
            drawLayer(layer)
        }
    }

    /**
     * A surface that scales itself (a lens swelling under the finger) would otherwise magnify
     * what it samples. Only scale is undone: glass chrome never rotates.
     */
    private fun DrawTransform.undoScale(layerBlock: GraphicsLayerScope.() -> Unit) {
        val transform = layerScope.apply {
            scaleX = 1f
            scaleY = 1f
            layerBlock()
        }
        if (transform.scaleX != 0f && transform.scaleY != 0f) {
            scale(1f / transform.scaleX, 1f / transform.scaleY, Offset.Zero)
        }
    }
}

/**
 * Whether [surface] lies in the window this scene is drawn in, outside the scene's own subtree.
 *
 * A surface inside the scene would sample a recording of itself: the render thread then draws a
 * node into itself and dies with SIGSEGV, the crash Kyant's issue #100 reports for a top bar
 * inside the recorded screen. That is a mistake in the code that placed it, so it fails here,
 * loudly and with a reason. A surface in another window (a dialog, a popup) has no scene behind
 * it to sample.
 */
private fun LayoutCoordinates.canBeSampledBy(surface: LayoutCoordinates): Boolean {
    var node: LayoutCoordinates? = surface
    var root: LayoutCoordinates = surface
    while (node != null) {
        check(node !== this) {
            "A glass surface samples the GlassStage scene it is drawn inside. Put it in the " +
                "stage's chrome slot, or draw it with Modifier.glassSurface, which samples nothing."
        }
        root = node
        node = node.parentCoordinates
    }
    return root === findRootCoordinates()
}

/** Records this node's content into [scene] once per frame and draws the recording. */
internal fun Modifier.recordScene(scene: SceneBackdrop): Modifier =
    this then RecordSceneElement(scene)

private data class RecordSceneElement(
    val scene: SceneBackdrop,
) : ModifierNodeElement<RecordSceneNode>() {
    override fun create() = RecordSceneNode(scene)

    override fun update(node: RecordSceneNode) = node.update(scene)

    override fun InspectorInfo.inspectableProperties() {
        name = "recordScene"
    }
}

private class RecordSceneNode(private var scene: SceneBackdrop) :
    Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode {

    fun update(scene: SceneBackdrop) {
        if (scene === this.scene) return
        this.scene.removed()
        this.scene = scene
        if (isAttached) scene.placed(requireLayoutCoordinates())
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val layer = scene.layer
        layer.record { this@draw.drawContent() }
        drawLayer(layer)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        scene.placed(coordinates)
    }

    override fun onDetach() {
        scene.removed()
    }
}
