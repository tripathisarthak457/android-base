package com.example.cmp.core.designsystem.component.sheet.surface

import android.graphics.Path as PlatformPath
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.util.lerp
import com.example.cmp.core.designsystem.component.sheet.SheetController
import com.example.cmp.core.designsystem.component.sheet.SheetModality
import com.example.cmp.core.designsystem.component.sheet.layout.SheetGeometry
import com.example.cmp.core.designsystem.component.sheet.layout.SurfaceRect
import com.example.cmp.core.designsystem.component.sheet.layout.surfaceBottomInBounds
import com.example.cmp.core.designsystem.theme.SheetMaterial
import com.example.cmp.core.designsystem.theme.tokens.DesignTokens

/**
 * The sheet's surface: the shadow where one is allowed, the body, the rim, and the clip its content
 * is drawn through, all one uniform round rect.
 *
 * While the sheet is attached the rect is constant in the sheet layer's coordinates, so a drag moves
 * the layer and re-records nothing here; while it floats the rect follows the expansion and this
 * node re-records one round rect, the rim and the clip per frame. Its children are their own layers,
 * so they are never re-recorded with it.
 */
internal fun Modifier.sheetSurface(controller: SheetController): Modifier = this then SheetSurfaceElement(controller)

private data class SheetSurfaceElement(val controller: SheetController) : ModifierNodeElement<SheetSurfaceNode>() {
    override fun create() = SheetSurfaceNode(controller)

    override fun update(node: SheetSurfaceNode) {
        node.controller = controller
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sheetSurface"
    }
}

private class SheetSurfaceNode(var controller: SheetController) : Modifier.Node(), DrawModifierNode {
    private val rect = SurfaceRect()
    private val platformPath = PlatformPath()
    private val clip = platformPath.asComposePath()
    private val clipRect = SurfaceRect()
    private val shadow = SheetShadowCache()

    override fun ContentDrawScope.draw() {
        val controller = controller
        val style = controller.style
        if (controller.frame == null || style == null) {
            drawContent()
            return
        }
        controller.surfaceRect(rect)
        drawShadow(controller)
        val bodyAlpha = controller.bodyAlpha()
        if (bodyAlpha > 0f) {
            val topLeft = Offset(rect.left, rect.top)
            val size = Size(rect.width, rect.height)
            val corner = CornerRadius(rect.radius)
            drawRoundRect(style.colors.container, topLeft, size, corner, alpha = bodyAlpha)
            style.containerBrush?.let { drawRoundRect(it, topLeft, size, corner, alpha = bodyAlpha) }
        }
        if (!clipRect.same(rect.left, rect.top, rect.right, rect.bottom, rect.radius)) {
            clipRect.set(rect.left, rect.top, rect.right, rect.bottom, rect.radius)
            platformPath.rewind()
            platformPath.addRoundRect(rect.left, rect.top, rect.right, rect.bottom, rect.radius, rect.radius, PlatformPath.Direction.CW)
        }
        clipPath(clip) {
            this@draw.drawContent()
            val dim = controller.coveredDim()
            if (dim.alpha > 0f) drawRect(dim)
        }
        val rim = style.colors.rim
        if (rim.alpha > 0f) {
            drawRoundRect(
                color = rim,
                topLeft = Offset(rect.left + RimInset, rect.top + RimInset),
                size = Size(rect.width - 2 * RimInset, rect.height - 2 * RimInset),
                cornerRadius = CornerRadius((rect.radius - RimInset).coerceAtLeast(0f)),
                style = RimStroke,
            )
        }
    }

    /** Only where a shadow separates the sheet from what is beside it, and never through a fade. */
    private fun ContentDrawScope.drawShadow(controller: SheetController) {
        val style = controller.style ?: return
        val shadowStyle = style.shadow ?: return
        val frame = controller.frame ?: return
        val modal = controller.modality is SheetModality.Modal
        val faintDim = style.colors.scrim.alpha < DesignTokens.Sheet.Shadow.faintScrimAlpha
        val allowed = !frame.isBottom || !modal || faintDim
        if (!allowed) return
        val alpha = if (frame.isBottom && !modal) 1f - controller.sheetState.expansionFraction else 1f
        with(shadow) { drawShadow(shadowStyle, rect.left, rect.top, rect.right, rect.bottom, rect.radius, alpha) }
    }
}

/**
 * The surface's outline in the sheet layer's coordinates. Reads no per-frame value while the
 * sheet is attached to the window.
 */
internal fun SheetController.surfaceRect(into: SurfaceRect) {
    val frame = frame ?: return
    val width = frame.sheetWidth
    when {
        frame.isCard -> into.set(0f, 0f, width, frame.cardHeight, frame.radius)
        !frame.isBottom -> into.set(0f, 0f, width, frame.restBottom - sheetState.placedTop(), frame.radius)
        floatingNow -> {
            val expansion = sheetState.expansionFraction
            val side = SheetGeometry.sideInset(frame.inset, SheetGeometry.floatAmount(true, expansion))
            val resting = SheetGeometry.restingBottom(frame.boundsHeight, frame.restBottom, frame.inset, true, frame.radius, frame.onBar)
            val bottom = SheetGeometry.floatingBottom(resting, frame.boundsHeight, frame.radius, expansion, sheetState.placedTop())
            into.set(side, 0f, width - side, bottom, frame.radius)
        }
        frame.onBar -> into.set(0f, 0f, width, surfaceBottomInBounds() - sheetState.placedTop(), frame.radius)
        else -> into.set(0f, 0f, width, SheetGeometry.attachedBottom(frame.layerHeight, frame.radius, frame.bandRoom), frame.radius)
    }
}

/**
 * How opaque the body is drawn: Solid always; Frosted and Glass translucent at partial detents and
 * opaque by halfway to Large. A Glass sheet whose live backdrop draws it leaves the body to
 * the backdrop slot.
 */
internal fun SheetController.bodyAlpha(): Float = when (material) {
    SheetMaterial.Frosted, SheetMaterial.Glass -> {
        if (material == SheetMaterial.Glass && liveBackdrop) return 0f
        val base = if (isLight) DesignTokens.Sheet.Frosted.bodyAlphaLight else DesignTokens.Sheet.Frosted.bodyAlphaDark
        lerp(base, 1f, (sheetState.expansionFraction / DesignTokens.Sheet.Glass.opaqueAt).coerceIn(0f, 1f))
    }
    else -> 1f
}

private const val RimInset = 0.5f
private val RimStroke = Stroke(width = 1f)
