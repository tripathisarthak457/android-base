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
import com.base.app.core.designsystem.foundation.offsetShadow
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.CardTreatment

/**
 * The default container for a block of related content.
 *
 * How it is drawn follows the design style — see [CardTreatment]. Outlined is the Utility default. A list of eight elevated cards is eight drop shadows
 * competing on one screen, which reads as noise; a hairline outline separates them just as well
 * and costs no overdraw. Elevation is reserved for something that genuinely floats above the
 * content — a sheet, a menu, a sticky action bar.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.md,
    color: Color = AppTheme.colors.surface,
    border: BorderStroke? = AppCardDefaults.border(),
    elevation: Dp = AppCardDefaults.elevation(),
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSurface(
        modifier = modifier.cardShadow(shape),
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
    border: BorderStroke? = AppCardDefaults.border(),
    elevation: Dp = AppCardDefaults.elevation(),
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    AppClickableSurface(
        onClick = onClick,
        modifier = modifier.cardShadow(shape),
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

/** What a card looks like when the caller does not say, per [CardTreatment]. */
object AppCardDefaults {

    @Composable
    fun border(): BorderStroke? = when (AppTheme.style.card) {
        CardTreatment.Outlined -> BorderStroke(AppTheme.sizes.borderWidth, AppTheme.colors.border)
        CardTreatment.Raised -> null
        CardTreatment.Offset -> BorderStroke(AppTheme.sizes.borderWidth, AppTheme.colors.contentPrimary)
    }

    @Composable
    fun elevation(): Dp =
        if (AppTheme.style.card == CardTreatment.Raised) AppTheme.elevation.card else AppTheme.elevation.none
}

@Composable
private fun Modifier.cardShadow(shape: Shape): Modifier =
    if (AppTheme.style.card == CardTreatment.Offset) {
        offsetShadow(shape, AppTheme.style.offsetShadow, AppTheme.colors.contentPrimary)
    } else {
        this
    }
