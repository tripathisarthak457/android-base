package com.base.app.core.designsystem.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.foundation.content.LocalContentColor
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.LocalAppSprings
import com.base.app.core.designsystem.theme.LocalReduceMotion
import kotlinx.coroutines.launch

/**
 * Bounces the element each time [trigger] changes, as SF Symbols' bounce does: a quick dip, then a
 * springy return that peaks just past full size. For a glyph that acknowledges something, such as
 * a bell when a notification arrives or a heart when it is tapped. Nothing bounces the first time,
 * nor under reduce motion. The scale is applied where the element is placed, so a bounce
 * recomposes nothing.
 */
fun Modifier.symbolBounce(trigger: Any?): Modifier = this then SymbolBounceElement(trigger)

/**
 * A glyph that changes with the symbol-replace effect: the old one shrinks away as the new one
 * grows in, both on the emphasis spring. Under reduce motion they only cross-fade.
 */
@Composable
fun AppSymbolReplace(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = AppTheme.sizes.icon,
) {
    val springs = AppTheme.springs
    val reduceMotion = AppTheme.reduceMotion
    AnimatedContent(
        targetState = icon,
        modifier = modifier,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(springs.fadeIn()) togetherWith fadeOut(springs.fadeOut()) using null
            } else {
                (fadeIn(springs.fadeIn()) + scaleIn(springs.emphasis.unit, initialScale = ReplacedScale)) togetherWith
                    (fadeOut(springs.fadeOut()) + scaleOut(springs.emphasis.unit, targetScale = ReplacedScale)) using null
            }
        },
        label = "symbolReplace",
    ) { shown ->
        AppIcon(imageVector = shown, contentDescription = contentDescription, tint = tint, size = size)
    }
}

private data class SymbolBounceElement(val trigger: Any?) : ModifierNodeElement<SymbolBounceNode>() {
    override fun create() = SymbolBounceNode(trigger)

    override fun update(node: SymbolBounceNode) = node.update(trigger)

    override fun InspectorInfo.inspectableProperties() {
        name = "symbolBounce"
        properties["trigger"] = trigger
    }
}

private class SymbolBounceNode(private var trigger: Any?) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private val scale = Animatable(1f)

    fun update(trigger: Any?) {
        if (trigger == this.trigger) return
        this.trigger = trigger
        if (!isAttached || currentValueOf(LocalReduceMotion).value) return
        val springs = currentValueOf(LocalAppSprings)
        coroutineScope.launch {
            scale.animateTo(springs.tabBar.iconBounceScale, springs.iconBounceDown.unit)
            scale.animateTo(1f, springs.iconBounceUp.unit)
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

/** How small the outgoing glyph gets and the incoming one starts. */
private const val ReplacedScale = 0.5f

@AppComponentPreviews
@Composable
private fun AppSymbolReplacePreview() {
    Row(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppSymbolReplace(icon = AppIcons.Heart, contentDescription = "Like")
        AppSymbolReplace(icon = AppIcons.StarFilled, contentDescription = "Starred", tint = AppTheme.colors.warning.base)
        AppSymbolReplace(icon = AppIcons.Check, contentDescription = "Done", size = AppTheme.sizes.iconLarge)
    }
}
