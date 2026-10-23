package com.example.standard.core.designsystem.component.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.standard.core.designsystem.component.button.AppLinkButton
import com.example.standard.core.designsystem.component.text.AppIcon
import com.example.standard.core.designsystem.component.text.AppText
import com.example.standard.core.designsystem.theme.AppTheme

/**
 * A slim bar for a condition that does not stop the screen working: offline, a stale cache, an
 * update available.
 */
@Composable
fun AppBanner(
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
        AppIcon(icon ?: tone.icon, contentDescription = null, tint = status.content, size = 16.dp)
        AppText(text = text, modifier = Modifier.weight(1f), style = AppTheme.typography.footnote, color = AppTheme.colors.contentPrimary)
        if (actionLabel != null && onAction != null) {
            AppLinkButton(text = actionLabel, onClick = onAction, style = AppTheme.typography.footnote)
        }
    }
}
