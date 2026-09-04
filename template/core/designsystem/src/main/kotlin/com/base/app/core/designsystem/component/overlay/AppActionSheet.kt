package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.animation.rememberAppTransitions
import com.base.app.core.designsystem.component.feedback.AppCircularProgress
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.foundation.appClickable
import com.base.app.core.designsystem.foundation.clickableNoIndication
import com.base.app.core.designsystem.theme.AppTheme

/** One choice in an [AppActionSheet]. */
data class SheetAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val isDestructive: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * A list of choices in a sheet, for "what would you like to do with this?".
 *
 * ## Cancel is separated, not just another row
 *
 * A gap and its own surface, because it is the one option that is never what the user came for.
 * Putting it flush with the others makes it a mis-tap target directly under the finger that just
 * opened the sheet.
 *
 * ## Every action dismisses first
 *
 * A sheet that stays open while its action runs can be tapped twice, and the second tap on
 * "Delete" is the one nobody wants to explain.
 */
@Composable
fun AppActionSheet(
    actions: List<SheetAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    cancelLabel: String? = "Cancel",
) {
    AppBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = title,
        showHandle = title == null,
    ) {
        message?.let {
            AppText(
                text = it,
                modifier = Modifier.padding(bottom = AppTheme.spacing.sm),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.contentTertiary,
            )
        }

        AppSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppTheme.shapes.md,
            color = AppTheme.colors.surfaceVariant,
        ) {
            Column {
                actions.forEach { action ->
                    val tint = when {
                        !action.enabled -> AppTheme.colors.contentDisabled
                        action.isDestructive -> AppTheme.colors.danger.content
                        else -> AppTheme.colors.contentPrimary
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appClickable(
                                onClick = {
                                    onDismissRequest()
                                    action.onClick()
                                },
                                enabled = action.enabled,
                                minTouchTarget = AppTheme.sizes.minTouchTarget,
                            )
                            .padding(AppTheme.spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        action.icon?.let {
                            AppIcon(it, contentDescription = null, tint = tint)
                        }
                        AppText(
                            text = action.label,
                            style = AppTheme.typography.bodyLarge,
                            color = tint,
                        )
                    }
                }
            }
        }

        cancelLabel?.let {
            AppSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.spacing.sm),
                shape = AppTheme.shapes.md,
                color = AppTheme.colors.surfaceVariant,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appClickable(
                            onClick = onDismissRequest,
                            minTouchTarget = AppTheme.sizes.minTouchTarget,
                        )
                        .padding(AppTheme.spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = it,
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colors.contentSecondary,
                    )
                }
            }
        }
    }
}

/**
 * A scrim with a spinner, over everything, while something irreversible is in flight.
 *
 * ## Use it sparingly
 *
 * Blocking the whole screen is the heaviest thing a loading state can do, and it is right in
 * exactly one situation: an operation that must not be started twice and cannot be undone —
 * placing an order, submitting a payment. For everything else the button's own loading state or a
 * skeleton is better, because they leave the user able to read the screen and to leave it.
 *
 * The scrim consumes pointer input, which is the part that actually prevents a double submit. A
 * spinner drawn on top without it looks blocking and is not.
 */
@Composable
fun AppLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val transitions = rememberAppTransitions()

    AnimatedVisibility(
        visible = visible,
        enter = transitions.fadeIn,
        exit = transitions.fadeOut,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.scrim)
                // Swallows every tap, drag and fling underneath. Without this the screen is still
                // fully interactive behind a picture of a spinner.
                .clickableNoIndication {},
            contentAlignment = Alignment.Center,
        ) {
            AppSurface(
                shape = AppTheme.shapes.md,
                color = AppTheme.colors.surface,
                elevation = AppTheme.elevation.modal,
            ) {
                Column(
                    modifier = Modifier.padding(AppTheme.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    AppCircularProgress(size = 28.dp, color = AppTheme.colors.accent)
                    label?.let {
                        AppText(
                            text = it,
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.contentSecondary,
                        )
                    }
                }
            }
        }
    }
}
