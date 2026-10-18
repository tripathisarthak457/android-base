package com.example.wp04.everything.core.designsystem.foundation.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.wp04.everything.core.designsystem.theme.AppTheme

/**
 * The container other components are built on: a shape, a fill, an optional outline, an optional
 * lift, and a content colour for everything inside it.
 *
 * The content is clipped to the shape only when [clipContent] says so: a clip costs a layer, and
 * most content cannot overflow its container anyway. A lifted surface casts a shadow in light theme;
 * in dark theme a shadow cannot be seen, so pass `surfaceElevated` as the colour instead.
 */
@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.md,
    color: Color = AppTheme.colors.surface,
    contentColor: Color = AppTheme.colors.contentPrimary,
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    clipContent: Boolean = false,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .then(
                if (elevation > 0.dp && colors.isLight) {
                    Modifier.shadow(elevation, shape, clip = false, ambientColor = colors.shadow, spotColor = colors.shadow)
                } else {
                    Modifier
                },
            )
            .then(if (clipContent) Modifier.clip(shape) else Modifier)
            .background(color, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier),
        contentAlignment = contentAlignment,
    ) {
        ProvideContentColor(contentColor) { content() }
    }
}
