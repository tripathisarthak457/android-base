package com.base.app.core.designsystem.component.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.HapticEffect
import com.base.app.core.designsystem.foundation.disabledAlpha
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A checkbox whose tick is drawn on rather than faded in.
 *
 * The stroke is revealed along its own length using a [PathMeasure], so the check appears to be
 * written in the direction a person would write it. A tick that cross-fades reads as a static
 * image swapping; one that draws reads as a response to the tap, and it costs about fifteen lines.
 *
 * The whole row is the touch target when a [label] is given — tapping the word next to a checkbox
 * and having nothing happen is the most common small frustration in a settings screen.
 */
@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion

    val fill by animateColorAsState(
        targetValue = if (checked) colors.accent else Color.Transparent,
        animationSpec = tween(motion.quick),
        label = "checkboxFill",
    )
    val border by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.borderStrong,
        animationSpec = tween(motion.quick),
        label = "checkboxBorder",
    )
    val tick by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        // Slightly slower than the fill, so the box is already coloured by the time the tick
        // starts drawing. Running them together makes the tick hard to see against the fill.
        animationSpec = tween(motion.medium, easing = motion.enter),
        label = "checkboxTick",
    )

    val haptics = rememberAppHaptics()
    val toggle = Modifier.toggleable(
        value = checked,
        enabled = enabled && onCheckedChange != null,
        role = Role.Checkbox,
        onValueChange = {
            haptics.perform(HapticEffect.Toggle)
            onCheckedChange?.invoke(it)
        },
    )

    SelectionRow(
        modifier = modifier,
        label = label,
        enabled = enabled,
        interaction = toggle,
    ) {
        Canvas(modifier = Modifier.size(BOX_SIZE)) {
            val stroke = 1.8.dp.toPx()
            val radius = CornerRadius(5.dp.toPx())
            val inset = stroke / 2f
            val boxSize = Size(size.width - stroke, size.height - stroke)

            drawRoundRect(
                color = fill,
                topLeft = Offset(inset, inset),
                size = boxSize,
                cornerRadius = radius,
            )
            drawRoundRect(
                color = border,
                topLeft = Offset(inset, inset),
                size = boxSize,
                cornerRadius = radius,
                style = Stroke(width = stroke),
            )

            if (tick > 0f) {
                val path = Path().apply {
                    moveTo(size.width * 0.26f, size.height * 0.52f)
                    lineTo(size.width * 0.44f, size.height * 0.70f)
                    lineTo(size.width * 0.76f, size.height * 0.32f)
                }
                val drawn = Path()
                PathMeasure().apply {
                    setPath(path, false)
                    getSegment(0f, length * tick, drawn, true)
                }
                drawPath(
                    path = drawn,
                    color = colors.onAccent,
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

/**
 * A radio button. Ring plus a dot that scales in from nothing.
 *
 * `Role.RadioButton` and `selectable` rather than `toggleable`, so assistive technology announces
 * it as one of a set rather than as an independent on/off — which is the difference between "one
 * of three, selected" and "checked".
 */
@Composable
fun AppRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors

    val ring by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.borderStrong,
        animationSpec = tween(AppTheme.motion.quick),
        label = "radioRing",
    )
    val dot by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = AppTheme.motion.press(),
        label = "radioDot",
    )

    val haptics = rememberAppHaptics()
    val select = Modifier.selectable(
        selected = selected,
        enabled = enabled && onClick != null,
        role = Role.RadioButton,
        onClick = {
            haptics.perform(HapticEffect.Select)
            onClick?.invoke()
        },
    )

    SelectionRow(
        modifier = modifier,
        label = label,
        enabled = enabled,
        interaction = select,
    ) {
        Canvas(modifier = Modifier.size(BOX_SIZE)) {
            val stroke = 1.8.dp.toPx()
            drawCircle(
                color = ring,
                radius = (size.minDimension - stroke) / 2f,
                style = Stroke(width = stroke),
            )
            if (dot > 0f) {
                drawCircle(
                    color = colors.accent,
                    radius = size.minDimension * 0.26f * dot,
                )
            }
        }
    }
}

/**
 * A switch.
 *
 * The thumb travels on a spring rather than a tween, so a rapid double-toggle reverses from where
 * the thumb actually is instead of restarting. It is the one control people flip back and forth
 * to see what happens, and a tween makes that feel unresponsive.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors

    val track by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.surfaceVariant,
        animationSpec = tween(AppTheme.motion.quick),
        label = "switchTrack",
    )
    val trackBorder by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.borderStrong,
        animationSpec = tween(AppTheme.motion.quick),
        label = "switchTrackBorder",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TRACK_WIDTH - THUMB_SIZE - THUMB_INSET else THUMB_INSET,
        animationSpec = AppTheme.motion.sheet(),
        label = "switchThumb",
    )

    val haptics = rememberAppHaptics()
    val toggle = Modifier.toggleable(
        value = checked,
        enabled = enabled && onCheckedChange != null,
        role = Role.Switch,
        onValueChange = {
            haptics.perform(HapticEffect.Toggle)
            onCheckedChange?.invoke(it)
        },
    )

    SelectionRow(
        modifier = modifier,
        label = label,
        enabled = enabled,
        interaction = toggle,
    ) {
        Canvas(modifier = Modifier.size(width = TRACK_WIDTH, height = TRACK_HEIGHT)) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(color = track, size = size, cornerRadius = radius)
            drawRoundRect(
                color = trackBorder,
                size = size,
                cornerRadius = radius,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = if (checked) colors.onAccent else colors.surface,
                radius = THUMB_SIZE.toPx() / 2f,
                center = Offset(
                    x = thumbOffset.toPx() + THUMB_SIZE.toPx() / 2f,
                    y = size.height / 2f,
                ),
            )
        }
    }
}

/**
 * The shared row: the control, and an optional label that is part of the same touch target.
 *
 * The interaction modifier is applied to the row when there is a label and to nothing when there
 * is not — a bare control keeps whatever target its parent gave it, which is what lets a checkbox
 * sit inside an already-clickable list row without two competing click handlers.
 */
@Composable
private fun SelectionRow(
    modifier: Modifier,
    label: String?,
    enabled: Boolean,
    interaction: Modifier,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .then(interaction)
            .disabledAlpha(enabled),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        control()
        label?.let {
            AppText(
                text = it,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.contentPrimary,
            )
        }
    }
}

private val BOX_SIZE = 22.dp
private val TRACK_WIDTH = 46.dp
private val TRACK_HEIGHT = 28.dp
private val THUMB_SIZE = 22.dp
private val THUMB_INSET = 3.dp
