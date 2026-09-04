package com.base.app.core.designsystem.component.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A transient message bar.
 *
 * ## It is toned, not uniform
 *
 * An error and a confirmation look different — a red-tinted surface with an error glyph versus a
 * green one — rather than both being a neutral dark slab. The user's first glance should tell
 * them whether something went wrong, before they have read a word.
 *
 * ## It appears at the bottom and slides from the bottom
 *
 * Sliding down from the top for a bottom-anchored bar means the bar travels across content it is
 * not related to. Entering from the nearest edge keeps the movement short and local.
 *
 * Auto-dismissal is not handled here. The visual and the timing are separate concerns; timing
 * belongs to the host that also knows about the queue — see `MessageHost` in `:core:ui`.
 */
@Composable
fun AppSnackbar(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    tone: AppTone = AppTone.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val statusColors = tone.colors

    AppSurface(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.sm,
        color = statusColors.subtle,
        contentColor = statusColors.content,
        border = androidx.compose.foundation.BorderStroke(
            AppTheme.sizes.borderWidth,
            statusColors.border,
        ),
        elevation = AppTheme.elevation.overlay,
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(tone.icon, contentDescription = null, tint = statusColors.content)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                title?.let {
                    AppText(
                        text = it,
                        style = AppTheme.typography.titleSmall,
                        color = statusColors.content,
                    )
                }
                AppText(
                    text = text,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.contentPrimary,
                )
            }

            if (actionLabel != null && onAction != null) {
                AppButton(
                    text = actionLabel,
                    onClick = {
                        onAction()
                        onDismiss?.invoke()
                    },
                    variant = ButtonVariant.Tertiary,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

/**
 * Positions a snackbar over the content, above the navigation bar, and animates it in and out.
 *
 * Kept separate from [AppSnackbar] so that the same bar can also be embedded inline — in a form,
 * or at the top of a list — without inheriting the floating placement.
 */
@Composable
fun BoxScope.AppSnackbarHost(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(AppTheme.motion.medium, easing = AppTheme.motion.enter),
            initialOffsetY = { it },
        ) + fadeIn(tween(AppTheme.motion.quick)),
        exit = slideOutVertically(
            animationSpec = tween(AppTheme.motion.quick, easing = AppTheme.motion.exit),
            targetOffsetY = { it },
        ) + fadeOut(tween(AppTheme.motion.instant)),
        modifier = modifier.align(Alignment.BottomCenter),
    ) {
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(AppTheme.spacing.lg),
        ) {
            content()
        }
    }
}
