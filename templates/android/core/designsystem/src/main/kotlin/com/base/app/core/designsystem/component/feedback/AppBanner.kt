package com.base.app.core.designsystem.component.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppLinkButton
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A slim bar for a condition that does not stop the screen working: offline, a stale cache, an
 * update available.
 */
@Composable
public fun AppBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: AppTone = AppTone.Warning,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val status = tone.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(status.subtle)
            .padding(horizontal = AppTheme.spacing.gutter, vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon ?: tone.icon, contentDescription = null, tint = status.content, size = AppBannerDefaults.IconSize)
        AppText(text = text, modifier = Modifier.weight(1f), style = AppTheme.typography.footnote, color = AppTheme.colors.contentPrimary)
        if (actionLabel != null && onAction != null) {
            AppLinkButton(text = actionLabel, onClick = onAction, style = AppTheme.typography.footnote)
        }
    }
}

/** The banner's metrics. */
public object AppBannerDefaults {
    public val IconSize: Dp = 16.dp
}

@AppComponentPreviews
@Composable
private fun AppBannerPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppBanner(text = "You are offline. Changes will sync when you reconnect.", icon = AppIcons.AlertTriangle)
        AppBanner(text = "Payment received.", tone = AppTone.Success, icon = AppIcons.CheckCircle)
        AppBanner(text = "The card was declined.", tone = AppTone.Error, actionLabel = "Try again", onAction = {})
        AppBanner(text = "A new version is ready to install.", tone = AppTone.Info, actionLabel = "Restart", onAction = {})
        AppBanner(text = "Maintenance tonight from 01:00 to 03:00.", tone = AppTone.Neutral)
    }
}
