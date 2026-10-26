package com.example.finalafter.crui.core.designsystem.foundation.interaction

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode

/** What a control does under the finger. Every control says which; nothing gets one by default. */
enum class PressFeedback {
    /** Nothing: the control's own change, a selection moving or a switch flipping, is the answer. */
    None,

    /** Shrinks under the finger and springs back: filled buttons, chips, cards. */
    Scale,

    /** Fades: text and icon buttons, links, top-bar actions. */
    Dim,

    /** The highlight colour over the row, on at once and fading after the finger lifts: rows, menu items. */
    Highlight,
}

/**
 * The app-wide indication: nothing is drawn. A bare `clickable` under it gives no feedback at all,
 * which is why controls use [pressable] and say what they want.
 */
object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 0
}
