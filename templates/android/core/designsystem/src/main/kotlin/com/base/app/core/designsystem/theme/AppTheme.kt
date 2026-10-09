package com.base.app.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.base.app.core.designsystem.component.feedback.LocalShimmer
import com.base.app.core.designsystem.component.feedback.ShimmerClock
import com.base.app.core.designsystem.component.overlay.AppOverlayHost
import com.base.app.core.designsystem.component.overlay.LocalOverlayHost
import com.base.app.core.designsystem.foundation.ChromeInset
import com.base.app.core.designsystem.foundation.LocalBottomChrome
import com.base.app.core.designsystem.foundation.LocalContentColor
import com.base.app.core.designsystem.foundation.LocalHapticsEnabled
import com.base.app.core.designsystem.foundation.LocalTextStyle
import com.base.app.core.designsystem.foundation.NoIndication

/** Which palette to render with. [System] follows the device; the other two override it. */
enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    @Composable
    @ReadOnlyComposable
    fun isDark(): Boolean = when (this) {
        System -> isSystemInDarkTheme()
        Light -> false
        Dark -> true
    }
}

/** The look the project was generated with. The app's root and the catalog both start from it. */
@Immutable
class AppThemeDefaults internal constructor(
    val designStyle: AppDesignStyle,
    val motionStyle: AppMotionStyle,
    val hapticsEnabled: Boolean,
    val fontName: String,
    val monoFontName: String,
)

/**
 * The single theme wrapper. Everything the app draws sits inside one of these; the outermost also
 * installs the window's overlay host and bottom-chrome holder, which nested themes share.
 *
 * Nothing in the app draws a press overlay unless a component asks for one: the default indication
 * is [NoIndication], and each control states its feedback through `Modifier.pressable`.
 *
 * [reduceMotion] overrides the system's "Remove animations" setting, for previews and the catalog;
 * left null, the setting is followed as it changes.
 */
@Composable
fun AppTheme(
    mode: ThemeMode = ThemeMode.System,
    designStyle: AppDesignStyle = AppTheme.defaults.designStyle,
    motionStyle: AppMotionStyle = AppTheme.defaults.motionStyle,
    hapticsEnabled: Boolean = AppTheme.defaults.hapticsEnabled,
    colors: AppColors? = null,
    fonts: AppFonts = themeFonts(),
    reduceMotion: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val style = remember(designStyle) { designStyle.style() }
    val dark = mode.isDark()
    // Explicit colours are taken as given; the style only restyles the palette it chose itself.
    val resolved = colors ?: remember(dark, style.surfaces) {
        (if (dark) DarkColors else LightColors).withSurfaces(style.surfaces)
    }
    val typography = remember(fonts, style.voice) { appTypography(fonts).withVoice(style.voice) }
    val selectionColors = remember(resolved) {
        TextSelectionColors(
            handleColor = resolved.accent,
            // Low enough alpha that selected text stays readable through the selection.
            backgroundColor = resolved.accent.copy(alpha = 0.28f),
        )
    }
    val systemReduceMotion = rememberSystemReduceMotion()
    val reduceMotionState: State<Boolean> =
        if (reduceMotion == null) systemReduceMotion else remember(reduceMotion) { mutableStateOf(reduceMotion) }

    val windowSize = rememberAppWindowSize()
    val layout = remember(windowSize) { AppLayout.forWindow(windowSize) }
    // The gutter every screen already uses follows the window, so margins grow on large screens
    // without any screen asking.
    val spacing = remember(layout) { AppSpacing(gutter = layout.gutter) }

    val outermost = LocalOverlayHost.current == null
    val bottomChrome = if (outermost) remember { ChromeInset() } else LocalBottomChrome.current
    val scope = rememberCoroutineScope()
    val shimmer = if (outermost) remember { ShimmerClock(scope) } else LocalShimmer.current

    CompositionLocalProvider(
        LocalAppColors provides resolved,
        LocalAppTypography provides typography,
        LocalAppSpacing provides spacing,
        LocalAppWindowSize provides windowSize,
        LocalAppLayout provides layout,
        LocalAppShapes provides style.shapes,
        LocalAppStyle provides style,
        LocalAppSprings provides motionStyle.springs(),
        LocalReduceMotion provides reduceMotionState,
        LocalContentColor provides resolved.contentPrimary,
        LocalTextStyle provides typography.body,
        LocalTextSelectionColors provides selectionColors,
        LocalIndication provides NoIndication,
        LocalHapticsEnabled provides hapticsEnabled,
        LocalBottomChrome provides bottomChrome,
        LocalShimmer provides shimmer,
    ) {
        if (outermost) AppOverlayHost(content) else content()
    }
}

/**
 * The accessor every component and screen reads tokens through: `AppTheme.colors.accent`,
 * `AppTheme.spacing.lg`, `AppTheme.springs.release`.
 */
object AppTheme {

    /** The design style, motion style, haptics and fonts the project was generated with. */
    val defaults = AppThemeDefaults(
        designStyle = AppDesignStyle.Utility,
        motionStyle = AppMotionStyle.Standard,
        hapticsEnabled = true,
        fontName = AppFontNames.Sans,
        monoFontName = AppFontNames.Mono,
    )

    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSpacing.current

    val shapes: AppShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalAppShapes.current

    val elevation: AppElevation
        @Composable
        @ReadOnlyComposable
        get() = AppElevationDefault

    val sizes: AppSizes
        @Composable
        @ReadOnlyComposable
        get() = AppSizesDefault

    val springs: AppSprings
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSprings.current

    /** Whether to skip motion that is not needed to understand a change. */
    val reduceMotion: Boolean
        @Composable
        get() = LocalReduceMotion.current.value

    val style: AppStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalAppStyle.current

    /** The window's size classes. Decide layout from this, never from orientation or device. */
    val windowSize: AppWindowSize
        @Composable
        @ReadOnlyComposable
        get() = LocalAppWindowSize.current

    /** Measurements that follow the window: gutters, pane widths, maximum content widths. */
    val layout: AppLayout
        @Composable
        @ReadOnlyComposable
        get() = LocalAppLayout.current
}

private val AppElevationDefault = AppElevation()
private val AppSizesDefault = AppSizes()
