package com.base.app.core.designsystem.component.button

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppClickableSurface
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The screen's main action, floating over its content. With [expanded] false it shrinks to its
 * icon; tie it to the list so the label steps aside while someone reads and comes back when they
 * scroll up:
 *
 * ```
 * AppFloatingActionButton(AppIcons.Plus, "New note", onClick, expanded = !listState.lastScrolledForward)
 * ```
 *
 * The label is always what TalkBack reads, expanded or not.
 */
@Composable
fun AppFloatingActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion

    AppClickableSurface(
        onClick = onClick,
        modifier = modifier.heightIn(min = FabSize),
        shape = AppTheme.shapes.lg,
        color = colors.accent,
        contentColor = colors.onAccent,
        elevation = AppTheme.elevation.overlay,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clearAndSetSemantics { contentDescription = label },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onAccent,
                size = AppTheme.sizes.iconLarge,
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally(tween(motion.medium, easing = motion.enter)) +
                    fadeIn(tween(motion.medium, easing = motion.enter)),
                exit = shrinkHorizontally(tween(motion.quick, easing = motion.exit)) +
                    fadeOut(tween(motion.quick, easing = motion.exit)),
            ) {
                AppText(
                    text = label,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                    style = AppTheme.typography.button,
                    color = colors.onAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val FabSize = 56.dp
