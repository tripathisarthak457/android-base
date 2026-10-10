package com.base.app.core.designsystem.component.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A row of an [AppSection]: a title, an optional subtitle under it and a [value] at the trailing
 * edge, as iOS's value-style cell. With [onClick] the row highlights under the finger and shows a
 * chevron, unless [showChevron] says it opens nothing further.
 */
@Composable
fun AppRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val titleColor = when {
        !enabled -> colors.contentDisabled
        destructive -> colors.danger
        else -> colors.contentPrimary
    }
    RowLayout(
        modifier = if (onClick != null) {
            modifier.pressable(onClick = onClick, feedback = PressFeedback.Highlight, enabled = enabled)
        } else {
            modifier
        },
        leading = leading,
        text = { RowText(title, subtitle, titleColor) },
        trailing = {
            if (value != null) {
                AppText(
                    text = value,
                    style = AppTheme.typography.body,
                    color = colors.contentSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing?.invoke()
            if (showChevron) {
                AppIcon(
                    AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.contentTertiary,
                    size = AppTheme.sizes.iconSmall,
                )
            }
        },
    )
}

/**
 * One choice among several, marked with a check when [selected]: the theme picker, a language
 * list. The whole row is the target, and it is announced as a selectable item.
 */
@Composable
fun AppChoiceRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    RowLayout(
        modifier = modifier
            .semantics { this.selected = selected }
            .pressable(
                onClick = onSelect,
                feedback = PressFeedback.Highlight,
                enabled = enabled,
                role = Role.RadioButton,
            ),
        leading = leading,
        text = { RowText(title, subtitle, if (enabled) colors.contentPrimary else colors.contentDisabled) },
        trailing = {
            if (selected) {
                AppIcon(AppIcons.Check, contentDescription = null, tint = colors.accentContent)
            }
        },
    )
}

@Composable
private fun RowText(title: String, subtitle: String?, titleColor: Color) {
    Column {
        AppText(text = title, style = AppTheme.typography.body, color = titleColor)
        if (subtitle != null) {
            AppText(
                text = subtitle,
                style = AppTheme.typography.footnote,
                color = AppTheme.colors.contentSecondary,
            )
        }
    }
}

@Composable
private fun RowLayout(
    modifier: Modifier,
    leading: (@Composable () -> Unit)?,
    text: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.sizes.minTouchTarget)
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(AppTheme.spacing.md))
        }
        Box(modifier = Modifier.weight(1f)) { text() }
        Row(
            modifier = Modifier.padding(start = AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            trailing()
        }
    }
}
