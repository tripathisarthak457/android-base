package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily

/**
 * The two typefaces the app draws with.
 *
 * This project was generated without downloadable fonts, so both are the platform's own — the app
 * inherits whatever the device ships. That is a perfectly good default and it costs nothing to
 * download.
 *
 * ## Using a specific typeface
 *
 * Drop the `.ttf` files into `res/font/` and build the families from them:
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
 *
 * Every one of the fifteen styles in [AppTypography] picks it up; nothing else changes. Prefer
 * separate files per weight over a single variable font: some OEM builds ignore the weight axis
 * and fake bold by smearing the regular weight.
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

@Composable
fun rememberAppFonts(
    fontName: String = AppFontNames.Sans,
    monoFontName: String = AppFontNames.Mono,
): AppFonts = PlatformFonts
