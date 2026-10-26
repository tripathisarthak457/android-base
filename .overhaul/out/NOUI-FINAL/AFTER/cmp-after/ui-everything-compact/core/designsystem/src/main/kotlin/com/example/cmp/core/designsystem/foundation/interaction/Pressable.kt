package com.example.cmp.core.designsystem.foundation.interaction

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role

/**
 * A tap target with the feedback it names, iOS style: no grey flash, haptics only when asked for,
 * and a hit area of at least 48dp however small the control draws (the space is reserved and the
 * content centred in it).
 *
 * One node does the work of `clickable`, the indication and the touch-target size, and the press
 * animation is read only where the content is placed, so pressing never recomposes or re-measures.
 * A press is shown after the tap timeout, as a scroll view delays it, so starting a scroll on a row
 * does not flash it; a tap quicker than that still shows the whole dip before the spring back.
 *
 * With a mouse or trackpad, a secondary click goes to [onSecondaryClick] (a context menu, at the
 * pointer's position in the control) and never to [onClick]; a row under the pointer shows half its
 * highlight, and the pointer turns into a hand. [onPressStart] runs as the finger lands, before the
 * press shows or the tap is known, so a destination can start loading during the tap: it must be
 * cheap and must not change what is on screen.
 */
public fun Modifier.pressable(
    onClick: () -> Unit,
    feedback: PressFeedback,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    haptic: AppHaptic? = null,
    onLongClick: (() -> Unit)? = null,
    onSecondaryClick: ((Offset) -> Unit)? = null,
    onPressStart: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier = (if (enabled) pointerHoverIcon(PointerIcon.Hand) else this) then PressableElement(
    onClick = onClick,
    feedback = feedback,
    enabled = enabled,
    role = role,
    onClickLabel = onClickLabel,
    haptic = haptic,
    onLongClick = onLongClick,
    onSecondaryClick = onSecondaryClick,
    onPressStart = onPressStart,
    interactionSource = interactionSource,
)

/**
 * [pressable] for a control that lines up with text, such as a link under a paragraph: it keeps its
 * label's width, so the label starts on the text column instead of centred in 48dp. Touch still
 * reaches 48dp across, through the platform's hit test for targets smaller than that.
 */
internal fun Modifier.inlinePressable(
    onClick: () -> Unit,
    feedback: PressFeedback,
    enabled: Boolean = true,
    role: Role? = Role.Button,
): Modifier = (if (enabled) pointerHoverIcon(PointerIcon.Hand) else this) then PressableElement(
    onClick = onClick,
    feedback = feedback,
    enabled = enabled,
    role = role,
    onClickLabel = null,
    haptic = null,
    onLongClick = null,
    onSecondaryClick = null,
    onPressStart = null,
    interactionSource = null,
    reserveWidth = false,
)

private data class PressableElement(
    val onClick: () -> Unit,
    val feedback: PressFeedback,
    val enabled: Boolean,
    val role: Role?,
    val onClickLabel: String?,
    val haptic: AppHaptic?,
    val onLongClick: (() -> Unit)?,
    val onSecondaryClick: ((Offset) -> Unit)?,
    val onPressStart: (() -> Unit)?,
    val interactionSource: MutableInteractionSource?,
    val reserveWidth: Boolean = true,
) : ModifierNodeElement<PressableNode>() {
    override fun create() = PressableNode(
        onClick, feedback, enabled, role, onClickLabel, haptic,
        onLongClick, onSecondaryClick, onPressStart, interactionSource, reserveWidth,
    )

    override fun update(node: PressableNode) = node.update(
        onClick, feedback, enabled, role, onClickLabel, haptic,
        onLongClick, onSecondaryClick, onPressStart, interactionSource, reserveWidth,
    )

    override fun InspectorInfo.inspectableProperties() {
        name = "pressable"
        properties["feedback"] = feedback
        properties["enabled"] = enabled
        properties["role"] = role
    }
}
