package io.example.fitcnetdb.core.designsystem.component.list

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.offset
import io.example.fitcnetdb.core.designsystem.component.selection.SwitchTrack
import io.example.fitcnetdb.core.designsystem.component.text.AppIcon
import io.example.fitcnetdb.core.designsystem.component.text.AppText
import io.example.fitcnetdb.core.designsystem.foundation.adaptive.centeredMaxWidth
import io.example.fitcnetdb.core.designsystem.foundation.draw.disabledAlpha
import io.example.fitcnetdb.core.designsystem.foundation.interaction.AppHaptic
import io.example.fitcnetdb.core.designsystem.foundation.interaction.PressFeedback
import io.example.fitcnetdb.core.designsystem.foundation.interaction.pressable
import io.example.fitcnetdb.core.designsystem.foundation.interaction.rememberAppHaptics
import io.example.fitcnetdb.core.designsystem.icon.AppIcons
import io.example.fitcnetdb.core.designsystem.theme.AppTheme

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
        modifier = modifier,
        interaction = Modifier
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
        modifier = modifier,
        interaction = Modifier
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

/**
 * One choice of several, as iOS Settings lists them: the chosen row carries a checkmark in the
 * accent colour. The whole row is the target, and a screen reader hears a radio button named by the
 * title, selected or not. The checkmark's room is kept on every row, so choosing never reflows text.
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
        modifier = modifier,
        interaction = Modifier
            .semantics { this.selected = selected }
            .pressable(onClick = onSelect, feedback = PressFeedback.Highlight, enabled = enabled, role = Role.RadioButton)
            .disabledAlpha(enabled),
        leading = leading,
        text = { RowText(title = title, subtitle = subtitle, titleColor = colors.contentPrimary) },
        trailing = {
            AppIcon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { alpha = if (selected) 1f else 0f },
                tint = colors.accentContent,
                size = CheckSize,
            )
        },
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
 *
 * On a wide pane the row stops at the readable width and centres, inside a list that still spans
 * the pane, so the list scrolls from anywhere and lines stay short enough to read. [interaction]
 * goes inside that width, so a highlight covers the row and not the margins.
 */
@Composable
private fun RowLayout(
    modifier: Modifier,
    interaction: Modifier,
    leading: (@Composable () -> Unit)?,
    text: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    val inset = remember { SeparatorInset() }
    val spacing = AppTheme.spacing
    val minHeight = AppTheme.sizes.rowHeight
    val readable = AppTheme.layout.readableMaxWidth
    Layout(
        contents = listOf(leading ?: {}, text, trailing ?: {}),
        modifier = modifier
            .then(if (readable.isFinite) Modifier.centeredMaxWidth(readable) else Modifier)
            .then(interaction)
            .then(inset),
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
private val CheckSize = 20.dp
