package com.example.standard.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.example.standard.core.designsystem.component.overlay.AppOverlayHost
import com.example.standard.core.designsystem.component.overlay.LocalOverlayHost
import com.example.standard.core.designsystem.foundation.adaptive.AppLayout
import com.example.standard.core.designsystem.foundation.adaptive.InputTraits
import com.example.standard.core.designsystem.foundation.adaptive.LocalAppLayout
import com.example.standard.core.designsystem.foundation.adaptive.LocalContainerClass
import com.example.standard.core.designsystem.foundation.adaptive.LocalInputTraits
import com.example.standard.core.designsystem.foundation.adaptive.LocalWindowClass
import com.example.standard.core.designsystem.foundation.adaptive.LocalWindowMetrics
import com.example.standard.core.designsystem.foundation.adaptive.LocalWindowPosture
import com.example.standard.core.designsystem.foundation.adaptive.SizeClass
import com.example.standard.core.designsystem.foundation.adaptive.WindowPosture
import com.example.standard.core.designsystem.foundation.adaptive.rememberWindowClass
import com.example.standard.core.designsystem.foundation.adaptive.rememberWindowMetrics
import com.example.standard.core.designsystem.foundation.content.LocalContentColor
import com.example.standard.core.designsystem.foundation.content.LocalTextStyle
import com.example.standard.core.designsystem.foundation.draw.LocalShimmerClock
import com.example.standard.core.designsystem.foundation.draw.ShimmerClock
import com.example.standard.core.designsystem.foundation.interaction.LocalHapticsEnabled
import com.example.standard.core.designsystem.foundation.interaction.NoIndication
import com.example.standard.core.designsystem.foundation.layout.ChromeInset
import com.example.standard.core.designsystem.foundation.layout.LocalBottomChrome
import com.example.standard.core.designsystem.foundation.scroll.AppOverscroll
import com.example.standard.core.designsystem.theme.tokens.DesignTokens

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
 * is [NoIndication], and each control states its feedback through `Modifier.pressable`. Scrolling
 * containers get iOS's rubber-band edge ([AppOverscroll]).
 *
 * [colors] default to the palette of [designStyle]'s surface tone in the mode's theme.
 * [reduceMotion] overrides the system's "Remove animations" setting, for previews and the catalog;
 * left null, the setting is followed as it changes. [posture] and [input] come from the activity,
 * which watches the window for them; a nested theme keeps its parent's.
 */
@Composable
fun AppTheme(
    mode: ThemeMode = ThemeMode.System,
    designStyle: AppDesignStyle = AppTheme.defaults.designStyle,
    motionStyle: AppMotionStyle = AppTheme.defaults.motionStyle,
    hapticsEnabled: Boolean = AppTheme.defaults.hapticsEnabled,
    colors: AppColors = designStyle.style().surfaces.colors(dark = mode.isDark()),
    fonts: AppFonts = themeFonts(),
    reduceMotion: Boolean? = null,
    posture: WindowPosture = AppTheme.posture,
    input: InputTraits = AppTheme.input,
    content: @Composable () -> Unit,
) {
    val style = designStyle.style()
    val typography = remember(fonts, style.voice) { appTypography(fonts, style.voice) }
    val selectionColors = remember(colors) {
        TextSelectionColors(
            handleColor = colors.accent,
            // Low enough alpha that selected text stays readable through the selection.
            backgroundColor = colors.accent.copy(alpha = 0.28f),
        )
    }
    val systemReduceMotion = rememberSystemReduceMotion()
    val override = rememberUpdatedState(reduceMotion)
    // One state for the theme's lifetime, whichever side decides: code that observes it outside
    // composition (a skeleton choosing whether to sweep) sees a change of the override too.
    val reduceMotionState: State<Boolean> = remember(systemReduceMotion) {
        derivedStateOf { override.value ?: systemReduceMotion.value }
    }

    val outermost = LocalOverlayHost.current == null
    val windowClass = rememberWindowClass()
    val windowMetrics = rememberWindowMetrics()
    // A theme nested inside a pane keeps the pane's class: its content is still laid out in the pane.
    val containerClass = if (outermost) windowClass else LocalContainerClass.current
    val layout = remember(containerClass) { AppLayout.forClass(containerClass) }
    // The gutter every screen already uses follows the pane, so margins grow on large screens
    // without any screen asking.
    val spacing = remember(layout) { AppSpacing(gutter = layout.gutter) }

    val bottomChrome = if (outermost) remember { ChromeInset() } else LocalBottomChrome.current
    val scope = rememberCoroutineScope()
    val metrics = rememberUpdatedState(windowMetrics)
    val shimmer = if (outermost) {
        remember { ShimmerClock(scope) { metrics.value.sizeDp } }
    } else {
        LocalShimmerClock.current
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppSpacing provides spacing,
        LocalWindowClass provides windowClass,
        LocalContainerClass provides containerClass,
        LocalWindowMetrics provides windowMetrics,
        LocalWindowPosture provides posture,
        LocalInputTraits provides input,
        LocalAppLayout provides layout,
        LocalAppShapes provides style.shapes,
        LocalAppStyle provides style,
        LocalAppSprings provides motionStyle.springs(),
        LocalReduceMotion provides reduceMotionState,
        LocalContentColor provides colors.contentPrimary,
        LocalTextStyle provides typography.body,
        LocalTextSelectionColors provides selectionColors,
        LocalIndication provides NoIndication,
        LocalHapticsEnabled provides hapticsEnabled,
        LocalBottomChrome provides bottomChrome,
        LocalShimmerClock provides shimmer,
    ) {
        if (outermost) {
            // Only the outermost theme sets the edge, so a screen that chose Android's stretch keeps
            // it inside a nested theme.
            CompositionLocalProvider(LocalOverscrollFactory provides AppOverscroll) { AppOverlayHost(content) }
        } else {
            content()
        }
    }
}

/**
 * The accessor every component and screen reads tokens through: `AppTheme.colors.accent`,
 * `AppTheme.spacing.lg`, `AppTheme.springs.release`.
 */
object AppTheme {

    /** The design style, motion style, haptics and fonts the project was generated with. */
    val defaults = AppThemeDefaults(
        designStyle = DesignTokens.Defaults.designStyle,
        motionStyle = DesignTokens.Defaults.motionStyle,
        hapticsEnabled = DesignTokens.Defaults.hapticsEnabled,
        fontName = DesignTokens.Defaults.fontName,
        monoFontName = DesignTokens.Defaults.monoFontName,
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

    /**
     * The window's size classes, for the shell: which navigation chrome, how many panes. Screens and
     * components read [containerClass] instead. Decide layout from these, never from orientation or
     * device.
     */
    val windowClass: SizeClass
        @Composable
        @ReadOnlyComposable
        get() = LocalWindowClass.current

    /** The size classes of the pane this is laid out in: the window's, unless a pane narrows it. */
    val containerClass: SizeClass
        @Composable
        @ReadOnlyComposable
        get() = LocalContainerClass.current

    /** How a foldable is held, and where its hinge is. */
    val posture: WindowPosture
        @Composable
        @ReadOnlyComposable
        get() = LocalWindowPosture.current

    /** The pointer and keyboard the person has right now. */
    val input: InputTraits
        @Composable
        @ReadOnlyComposable
        get() = LocalInputTraits.current

    /** Measurements that follow the pane: gutters, pane widths, maximum content widths. */
    val layout: AppLayout
        @Composable
        @ReadOnlyComposable
        get() = LocalAppLayout.current
}

private val AppElevationDefault = AppElevation()
private val AppSizesDefault = AppSizes()
