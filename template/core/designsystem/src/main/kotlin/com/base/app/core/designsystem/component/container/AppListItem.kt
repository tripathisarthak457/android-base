package com.base.app.core.designsystem.component.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.appClickable
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A row: an optional leading slot, a title with an optional supporting line, and an optional
 * trailing slot.
 *
 * Ubiquitous enough that letting each screen assemble its own `Row` guarantees three different
 * heights, two different gaps and one that forgets the minimum touch target. The slots are
 * composable rather than typed as icons so that a row can carry an avatar, a checkbox, a chevron
 * or a badge without this component learning about any of them.
 */
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    overline: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val spacing = AppTheme.spacing
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.appClickable(onClick = onClick, enabled = enabled, minTouchTarget = 0.dp)
                } else {
                    Modifier
                },
            )
            .defaultMinSize(minHeight = AppTheme.sizes.minTouchTarget)
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            overline?.let {
                AppText(
                    text = it,
                    style = AppTheme.typography.labelSmall,
                    color = colors.contentTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AppText(
                text = title,
                style = AppTheme.typography.titleMedium,
                color = colors.contentPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            supporting?.let {
                AppText(
                    text = it,
                    style = AppTheme.typography.bodySmall,
                    color = colors.contentTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        trailing?.invoke()
    }
}
