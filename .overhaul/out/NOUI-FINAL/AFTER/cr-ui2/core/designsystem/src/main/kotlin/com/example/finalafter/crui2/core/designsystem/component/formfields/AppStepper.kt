package com.example.finalafter.crui2.core.designsystem.component.formfields

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui2.core.designsystem.R
import com.example.finalafter.crui2.core.designsystem.component.button.AppIconButton
import com.example.finalafter.crui2.core.designsystem.component.button.ButtonSize
import com.example.finalafter.crui2.core.designsystem.component.button.ButtonVariant
import com.example.finalafter.crui2.core.designsystem.component.text.AppFiguresText
import com.example.finalafter.crui2.core.designsystem.foundation.interaction.AppHaptic
import com.example.finalafter.crui2.core.designsystem.foundation.interaction.rememberAppHaptics
import com.example.finalafter.crui2.core.designsystem.icon.AppIcons
import com.example.finalafter.crui2.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A quantity stepper: minus, the value, plus, each button a 48dp target. Holding a button repeats
 * it after half a second, faster the longer it is held, as iOS's does. The value's digits keep one
 * width, so it does not jog as it changes. A screen reader can step it from the actions menu.
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
    // A held button keeps repeating across recompositions, so everything it reads is the latest.
    val current by rememberUpdatedState(value)
    val bounds by rememberUpdatedState(range)
    val increment by rememberUpdatedState(step)
    val decreaseLabel = stringResource(R.string.designsystem_decrease)
    val increaseLabel = stringResource(R.string.designsystem_increase)
    val onChange by rememberUpdatedState(onValueChange)
    fun change(to: Int) {
        val next = to.coerceIn(bounds)
        if (next == current) return
        haptics.perform(AppHaptic.Selection)
        onChange(next)
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
            onClick = { change(current - increment) },
            modifier = Modifier.repeatWhileHeld(enabled && value > range.first) { change(current - increment) },
            variant = ButtonVariant.Tinted,
            size = ButtonSize.Small,
            enabled = enabled && value > range.first,
        )
        AppFiguresText(
            text = value.toString(),
            modifier = Modifier.widthIn(min = AppStepperDefaults.ValueMinWidth),
            style = AppTheme.typography.headline,
            color = AppTheme.colors.contentPrimary,
            textAlign = TextAlign.Center,
        )
        AppIconButton(
            icon = AppIcons.Plus,
            contentDescription = increaseLabel,
            onClick = { change(current + increment) },
            modifier = Modifier.repeatWhileHeld(enabled && value < range.last) { change(current + increment) },
            variant = ButtonVariant.Tinted,
            size = ButtonSize.Small,
            enabled = enabled && value < range.last,
        )
    }
}

/**
 * Runs [onRepeat] while the button is held: first after [RepeatDelayMillis], then ever faster. A
 * press that repeated does not also click when it lifts.
 */
private fun Modifier.repeatWhileHeld(enabled: Boolean, onRepeat: () -> Unit): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(Unit) {
            coroutineScope {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var repeated = false
                    val repeating = launch {
                        delay(RepeatDelayMillis)
                        var interval = RepeatStartMillis
                        while (true) {
                            repeated = true
                            onRepeat()
                            delay(interval)
                            interval = (interval * RepeatAcceleration).toLong().coerceAtLeast(RepeatFastestMillis)
                        }
                    }
                    val up = waitForUpOrCancellation(PointerEventPass.Initial)
                    repeating.cancel()
                    if (repeated) up?.consume()
                }
            }
        }
    }

private const val RepeatDelayMillis = 500L
private const val RepeatStartMillis = 150L
private const val RepeatFastestMillis = 40L
private const val RepeatAcceleration = 0.85f

/** The stepper's value column, wide enough for two digits. */
object AppStepperDefaults {
    val ValueMinWidth = 32.dp
}

@AppComponentPreviews
@Composable
private fun AppStepperPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppStepper(value = 1, onValueChange = {})
        AppStepper(value = 0, onValueChange = {}, range = 0..10)
        AppStepper(value = 99, onValueChange = {})
        AppStepper(value = 3, onValueChange = {}, enabled = false)
    }
}
