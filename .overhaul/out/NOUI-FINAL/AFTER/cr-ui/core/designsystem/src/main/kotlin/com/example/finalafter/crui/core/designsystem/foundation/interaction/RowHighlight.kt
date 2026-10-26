package com.example.finalafter.crui.core.designsystem.foundation.interaction

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import com.example.finalafter.crui.core.designsystem.theme.LocalAppColors
import com.example.finalafter.crui.core.designsystem.theme.LocalAppSprings
import kotlinx.coroutines.launch

/**
 * A row under the finger, as iOS draws a table cell: the highlight colour at once, fading out over
 * a fifth of a second after the finger lifts. Translucent, so it reads on any surface.
 */
object RowHighlight : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = RowHighlightNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 1
}

private class RowHighlightNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val alpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            // Counted, so a second finger lifting does not clear the first one's highlight.
            var presses = 0
            interactionSource.interactions.collect { interaction ->
                presses = when (interaction) {
                    is PressInteraction.Press -> presses + 1
                    is PressInteraction.Release, is PressInteraction.Cancel -> (presses - 1).coerceAtLeast(0)
                    else -> return@collect
                }
                if (presses > 0) {
                    alpha.snapTo(1f)
                } else {
                    val fade = currentValueOf(LocalAppSprings).highlightFade<Float>()
                    launch { alpha.animateTo(0f, fade) }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val value = alpha.value
        if (value > 0f) drawRect(currentValueOf(LocalAppColors).highlight, alpha = value)
    }
}
