package com.base.app.core.designsystem.foundation

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.base.app.core.designsystem.theme.LocalAppColors
import com.base.app.core.designsystem.theme.LocalAppSprings
import com.base.app.core.designsystem.theme.LocalReduceMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                    val fade = currentValueOf(LocalAppSprings).highlightFadeOut
                    launch { alpha.animateTo(0f, tween(fade)) }
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

/**
 * A tap target with the feedback it names, iOS style: no grey flash, haptics only when asked for,
 * and a hit area of at least 48dp however small the control draws (the space is reserved and the
 * content centred in it).
 *
 * One node does the work of `clickable`, the indication and the touch-target size, and the press
 * animation is read only where the content is placed, so pressing never recomposes or re-measures.
 * A press is shown after the tap timeout, as a scroll view delays it, so starting a scroll on a row
 * does not flash it; a tap quicker than that still shows the whole dip before the spring back.
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    feedback: PressFeedback,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    haptic: AppHaptic? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier = this then PressableElement(
    onClick = onClick,
    feedback = feedback,
    enabled = enabled,
    role = role,
    onClickLabel = onClickLabel,
    haptic = haptic,
    onLongClick = onLongClick,
    interactionSource = interactionSource,
)

/**
 * Reserves at least the 48dp touch target around content that draws smaller, and centres it there.
 * The reserved space belongs to the node, so a pointer input before this modifier covers all of it.
 */
fun Modifier.minimumTouchTarget(): Modifier = this then MinimumTouchTargetElement

private object MinimumTouchTargetElement : ModifierNodeElement<MinimumTouchTargetNode>() {
    override fun create() = MinimumTouchTargetNode()

    override fun update(node: MinimumTouchTargetNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "minimumTouchTarget"
    }

    override fun equals(other: Any?) = other === this

    override fun hashCode() = 2
}

private class MinimumTouchTargetNode : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val target = MinTouchTarget.roundToPx()
        val width = constraints.constrainWidth(maxOf(placeable.width, target))
        val height = constraints.constrainHeight(maxOf(placeable.height, target))
        return layout(width, height) {
            placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2)
        }
    }
}

private data class PressableElement(
    val onClick: () -> Unit,
    val feedback: PressFeedback,
    val enabled: Boolean,
    val role: Role?,
    val onClickLabel: String?,
    val haptic: AppHaptic?,
    val onLongClick: (() -> Unit)?,
    val interactionSource: MutableInteractionSource?,
) : ModifierNodeElement<PressableNode>() {
    override fun create() =
        PressableNode(onClick, feedback, enabled, role, onClickLabel, haptic, onLongClick, interactionSource)

    override fun update(node: PressableNode) =
        node.update(onClick, feedback, enabled, role, onClickLabel, haptic, onLongClick, interactionSource)

    override fun InspectorInfo.inspectableProperties() {
        name = "pressable"
        properties["feedback"] = feedback
        properties["enabled"] = enabled
        properties["role"] = role
    }
}

