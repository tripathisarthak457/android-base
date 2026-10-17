package com.example.modulesmodular.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.modulesmodular.core.designsystem.component.feedback.AppAvatar
import com.example.modulesmodular.core.designsystem.component.text.AppIcon
import com.example.modulesmodular.core.designsystem.icon.AppIcons
import com.example.modulesmodular.core.designsystem.theme.AppTheme

/**
 * A remote image, on the skeleton colour while it loads and with a glyph when it fails.
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
    if (url.isNullOrBlank()) {
        ImageFallback(modifier, errorIcon)
        return
    }

    val colors = AppTheme.colors
    val loading = remember(colors.skeleton) { ColorPainter(colors.skeleton) }
    val glyph = rememberVectorPainter(errorIcon)
    val failed = remember(glyph, colors.surfaceVariant, colors.contentTertiary) {
        GlyphPainter(glyph, background = colors.surfaceVariant, tint = colors.contentTertiary)
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

/** A person's picture, falling back to their initials. */
@Composable
fun AppUserAvatar(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    if (imageUrl.isNullOrBlank()) {
        AppAvatar(name = name, modifier = modifier, size = size)
        return
    }

    AppAvatar(name = name, modifier = modifier, size = size) {
        AppNetworkImage(
            url = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ImageFallback(modifier: Modifier, icon: ImageVector) {
    Box(
        modifier = modifier.background(AppTheme.colors.surfaceVariant),
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
