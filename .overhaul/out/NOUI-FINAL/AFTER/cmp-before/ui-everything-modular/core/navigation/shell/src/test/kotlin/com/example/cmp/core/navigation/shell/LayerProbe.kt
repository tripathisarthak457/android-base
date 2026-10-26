package com.example.cmp.core.navigation.shell

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.toSize
import com.example.cmp.core.common.navigation.AppNavKey

/** The layout node of [key]'s screen, tagged with its key; from the unmerged tree, so a covered screen is found too. */
internal fun ComposeTestRule.screenNode(key: AppNavKey): LayoutInfo =
    onNodeWithTag(key.toString(), useUnmergedTree = true).fetchSemanticsNode().layoutInfo

/**
 * Which of the layers around [screen] are textures of their own (`Offscreen`), counted from the
 * screen outwards: 0 is the innermost layer around it. Every `graphicsLayer` from the screen's own
 * node up to the window is read as its block sets it now, which is what the next frame's layer
 * gets.
 *
 * Only the innermost layer may be the back card's texture: its display list holds the screen and
 * nothing that changes while the card is up, so it is rendered once per gesture. A texture one
 * layer further out would also hold what the scene draws over the card, and be rendered again
 * whenever that changes.
 */
internal fun ComposeTestRule.texturesAround(screen: LayoutInfo): List<Int> = runOnUiThread {
    val strategies = ArrayList<CompositingStrategy>()
    var node: LayoutInfo? = screen
    while (node != null) {
        val size = node.coordinates.size.toSize()
        for (info in node.getModifierInfo().asReversed()) {
            val element = info.modifier as? InspectableValue ?: continue
            if (element.nameFallback != GRAPHICS_LAYER) continue
            val layer = LayerSettings(size, node.density.density, node.density.fontScale)
            for (property in element.inspectableElements) {
                when (property.name) {
                    "block" -> {
                        @Suppress("UNCHECKED_CAST")
                        (property.value as GraphicsLayerScope.() -> Unit).invoke(layer)
                    }
                    "compositingStrategy" -> layer.compositingStrategy = property.value as CompositingStrategy
                }
            }
            strategies += layer.compositingStrategy
        }
        node = node.parentInfo
    }
    strategies.indices.filter { strategies[it] == CompositingStrategy.Offscreen }
}

private const val GRAPHICS_LAYER = "graphicsLayer"

/** What one `graphicsLayer` block sets, run against a scope of its own instead of the layer. */
private class LayerSettings(
    override val size: Size,
    override val density: Float,
    override val fontScale: Float,
) : GraphicsLayerScope {
    override var scaleX = 1f
    override var scaleY = 1f
    override var alpha = 1f
    override var translationX = 0f
    override var translationY = 0f
    override var shadowElevation = 0f
    override var rotationX = 0f
    override var rotationY = 0f
    override var rotationZ = 0f
    override var cameraDistance = DefaultCameraDistance
    override var transformOrigin = TransformOrigin.Center
    override var shape: Shape = RectangleShape
    override var clip = false
    override var compositingStrategy = CompositingStrategy.Auto
}
