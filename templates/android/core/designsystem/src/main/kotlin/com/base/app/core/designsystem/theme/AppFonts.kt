package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.base.app.core.designsystem.R
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The two typefaces the app draws with: a sans for everything and a mono for codes and figures that
 * are read digit by digit.
 */
@Immutable
data class AppFonts(
    val sans: FontFamily,
    val mono: FontFamily,
)

/** The Google Fonts families the app downloads. */
object AppFontNames {
    const val Sans = "DM Sans"
    const val Mono = "JetBrains Mono"
}

/** What a preview, a test, or a process whose fonts did not arrive in time draws with. */
val PlatformFonts = AppFonts(sans = FontFamily.SansSerif, mono = FontFamily.Monospace)

private val Provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun googleFamily(name: String, weights: List<FontWeight>): FontFamily {
    val font = GoogleFont(name, bestEffort = true)
    return FontFamily(weights.map { weight -> Font(googleFont = font, fontProvider = Provider, weight = weight) })
}

/**
 * Decides once per process whether the theme draws with the downloaded families.
 *
 * A downloaded family renders the platform font first and swaps when the file arrives, reflowing
 * text someone is already reading. So the families are fetched while the splash is up, and used
 * only if that finished inside the budget. Otherwise the platform families stay for the whole
 * process, and the downloaded ones, cached on the device by then, apply from the next cold start.
 * Main thread only.
 */
class FontPreload(private val downloaded: AppFonts) {
    private var arrived = false
    private var chosen: AppFonts? = null

    /** Fetches the families, waiting at most [budget]. True when the theme will draw with them. */
    suspend fun run(resolver: FontFamily.Resolver, budget: Duration = PreloadBudget): Boolean =
        run(budget) { resolver.preload(it) }

    internal suspend fun run(budget: Duration, load: suspend (FontFamily) -> Unit): Boolean {
        chosen?.let { return it === downloaded }
        arrived = withTimeoutOrNull(budget) {
            load(downloaded.sans)
            load(downloaded.mono)
        } != null
        return arrived
    }

    /** The families to draw with, fixed the first time anything asks. */
    val fonts: AppFonts
        get() = chosen ?: (if (arrived) downloaded else PlatformFonts).also { chosen = it }
}

/**
 * The longest the splash waits for fonts. Past this, a slower network costs a launch in the
 * platform font rather than a blank screen.
 */
val PreloadBudget: Duration = 150.milliseconds

/** The app's preload: the startup gate runs it, and [AppTheme] draws with its answer. */
val AppFontPreload = FontPreload(
    AppFonts(
        sans = googleFamily(AppFontNames.Sans, sansWeights(AppTheme.defaults.designStyle.style().voice)),
        mono = googleFamily(AppFontNames.Mono, MonoWeights),
    ),
)

internal fun themeFonts(): AppFonts = AppFontPreload.fonts
