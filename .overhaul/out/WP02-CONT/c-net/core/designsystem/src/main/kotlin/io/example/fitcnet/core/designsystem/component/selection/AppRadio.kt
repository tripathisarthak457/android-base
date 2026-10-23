package io.example.fitcnet.core.designsystem.component.selection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.example.fitcnet.core.designsystem.foundation.interaction.AppHaptic
import io.example.fitcnet.core.designsystem.foundation.interaction.rememberAppHaptics
import io.example.fitcnet.core.designsystem.theme.AppTheme

/** A radio button, with an optional [label] that is part of the same target. */
@Composable
fun AppRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val dot = animateFloatAsState(if (selected) 1f else 0f, AppTheme.springs.thumb.unit, label = "radio")
    val interaction = if (enabled && onClick != null) {
        Modifier.selectable(selected = selected, indication = null, interactionSource = null, role = Role.RadioButton) {
            if (!selected) haptics.perform(AppHaptic.Selection)
            onClick()
        }
    } else {
        Modifier.semantics { this.selected = selected }
    }

    LabelledControl(modifier.then(interaction), label, enabled) {
        Canvas(Modifier.size(BoxSize)) {
            val radius = size.minDimension / 2f
            val p = dot.value
            drawCircle(lerp(colors.outline, colors.accent, p.coerceIn(0f, 1f)), radius - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            if (p > 0f) drawCircle(colors.accent, radius * 0.5f * p, Offset(size.width / 2f, size.height / 2f))
        }
    }
}
