package com.base.app.core.designsystem.component.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/** The visual severity of a message. */
enum class AppTone { Success, Error, Warning, Info }

/**
 * Where an [AppSnackbar] appears: at the bottom of the box it is placed in, above the navigation
 * bar, sliding up into view and back down out of it.
 */
@Composable
fun BoxScope.AppSnackbarHost(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val springs = AppTheme.springs
    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(AppTheme.spacing.gutter),
        enter = slideInVertically(springs.sheet.offset) { it } + fadeIn(springs.sheet.unit),
        exit = slideOutVertically(springs.sheet.offset) { it } + fadeOut(springs.sheet.unit),
    ) {
        content()
    }
}

/** A short message with an optional action, announced politely when it appears. */
@Composable
fun AppSnackbar(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    tone: AppTone = AppTone.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .widthIn(max = MaxWidth)
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .background(colors.contentPrimary, AppTheme.shapes.lg)
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(tone.icon, contentDescription = null, tint = tone.tint(colors.danger, colors.background))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.md),
            ) {
                if (title != null) {
                    AppText(text = title, style = AppTheme.typography.headline, color = colors.background)
                }
                AppText(text = text, style = AppTheme.typography.subheadline, color = colors.background)
            }
            if (actionLabel != null && onAction != null) {
                AppButton(
                    text = actionLabel,
                    onClick = onAction,
                    variant = ButtonVariant.Plain,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

private val AppTone.icon: ImageVector
    get() = when (this) {
        AppTone.Success -> AppIcons.CheckCircle
        AppTone.Error -> AppIcons.XCircle
        AppTone.Warning -> AppIcons.AlertTriangle
        AppTone.Info -> AppIcons.Info
    }

private fun AppTone.tint(danger: Color, onInverse: Color): Color =
    if (this == AppTone.Error) danger else onInverse

private val MaxWidth = 560.dp
