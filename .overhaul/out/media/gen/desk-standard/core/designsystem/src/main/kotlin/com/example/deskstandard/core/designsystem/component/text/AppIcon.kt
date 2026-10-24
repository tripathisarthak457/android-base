package com.example.deskstandard.core.designsystem.component.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.example.deskstandard.core.designsystem.foundation.content.LocalContentColor
import com.example.deskstandard.core.designsystem.foundation.draw.LocalRedacted
import com.example.deskstandard.core.designsystem.foundation.draw.skeleton
import com.example.deskstandard.core.designsystem.theme.AppTheme

/** An icon, tinted to the surrounding content colour unless told otherwise. */
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = AppTheme.sizes.icon,
) {
    AppIcon(imageVector, contentDescription, { tint }, modifier, size)
}

/**
 * An icon whose tint animates: [tint] is read while drawing, so a colour change redraws the glyph
 * and recomposes nothing.
 */
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: ColorProducer,
    modifier: Modifier = Modifier,
    size: Dp = AppTheme.sizes.icon,
) {
    val painter = rememberVectorPainter(imageVector)
    val glyph = if (LocalRedacted.current) {
        Modifier.skeleton(AppTheme.shapes.xs)
    } else {
        TintedPainterElement(painter, tint)
    }
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        role = Role.Image
                    }
                } else {
                    Modifier
                },
            )
            .then(glyph),
    )
}

private data class TintedPainterElement(
    val painter: VectorPainter,
    val tint: ColorProducer,
) : ModifierNodeElement<TintedPainterNode>() {
    override fun create() = TintedPainterNode(painter, tint)

    override fun update(node: TintedPainterNode) {
        node.painter = painter
        node.tint = tint
        node.invalidate()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "tintedPainter"
    }
}

private class TintedPainterNode(
    var painter: VectorPainter,
    var tint: ColorProducer,
) : Modifier.Node(), DrawModifierNode {
    private var lastColor = Color.Unspecified
    private var filter: ColorFilter? = null

    fun invalidate() = invalidateDraw()

    override fun ContentDrawScope.draw() {
        val color = tint()
        if (color != lastColor) {
            lastColor = color
            filter = ColorFilter.tint(color)
        }
        with(painter) { draw(size, colorFilter = filter) }
    }
}
