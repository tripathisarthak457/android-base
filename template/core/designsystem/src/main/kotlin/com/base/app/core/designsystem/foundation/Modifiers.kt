package com.base.app.core.designsystem.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion
import kotlinx.coroutines.launch

/** Shrinks under the finger, then pops back past its resting size. */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    scaleTo: Float = AppTheme.motion.pressScale,
): Modifier {
    val motion = AppTheme.motion
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return this

    val scale = remember { Animatable(1f) }

    LaunchedEffect(interactionSource, enabled, scaleTo, motion) {
        if (!enabled) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> launch {
                    scale.animateTo(scaleTo, motion.press())
                }

                is PressInteraction.Release -> launch {
                    if (motion.pressOvershoot > 1f) {
                        scale.animateTo(motion.pressOvershoot, motion.pressPop())
                    }
                    scale.animateTo(1f, motion.press())
                }

                is PressInteraction.Cancel -> launch {
                    scale.animateTo(1f, motion.press())
                }
            }
        }
    }

    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** A tappable icon or glyph that bounces, for the places that are not a whole button. */
@Composable
fun Modifier.bounceClick(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    haptic: HapticEffect? = HapticEffect.Tap,
    minTouchTarget: Dp = AppTheme.sizes.minTouchTarget,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberAppHaptics()
    return this
        .pressScale(interactionSource, enabled)
        .defaultMinSize(minWidth = minTouchTarget, minHeight = minTouchTarget)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
        ) {
            haptic?.let(haptics::perform)
            onClick()
        }
}

/** A clickable with no visual indication at all. */
fun Modifier.clickableNoIndication(
    enabled: Boolean = true,
    role: Role? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = clickable(
    // Null lets clickable create an interaction source only once something needs one.
    interactionSource = null,
    indication = null,
    enabled = enabled,
    role = role,
    onClickLabel = onClickLabel,
    onClick = onClick,
)

/**
 * The standard clickable: the theme's indication, a haptic, and a touch target that meets the 48dp
 * floor however small the visual is.
 */
@Composable
fun Modifier.appClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = null,
    onClickLabel: String? = null,
    haptic: HapticEffect? = HapticEffect.Tap,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    minTouchTarget: Dp = AppTheme.sizes.minTouchTarget,
): Modifier {
    val haptics = rememberAppHaptics()
    return this
        .defaultMinSize(minWidth = minTouchTarget, minHeight = minTouchTarget)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
        ) {
            // Every control gets a haptic by default; `haptic = null` opts out where the parent
            // already buzzed.
            haptic?.let(haptics::perform)
            onClick()
        }
}

/** The single definition of what "disabled" looks like. */
@Composable
fun Modifier.disabledAlpha(enabled: Boolean): Modifier =
    if (enabled) this else alpha(DISABLED_ALPHA)

const val DISABLED_ALPHA = 0.38f
