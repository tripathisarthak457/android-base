package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
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

/*
 * The scale for a pair of typefaces, in a style's voice. Only weights move between voices: sizes
 * and line heights stay on the scale, so a style change never reflows a layout, and the fonts
 * download only the weights the voice uses. Large sizes are tracked in, as display cuts are; the
 * smallest are opened up so they stay legible.
 */
fun appTypography(fonts: AppFonts, voice: TypeVoice = TypeVoice.Neutral): AppTypography {
    val sans = fonts.sans
    return with(DesignTokens.Type) {
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
            mono = mono.style(voice, fonts.mono),
        )
    }
}

/** The scale set in the platform's own typefaces. */
val DefaultTypography: AppTypography = appTypography(PlatformFonts)

/** The weights of the sans family that [voice] draws with, lightest first: exactly what to download. */
fun sansWeights(voice: TypeVoice): List<FontWeight> =
    appTypography(PlatformFonts, voice).sansRoles.mapNotNull { it.fontWeight }.distinct().sortedBy { it.weight }

/** The weights of the mono family the scale uses. */
val MonoWeights: List<FontWeight> = listOfNotNull(DefaultTypography.mono.fontWeight)

/** The finest step text sized to fit shrinks by: a quarter point, below what the eye tells apart. */
internal val AutoSizeStep: TextUnit = 0.25.sp

internal val LocalAppTypography = staticCompositionLocalOf { DefaultTypography }
