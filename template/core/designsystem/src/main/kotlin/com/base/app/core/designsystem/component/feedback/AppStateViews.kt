package com.base.app.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The "there is nothing here" state.
 *
 * Every empty list gets one of these rather than blank space, and every one of them takes a
 * [title] and an [action]. An empty screen with no explanation is indistinguishable from a screen
 * that failed to load, and the user's next move is to close the app.
 */
@Composable
fun AppEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateLayout(
        modifier = modifier,
        icon = icon,
        iconTint = AppTheme.colors.contentTertiary,
        iconBackground = AppTheme.colors.surfaceVariant,
        title = title,
        message = message,
        actionLabel = actionLabel,
        onAction = onAction,
        actionVariant = ButtonVariant.Secondary,
    )
}

/**
 * The "it did not load" state.
 *
 * [isOffline] chooses both the glyph and the copy, because those two failures need different
 * words and a different promise: an offline retry is "try again when you are back", a server
 * error is "try again now". Collapsing them into one message means one of the two is always
 * wrong.
 */
@Composable
fun AppErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    isOffline: Boolean = false,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Try again",
) {
    StateLayout(
        modifier = modifier,
        icon = if (isOffline) AppIcons.WifiOff else AppIcons.AlertTriangle,
        iconTint = AppTheme.colors.danger.content,
        iconBackground = AppTheme.colors.danger.subtle,
        title = title ?: if (isOffline) "You are offline" else "Something went wrong",
        message = message,
        actionLabel = if (onRetry != null) retryLabel else null,
        onAction = onRetry,
        actionVariant = ButtonVariant.Primary,
    )
}

@Composable
private fun StateLayout(
    modifier: Modifier,
    icon: ImageVector?,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    message: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    actionVariant: ButtonVariant,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md, Alignment.CenterVertically),
    ) {
        icon?.let {
            AppSurface(
                modifier = Modifier.size(64.dp),
                shape = AppTheme.shapes.pill,
                color = iconBackground,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppIcon(it, contentDescription = null, tint = iconTint, size = 28.dp)
                }
            }
        }

        AppText(
            text = title,
            style = AppTheme.typography.headingMedium,
            color = AppTheme.colors.contentPrimary,
            textAlign = TextAlign.Center,
        )

        message?.let {
            AppText(
                text = it,
                modifier = Modifier.widthIn(max = 320.dp),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.contentTertiary,
                textAlign = TextAlign.Center,
            )
        }

        if (actionLabel != null && onAction != null) {
            AppButton(
                text = actionLabel,
                onClick = onAction,
                variant = actionVariant,
                modifier = Modifier.padding(top = AppTheme.spacing.sm),
            )
        }
    }
}

/**
 * A slim bar for a whole-screen condition that does not stop the screen working — offline, a
 * stale cache, an update available.
 *
 * Distinct from a snackbar because it persists: it stays until the condition clears, so it must
 * not steal the tap target of anything beneath it or animate on a timer.
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
    val statusColors = tone.colors

    AppSurface(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.none,
        color = statusColors.subtle,
        contentColor = statusColors.content,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppTheme.spacing.gutter,
                vertical = AppTheme.spacing.sm,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(icon ?: tone.icon, contentDescription = null, size = 16.dp)
            AppText(
                text = text,
                modifier = Modifier.weight(1f),
                style = AppTheme.typography.bodySmall,
            )
            if (actionLabel != null && onAction != null) {
                AppButton(
                    text = actionLabel,
                    onClick = onAction,
                    variant = ButtonVariant.Tertiary,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}
