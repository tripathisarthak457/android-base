package com.base.app.core.designsystem.component.selection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

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
        Canvas(Modifier.size(AppCheckboxDefaults.Size)) {
            val radius = size.minDimension / 2f
            val p = dot.value
            drawCircle(lerp(colors.outline, colors.accent, p.coerceIn(0f, 1f)), radius - (AppRadioDefaults.RingWidth / 2).toPx(), style = Stroke(AppRadioDefaults.RingWidth.toPx()))
            if (p > 0f) drawCircle(colors.accent, radius * 0.5f * p, Offset(size.width / 2f, size.height / 2f))
        }
    }
}

/** The radio button's ring; its size is [AppCheckboxDefaults.Size]. */
internal object AppRadioDefaults {
    val RingWidth = 1.5.dp
}

@AppComponentPreviews
@Composable
private fun AppRadioPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppRadio(selected = true, onClick = {}, label = "Standard delivery")
        AppRadio(selected = false, onClick = {}, label = "Express delivery")
        AppRadio(selected = false, onClick = {}, label = "Collect in store", enabled = false)
        AppRadio(selected = true, onClick = null)
        AppRadio(selected = false, onClick = {}, label = "Leave it with a neighbour if nobody answers the door")
    }
}
