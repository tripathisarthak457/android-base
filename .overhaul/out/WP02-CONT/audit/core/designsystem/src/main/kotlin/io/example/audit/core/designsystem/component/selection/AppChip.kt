package io.example.audit.core.designsystem.component.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.example.audit.core.designsystem.R
import io.example.audit.core.designsystem.component.text.AppIcon
import io.example.audit.core.designsystem.component.text.AppText
import io.example.audit.core.designsystem.foundation.draw.ContentUnderBone
import io.example.audit.core.designsystem.foundation.draw.LocalRedacted
import io.example.audit.core.designsystem.foundation.draw.disabledAlpha
import io.example.audit.core.designsystem.foundation.draw.skeleton
import io.example.audit.core.designsystem.foundation.interaction.PressFeedback
import io.example.audit.core.designsystem.foundation.interaction.collectHeld
import io.example.audit.core.designsystem.foundation.interaction.pressable
import io.example.audit.core.designsystem.icon.AppIcons
import io.example.audit.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch

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
    val removeInteractions = remember { MutableInteractionSource() }
    val bone = LocalRedacted.current
    val redacted = if (bone) Modifier.skeleton(shape) else Modifier

    Box(modifier = modifier.disabledAlpha(enabled), contentAlignment = Alignment.CenterEnd) {
        Row(
            modifier = Modifier
                .pressable(onClick = onClick, feedback = PressFeedback.Scale, enabled = enabled, role = Role.Button)
                .semantics { this.selected = selected }
                .then(redacted)
                .background(container, shape)
                .heightIn(min = ChipHeight)
                .padding(start = 12.dp, end = if (onRemove != null) RemoveEndPadding else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContentUnderBone(bone) {
                leadingIcon?.let {
                    AppIcon(it, contentDescription = null, tint = content, size = AppTheme.sizes.iconSmall)
                }
                AppText(
                    text = label,
                    style = AppTheme.typography.subheadline,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onRemove != null) {
                    val held = rememberHeldFraction(removeInteractions)
                    val dimAlpha = AppTheme.springs.pressDimAlpha
                    AppIcon(
                        AppIcons.Close,
                        contentDescription = null,
                        tint = content,
                        size = RemoveGlyph,
                        modifier = Modifier.graphicsLayer {
                            alpha = lerp(1f, dimAlpha, held.value)
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        },
                    )
                }
            }
        }
        if (onRemove != null) {
            // The cross is drawn in the row, where it spaces like iOS's; its target lies over it here,
            // so the 48dp a touch target reserves never pushes the cross away from the label.
            val removeLabel = stringResource(R.string.designsystem_remove_item, label)
            Box(
                Modifier
                    .pressable(
                        onClick = onRemove,
                        feedback = PressFeedback.None,
                        enabled = enabled,
                        onClickLabel = removeLabel,
                        interactionSource = removeInteractions,
                    )
                    .size(RemoveTargetWidth, ChipHeight)
                    .semantics { contentDescription = removeLabel },
            )
        }
    }
}

/** 1 while [interactions] holds a press, fading to 0 after it: a dim read only in the layer. */
@Composable
private fun rememberHeldFraction(interactions: InteractionSource): Animatable<Float, AnimationVector1D> {
    val springs = AppTheme.springs
    val held = remember { Animatable(0f) }
    LaunchedEffect(interactions, springs) {
        interactions.collectHeld { pressed ->
            launch { if (pressed) held.snapTo(1f) else held.animateTo(0f, tween(springs.fadeSlow)) }
        }
    }
    return held
}

private val ChipHeight = 32.dp
private val RemoveGlyph = 14.dp
private val RemoveEndPadding = 10.dp
private val RemoveTargetWidth = RemoveEndPadding + RemoveGlyph + 6.dp
