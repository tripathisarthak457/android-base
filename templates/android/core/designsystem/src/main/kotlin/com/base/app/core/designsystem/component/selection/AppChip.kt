package com.base.app.core.designsystem.component.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.PressFeedback
import com.base.app.core.designsystem.foundation.disabledAlpha
import com.base.app.core.designsystem.foundation.pressable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A chip: a filter, a choice, or a removable token. It shrinks under the finger. A selected chip
 * takes the accent's tint and its legible text colour. [onRemove] adds a remove button that is its
 * own 48dp target, so a tap near the cross never selects the chip instead.
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
    val container = if (selected) colors.accentSubtle else colors.fill
    val content = if (selected) colors.accentContent else colors.contentPrimary
    val shape = AppTheme.style.chipShape

    Box(modifier = modifier.disabledAlpha(enabled), contentAlignment = Alignment.CenterEnd) {
        Row(
            modifier = Modifier
                .pressable(onClick = onClick, feedback = PressFeedback.Scale, enabled = enabled, role = Role.Button)
                .semantics { this.selected = selected }
                .background(container, shape)
                .heightIn(min = ChipHeight)
                .padding(start = 12.dp, end = if (onRemove != null) RemoveSpace else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let { AppIcon(it, contentDescription = null, tint = content, size = AppTheme.sizes.iconSmall) }
            AppText(
                text = label,
                style = AppTheme.typography.subheadline,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .pressable(
                        onClick = onRemove,
                        feedback = PressFeedback.Dim,
                        enabled = enabled,
                        onClickLabel = stringResource(R.string.designsystem_remove_item, label),
                    )
                    .width(RemoveSpace),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    AppIcons.Close,
                    contentDescription = stringResource(R.string.designsystem_remove_item, label),
                    tint = content,
                    size = 14.dp,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

private val ChipHeight = 32.dp
private val RemoveSpace = 30.dp