private class PressableNode(
    private var onClick: () -> Unit,
    private var feedback: PressFeedback,
    private var enabled: Boolean,
    private var role: Role?,
    private var onClickLabel: String?,
    private var haptic: AppHaptic?,
    private var onLongClick: (() -> Unit)?,
    private var externalSource: MutableInteractionSource?,
) : DelegatingNode(),
    LayoutModifierNode,
    DrawModifierNode,
    SemanticsModifierNode,
    KeyInputModifierNode,
    CompositionLocalConsumerModifierNode {

    /** 0 at rest, 1 fully pressed. Read only in [layerBlock] and, for a highlight, in [draw]. */
    private val progress = Animatable(0f)
    private var pressAnimation: Job? = null
    private var press: PressInteraction.Press? = null
    private var keyPressed = false

    private var focus: FocusInteraction.Focus? = null
    private var childOffset = IntOffset.Zero
    private var childSize = IntSize.Zero
    private var scaleTarget = 1f

    private val focusTarget = delegate(
        FocusTargetModifierNode(focusability = focusabilityFor(enabled)) { _, current ->
            onFocusChanged(current.isFocused)
        },
    )

    private val pointerInput = delegate(SuspendingPointerInputModifierNode { detectTaps() })

    fun update(
        onClick: () -> Unit,
        feedback: PressFeedback,
        enabled: Boolean,
        role: Role?,
        onClickLabel: String?,
        haptic: AppHaptic?,
        onLongClick: (() -> Unit)?,
        interactionSource: MutableInteractionSource?,
    ) {
        val gestureChanged = enabled != this.enabled || (onLongClick == null) != (this.onLongClick == null)
        val semanticsChanged = enabled != this.enabled || role != this.role ||
            onClickLabel != this.onClickLabel || (onLongClick == null) != (this.onLongClick == null)
        val sourceChanged = interactionSource != externalSource
        val feedbackChanged = feedback != this.feedback

        if (sourceChanged || (gestureChanged && !enabled)) cancelPress()

        this.onClick = onClick
        this.feedback = feedback
        this.enabled = enabled
        this.role = role
        this.onClickLabel = onClickLabel
        this.haptic = haptic
        this.onLongClick = onLongClick
        this.externalSource = interactionSource

        focusTarget.focusability = focusabilityFor(enabled)
        if (feedbackChanged) invalidateDraw()
        if (gestureChanged) pointerInput.resetPointerInputHandler()
        if (semanticsChanged) invalidateSemantics()
    }

    private fun focusabilityFor(enabled: Boolean) = if (enabled) Focusability.SystemDefined else Focusability.Never

    private suspend fun PointerInputScope.detectTaps() {
        if (!enabled) return
        val longClick = onLongClick
        detectTapGestures(
            onPress = { offset ->
                // Shown after the tap timeout, as a scroll view delays it, so a row the finger
                // lands on while starting a scroll does not flash.
                var shown = false
                val reveal = coroutineScope.launch {
                    delay(TapTimeout)
                    shown = true
                    pressIn(offset)
                }
                val released = tryAwaitRelease()
                if (shown) {
                    pressOut(tapped = released)
                } else {
                    reveal.cancel()
                    if (released) {
                        pressIn(offset)
                        pressOut(tapped = true)
                    }
                }
            },
            onLongPress = longClick?.let { action ->
                { _ ->
                    perform(AppHaptic.LongPress)
                    action()
                }
            },
            onTap = { click() },
        )
    }

    private fun click() {
        haptic?.let(::perform)
        onClick()
    }

    private fun perform(haptic: AppHaptic) {
        if (currentValueOf(LocalHapticsEnabled)) currentValueOf(LocalView).performHapticFeedback(haptic.constant())
    }

    private fun pressIn(offset: Offset) {
        val interaction = PressInteraction.Press(offset)
        press = interaction
        emit(interaction)
        val springs = currentValueOf(LocalAppSprings)
        pressAnimation?.cancel()
        pressAnimation = when (effectiveFeedback()) {
            PressFeedback.Scale -> coroutineScope.launch { progress.animateTo(1f, springs.press.unit) }
            PressFeedback.Dim, PressFeedback.Highlight -> coroutineScope.launch { progress.snapTo(1f) }
            PressFeedback.None -> null
        }
    }

    private fun pressOut(tapped: Boolean) {
        val interaction = press ?: return
        press = null
        emit(if (tapped) PressInteraction.Release(interaction) else PressInteraction.Cancel(interaction))
        val springs = currentValueOf(LocalAppSprings)
        pressAnimation?.cancel()
        pressAnimation = when (effectiveFeedback()) {
            PressFeedback.Scale -> coroutineScope.launch {
                // A quick tap is over before the press has shown. Finish the dip first, briefly, so
                // the tap is felt; a held press has already arrived and goes straight to the bounce.
                if (tapped && progress.value < HALF_PRESSED) progress.animateTo(1f, tween(DIP_MILLIS))
                progress.animateTo(0f, springs.release.unit)
            }
            PressFeedback.Dim -> coroutineScope.launch { progress.animateTo(0f, tween(springs.fadeSlow)) }
            PressFeedback.Highlight -> coroutineScope.launch {
                // A tap is shown even when it ends before the press did: the row flashes and fades.
                if (tapped) progress.snapTo(1f)
                progress.animateTo(0f, tween(springs.highlightFadeOut))
            }
            PressFeedback.None -> null
        }
    }

    private fun cancelPress() {
        pressOut(tapped = false)
        keyPressed = false
    }

    private fun emit(interaction: Interaction) {
        externalSource?.tryEmit(interaction)
    }

    /** Scale is motion, so with motion reduced it becomes a dim, which still answers the press. */
    private fun effectiveFeedback(): PressFeedback =
        if (feedback == PressFeedback.Scale && currentValueOf(LocalReduceMotion).value) PressFeedback.Dim else feedback

    private fun onFocusChanged(focused: Boolean) {
        val current = focus
        if (focused && current == null) {
            FocusInteraction.Focus().also {
                focus = it
                emit(it)
            }
        } else if (!focused && current != null) {
            focus = null
            emit(FocusInteraction.Unfocus(current))
        }
        invalidateDraw()
    }

    override fun onDetach() {
        cancelPress()
        focus?.let { emit(FocusInteraction.Unfocus(it)) }
        focus = null
    }

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val value = progress.value
        when (effectiveFeedback()) {
            PressFeedback.Scale -> {
                val scale = lerp(1f, scaleTarget, value)
                scaleX = scale
                scaleY = scale
            }
            PressFeedback.Dim -> {
                alpha = lerp(1f, currentValueOf(LocalAppSprings).pressDimAlpha, value)
                // One glyph or label does not need an offscreen buffer to fade.
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            else -> Unit
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val target = MinTouchTarget.roundToPx()
        val width = constraints.constrainWidth(maxOf(placeable.width, target))
        val height = constraints.constrainHeight(maxOf(placeable.height, target))
        childOffset = IntOffset((width - placeable.width) / 2, (height - placeable.height) / 2)
        childSize = IntSize(placeable.width, placeable.height)
        val springs = currentValueOf(LocalAppSprings)
        scaleTarget = when {
            placeable.height >= LargeSurface.roundToPx() -> springs.pressScaleLarge
            maxOf(placeable.width, placeable.height) <= CompactControl.roundToPx() -> springs.pressScaleCompact
            else -> springs.pressScale
        }
        val animated = feedback == PressFeedback.Scale || feedback == PressFeedback.Dim
        return layout(width, height) {
            if (animated) {
                placeable.placeWithLayer(childOffset.x, childOffset.y, layerBlock = layerBlock)
            } else {
                placeable.place(childOffset.x, childOffset.y)
            }
        }
    }

    // The highlight is drawn here rather than by a delegated RowHighlight node: a node that draws
    // itself is the only draw node Compose visits, so a delegate's draw would never run.
    override fun ContentDrawScope.draw() {
        drawContent()
        if (feedback == PressFeedback.Highlight) {
            val value = progress.value
            if (value > 0f) drawRect(currentValueOf(LocalAppColors).highlight, alpha = value)
        }
        if (focus == null) return
        val inset = FocusRingGap.toPx()
        val stroke = FocusRingWidth.toPx()
        val ringSize = Size(childSize.width + inset * 2, childSize.height + inset * 2)
        val radius = minOf(ringSize.minDimension / 2, FocusRingCorner.toPx())
        drawRoundRect(
            color = currentValueOf(LocalAppColors).focusRing,
            topLeft = Offset(childOffset.x - inset, childOffset.y - inset),
            size = ringSize,
            cornerRadius = CornerRadius(radius),
            style = Stroke(stroke),
        )
    }

    override val shouldMergeDescendantSemantics: Boolean
        get() = true

    override fun SemanticsPropertyReceiver.applySemantics() {
        this@PressableNode.role?.let { role = it }
        onClick(label = onClickLabel) {
            click()
            true
        }
        this@PressableNode.onLongClick?.let { action ->
            onLongClick {
                action()
                true
            }
        }
        if (!enabled) disabled()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!enabled || event.key !in ClickKeys) return false
        return when (event.type) {
            KeyEventType.KeyDown -> {
                if (!keyPressed) {
                    keyPressed = true
                    pressIn(Offset(childSize.width / 2f, childSize.height / 2f))
                }
                true
            }
            KeyEventType.KeyUp -> {
                if (keyPressed) {
                    keyPressed = false
                    pressOut(tapped = true)
                    click()
                }
                true
            }
            else -> false
        }
    }

    override fun onPreKeyEvent(event: KeyEvent): Boolean = false
}

private val MinTouchTarget = 48.dp
private val LargeSurface = 80.dp
private val CompactControl = 56.dp
private val FocusRingWidth = 2.dp
private val FocusRingGap = 3.dp
private val FocusRingCorner = 14.dp
private val ClickKeys = setOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter, Key.Spacebar)
private const val HALF_PRESSED = 0.5f
private const val DIP_MILLIS = 80
private val TapTimeout: Long = ViewConfiguration.getTapTimeout().toLong()
