package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import com.base.app.core.designsystem.theme.tokens.DesignTokens

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

private fun DesignTokens.TypeRole.style(family: FontFamily) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight(weight),
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = tracking,
    lineHeightStyle = Metrics,
)

/*
 * The scale for a pair of typefaces. Large sizes are tracked in, as display cuts are; the smallest
 * are opened up so they stay legible. Each role's size, line height, tracking and weight are in
 * `DesignTokens.Type`.
 */
fun appTypography(fonts: AppFonts): AppTypography {
    val sans = fonts.sans
    return with(DesignTokens.Type) {
        AppTypography(
            largeTitle = largeTitle.style(sans),
            title1 = title1.style(sans),
            title2 = title2.style(sans),
            title3 = title3.style(sans),
            headline = headline.style(sans),
            body = body.style(sans),
            callout = callout.style(sans),
            subheadline = subheadline.style(sans),
            footnote = footnote.style(sans),
            caption1 = caption1.style(sans),
            caption2 = caption2.style(sans),
            mono = mono.style(fonts.mono),
        )
    }
}

/** The scale set in the platform's own typefaces. */
val DefaultTypography: AppTypography = appTypography(PlatformFonts)

/** The weights of the sans family the scale draws with, lightest first: exactly what to download. */
val SansWeights: List<FontWeight> =
    DefaultTypography.sansRoles.mapNotNull { it.fontWeight }.distinct().sortedBy { it.weight }

/** The weights of the mono family the scale uses. */
val MonoWeights: List<FontWeight> = listOfNotNull(DefaultTypography.mono.fontWeight)

internal val LocalAppTypography = staticCompositionLocalOf { DefaultTypography }
