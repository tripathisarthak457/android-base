package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily

/**
 * The two typefaces the app draws with. This project was generated without downloadable fonts, so
 * both are the platform's own — the app inherits whatever the device ships.
 *
 * ```
 * val brand = FontFamily(
 *     Font(R.font.brand_regular, FontWeight.Normal),
 *     Font(R.font.brand_medium, FontWeight.Medium),
 *     Font(R.font.brand_semibold, FontWeight.SemiBold),
 *     Font(R.font.brand_bold, FontWeight.Bold),
 *     Font(R.font.brand_extrabold, FontWeight.ExtraBold),
 * )
 *
 * AppTheme(fonts = AppFonts(sans = brand, mono = FontFamily.Monospace)) { … }
 * ```
 */
@Immutable
data class AppFonts(
    val sans: FontFamily,
    val mono: FontFamily,
)

/** Kept so the theme's signature matches the downloadable-fonts build. Unused here. */
object AppFontNames {
    const val Sans = "sans-serif"
    const val Mono = "monospace"
}

/** The platform families. */
val PlatformFonts = AppFonts(sans = FontFamily.SansSerif, mono = FontFamily.Monospace)

/** The signature the theme calls, kept identical to the downloadable-fonts build. */
@Suppress("UnusedParameter")
@Composable
fun rememberAppFonts(
    fontName: String = AppFontNames.Sans,
    monoFontName: String = AppFontNames.Mono,
): AppFonts = PlatformFonts
