package com.example.finalafter.crui2.core.designsystem.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.example.finalafter.crui2.core.designsystem.theme.AppColors
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.designsystem.theme.BarIndicator
import com.example.finalafter.crui2.core.designsystem.theme.BarTreatment

/** What a treatment decides: whether the bar floats as a capsule, its colours and its indicator. */
@Immutable
internal class BarLook(
    val floating: Boolean,
    val surface: Color,
    val border: Color,
    val shadow: Boolean,
    val shape: Shape,
    val indicator: Color,
    /** A short rule over the selected tab instead of a pill. */
    val rule: Boolean,
    val selectedContent: Color,
    val content: Color,
    val uppercaseLabels: Boolean,
)

@Composable
internal fun barLook(treatment: BarTreatment): BarLook {
    val colors = AppTheme.colors
    val shape = AppTheme.shapes.pill
    val indicator = AppTheme.style.indicator
    val uppercase = AppTheme.style.uppercaseLabels
    return remember(treatment, indicator, colors, shape, uppercase) {
        barLookFor(treatment, indicator, colors, shape, uppercase)
    }
}

/*
 * The selected tab is tinted, as iOS tints it, rather than filled with the accent: a saturated pill
 * nearly the bar's height crowds it. A style whose indicator is a pill keeps a solid one in
 * barIndicator. Floating and Translucent are both the capsule; Translucent lets the content show
 * faintly through its surface. In dark theme a capsule sits on the elevated surface with a faint
 * light outline, since the same colour as the cards under it would merge with them, and it casts no
 * shadow, which would be invisible there and still cost a blur.
 */
private fun barLookFor(
    treatment: BarTreatment,
    indicator: BarIndicator,
    colors: AppColors,
    shape: Shape,
    uppercase: Boolean,
): BarLook {
    val floating = treatment != BarTreatment.Docked
    val raisedInDark = floating && !colors.isLight
    val solid = indicator == BarIndicator.Pill
    val surface = if (raisedInDark) colors.surfaceElevated else colors.surface
    return BarLook(
        floating = floating,
        surface = if (treatment == BarTreatment.Translucent) surface.copy(alpha = TranslucentAlpha) else surface,
        border = if (raisedInDark) colors.sheen.copy(alpha = AppTabBarDefaults.DarkOutlineAlpha) else colors.separator,
        shadow = floating && colors.isLight,
        shape = shape,
        indicator = if (solid) colors.barIndicator else colors.accentSubtle,
        rule = false,
        selectedContent = if (solid) colors.onAccent else colors.accentContent,
        content = colors.contentSecondary,
        uppercaseLabels = uppercase,
    )
}

/** The Translucent capsule's surface opacity (WP03's BarTreatment.Translucent). */
private const val TranslucentAlpha = 0.88f
