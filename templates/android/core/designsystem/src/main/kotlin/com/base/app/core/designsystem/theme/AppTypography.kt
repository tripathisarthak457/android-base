package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * The type scale, named by role and sized as iOS Dynamic Type is at its default setting (an
 * Android sp is close to an iOS point). [body] is the text style everything inherits.
 */
@Immutable
data class AppTypography(
    val largeTitle: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subheadline: TextStyle,
    val footnote: TextStyle,
    val caption1: TextStyle,
    val caption2: TextStyle,
    val mono: TextStyle,
) {
    internal val sansRoles: List<TextStyle>
        get() = listOf(largeTitle, title1, title2, title3, headline, body, callout, subheadline, footnote, caption1, caption2)
}

private val Metrics = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double,
    family: FontFamily,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = Metrics,
)

/*
 * The scale for a given pair of typefaces. A function rather than a constant, because the typeface
 * is a one-string decision made at the theme; see [AppFonts]. Large sizes are tracked in, as
 * display cuts are; the smallest are opened up so they stay legible.
 */
fun appTypography(fonts: AppFonts): AppTypography {
    val sans = fonts.sans
    return AppTypography(
        largeTitle = style(34, 41, FontWeight.Bold, -0.6, sans),
        title1 = style(28, 34, FontWeight.Bold, -0.5, sans),
        title2 = style(22, 28, FontWeight.Bold, -0.35, sans),
        title3 = style(20, 25, FontWeight.SemiBold, -0.25, sans),
        headline = style(17, 22, FontWeight.SemiBold, -0.15, sans),
        body = style(17, 22, FontWeight.Normal, -0.15, sans),
        callout = style(16, 21, FontWeight.Normal, -0.1, sans),
        subheadline = style(15, 20, FontWeight.Normal, -0.05, sans),
        footnote = style(13, 18, FontWeight.Normal, 0.0, sans),
        caption1 = style(12, 16, FontWeight.Normal, 0.05, sans),
        caption2 = style(11, 13, FontWeight.Medium, 0.1, sans),
        mono = style(15, 20, FontWeight.Normal, 0.0, fonts.mono),
    )
}

/** The scale set in the platform's own typefaces. */
val DefaultTypography: AppTypography = appTypography(PlatformFonts)

/** The weights of the sans family that [voice] draws with, lightest first: exactly what to download. */
fun sansWeights(voice: TypeVoice): List<FontWeight> =
    DefaultTypography.withVoice(voice).sansRoles.mapNotNull { it.fontWeight }.distinct().sortedBy { it.weight }

/** The weights of the mono family the scale uses. */
val MonoWeights: List<FontWeight> = listOf(FontWeight.Normal)

internal val LocalAppTypography = staticCompositionLocalOf { DefaultTypography }
