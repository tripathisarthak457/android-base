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
 *
 * Set in the platform's own typeface, Roboto on Android and SF on iOS: iOS has no downloadable
 * fonts, and bundling a family's weights costs about a megabyte. A custom face goes in
 * `composeResources/font/` and replaces [FontFamily.Default] here.
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
)

private val Metrics = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun DesignTokens.TypeRole.style(voice: TypeVoice, family: FontFamily) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight(
        when (voice) {
            TypeVoice.Neutral -> weightNeutral
            TypeVoice.Bold -> weightBold
            TypeVoice.Editorial -> weightEditorial
            TypeVoice.Rounded -> weightRounded
        },
    ),
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = tracking,
    lineHeightStyle = Metrics,
)

/**
 * The scale in a style's voice. Only weights move between voices: sizes and line heights stay on
 * the scale, so a style change never reflows a layout.
 */
fun appTypography(voice: TypeVoice = TypeVoice.Neutral): AppTypography = with(DesignTokens.Type) {
    val sans = FontFamily.Default
    AppTypography(
        largeTitle = largeTitle.style(voice, sans),
        title1 = title1.style(voice, sans),
        title2 = title2.style(voice, sans),
        title3 = title3.style(voice, sans),
        headline = headline.style(voice, sans),
        body = body.style(voice, sans),
        callout = callout.style(voice, sans),
        subheadline = subheadline.style(voice, sans),
        footnote = footnote.style(voice, sans),
        caption1 = caption1.style(voice, sans),
        caption2 = caption2.style(voice, sans),
        mono = mono.style(voice, FontFamily.Monospace),
    )
}

private val TypographyByVoice = TypeVoice.entries.associateWith(::appTypography)

internal fun TypeVoice.typography(): AppTypography = TypographyByVoice.getValue(this)

internal val LocalAppTypography = staticCompositionLocalOf { TypeVoice.Neutral.typography() }
