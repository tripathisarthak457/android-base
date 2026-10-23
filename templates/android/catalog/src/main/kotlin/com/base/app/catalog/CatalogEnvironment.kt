package com.base.app.catalog

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.base.app.core.designsystem.foundation.adaptive.InputTraits
import com.base.app.core.designsystem.foundation.adaptive.WindowPosture
import com.base.app.core.designsystem.theme.AppDesignStyle
import com.base.app.core.designsystem.theme.AppMotionStyle
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode

/**
 * What every page is seen in. It starts from the project's own look, so a generated catalog opens
 * in the style the app ships with, and each value can be changed from the index or set from the
 * launch intent for screenshots: `--es theme dark --ef fontScale 2.0 --ez rtl true`.
 * [prepareSheets] false (`--ez prepareSheets false`) opens the list sheets without preparing them on
 * the press, the path measured against.
 */
@Immutable
data class CatalogEnvironment(
    val themeMode: ThemeMode = ThemeMode.System,
    val designStyle: AppDesignStyle = AppTheme.defaults.designStyle,
    val motionStyle: AppMotionStyle = AppTheme.defaults.motionStyle,
    val rtl: Boolean = false,
    val fontScale: Float = 1f,
    val reduceMotion: Boolean = false,
    val prepareSheets: Boolean = true,
) {
    companion object {
        val FontScales = listOf(1f, 1.3f, 2f)

        fun from(intent: Intent?): CatalogEnvironment {
            val base = CatalogEnvironment()
            intent ?: return base
            return base.copy(
                themeMode = when (intent.getStringExtra("theme")) {
                    "light" -> ThemeMode.Light
                    "dark" -> ThemeMode.Dark
                    else -> base.themeMode
                },
                designStyle = AppDesignStyle.entries.firstOrNull { it.name == intent.getStringExtra("style") } ?: base.designStyle,
                motionStyle = AppMotionStyle.entries.firstOrNull { it.name == intent.getStringExtra("motion") } ?: base.motionStyle,
                rtl = intent.getBooleanExtra("rtl", base.rtl),
                fontScale = intent.getFloatExtra("fontScale", base.fontScale),
                reduceMotion = intent.getBooleanExtra("reduceMotion", base.reduceMotion),
                prepareSheets = intent.getBooleanExtra("prepareSheets", base.prepareSheets),
            )
        }

        val Saver: Saver<CatalogEnvironment, List<Any>> = Saver(
            save = {
                listOf(it.themeMode.name, it.designStyle.name, it.motionStyle.name, it.rtl, it.fontScale, it.reduceMotion, it.prepareSheets)
            },
            restore = {
                CatalogEnvironment(
                    themeMode = ThemeMode.valueOf(it[0] as String),
                    designStyle = AppDesignStyle.valueOf(it[1] as String),
                    motionStyle = AppMotionStyle.valueOf(it[2] as String),
                    rtl = it[3] as Boolean,
                    fontScale = it[4] as Float,
                    reduceMotion = it[5] as Boolean,
                    prepareSheets = it[6] as Boolean,
                )
            },
        )
    }
}

/** The environment the page being shown is in, for a demo that re-themes part of a page. */
val LocalCatalogEnvironment = staticCompositionLocalOf { CatalogEnvironment() }

/**
 * The theme as [environment] describes it, around [content]. A demo passes [posture] or [input] to
 * simulate a foldable or a keyboard for what it holds.
 */
@Composable
fun ProvideEnvironment(
    environment: CatalogEnvironment,
    posture: WindowPosture = AppTheme.posture,
    input: InputTraits = AppTheme.input,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalCatalogEnvironment provides environment,
        LocalLayoutDirection provides if (environment.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalDensity provides Density(density.density, environment.fontScale),
    ) {
        AppTheme(
            mode = environment.themeMode,
            designStyle = environment.designStyle,
            motionStyle = environment.motionStyle,
            reduceMotion = environment.reduceMotion,
            posture = posture,
            input = input,
            content = content,
        )
    }
}
