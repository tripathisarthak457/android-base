package com.base.app.core.designsystem.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import com.base.app.core.designsystem.foundation.adaptive.AppLayout
import com.base.app.core.designsystem.foundation.adaptive.InputTraits
import com.base.app.core.designsystem.foundation.adaptive.LocalAppLayout
import com.base.app.core.designsystem.foundation.adaptive.LocalContainerClass
import com.base.app.core.designsystem.foundation.adaptive.LocalInputTraits
import com.base.app.core.designsystem.foundation.adaptive.LocalWindowClass
import com.base.app.core.designsystem.foundation.adaptive.LocalWindowMetrics
import com.base.app.core.designsystem.foundation.adaptive.LocalWindowPosture
import com.base.app.core.designsystem.foundation.adaptive.SizeClass
import com.base.app.core.designsystem.foundation.adaptive.WindowPosture
import com.base.app.core.designsystem.foundation.adaptive.rememberWindowClass
import com.base.app.core.designsystem.foundation.adaptive.rememberWindowMetrics
import com.base.app.core.designsystem.theme.tokens.DesignTokens

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

/** What the project was generated with: haptics and fonts. */
@Immutable
class AppThemeDefaults internal constructor(
    val hapticsEnabled: Boolean,
    val fontName: String,
    val monoFontName: String,
)

/**
 * The theme skeleton: one slot for each kind of token the app's components read (colours, type,
 * spacing, shapes, sizes, elevation, springs and reduce-motion) and the window's size classes,
 * posture and input, with no component behind them. The values are neutral, with the project's
 * own colours and fonts; change them in `tokens/DesignTokens.kt`, and build components that read
 * them through [AppTheme].
 *
 * Nothing draws a press overlay unless a component asks for one: the default indication draws
 * nothing, so a `clickable` without an indication of its own gives no grey flash.
 *
 * [colors] default to the project's palette in the mode's theme.
 * [reduceMotion] overrides the system's "Remove animations" setting, for previews and tests; left
 * null, the setting is followed as it changes. [posture] and [input] come from the activity, which
 * watches the window for them; a nested theme keeps its parent's.
 */
@Composable
fun AppTheme(
    mode: ThemeMode = ThemeMode.System,
    colors: AppColors = appColors(dark = mode.isDark()),
    fonts: AppFonts = themeFonts(),
    hapticsEnabled: Boolean = AppTheme.defaults.hapticsEnabled,
    reduceMotion: Boolean? = null,
    posture: WindowPosture = AppTheme.posture,
    input: InputTraits = AppTheme.input,
    content: @Composable () -> Unit,
) {
    val typography = remember(fonts) { appTypography(fonts) }
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
    // composition sees a change of the override too.
    val reduceMotionState: State<Boolean> = remember(systemReduceMotion) {
        derivedStateOf { override.value ?: systemReduceMotion.value }
    }

    val outermost = !LocalThemeInstalled.current
    val windowClass = rememberWindowClass()
    val windowMetrics = rememberWindowMetrics()
    // A theme nested inside a pane keeps the pane's class: its content is still laid out in the
    // pane.
    val containerClass = if (outermost) windowClass else LocalContainerClass.current
    val layout = remember(containerClass) { AppLayout.forClass(containerClass) }
    // The gutter follows the pane, so margins grow on large screens without any screen asking.
    val spacing = remember(layout) { AppSpacing(gutter = layout.gutter) }

    CompositionLocalProvider(
        LocalThemeInstalled provides true,
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppSpacing provides spacing,
        LocalWindowClass provides windowClass,
        LocalContainerClass provides containerClass,
        LocalWindowMetrics provides windowMetrics,
        LocalWindowPosture provides posture,
        LocalInputTraits provides input,
        LocalAppLayout provides layout,
        LocalReduceMotion provides reduceMotionState,
        LocalTextSelectionColors provides selectionColors,
        LocalIndication provides NoIndication,
        LocalHapticsEnabled provides hapticsEnabled,
        content = content,
    )
}

/**
 * The accessor every component and screen reads tokens through: `AppTheme.colors.accent`,
 * `AppTheme.spacing.lg`, `AppTheme.springs.release`.
 */
object AppTheme {

    /** The haptics and fonts the project was generated with. */
    val defaults = AppThemeDefaults(
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
        get() = AppShapesDefault

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
        get() = DefaultSprings

    /** Whether to skip motion that is not needed to understand a change. */
    val reduceMotion: Boolean
        @Composable
        get() = LocalReduceMotion.current.value

    /** Whether controls may play haptics. The device's own setting still applies on top. */
    val hapticsEnabled: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalHapticsEnabled.current

    /**
     * The window's size classes, for whatever decides the navigation chrome and the number of
     * panes. Screens and components read [containerClass] instead. Decide layout from these, never
     * from orientation or device.
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

/** The theme's policy on press feedback: none, unless a component draws its own. */
private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 0
}

private val LocalHapticsEnabled = staticCompositionLocalOf { true }

/** Whether an outer theme is already installed, so a nested one keeps its pane's size classes. */
private val LocalThemeInstalled = staticCompositionLocalOf { false }

private val AppShapesDefault = AppShapes()
private val AppElevationDefault = AppElevation()
private val AppSizesDefault = AppSizes()
