package com.example.finalafter.crui3.core.designsystem.foundation.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui3.core.designsystem.theme.AppColors
import com.example.finalafter.crui3.core.designsystem.theme.AppTheme
import com.kyant.shapes.Capsule

/** The values glass is drawn with, for the app's theme. */
object GlassDefaults {

    /**
     * The tab bar's capsule, its accessory's and its trailing button's, with continuous corners. A
     * continuous corner is a path rather than a round rect, which costs more to clip, so smaller
     * glass keeps circular corners.
     */
    val ChromeShape: Shape = Capsule()

    /**
     * How far chrome blurs the scene. Larger radii read as frosted rather than glass and cost
     * more.
     */
    val ChromeBlur: Dp = 10.dp

    /** The rim's width, inside the shape's edge. */
    val RimWidth: Dp = 0.75.dp

    /** Content glass: a card, a chip, a field, a button over content. */
    @Composable
    @ReadOnlyComposable
    fun contentColors(): GlassColors {
        val colors = AppTheme.colors
        return GlassColors(
            body = colors.surface.copy(alpha = if (colors.isLight) 0.62f else 0.72f),
            rim = rim(colors),
            sheen = colors.sheen.copy(alpha = if (colors.isLight) 0.45f else 0.10f),
        )
    }

    /** Chrome over the blurred scene: a light tint, so the content under the bar shows through. */
    @Composable
    @ReadOnlyComposable
    fun chromeColors(): GlassColors {
        val colors = AppTheme.colors
        return GlassColors(
            body = colors.surface.copy(alpha = if (colors.isLight) 0.40f else 0.35f),
            rim = rim(colors),
            sheen = Color.Transparent,
        )
    }

    /** Chrome that samples nothing: nearly opaque, so its labels read over any content. */
    @Composable
    @ReadOnlyComposable
    fun frostedColors(): GlassColors {
        val colors = AppTheme.colors
        return GlassColors(
            body = colors.surface.copy(alpha = if (colors.isLight) 0.92f else 0.94f),
            rim = rim(colors),
            sheen = colors.sheen.copy(alpha = if (colors.isLight) 0.30f else 0.06f),
        )
    }

    /**
     * The shadow under floating chrome; none in dark theme, where it would not show and would still
     * cost its blur.
     */
    @Composable
    @ReadOnlyComposable
    fun chromeShadow(): Shadow? = if (AppTheme.colors.isLight) ChromeShadow else null

    /** The shadow under content glass; none in dark theme. */
    @Composable
    @ReadOnlyComposable
    fun contentShadow(): Shadow? = if (AppTheme.colors.isLight) ContentShadow else null

    private fun rim(colors: AppColors): Color = colors.sheen.copy(alpha = if (colors.isLight) 0.85f else 0.20f)

    private val ChromeShadow = Shadow(radius = 16.dp, offset = DpOffset(0.dp, 4.dp), alpha = 0.10f)
    private val ContentShadow = Shadow(radius = 10.dp, offset = DpOffset(0.dp, 3.dp), alpha = 0.08f)
}
