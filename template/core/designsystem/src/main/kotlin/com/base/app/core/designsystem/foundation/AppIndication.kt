package com.base.app.core.designsystem.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * What every clickable in the app draws when it is pressed, hovered or focused.
 *
 * Compose ships no default indication once Material is out of the picture — `LocalIndication`
 * falls back to a plain platform-grey overlay with no animation at all — so this is the
 * replacement, provided once in `AppTheme`.
 *
 * ## A state layer, not a ripple
 *
 * A ripple expands from the touch point and is unmistakably one design language. This is a flat
 * overlay that fades in and out, which reads as the surface responding rather than as an
 * animation playing on top of it, and stays legible on a component too small for a ripple to
 * resolve — a 24dp icon button, a chip.
 *
 * The fade matters as much as the colour. Snapping the overlay on and off makes a fast tap look
 * like a flicker and a slow one look like a stuck state; ~90ms in and ~160ms out reads as
 * "acknowledged" at both speeds.
 *
 * ## Implemented as a node, not a composable
 *
 * `IndicationNodeFactory` runs in the draw phase without allocating a composable per clickable.
 * The older `rememberUpdatedInstance` API composes for every interactive element on screen, which
 * on a list of a hundred rows is a hundred compositions doing nothing but waiting for a press.
 */
class AppIndication(private val color: Color) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        AppIndicationNode(interactionSource, color)

    override fun equals(other: Any?): Boolean =
        other is AppIndication && other.color == color

    override fun hashCode(): Int = color.hashCode()
}

private class AppIndicationNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {

    private val overlayAlpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            // Presses are counted rather than tracked as a boolean. A second pointer going down
            // before the first comes up would otherwise clear the overlay while a finger is
            // still on the component.
            var presses = 0
            var hovered = false
            var focused = false

            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses++
                    is PressInteraction.Release -> presses--
                    is PressInteraction.Cancel -> presses--
                    is HoverInteraction.Enter -> hovered = true
                    is HoverInteraction.Exit -> hovered = false
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                }

                val target = when {
                    presses > 0 -> PRESSED_ALPHA
                    focused -> FOCUSED_ALPHA
                    hovered -> HOVERED_ALPHA
                    else -> 0f
                }

                launch {
                    overlayAlpha.animateTo(
                        targetValue = target,
                        animationSpec = tween(
                            durationMillis = if (target > overlayAlpha.value) FADE_IN else FADE_OUT,
                        ),
                    )
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = overlayAlpha.value
        if (alpha > 0f) {
            drawRect(color = color.copy(alpha = alpha), size = size)
        }
    }

    private companion object {
        const val PRESSED_ALPHA = 0.09f
        const val FOCUSED_ALPHA = 0.06f
        const val HOVERED_ALPHA = 0.04f
        const val FADE_IN = 90
        const val FADE_OUT = 160
    }
}
