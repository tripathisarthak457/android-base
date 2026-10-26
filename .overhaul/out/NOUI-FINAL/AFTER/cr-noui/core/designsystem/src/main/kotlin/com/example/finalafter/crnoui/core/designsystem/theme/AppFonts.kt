package com.example.finalafter.crnoui.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.example.finalafter.crnoui.core.designsystem.R
import com.example.finalafter.crnoui.core.designsystem.theme.tokens.DesignTokens
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout

/**
 * The two typefaces the app draws with: a sans for everything and a mono for codes and figures that
 * are read digit by digit.
 */
@Immutable
data class AppFonts(
    val sans: FontFamily,
    val mono: FontFamily,
)

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
 * Decides once per process whether the theme draws with the families it was given.
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

    /**
     * Fetches the families, waiting at most [budget]. True when the theme will draw with them. A
     * device with no font provider, or one that answers late, gets the platform families; it never
     * fails the launch.
     */
    suspend fun run(resolver: FontFamily.Resolver, budget: Duration = PreloadBudget): Boolean =
        run(budget) { resolver.preload(it) }

    internal suspend fun run(budget: Duration, load: suspend (FontFamily) -> Unit): Boolean {
        chosen?.let { return it === downloaded }
        arrived = try {
            withTimeout(budget) {
                load(downloaded.sans)
                load(downloaded.mono)
            }
            true
        } catch (late: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            false
        } catch (failed: IllegalStateException) {
            // The resolver reports every font it could not fetch this way, the budget running out
            // included: it catches the cancellation and throws IllegalStateException in its place.
            currentCoroutineContext().ensureActive()
            false
        }
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

/**
 * The app's preload: the startup gate runs it, and [AppTheme] draws with its answer. It downloads
 * exactly the weights the type scale uses.
 */
val AppFontPreload = FontPreload(
    AppFonts(
        sans = googleFamily(DesignTokens.Defaults.fontName, SansWeights),
        mono = googleFamily(DesignTokens.Defaults.monoFontName, MonoWeights),
    ),
)

internal fun themeFonts(): AppFonts = AppFontPreload.fonts
