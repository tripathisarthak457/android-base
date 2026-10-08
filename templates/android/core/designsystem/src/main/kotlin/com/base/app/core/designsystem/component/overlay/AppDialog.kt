package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A modal dialog. `Dialog` puts its content on screen the instant it composes, with no transition
 * of any kind — one of several things Material supplied that has to be rebuilt here.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val scale by animateFloatAsState(
            targetValue = if (visible) 1f else 0.92f,
            animationSpec = AppTheme.motion.sheet(),
            label = "dialogScale",
        )
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(AppTheme.motion.quick),
            label = "dialogAlpha",
        )

        // Centred here, not by the Dialog: with usePlatformDefaultWidth = false the window fills
        // the screen.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AppSurface(
                modifier = modifier
                    .padding(AppTheme.spacing.xl)
                    .widthIn(max = MAX_WIDTH)
                    .scale(scale)
                    .alpha(alpha),
                shape = AppTheme.shapes.lg,
                color = AppTheme.colors.surface,
                elevation = AppTheme.elevation.modal,
            ) {
                Column(
                    modifier = Modifier.padding(AppTheme.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                    content = content,
                )
            }
        }
    }
}

/** The confirmation dialog: an optional icon, a title, a message, and one or two actions. */
@Composable
fun AppAlertDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissLabel: String? = null,
    icon: ImageVector? = null,
    tone: AppTone = AppTone.Info,
) {
    AppDialog(onDismissRequest = onDismissRequest, modifier = modifier) {
        val statusColors = tone.colors

        icon?.let {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(bottom = AppTheme.spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                AppSurface(
                    modifier = Modifier.size(40.dp),
                    shape = AppTheme.shapes.pill,
                    color = statusColors.subtle,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AppIcon(it, contentDescription = null, tint = statusColors.content)
                    }
                }
            }
        }

        AppText(
            text = title,
            style = AppTheme.typography.headingMedium,
            color = AppTheme.colors.contentPrimary,
        )

        message?.let {
            AppText(
                text = it,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.contentSecondary,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            dismissLabel?.let {
                AppButton(
                    text = it,
                    onClick = onDismissRequest,
                    variant = ButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
            AppButton(
                text = confirmLabel,
                onClick = {
                    onDismissRequest()
                    onConfirm()
                },
                variant = if (tone == AppTone.Error) ButtonVariant.Destructive else ButtonVariant.Primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val MAX_WIDTH = 400.dp
