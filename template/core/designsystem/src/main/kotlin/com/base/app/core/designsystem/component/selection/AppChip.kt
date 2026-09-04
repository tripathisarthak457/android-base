package com.base.app.core.designsystem.component.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppClickableSurface
import com.base.app.core.designsystem.foundation.clickableNoIndication
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A chip: a filter, a choice, or a removable token.
 *
 * Selection is carried by fill *and* border, not by fill alone. A selected chip distinguished
 * only by a pale tint is invisible to a colour-blind user and nearly invisible in sunlight; the
 * border change gives the state a second, non-colour signal.
 */
@Composable
fun AppChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    onRemove: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors

    val container by animateColorAsState(
        targetValue = if (selected) colors.accentSubtle else colors.surface,
        animationSpec = tween(AppTheme.motion.quick),
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.contentSecondary,
        animationSpec = tween(AppTheme.motion.quick),
        label = "chipContent",
    )
    val outline by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.border,
        animationSpec = tween(AppTheme.motion.quick),
        label = "chipOutline",
    )

    AppClickableSurface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 34.dp),
        enabled = enabled,
        shape = AppTheme.shapes.pill,
        color = container,
        contentColor = content,
        border = BorderStroke(AppTheme.sizes.borderWidth, outline),
        role = Role.Button,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let { AppIcon(it, contentDescription = null, size = 15.dp) }
            AppText(
                text = label,
                style = AppTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            onRemove?.let { remove ->
                AppIcon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Remove $label",
                    size = 15.dp,
                    // No indication of its own: the chip already responds, and a second overlay
                    // inside the first reads as two nested buttons rather than one with an
                    // affordance.
                    modifier = Modifier.clickableNoIndication(enabled = enabled, onClick = remove),
                )
            }
        }
    }
}
