package com.base.app.core.designsystem.component.formfields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppFiguresText
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A quantity stepper: minus, the value, plus, each button a 48dp target. The value's digits keep
 * one width, so it does not jog as it changes. A screen reader can step it from the actions menu.
 */
@Composable
fun AppStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..99,
    step: Int = 1,
    enabled: Boolean = true,
) {
    val haptics = rememberAppHaptics()
    val decreaseLabel = stringResource(R.string.designsystem_decrease)
    val increaseLabel = stringResource(R.string.designsystem_increase)
    fun change(to: Int) {
        val next = to.coerceIn(range)
        if (next == value) return
        haptics.perform(AppHaptic.Selection)
        onValueChange(next)
    }
    Row(
        modifier = modifier.semantics {
            stateDescription = value.toString()
            customActions = listOf(
                CustomAccessibilityAction(decreaseLabel) { change(value - step); true },
                CustomAccessibilityAction(increaseLabel) { change(value + step); true },
            )
        },
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            icon = AppIcons.Minus,
            contentDescription = decreaseLabel,
            onClick = { change(value - step) },
            variant = ButtonVariant.Tinted,
            size = ButtonSize.Small,
            enabled = enabled && value > range.first,
        )
        AppFiguresText(
            text = value.toString(),
            modifier = Modifier.widthIn(min = 32.dp),
            style = AppTheme.typography.headline,
            color = AppTheme.colors.contentPrimary,
            textAlign = TextAlign.Center,
        )
        AppIconButton(
            icon = AppIcons.Plus,
            contentDescription = increaseLabel,
            onClick = { change(value + step) },
            variant = ButtonVariant.Tinted,
            size = ButtonSize.Small,
            enabled = enabled && value < range.last,
        )
    }
}
