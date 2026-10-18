package com.example.hud.standard.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.example.hud.standard.core.designsystem.component.feedback.AppAvatar
import com.example.hud.standard.core.designsystem.component.feedback.AppSkeleton
import com.example.hud.standard.core.designsystem.component.text.AppIcon
import com.example.hud.standard.core.designsystem.icon.AppIcons
import com.example.hud.standard.core.designsystem.theme.AppTheme

/** A remote image, with a skeleton while it loads and a glyph when it fails. */
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

    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = { AppSkeleton(modifier = Modifier.fillMaxSize()) },
        error = { ImageFallback(Modifier.fillMaxSize(), errorIcon) },
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
