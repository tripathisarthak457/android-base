package com.base.app.core.designsystem.component.container

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.foundation.AppClickableSurface
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The default container for a block of related content.
 *
 * Outlined rather than elevated by default. A list of eight elevated cards is eight drop shadows
 * competing on one screen, which reads as noise; a hairline outline separates them just as well
 * and costs no overdraw. Elevation is reserved for something that genuinely floats above the
 * content — a sheet, a menu, a sticky action bar.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.md,
    color: Color = AppTheme.colors.surface,
    border: BorderStroke? = BorderStroke(AppTheme.sizes.borderWidth, AppTheme.colors.border),
    elevation: Dp = AppTheme.elevation.none,
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSurface(
        modifier = modifier,
        shape = shape,
        color = color,
        border = border,
        elevation = elevation,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = AppTheme.shapes.md,
    color: Color = AppTheme.colors.surface,
    border: BorderStroke? = BorderStroke(AppTheme.sizes.borderWidth, AppTheme.colors.border),
    elevation: Dp = AppTheme.elevation.none,
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    AppClickableSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = color,
        border = border,
        elevation = elevation,
        // A card is large enough that scaling the whole thing on press reads as the layout
        // jumping rather than as a button depressing. The overlay alone is the right feedback.
        scaleOnPress = false,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
