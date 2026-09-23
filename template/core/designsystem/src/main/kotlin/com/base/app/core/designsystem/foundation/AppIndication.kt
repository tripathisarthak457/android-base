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

/** What every clickable in the app draws when it is pressed, hovered or focused. */
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
            // Count presses rather than using a boolean, so a second finger lifting does not clear
            // the overlay.
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
