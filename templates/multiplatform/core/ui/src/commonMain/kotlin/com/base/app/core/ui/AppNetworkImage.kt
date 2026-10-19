package com.base.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A remote image over a quiet placeholder fill, with [errorIcon] in its place when it cannot be
 * loaded.
 *
 * The image is a painter drawn by one node rather than content composed into loading and error
 * slots, so a list of these never subcomposes a row.
 */
@Composable
fun AppNetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    errorIcon: ImageVector = AppIcons.ImageIcon,
) {
    var failed by remember(url) { mutableStateOf(url.isNullOrBlank()) }
    Box(modifier = modifier.background(AppTheme.colors.skeleton), contentAlignment = Alignment.Center) {
        if (failed) {
            AppIcon(errorIcon, contentDescription = contentDescription, tint = AppTheme.colors.contentTertiary)
        } else {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                onError = { failed = true },
            )
        }
    }
}
