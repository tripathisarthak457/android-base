package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * The type scale, named by role. Fifteen styles rather than a continuum, because a scale a
 * developer has to choose a size from is a scale that grows a sixteenth size the first week.
 */
@Immutable
data class AppTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,

    val headingLarge: TextStyle,
    val headingMedium: TextStyle,
    val headingSmall: TextStyle,

    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,

    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,

    val label: TextStyle,
    val labelSmall: TextStyle,
    val caption: TextStyle,

    val button: TextStyle,
    val mono: TextStyle,
)

private val Metrics = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

@Suppress("DEPRECATION")
private val Platform = PlatformTextStyle(includeFontPadding = false)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
    family: FontFamily,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = Metrics,
    platformStyle = Platform,
)

/*
 * The scale, for a given pair of typefaces. A function rather than a constant, because the typeface
 * is a one-string decision made at the theme — see [AppFonts].
 */
fun appTypography(fonts: AppFonts): AppTypography {
    val sans = fonts.sans
    return AppTypography(
        displayLarge = style(32, 38, FontWeight.ExtraBold, -0.8, sans),
        displayMedium = style(28, 34, FontWeight.ExtraBold, -0.6, sans),
        displaySmall = style(24, 30, FontWeight.Bold, -0.5, sans),

        headingLarge = style(20, 26, FontWeight.Bold, -0.3, sans),
        headingMedium = style(17, 23, FontWeight.Bold, -0.2, sans),
        headingSmall = style(15, 21, FontWeight.Bold, -0.1, sans),

        titleLarge = style(16, 22, FontWeight.SemiBold, 0.0, sans),
        titleMedium = style(14, 20, FontWeight.SemiBold, 0.0, sans),
        titleSmall = style(13, 18, FontWeight.SemiBold, 0.0, sans),

        bodyLarge = style(16, 24, FontWeight.Normal, 0.0, sans),
        bodyMedium = style(14, 21, FontWeight.Normal, 0.0, sans),
        bodySmall = style(13, 19, FontWeight.Normal, 0.0, sans),

        // Uppercase labels need tracking added back — capitals at small sizes crowd each other.
        label = style(12, 16, FontWeight.SemiBold, 0.3, sans),
        labelSmall = style(11, 14, FontWeight.SemiBold, 0.4, sans),
        caption = style(11, 15, FontWeight.Normal, 0.1, sans),

        button = style(15, 20, FontWeight.Bold, -0.1, sans),
        mono = style(13, 18, FontWeight.Medium, 0.0, fonts.mono),
    )
}

/** The scale set in the platform's own typefaces. */
val DefaultTypography: AppTypography = appTypography(PlatformFonts)

internal val LocalAppTypography = staticCompositionLocalOf { DefaultTypography }
