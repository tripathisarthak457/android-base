package io.example.fitcnetdb.core.designsystem.component.sheet.chrome

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.example.fitcnetdb.core.designsystem.theme.AppTheme
import io.example.fitcnetdb.core.designsystem.theme.tokens.DesignTokens

/**
 * Draws the grabber pill in the enclosing sheet's grabber colour, as a contrast pill when the
 * content runs under it; outside a sheet, in contentTertiary, in place. Its tap, keys, hover, focus,
 * 48 dp target and accessibility actions belong to the sheet's grabber node, so a custom grabber
 * lambda that draws something else keeps all of them.
 */
@Composable
fun AppSheetGrabber(modifier: Modifier = Modifier) {
    val look = LocalSheetGrabber.current
    val outside = AppTheme.colors.contentTertiary.copy(alpha = DesignTokens.Sheet.Grabber.alpha)
    Spacer(
        modifier.size(PillSize).drawWithCache {
            val radius = CornerRadius(size.height / 2f)
            val edge = EdgeWidth.toPx()
            val ring = Stroke(edge)
            val ringOffset = Offset(edge / 2f, edge / 2f)
            val ringSize = Size(size.width - edge, size.height - edge)
            onDrawBehind {
                if (look?.contrast == true) {
                    drawRoundRect(ContrastFill, cornerRadius = radius)
                    drawRoundRect(ContrastEdge, topLeft = ringOffset, size = ringSize, cornerRadius = radius, style = ring)
                } else {
                    val color = look?.color ?: outside
                    drawRoundRect(if (look?.hovered == true) color.copy(alpha = DesignTokens.Sheet.Grabber.hoverAlpha) else color, cornerRadius = radius)
                }
            }
        },
    )
}

/** How the enclosing sheet's grabber node asks its pill to look; null outside a sheet. */
@Stable
internal class SheetGrabberLook(color: Color, contrast: Boolean) {
    var color by mutableStateOf(color)
    var contrast by mutableStateOf(contrast)
    var hovered by mutableStateOf(false)
}

internal val LocalSheetGrabber = staticCompositionLocalOf<SheetGrabberLook?> { null }

private val PillSize = DpSize(DesignTokens.Sheet.Grabber.width, DesignTokens.Sheet.Grabber.height)
private val ContrastFill = Color.White.copy(alpha = DesignTokens.Sheet.Grabber.contrastAlpha)
private val ContrastEdge = Color.Black.copy(alpha = 0.2f)
private val EdgeWidth = 1.dp
