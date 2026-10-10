package com.base.app.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.resources.Res
import com.base.app.core.designsystem.resources.designsystem_error_title
import com.base.app.core.designsystem.resources.designsystem_offline_title
import com.base.app.core.designsystem.resources.designsystem_try_again
import com.base.app.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

enum class StateKind { Empty, Error, Offline }

/**
 * What a screen shows instead of its content: nothing here yet, it did not load, or there is no
 * connection. An error or offline state has a title and a "Try again" of its own unless told
 * otherwise. The state scrolls when the window is too short for it, so its action is never cut
 * off.
 */
@Composable
fun AppStateView(
    modifier: Modifier = Modifier,
    kind: StateKind = StateKind.Empty,
    title: String? = null,
    message: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val resolvedTitle = title ?: when (kind) {
        StateKind.Empty -> null
        StateKind.Error -> stringResource(Res.string.designsystem_error_title)
        StateKind.Offline -> stringResource(Res.string.designsystem_offline_title)
    }
    val resolvedIcon = icon ?: when (kind) {
        StateKind.Empty -> AppIcons.ListView
        StateKind.Error -> AppIcons.AlertTriangle
        StateKind.Offline -> AppIcons.AlertCircle
    }
    val resolvedAction = actionLabel ?: if (kind != StateKind.Empty && onAction != null) {
        stringResource(Res.string.designsystem_try_again)
    } else {
        null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md, Alignment.CenterVertically),
    ) {
        AppIcon(
            resolvedIcon,
            contentDescription = null,
            tint = AppTheme.colors.contentTertiary,
            size = AppTheme.sizes.iconLarge * 2,
        )
        if (resolvedTitle != null) {
            AppText(
                text = resolvedTitle,
                style = AppTheme.typography.title3,
                color = AppTheme.colors.contentPrimary,
                textAlign = TextAlign.Center,
            )
        }
        if (message != null) {
            AppText(
                text = message,
                modifier = Modifier.widthIn(max = MessageMaxWidth),
                style = AppTheme.typography.body,
                color = AppTheme.colors.contentSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (resolvedAction != null && onAction != null) {
            AppButton(
                text = resolvedAction,
                onClick = onAction,
                modifier = Modifier.padding(top = AppTheme.spacing.sm),
                variant = ButtonVariant.Tinted,
            )
        }
    }
}

private val MessageMaxWidth = 360.dp
