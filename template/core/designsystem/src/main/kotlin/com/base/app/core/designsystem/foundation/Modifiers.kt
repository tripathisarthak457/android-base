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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion
import kotlinx.coroutines.launch

/**
 * Shrinks under the finger, then pops back past its resting size.
 *
 * The whole of the physical feel of a control. Three things happen on a tap and they are meant to
 * be felt rather than seen: the state-layer overlay from [AppIndication] says "you touched
 * something", the shrink says "it moved under you", and the overshoot on release says "and it
 * answered". How far it shrinks and how much it overshoots come from [AppMotionStyle].
 *
 * ## Why the release is animated separately
 *
 * A tap can be over in forty milliseconds. Driving the scale from a `pressed` boolean means the
 * spring is still on its way down when the finger leaves, so a quick tap — which is most taps —
 * barely moves at all and reads as unregistered. Running an explicit overshoot on release makes
 * the feedback independent of how long the finger was down.
 *
 * Each interaction is animated in its own coroutine. `Animatable` cancels whatever it was doing
 * when a new animation starts on it, so a second tap during the first one's pop interrupts it
 * from wherever it got to rather than queueing behind it.
 *
 * Pass the same [interactionSource] the component's `clickable` uses, so the scale and the
 * overlay are driven by one source of truth and cannot disagree.
 */
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

/**
 * A tappable icon or glyph that bounces, for the places that are not a whole button.
 *
 * The reveal toggle inside a password field, the remove cross on a chip, a star in a rating: each
 * is a real target with no container of its own to animate. Without this they are the one part of
 * a screen that does not respond to being touched, which is more noticeable than it sounds.
 *
 * No indication overlay — a state layer inside a control that already has one reads as two nested
 * buttons. The bounce is the whole of the feedback.
 */
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

/**
 * A clickable with no visual indication at all.
 *
 * For a target whose feedback is something else entirely — a checkbox row where the box animates,
 * a card that navigates and is already animating out. Reaching for this because the overlay
 * "looks wrong" on a component usually means the component needs a shape, not that it needs to
 * stop responding.
 */
fun Modifier.clickableNoIndication(
    enabled: Boolean = true,
    role: Role? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        role = role,
        onClickLabel = onClickLabel,
        onClick = onClick,
    )
}

/**
 * The standard clickable: the theme's indication, a haptic, and a touch target that meets the
 * 48dp floor however small the visual is.
 *
 * A 24dp icon button with a 24dp touch target is the most common accessibility defect in a
 * hand-rolled design system, and it is invisible to everyone whose thumb happens to be accurate.
 * Enforcing the minimum here means a caller cannot forget.
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
            // Fired here rather than by each caller, so a control cannot be added to the app
            // without one. `haptic = null` is the opt-out, for the handful of targets where a
            // second buzz lands on top of one the parent already played.
            haptic?.let(haptics::perform)
            onClick()
        }
}

/**
 * The single definition of what "disabled" looks like.
 *
 * A component that dims itself by picking a lighter colour is a component that will drift from
 * the others; one alpha applied uniformly cannot.
 */
@Composable
fun Modifier.disabledAlpha(enabled: Boolean): Modifier =
    if (enabled) this else alpha(DISABLED_ALPHA)

const val DISABLED_ALPHA = 0.38f
