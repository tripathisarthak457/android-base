package com.example.finalafter.crui2.core.designsystem.component.container

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui2.core.designsystem.component.text.AppText
import com.example.finalafter.crui2.core.designsystem.foundation.content.AppSurface
import com.example.finalafter.crui2.core.designsystem.foundation.draw.disabledAlpha
import com.example.finalafter.crui2.core.designsystem.foundation.draw.offsetShadow
import com.example.finalafter.crui2.core.designsystem.foundation.interaction.PressFeedback
import com.example.finalafter.crui2.core.designsystem.foundation.interaction.pressable
import com.example.finalafter.crui2.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.designsystem.theme.CardTreatment

/** The default container for a block of related content. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.md,
    color: Color = AppCardDefaults.color(),
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

/**
 * A card that opens something. It shrinks slightly under the finger, by less than a button does
 * since it is larger, and never lays a film over what it shows.
 */
@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    shape: Shape = AppTheme.shapes.md,
    color: Color = AppCardDefaults.color(),
    border: BorderStroke? = AppCardDefaults.border(),
    elevation: Dp = AppCardDefaults.elevation(),
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSurface(
        modifier = modifier
            .pressable(onClick = onClick, feedback = PressFeedback.Scale, enabled = enabled, role = Role.Button, onClickLabel = onClickLabel)
            .disabledAlpha(enabled)
            .cardShadow(shape),
        shape = shape,
        color = color,
        border = border,
        elevation = elevation,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** What a card looks like when the caller does not say, per [CardTreatment]. */
object AppCardDefaults {

    /** Raised cards sit on the elevated surface, so in dark theme they still read as lifted. */
    @Composable
    fun color(): Color =
        if (AppTheme.style.card == CardTreatment.Raised) AppTheme.colors.surfaceElevated else AppTheme.colors.surface

    @Composable
    fun border(): BorderStroke? = when (AppTheme.style.card) {
        CardTreatment.Outlined -> BorderStroke(AppTheme.style.borderWidth, AppTheme.colors.separator)
        CardTreatment.Raised -> null
        CardTreatment.Offset -> BorderStroke(AppTheme.style.borderWidth, AppTheme.colors.contentPrimary)
    }

    @Composable
    fun elevation(): Dp = if (AppTheme.style.card == CardTreatment.Raised) AppTheme.elevation.card else 0.dp
}

@Composable
private fun Modifier.cardShadow(shape: Shape): Modifier =
    if (AppTheme.style.card == CardTreatment.Offset) {
        offsetShadow(shape, AppTheme.style.offsetShadow, AppTheme.colors.contentPrimary)
    } else {
        this
    }

@AppComponentPreviews
@Composable
private fun AppCardPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppCard {
            AppText(text = "Weekly summary", style = AppTheme.typography.headline)
            AppText(text = "Twelve orders, three returns.", color = AppTheme.colors.contentSecondary)
        }
        AppCard(onClick = {}) {
            AppText(text = "Tap to open the order with the longest title anyone has ever given an order")
        }
        AppCard(onClick = {}, enabled = false) {
            AppText(text = "Archived")
        }
    }
}
