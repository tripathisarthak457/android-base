package com.base.app.core.designsystem.foundation.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.foundation.interaction.collectHeld
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * What a piece of drawn glass does under a finger, as the Kyant catalog's `LiquidButton` does it:
 * it swells a little, slides after a finger that moves on it (no further than a hyperbolic
 * tangent lets it, so it never leaves the finger's reach), stretches along that pull, and glows
 * where the finger is. Glass inside a screen samples nothing (it would sample a recording of
 * itself), so this is the whole of its liquid feel.
 *
 * [strength] and [touch] drive [pressGlow]; [liquidPress] applies the rest in its layer. The press
 * is taken from the control's own interaction source, which `pressable` feeds after the tap
 * timeout, so a finger that lands to start a scroll never swells the control, and a quick tap
 * still shows a whole swell before the spring back.
 */
@Stable
internal class LiquidPressState {

    internal val progress = Animatable(0f)
    internal val recoil = Animatable(Offset.Zero, Offset.VectorConverter)
    internal var held by mutableStateOf(false)
        private set

    // Where the finger landed, in the control's coordinates, and how far it has pulled since. The
    // pointer pass writes them; the layer and draw phases read them.
    internal var down = Offset.Zero
    internal var finger by mutableStateOf(Offset.Zero)

    /** 0 at rest, 1 fully pressed. Read it in draw or in a layer block. */
    val strength: Float
        get() = progress.value

    /** How far the finger has pulled the control: the live pull, or its recoil once let go. */
    val pull: Offset
        get() = if (held) finger else recoil.value

    /** Where the glow pools: the finger, trailing back to where it landed once let go. */
    val touch: Offset
        get() = down + pull

    /** Follows [source] until cancelled: a hold swells the control, letting go releases it. */
    suspend fun follow(source: InteractionSource, press: SpringSpec<Float>, release: SpringSpec<Float>) {
        val recoilSpring = spring(release.dampingRatio, release.stiffness, Offset.VisibilityThreshold)
        coroutineScope {
            var motion: Job? = null
            source.collectHeld { pressed ->
                held = pressed
                motion?.cancel()
                motion = launch {
                    if (pressed) {
                        recoil.snapTo(Offset.Zero)
                        progress.animateTo(1f, press)
                    } else {
                        recoil.snapTo(finger)
                        launch { recoil.animateTo(Offset.Zero, recoilSpring) }
                        // A quick tap is over before the swell has shown: finish it first, briefly,
                        // so the tap is felt.
                        if (progress.value < HalfPressed) progress.animateTo(1f, tween(DipMillis))
                        progress.animateTo(0f, release)
                    }
                }
            }
        }
    }

    private companion object {
        const val HalfPressed = 0.5f
        const val DipMillis = 60
    }
}

/** A [LiquidPressState] following [source] on the theme's press and release springs. */
@Composable
internal fun rememberLiquidPress(source: InteractionSource): LiquidPressState {
    val state = remember { LiquidPressState() }
    val springs = AppTheme.springs
    LaunchedEffect(state, source, springs) { state.follow(source, springs.press.unit, springs.release.unit) }
    return state
}

/**
 * Swells, slides and stretches this control with [state], in its layer, and keeps [state] told
 * where the finger is. Put it outside the glass body, so the body moves with the control; put
 * [pressGlow] inside it.
 */
internal fun Modifier.liquidPress(state: LiquidPressState): Modifier = this then LiquidPressElement(state)

private data class LiquidPressElement(val state: LiquidPressState) : ModifierNodeElement<LiquidPressNode>() {
    override fun create() = LiquidPressNode(state)

    override fun update(node: LiquidPressNode) {
        node.state = state
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidPress"
    }
}

private class LiquidPressNode(var state: LiquidPressState) : Modifier.Node(), LayoutModifierNode, PointerInputModifierNode {

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val state = state
        val strength = state.strength
        val pull = state.pull
        val height = size.height
        if (height > 0f && size.width > 0f) {
            val swell = 1f + PressSwell.toPx() / height * strength
            // The slide eases toward the control's shorter side and never reaches it.
            val reach = size.minDimension
            translationX = reach * tanh(PullRate * pull.x / reach)
            translationY = reach * tanh(PullRate * pull.y / reach)
            // Pulled, the glass stretches along the pull, by as much as it swelled, and the
            // narrower side gives less.
            val stretch = PressSwell.toPx() / height
            val angle = atan2(pull.y, pull.x)
            scaleX = swell + stretch * abs(cos(angle) * pull.x / size.maxDimension) * (size.width / height).coerceAtMost(1f)
            scaleY = swell + stretch * abs(sin(angle) * pull.y / size.maxDimension) * (height / size.width).coerceAtMost(1f)
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    // Watched on the initial pass and never consumed: the control's own tap handling decides what
    // a touch means; this only follows where the finger is.
    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial) return
        val change = pointerEvent.changes.firstOrNull() ?: return
        when (pointerEvent.type) {
            PointerEventType.Press -> {
                state.down = change.position
                state.finger = Offset.Zero
            }
            PointerEventType.Move -> if (change.pressed) state.finger = change.position - state.down
            else -> Unit
        }
    }

    override fun onCancelPointerInput() = Unit
}

/** The catalog's press: the control grows by this over its height, and stretches by as much. */
private val PressSwell = 4.dp

/** The catalog's slide: a pull moves the control a twentieth as far at first, then ever less. */
private const val PullRate = 0.05f
