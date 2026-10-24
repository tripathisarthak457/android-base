package com.example.deskstandard.core.designsystem.foundation.interaction

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction

/**
 * Calls [onChange] each time this source goes from nothing held to a press or a drag, and back: for
 * a control whose look follows a hold through an animation, instead of collecting the hold as state
 * and recomposing for it.
 */
internal suspend fun InteractionSource.collectHeld(onChange: (held: Boolean) -> Unit) {
    val holds = mutableSetOf<Interaction>()
    var held = false
    interactions.collect { interaction ->
        when (interaction) {
            is PressInteraction.Press, is DragInteraction.Start -> holds += interaction
            is PressInteraction.Release -> holds -= interaction.press
            is PressInteraction.Cancel -> holds -= interaction.press
            is DragInteraction.Stop -> holds -= interaction.start
            is DragInteraction.Cancel -> holds -= interaction.start
        }
        if (holds.isNotEmpty() != held) {
            held = !held
            onChange(held)
        }
    }
}
