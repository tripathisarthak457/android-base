package com.example.finalafter.crui3.core.designsystem.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import com.example.finalafter.crui3.core.designsystem.theme.AppTheme
import com.example.finalafter.crui3.core.designsystem.theme.LocalAppSprings
import com.example.finalafter.crui3.core.designsystem.theme.LocalReduceMotion
import kotlinx.coroutines.launch

/**
 * A number that runs up to [target] the first time it is shown and to each new value after, so a
 * count that changes while someone watches reads as having changed. It starts from zero only once
 * per saved state: coming back to a screen does not replay it.
 */
@Composable
fun rememberCountUp(target: Int): Int {
    val springs = AppTheme.springs
    val reduceMotion = AppTheme.reduceMotion
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val shown by animateIntAsState(
        targetValue = if (started || reduceMotion) target else 0,
        animationSpec = if (reduceMotion) snap() else springs.arrival(springs.fadeSlow * 2),
        label = "countUp",
    )
    return shown
}

/**
 * A small overshoot each time [on] turns true (a mark taking, a choice landing) and nothing when
 * the element is first drawn already on. Applied where the content is placed, so the pop redraws
 * the element and recomposes nothing.
 */
fun Modifier.popWhenOn(on: Boolean): Modifier = this then PopWhenOnElement(on)

private data class PopWhenOnElement(val on: Boolean) : ModifierNodeElement<PopWhenOnNode>() {
    override fun create() = PopWhenOnNode(on)

    override fun update(node: PopWhenOnNode) = node.update(on)

    override fun InspectorInfo.inspectableProperties() {
        name = "popWhenOn"
        properties["on"] = on
    }
}

private class PopWhenOnNode(private var on: Boolean) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private val scale = Animatable(1f)

    fun update(on: Boolean) {
        val turnedOn = on && !this.on
        this.on = on
        if (!turnedOn || !isAttached || currentValueOf(LocalReduceMotion).value) return
        val springs = currentValueOf(LocalAppSprings)
        coroutineScope.launch {
            scale.snapTo(springs.pressScale)
            scale.animateTo(1f, springs.emphasis.unit)
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale.value
                scaleY = scale.value
            }
        }
    }
}
