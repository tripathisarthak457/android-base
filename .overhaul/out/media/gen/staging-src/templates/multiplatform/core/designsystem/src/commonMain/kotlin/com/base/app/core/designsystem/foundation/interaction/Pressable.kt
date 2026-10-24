package com.base.app.core.designsystem.foundation.interaction

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.semantics.Role
import com.base.app.core.designsystem.theme.LocalAppColors
import com.base.app.core.designsystem.theme.LocalAppSprings
import com.base.app.core.designsystem.theme.tokens.DesignTokens
import kotlinx.coroutines.launch

/**
 * Makes an element tappable, with the press [feedback] the element states and no grey overlay.
 * Semantics, focus, keyboard activation and the touch target come from `clickable`; the feedback is
 * drawn by one node that reads its progress in the draw phase, so a press never recomposes.
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    feedback: PressFeedback,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = feedback.indication,
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    onClick = onClick,
)

private val PressFeedback.indication: IndicationNodeFactory
    get() = when (this) {
        PressFeedback.None -> NoIndication
        PressFeedback.Scale -> ScaleIndication
        PressFeedback.Dim -> DimIndication
        PressFeedback.Highlight -> HighlightIndication
    }

private object ScaleIndication : PressIndication(PressFeedback.Scale)

private object DimIndication : PressIndication(PressFeedback.Dim)

private object HighlightIndication : PressIndication(PressFeedback.Highlight)

private abstract class PressIndication(private val feedback: PressFeedback) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressNode(interactionSource, feedback)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = feedback.hashCode()
}

private class PressNode(
    private val interactionSource: InteractionSource,
    private val feedback: PressFeedback,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val progress = Animatable(0f)
    private val layerPaint = Paint()

    override fun onAttach() {
        coroutineScope.launch {
            // Counted, so a second finger lifting does not clear the first one's press.
            var pressed = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressed++
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressed--
                    else -> return@collect
                }
                if (pressed > 0) press() else release()
            }
        }
    }

    private fun press() {
        coroutineScope.launch {
            // A row lights at once, as iOS's does; a shrink or a dim is quick but still travels.
            if (feedback == PressFeedback.Highlight) {
                progress.snapTo(1f)
            } else {
                progress.animateTo(1f, currentValueOf(LocalAppSprings).press.unit)
            }
        }
    }

    private fun release() {
        coroutineScope.launch {
            val spec = if (feedback == PressFeedback.Highlight) {
                tween(DesignTokens.Motion.highlightFadeOutMs)
            } else {
                currentValueOf(LocalAppSprings).release.unit
            }
            progress.animateTo(0f, spec)
        }
    }

    override fun ContentDrawScope.draw() {
        val amount = progress.value
        if (amount == 0f) {
            drawContent()
            return
        }
        when (feedback) {
            PressFeedback.Scale -> {
                val scale = 1f - (1f - DesignTokens.Motion.pressScale) * amount
                scale(scale) { this@draw.drawContent() }
            }
            PressFeedback.Dim -> {
                // A layer the size of the control, and only while it is held.
                layerPaint.alpha = 1f - (1f - DesignTokens.Motion.pressDimAlpha) * amount
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), layerPaint)
                    drawContent()
                    canvas.restore()
                }
            }
            PressFeedback.Highlight -> {
                drawRect(currentValueOf(LocalAppColors).highlight, alpha = amount)
                drawContent()
            }
            PressFeedback.None -> drawContent()
        }
    }
}
