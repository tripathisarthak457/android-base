package com.base.app.core.designsystem.component.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.StatusColors

/**
 * The visual severity of a message, banner or pill.
 *
 * This is the design system's own vocabulary, deliberately separate from `:core:common`'s
 * `MessageKind` — which is an application concept. `:core:ui` maps one to the other in five
 * lines, and in exchange `:core:designsystem` depends on nothing but Compose, which is what
 * keeps the catalog app (and the iteration loop for anyone working on a component) down to two
 * modules to rebuild.
 */
enum class AppTone {
    Success,
    Error,
    Warning,
    Info,
    Neutral,
    ;

    val colors: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = when (this) {
            Success -> AppTheme.colors.success
            Error -> AppTheme.colors.danger
            Warning -> AppTheme.colors.warning
            Info -> AppTheme.colors.info
            Neutral -> AppTheme.colors.neutral
        }

    val icon: ImageVector
        get() = when (this) {
            Success -> AppIcons.CheckCircle
            Error -> AppIcons.XCircle
            Warning -> AppIcons.AlertTriangle
            Info -> AppIcons.Info
            Neutral -> AppIcons.Info
        }
}
