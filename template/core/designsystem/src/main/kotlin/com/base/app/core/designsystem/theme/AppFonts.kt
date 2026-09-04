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
 * ## Changing the whole app's font
 *
 * One string, at the one call to [AppTheme]:
 *
 * ```
 * AppTheme(fontName = "Manrope") { … }
 * ```
 *
 * Every one of the fifteen styles in [AppTypography] is built from it, so nothing else in the
 * project mentions a typeface and there is no per-screen override to find and update. Any family
 * on fonts.google.com works; the name is the one on the family's own page, spelled and cased the
 * same way ("DM Sans", "Plus Jakarta Sans", "Noto Sans").
 *
 * ## Why the name and not a FontFamily
 *
 * A `FontFamily` built at the call site has to repeat the provider, the five weights and the
 * fallback, and every place that does it is a place that can get one of them wrong. A name is the
 * part that actually varies.
 *
 * ## Why downloaded rather than bundled
 *
 * The provider delivers a real, separate file per weight. Bundled *variable* fonts have their
 * weight axis silently ignored on some OEM builds — Xiaomi's among them — which fakes bold by
 * smearing the regular weight and makes the whole design look thin and off-register on exactly
 * the devices you do not have on your desk. It also keeps the APK smaller, and the first render
 * on a cold install falls back to the platform font for a frame rather than failing.
 *
 * To bundle instead: drop the .ttf files into `res/font/`, and pass a hand-built family to
 * [AppTheme] via `fonts = AppFonts(sans = …, mono = …)`. The name parameters are then unused and
 * nothing else changes.
 */
@Immutable
data class AppFonts(
    val sans: FontFamily,
    val mono: FontFamily,
)

/**
 * The defaults.
 *
 * DM Sans is a low-contrast geometric sans with a large x-height: it stays legible at 11sp for a
 * caption and still has enough character at 32sp for a display line, which is the whole ask of a
 * single UI typeface. JetBrains Mono is the monospace companion — its zero is slashed and its
 * `l`/`1`/`I` are unmistakable, which is the entire reason a reference code is set in mono.
 */
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

/**
 * A downloadable family.
 *
 * `bestEffort = true` lets the provider substitute the nearest weight it has rather than
 * returning nothing — a family published without an ExtraBold renders its Bold instead of
 * rendering blank, which is the failure mode that makes downloadable fonts feel unreliable.
 *
 * There is no explicit fallback list: an async font that fails to resolve — no Play Services, a
 * name with a typo, an offline first launch — falls back to the platform typeface through the
 * font resolver, and text renders in the meantime rather than waiting invisibly. That is worth
 * checking after changing the name, because a misspelling degrades quietly to the system font
 * rather than failing loudly.
 */
private fun googleFamily(name: String, weights: List<FontWeight>): FontFamily {
    val font = GoogleFont(name, bestEffort = true)
    return FontFamily(
        weights.map { weight -> Font(googleFont = font, fontProvider = Provider, weight = weight) },
    )
}

/**
 * Resolves the two families for [fontName] and [monoFontName], once per name.
 *
 * `remember`ed on the names because building a family allocates one `Font` per weight and the
 * resolver caches on identity — rebuilding it every recomposition re-resolves eight fonts on
 * every frame that touches the theme.
 */
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
