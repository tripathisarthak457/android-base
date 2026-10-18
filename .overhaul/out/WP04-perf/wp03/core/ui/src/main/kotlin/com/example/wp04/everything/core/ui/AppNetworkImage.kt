package com.example.wp04.everything.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.example.wp04.everything.core.designsystem.component.feedback.AppSkeleton
import com.example.wp04.everything.core.designsystem.component.text.AppIcon
import com.example.wp04.everything.core.designsystem.foundation.draw.LocalRedacted
import com.example.wp04.everything.core.designsystem.icon.AppIcons
import com.example.wp04.everything.core.designsystem.theme.AppTheme

/**
 * A remote image, with a skeleton while it loads and a glyph when it fails. The skeleton is square:
 * the caller clips the image to its shape, and the skeleton with it. Inside an active
 * `AppRedacted` it is a skeleton without loading anything.
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
        ImageFallback(modifier, errorIcon)
        return
    }

    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = { AppSkeleton(modifier = Modifier.fillMaxSize(), shape = RectangleShape) },
        error = { ImageFallback(Modifier.fillMaxSize(), errorIcon) },
    )
}

@Composable
private fun ImageFallback(modifier: Modifier, icon: ImageVector) {
    Box(
        modifier = modifier.background(AppTheme.colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon, contentDescription = null, tint = AppTheme.colors.contentTertiary)
    }
}
