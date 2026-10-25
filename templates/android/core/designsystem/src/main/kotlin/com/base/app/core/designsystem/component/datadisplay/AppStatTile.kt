package com.base.app.core.designsystem.component.datadisplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.content.AppSurface
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.StatusColors

/** A single headline number with its label, and optionally which way it moved. */
@Composable
fun AppStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    deltaIsPositive: Boolean = true,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val deltaColors: StatusColors = if (deltaIsPositive) colors.success else colors.danger

    AppSurface(
        modifier = modifier.then(
            if (onClick != null) Modifier.pressable(onClick = onClick, feedback = PressFeedback.Scale) else Modifier,
        ),
        shape = AppTheme.shapes.md,
        color = colors.surface,
        border = BorderStroke(AppTheme.style.borderWidth, colors.separator),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.let {
                    AppIcon(it, contentDescription = null, tint = colors.contentTertiary, size = 16.dp)
                }
                AppText(
                    text = label,
                    style = AppTheme.typography.caption2,
                    color = colors.contentTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            AppText(
                text = value,
                style = AppTheme.typography.title2,
                color = colors.contentPrimary,
                maxLines = 1,
            )

            delta?.let {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(
                        imageVector = if (deltaIsPositive) AppIcons.ArrowUp else AppIcons.ArrowDown,
                        contentDescription = stringResource(
                            if (deltaIsPositive) R.string.designsystem_trend_up else R.string.designsystem_trend_down,
                        ),
                        tint = deltaColors.content,
                        size = 14.dp,
                    )
                    AppText(
                        text = it,
                        style = AppTheme.typography.caption2,
                        color = deltaColors.content,
                    )
                }
            }
        }
    }
}

@AppComponentPreviews
@Composable
private fun AppStatTilePreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            AppStatTile(label = "Revenue", value = "12.4k", delta = "+8%", icon = AppIcons.CreditCard, modifier = Modifier.weight(1f))
            AppStatTile(label = "Refunds", value = "312", delta = "-2%", deltaIsPositive = false, modifier = Modifier.weight(1f))
        }
        AppStatTile(label = "Active subscribers across every plan and region", value = "48,210", onClick = {})
    }
}
