package com.base.app.core.designsystem.component.selection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.draw.disabledAlpha
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.minimumTouchTarget
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A checkbox, with an optional [label] that is part of the same target. The tick draws itself in.
 * With no [onCheckedChange] it only shows a state and takes no touches, so a row around it keeps
 * its own tap.
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
    val haptics = rememberAppHaptics()
    val progress = animateFloatAsState(if (checked) 1f else 0f, AppTheme.springs.thumb.unit, label = "checkbox")
    val interaction = if (enabled && onCheckedChange != null) {
        Modifier.toggleable(value = checked, indication = null, interactionSource = null, role = Role.Checkbox) {
            haptics.perform(if (it) AppHaptic.ToggleOn else AppHaptic.ToggleOff)
            onCheckedChange(it)
        }
    } else {
        Modifier.semantics { toggleableState = ToggleableState(checked) }
    }

    LabelledControl(modifier.then(interaction), label, enabled) {
        Spacer(
            Modifier
                .size(AppCheckboxDefaults.Size)
                .drawWithCache {
                    // Built once per size; the tick is drawn as a growing segment of the same path.
                    val tick = Path().apply {
                        moveTo(size.width * 0.26f, size.height * 0.52f)
                        lineTo(size.width * 0.43f, size.height * 0.69f)
                        lineTo(size.width * 0.75f, size.height * 0.33f)
                    }
                    val measure = PathMeasure().apply { setPath(tick, false) }
                    val segment = Path()
                    val radius = CornerRadius(AppCheckboxDefaults.Corner.toPx())
                    val ring = Stroke(AppCheckboxDefaults.RingWidth.toPx())
                    val tickStroke = Stroke(AppCheckboxDefaults.TickWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    onDrawBehind {
                        val p = progress.value
                        drawRoundRect(colors.outline, cornerRadius = radius, style = ring, alpha = 1f - p)
                        if (p > 0f) {
                            drawRoundRect(colors.accent, cornerRadius = radius, alpha = p.coerceAtMost(1f))
                            segment.reset()
                            measure.getSegment(0f, measure.length * p.coerceIn(0f, 1f), segment, true)
                            drawPath(segment, colors.onAccent, style = tickStroke)
                        }
                    }
                },
        )
    }
}

/** The control and its label, one 48dp target. */
@Composable
internal fun LabelledControl(
    modifier: Modifier,
    label: String?,
    enabled: Boolean,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.minimumTouchTarget().disabledAlpha(enabled),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        control()
        label?.let { AppText(text = it, style = AppTheme.typography.body, color = AppTheme.colors.contentPrimary) }
    }
}

/**
 * The checkbox's metrics, iOS's 22 dp control. [Size] is the radio button's too, so the two line up
 * in a form.
 */
object AppCheckboxDefaults {
    val Size = 22.dp
    val Corner = 6.dp
    val RingWidth = 1.5.dp
    val TickWidth = 2.dp
}

@AppComponentPreviews
@Composable
private fun AppCheckboxPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppCheckbox(checked = true, onCheckedChange = {}, label = "Email me receipts")
        AppCheckbox(checked = false, onCheckedChange = {}, label = "Share anonymous usage data")
        AppCheckbox(checked = true, onCheckedChange = {}, label = "Required by your organisation", enabled = false)
        AppCheckbox(checked = false, onCheckedChange = null)
        AppCheckbox(
            checked = false,
            onCheckedChange = {},
            label = "I have read the terms of service and the privacy policy, and I agree to both of them",
        )
    }
}
