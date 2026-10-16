package com.base.app.core.designsystem.component.sheet.stack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.lerp
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/**
 * The card stack's numbers (§6.4 of the sheet specification): how far the presenter behind a sheet
 * recedes at expansion `e`, and the one dim drawn over it. Pure, so the dim's maths is tested
 * against the two-rect composite it replaces.
 */
internal object CardStackMath {

    /** The presenter's scale about its top centre: 1 upright, 0.9165 behind a sheet at Large. */
    fun scale(expansion: Float): Float = 1f - (1f - DesignTokens.Sheet.CardStack.scale) * expansion

    /** How far the presenter's top moves down: to the safe area's top, 10 dp above the sheet. */
    fun translationY(expansion: Float, safeTop: Float): Float = safeTop * expansion

    /**
     * The presenter's corner: from the display's own (scaled down a little, so the card reads as
     * inside the screen) to the card's at Large. A display with corners under the minimum starts
     * square.
     */
    fun corner(expansion: Float, displayCorner: Float, density: Float): Float {
        val minimum = DesignTokens.Sheet.CardStack.deviceCornerMin.value * density
        val start = if (displayCorner >= minimum) DesignTokens.Sheet.CardStack.deviceCornerFactor * displayCorner else 0f
        return lerp(start, DesignTokens.Sheet.CardStack.corner.value * density, expansion)
    }

    /**
     * The one rect over the presenter: the exact composite of the card overlay ([overlay] at
     * [overlayAlpha], weighted by expansion [lift]) under the sheet's [scrim] (its alpha weighted by
     * presentation [presence]). Its alpha `1 − (1 − a₁)(1 − a₂)` never falls as the sheet rises, so
     * the dim never lightens on the way to Large. Transparent when there is nothing to draw.
     */
    fun mergedDim(presence: Float, lift: Float, overlay: Color, overlayAlpha: Float, scrim: Color): Color {
        val a1 = overlayAlpha * lift.coerceIn(0f, 1f)
        val a2 = scrim.alpha * presence.coerceIn(0f, 1f)
        val alpha = 1f - (1f - a1) * (1f - a2)
        if (alpha <= 0f) return Color.Transparent
        val overlayWeight = a1 * (1f - a2) / alpha
        val scrimWeight = a2 / alpha
        return Color(
            red = overlay.red * overlayWeight + scrim.red * scrimWeight,
            green = overlay.green * overlayWeight + scrim.green * scrimWeight,
            blue = overlay.blue * overlayWeight + scrim.blue * scrimWeight,
            alpha = alpha,
        )
    }
}
