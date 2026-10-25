package com.base.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.base.app.core.designsystem.component.feedback.AppSkeleton
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.foundation.draw.LocalRedacted
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A remote image, on the skeleton colour while it loads and with a glyph when it fails. Inside an
 * active `AppRedacted` it is a square skeleton without loading anything: the caller clips the image
 * to its shape, and the skeleton with it.
 *
 * Both stand-ins are painters, so a load changes only what is drawn: nothing recomposes when the
 * image arrives, which keeps a scrolling list of these at one composition per row.
 */
@Composable
fun AppNetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    errorIcon: ImageVector = AppIcons.ImageIcon,
) {
    if (LocalRedacted.current) {
        AppSkeleton(modifier = modifier, shape = RectangleShape)
        return
    }
    if (url.isNullOrBlank()) {
        ImageFallback(errorIcon, modifier)
        return
    }

    val colors = AppTheme.colors
    val loading = remember(colors.skeleton) { ColorPainter(colors.skeleton) }
    val glyph = rememberVectorPainter(errorIcon)
    val failed = remember(glyph, colors.fill, colors.contentTertiary) {
        GlyphPainter(glyph, background = colors.fill, tint = colors.contentTertiary)
    }

    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = loading,
        error = failed,
        contentScale = contentScale,
    )
}

@Composable
private fun ImageFallback(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(AppTheme.colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon, contentDescription = null, tint = AppTheme.colors.contentTertiary)
    }
}

/**
 * Fills its bounds with [background] and draws [glyph] centred at the glyph's own size. Its size is
 * unspecified, so the image's content scale stretches the background and never the glyph.
 */
private class GlyphPainter(
    private val glyph: VectorPainter,
    private val background: Color,
    tint: Color,
) : Painter() {

    private val filter = ColorFilter.tint(tint)

    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRect(background)
        val glyphSize = glyph.intrinsicSize
        translate(left = (size.width - glyphSize.width) / 2, top = (size.height - glyphSize.height) / 2) {
            with(glyph) { draw(glyphSize, colorFilter = filter) }
        }
    }
}
