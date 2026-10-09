package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The two typefaces the app draws with. This project was generated without downloadable fonts, so
 * both are the platform's own: the app inherits whatever the device ships.
 *
 * ```
 * val brand = FontFamily(
 *     Font(R.font.brand_regular, FontWeight.Normal),
 *     Font(R.font.brand_medium, FontWeight.Medium),
 *     Font(R.font.brand_semibold, FontWeight.SemiBold),
 *     Font(R.font.brand_bold, FontWeight.Bold),
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

/** The platform families' names, so the theme's defaults read the same as a downloadable-fonts build. */
object AppFontNames {
    const val Sans = "sans-serif"
    const val Mono = "monospace"
}

/** The platform families. */
val PlatformFonts = AppFonts(sans = FontFamily.SansSerif, mono = FontFamily.Monospace)

/**
 * Decides once per process which families the theme draws with. With platform fonts only there is
 * nothing to download, so the startup gate's call returns at once; the class stays so that a project
 * which bundles or downloads a family later gets the no-reflow behaviour by passing it here.
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

/** The longest the splash waits for fonts. */
val PreloadBudget: Duration = 150.milliseconds

/** The app's preload: the startup gate runs it, and [AppTheme] draws with its answer. */
val AppFontPreload = FontPreload(PlatformFonts)

internal fun themeFonts(): AppFonts = AppFontPreload.fonts
