package com.base.app.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
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
import com.base.app.core.designsystem.foundation.content.LocalContentColor
import com.base.app.core.designsystem.foundation.content.LocalTextStyle
import com.base.app.core.designsystem.foundation.interaction.NoIndication
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

/** The look the project was generated with. The app's root starts from it. */
@Immutable
class AppThemeDefaults internal constructor(
    val designStyle: AppDesignStyle,
    val motionStyle: AppMotionStyle,
)

/**
 * The single theme wrapper. Everything the app draws sits inside one of these.
 *
 * Nothing draws a press overlay unless a component asks for one: the default indication is
 * [NoIndication], and each control states its feedback through `Modifier.pressable`.
 *
 * The window's size classes are read here, once, and change only when a breakpoint is crossed: a
 * rotation or a Stage Manager resize on iPad animates the window's size on every frame, and a
 * theme that read the size itself would recompose the whole app on each one. [posture] and [input]
 * come from the platform, which watches the window for them; a nested theme keeps its parent's.
 */
@Composable
fun AppTheme(
    mode: ThemeMode = ThemeMode.System,
    designStyle: AppDesignStyle = AppTheme.defaults.designStyle,
    motionStyle: AppMotionStyle = AppTheme.defaults.motionStyle,
    colors: AppColors = designStyle.style().surfaces.colors(dark = mode.isDark()),
    posture: WindowPosture = AppTheme.posture,
    input: InputTraits = AppTheme.input,
    content: @Composable () -> Unit,
) {
    val style = designStyle.style()
    val typography = style.voice.typography()
    val selectionColors = remember(colors) {
        TextSelectionColors(
            handleColor = colors.accent,
            // Low enough alpha that selected text stays readable through the selection.
            backgroundColor = colors.accent.copy(alpha = 0.28f),
        )
    }

    val windowClass = rememberWindowClass()
    val windowMetrics = rememberWindowMetrics()
    val layout = remember(windowClass) { AppLayout.forClass(windowClass) }
    // The gutter every screen already uses follows the window, so margins grow on large screens
    // without any screen asking.
    val spacing = remember(layout) { AppSpacing(gutter = layout.gutter) }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppSpacing provides spacing,
        LocalWindowClass provides windowClass,
        LocalContainerClass provides windowClass,
        LocalWindowMetrics provides windowMetrics,
        LocalWindowPosture provides posture,
        LocalInputTraits provides input,
        LocalAppLayout provides layout,
        LocalAppStyle provides style,
        LocalAppSprings provides motionStyle.springs(),
        LocalContentColor provides colors.contentPrimary,
        LocalTextStyle provides typography.body,
        LocalTextSelectionColors provides selectionColors,
        LocalIndication provides NoIndication,
        content = content,
    )
}

/**
 * The accessor every component and screen reads tokens through: `AppTheme.colors.accent`,
 * `AppTheme.spacing.lg`, `AppTheme.springs.release`.
 */
object AppTheme {

    /** The design style and motion style the project was generated with. */
    val defaults = AppThemeDefaults(
        designStyle = DesignTokens.Defaults.designStyle,
        motionStyle = DesignTokens.Defaults.motionStyle,
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
        get() = DefaultShapes

    val sizes: AppSizes
        @Composable
        @ReadOnlyComposable
        get() = DefaultSizes

    val springs: AppSprings
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSprings.current

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

    /** How a foldable is held, and where its hinge is. Always flat on iOS. */
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
