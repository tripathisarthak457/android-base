package com.example.everything.core.designsystem.component.sheet.stack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.lerp
import com.example.everything.core.designsystem.theme.tokens.DesignTokens

/**
 * The card stack's numbers: how far the presenter behind a sheet
 * recedes at expansion `e`, and the one dim drawn over it.
 */
internal object CardStackMath {

    /** The presenter's scale about its top centre: 1 upright, 0.9165 behind a sheet at Large. */
    fun scale(expansion: Float): Float = 1f - (1f - DesignTokens.Sheet.CardStack.scale) * expansion

    /** How far the presenter's top moves down: to the safe area's top, 10 dp above the sheet's. */
    fun translationY(expansion: Float, safeTop: Float): Float = safeTop * expansion

    /**
     * The presenter's corner: from the display's own, a little smaller so the card reads as inside
     * the screen, to the card's at Large. A display whose corners are under the minimum starts square.
     */
    fun corner(expansion: Float, displayCorner: Float, density: Float): Float {
        val minimum = DesignTokens.Sheet.CardStack.deviceCornerMin.value * density
        val start = if (displayCorner >= minimum) DesignTokens.Sheet.CardStack.deviceCornerFactor * displayCorner else 0f
        return lerp(start, DesignTokens.Sheet.CardStack.corner.value * density, expansion)
    }

    /** The scale of a sheet covered by another, about its top centre, at the covering sheet's expansion. */
    fun coveredScale(expansion: Float): Float = 1f - (1f - DesignTokens.Sheet.Stack.coveredScale) * expansion

    /** How far a covered sheet lifts, at the covering sheet's expansion, in a bounds of [boundsHeight]. */
    fun coveredLift(expansion: Float, boundsHeight: Float): Float = DesignTokens.Sheet.Stack.coveredLift * boundsHeight * expansion
}

/**
 * The dims that reach one presenter, merged into one rect: each layer folded over the ones beneath
 * it with the "over" operator, so the rect has exactly the colour of drawing them one by one. Its
 * alpha `1 − Π(1 − aᵢ)` never falls as a layer strengthens, so the dim never lightens as a sheet
 * rises. Held in place and reset per frame: nothing is allocated while drawing.
 */
internal class DimFold {
    private var red = 0f
    private var green = 0f
    private var blue = 0f
    private var alpha = 0f

    fun reset() {
        red = 0f
        green = 0f
        blue = 0f
        alpha = 0f
    }

    /** Draws [color]'s red, green and blue at [layerAlpha] over what is folded so far. */
    fun over(color: Color, layerAlpha: Float) {
        val a = layerAlpha.coerceIn(0f, 1f)
        if (a <= 0f) return
        val below = alpha
        val combined = 1f - (1f - below) * (1f - a)
        val keep = below * (1f - a) / combined
        val add = a / combined
        red = red * keep + color.red * add
        green = green * keep + color.green * add
        blue = blue * keep + color.blue * add
        alpha = combined
    }

    fun color(): Color = if (alpha <= 0f) Color.Transparent else Color(red, green, blue, alpha)
}
