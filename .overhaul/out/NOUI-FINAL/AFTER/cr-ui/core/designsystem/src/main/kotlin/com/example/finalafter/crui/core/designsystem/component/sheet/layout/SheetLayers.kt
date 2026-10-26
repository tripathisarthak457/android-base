package com.example.finalafter.crui.core.designsystem.component.sheet.layout

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import com.example.finalafter.crui.core.designsystem.component.sheet.SheetController
import com.example.finalafter.crui.core.designsystem.component.sheet.SheetPlacement
import com.example.finalafter.crui.core.designsystem.component.sheet.stack.CardStackMath
import com.example.finalafter.crui.core.designsystem.theme.tokens.DesignTokens

/**
 * The layer blocks of one sheet, built once: everything that moves the sheet without moving its
 * layout. Each reads the sheet's state while the layer updates, so a frame of a back gesture, a
 * stack recede or a keyboard changes layer properties and records nothing.
 */
internal class SheetLayers(private val controller: SheetController) {
    private val pose = PoseRecorder()

    /**
     * The sheet layer: the predictive back shrink, the recede of a covered sheet, its
     * owner's pose, the park and the cross-fade. Composed into one scale and translation about
     * the layer's top-left, since a layer has one pivot.
     */
    val sheet: GraphicsLayerScope.() -> Unit = {
        val state = controller.sheetState
        val frame = controller.frame
        val width = size.width
        val height = size.height
        var scaleX = 1f
        var scaleY = 1f
        var pivotX = 0f
        var pivotY = 0f
        val back = state.backProgress
        if (frame != null && back > 0f) {
            when {
                frame.placement == SheetPlacement.Center -> {
                    val scale = 1f - (1f - DesignTokens.Sheet.Back.formScale) * back
                    scaleX = scale
                    scaleY = scale
                    pivotX = width / 2f
                    pivotY = height / 2f
                }
                frame.horizontal -> {
                    val ownEdge = controller.backFromLeft == frame.fromLeft
                    val change = if (ownEdge) -DesignTokens.Sheet.Side.backShrink.toPx() else DesignTokens.Sheet.Side.backGrow.toPx()
                    scaleX = 1f + change * back / width
                    scaleY = 1f - DesignTokens.Sheet.Side.backY.toPx() * back / height
                    pivotX = if (frame.fromLeft) 0f else width
                    pivotY = height / 2f
                }
                else -> {
                    val visible = controller.visibleBottomInLayer().coerceIn(1f, height)
                    scaleX = 1f - minOf(DesignTokens.Sheet.Back.shrinkX.toPx(), width) * back / width
                    scaleY = 1f - minOf(DesignTokens.Sheet.Back.shrinkY.toPx(), visible) * back / visible
                    pivotX = width / 2f
                    pivotY = visible
                }
            }
        }
        var translationX = (1f - scaleX) * pivotX
        var translationY = (1f - scaleY) * pivotY

        val cover = controller.coverExpansion()
        if (cover > 0f && !controller.reduceMotion) {
            val scale = CardStackMath.coveredScale(cover)
            val lift = CardStackMath.coveredLift(cover, frame?.boundsHeight ?: 0f)
            translationX = scale * translationX + (1f - scale) * width / 2f
            translationY = scale * translationY - lift
            scaleX *= scale
            scaleY *= scale
        }

        var alpha = state.fade
        if (state.parkedInstantly || controller.hiddenBehindStack()) alpha = 0f
        pose.reset(size, density, fontScale)
        controller.applyEntryPose(pose)
        if (!pose.isIdentity) {
            val originX = pose.transformOrigin.pivotFractionX * width
            val originY = pose.transformOrigin.pivotFractionY * height
            translationX = pose.scaleX * translationX + (1f - pose.scaleX) * originX + pose.translationX
            translationY = pose.scaleY * translationY + (1f - pose.scaleY) * originY + pose.translationY
            scaleX *= pose.scaleX
            scaleY *= pose.scaleY
            alpha *= pose.alpha
        }

        this.transformOrigin = TopLeft
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.translationX = translationX
        this.translationY = translationY
        this.alpha = alpha
        // Alpha modulates each draw instead of compositing the sheet offscreen: a fade costs nothing extra.
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }

    /** The content counter-scales during the back shrink about its top, so its text keeps its shape. */
    val content: GraphicsLayerScope.() -> Unit = {
        val back = controller.sheetState.backProgress
        val frame = controller.frame
        if (back > 0f && frame != null && frame.isBottom) {
            val width = frame.sheetWidth
            val visible = controller.visibleBottomInLayer().coerceAtLeast(1f)
            val sx = 1f - minOf(DesignTokens.Sheet.Back.shrinkX.toPx(), width) * back / width
            val sy = 1f - minOf(DesignTokens.Sheet.Back.shrinkY.toPx(), visible) * back / visible
            transformOrigin = TopCentre
            scaleY = sx / sy
        } else {
            scaleY = 1f
        }
    }

    /** The footer rides the sheet's visible bottom, or the keyboard, and never above the header. */
    val footer: GraphicsLayerScope.() -> Unit = {
        translationY = controller.footerTopInLayer(size.height)
    }
}

/**
 * A layer scope that only records, so an owner's pose can be read and composed with the sheet's own
 * transform rather than overwriting it.
 */
private class PoseRecorder : GraphicsLayerScope {
    override var scaleX = 1f
    override var scaleY = 1f
    override var alpha = 1f
    override var translationX = 0f
    override var translationY = 0f
    // allow(sheet-shadow): GraphicsLayerScope declares it; the pose recorder draws no shadow.
    override var shadowElevation = 0f
    override var rotationX = 0f
    override var rotationY = 0f
    override var rotationZ = 0f
    override var cameraDistance = DefaultCameraDistance
    override var transformOrigin = TransformOrigin.Center
    override var shape: Shape = RectangleShape
    override var clip = false
    override var size: Size = Size.Zero
    override var density = 1f
    override var fontScale = 1f

    val isIdentity: Boolean
        get() = scaleX == 1f && scaleY == 1f && translationX == 0f && translationY == 0f && alpha == 1f

    fun reset(size: Size, density: Float, fontScale: Float) {
        scaleX = 1f
        scaleY = 1f
        alpha = 1f
        translationX = 0f
        translationY = 0f
        transformOrigin = TransformOrigin.Center
        this.size = size
        this.density = density
        this.fontScale = fontScale
    }
}

private val TopLeft = TransformOrigin(0f, 0f)
private val TopCentre = TransformOrigin(0.5f, 0f)
private const val DefaultCameraDistance = 8f
