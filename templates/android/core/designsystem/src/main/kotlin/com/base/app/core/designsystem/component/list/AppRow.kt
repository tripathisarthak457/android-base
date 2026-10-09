package com.base.app.core.designsystem.component.list

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.base.app.core.designsystem.component.selection.SwitchTrack
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppHaptic
import com.base.app.core.designsystem.foundation.PressFeedback
import com.base.app.core.designsystem.foundation.disabledAlpha
import com.base.app.core.designsystem.foundation.pressable
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A list row, as iOS draws one: an optional leading icon or avatar, a title with an optional
 * subtitle, and on the trailing side a [value], a [trailing] control and, for a row that opens
 * something, a chevron. A tappable row highlights under the finger. [destructive] turns the title
 * red for "Sign out" and "Delete account". Its height is a minimum, so it grows with the font size.
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
    RowLayout(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.pressable(onClick = onClick, feedback = PressFeedback.Highlight, enabled = enabled)
                } else {
                    Modifier
                },
            )
            .disabledAlpha(enabled),
        leading = leading,
        text = {
            RowText(
                title = title,
                subtitle = subtitle,
                titleColor = if (destructive) colors.danger.content else colors.contentPrimary,
            )
        },
        trailing = if (value != null || trailing != null || (showChevron && !destructive)) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    value?.let {
                        AppText(
                            text = it,
                            style = AppTheme.typography.body,
                            color = colors.contentSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    trailing?.invoke()
                    if (showChevron && !destructive) {
                        AppIcon(AppIcons.ChevronRight, contentDescription = null, tint = colors.contentTertiary, size = ChevronSize)
                    }
                }
            }
        } else {
            null
        },
    )
}

/**
 * A row whose whole width toggles a switch, as a settings row should: one target, read by a screen
 * reader as a single switch with the row's title as its name. Toggling plays the toggle haptic.
 */
@Composable
fun AppToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val haptics = rememberAppHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    RowLayout(
        modifier = modifier
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = { on ->
                    haptics.perform(if (on) AppHaptic.ToggleOn else AppHaptic.ToggleOff)
                    onCheckedChange(on)
                },
            )
            .disabledAlpha(enabled),
        leading = leading,
        text = { RowText(title = title, subtitle = subtitle, titleColor = AppTheme.colors.contentPrimary) },
        trailing = { SwitchTrack(checked = checked, interactionSource = interactionSource) },
    )
}

@Composable
private fun RowText(title: String, subtitle: String?, titleColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppText(
            text = title,
            style = AppTheme.typography.body,
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let {
            AppText(
                text = it,
                style = AppTheme.typography.subheadline,
                color = AppTheme.colors.contentSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Leading, text, trailing in one pass. The text takes what the two sides leave, so a long title
 * wraps instead of running under the value, and the row reports where its text starts so a section
 * can begin the hairline above the next row there.
 */
@Composable
private fun RowLayout(
    modifier: Modifier,
    leading: (@Composable () -> Unit)?,
    text: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    val inset = remember { SeparatorInset() }
    val spacing = AppTheme.spacing
    val minHeight = AppTheme.sizes.rowHeight
    Layout(
        contents = listOf(leading ?: {}, text, trailing ?: {}),
        modifier = modifier.then(inset),
    ) { (leadingSlot, textSlot, trailingSlot), constraints ->
        val horizontal = spacing.lg.roundToPx()
        val vertical = RowVerticalPadding.roundToPx()
        val gap = spacing.md.roundToPx()
        val inner = constraints.copy(minWidth = 0, minHeight = 0).offset(horizontal = -horizontal * 2, vertical = -vertical * 2)

        val leadingPlaceable = leadingSlot.firstOrNull()?.measure(inner)
        val trailingPlaceable = trailingSlot.firstOrNull()?.measure(inner)
        val leadingWidth = leadingPlaceable?.let { it.width + gap } ?: 0
        val trailingWidth = trailingPlaceable?.let { it.width + gap } ?: 0
        val textConstraints = if (inner.hasBoundedWidth) {
            Constraints(maxWidth = (inner.maxWidth - leadingWidth - trailingWidth).coerceAtLeast(0))
        } else {
            Constraints()
        }
        val textPlaceable = textSlot.first().measure(textConstraints)

        val content = maxOf(leadingPlaceable?.height ?: 0, textPlaceable.height, trailingPlaceable?.height ?: 0)
        val height = constraints.constrainHeight(maxOf(content + vertical * 2, minHeight.roundToPx()))
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            horizontal * 2 + leadingWidth + textPlaceable.width + trailingWidth
        }
        inset.start = (horizontal + leadingWidth).toFloat()

        layout(width, height) {
            fun centred(h: Int) = (height - h) / 2
            leadingPlaceable?.placeRelative(horizontal, centred(leadingPlaceable.height))
            textPlaceable.placeRelative(horizontal + leadingWidth, centred(textPlaceable.height))
            trailingPlaceable?.placeRelative(width - horizontal - trailingPlaceable.width, centred(trailingPlaceable.height))
        }
    }
}

private val RowVerticalPadding = 11.dp
private val ChevronSize = 16.dp
