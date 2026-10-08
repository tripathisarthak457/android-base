package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.base.app.core.designsystem.R

/**
 * The two typefaces the app draws with, chosen by name.
 *
 * ```
 * AppTheme(fontName = "Manrope") { … }
 * ```
 */
@Immutable
data class AppFonts(
    val sans: FontFamily,
    val mono: FontFamily,
)

/** The defaults. */
object AppFontNames {
    const val Sans = "DM Sans"
    const val Mono = "JetBrains Mono"
}

private val Provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/** The weights [AppTypography] actually asks for. Requesting more downloads files nothing uses. */
private val SansWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
)

private val MonoWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
)

/** A downloadable family. */
private fun googleFamily(name: String, weights: List<FontWeight>): FontFamily {
    val font = GoogleFont(name, bestEffort = true)
    return FontFamily(
        weights.map { weight -> Font(googleFont = font, fontProvider = Provider, weight = weight) },
    )
}

/** Resolves the two families for [fontName] and [monoFontName], once per name. */
@Composable
fun rememberAppFonts(
    fontName: String = AppFontNames.Sans,
    monoFontName: String = AppFontNames.Mono,
): AppFonts = remember(fontName, monoFontName) {
    AppFonts(
        sans = googleFamily(fontName, SansWeights),
        mono = googleFamily(monoFontName, MonoWeights),
    )
}

/** What a preview, a test, or a first frame draws with before a downloaded family resolves. */
val PlatformFonts = AppFonts(sans = FontFamily.SansSerif, mono = FontFamily.Monospace)
