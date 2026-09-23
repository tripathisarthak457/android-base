package com.base.app.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import com.base.app.core.designsystem.foundation.AppIndication
import com.base.app.core.designsystem.foundation.LocalAppHapticsEnabled
import com.base.app.core.designsystem.foundation.LocalContentColor
import com.base.app.core.designsystem.foundation.LocalTextStyle

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

/** The single theme wrapper. Everything the app draws sits inside exactly one of these. */
@Composable
fun AppTheme(
    mode: ThemeMode = ThemeMode.System,
    colors: AppColors? = null,
    fontName: String = AppFontNames.Sans,
    monoFontName: String = AppFontNames.Mono,
    fonts: AppFonts? = null,
    motionStyle: AppMotionStyle = AppMotionStyle.Standard,
    designStyle: AppDesignStyle = AppDesignStyle.Utility,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val style = remember(designStyle) { designStyle.style() }
    val dark = mode.isDark()
    // Explicit colours are taken as given; the style only restyles the palette it chose itself.
    val resolved = colors ?: remember(dark, style.surfaces) {
        (if (dark) DarkColors else LightColors).withSurfaces(style.surfaces)
    }
    val resolvedFonts = fonts ?: rememberAppFonts(fontName, monoFontName)
    val typography = remember(resolvedFonts, style.voice) { appTypography(resolvedFonts).withVoice(style.voice) }
    val motion = remember(motionStyle) { motionStyle.motion() }
    val sizes = remember(style) {
        AppSizes(borderWidth = style.borderWidth, borderWidthStrong = style.borderWidthStrong)
    }

    val selectionColors = remember(resolved) {
        TextSelectionColors(
            handleColor = resolved.accent,
            // Low enough alpha that selected text stays readable through the selection.
            backgroundColor = resolved.accent.copy(alpha = 0.28f),
        )
    }

    val indication = remember(resolved) { AppIndication(resolved.contentPrimary) }

    CompositionLocalProvider(
        LocalAppColors provides resolved,
        LocalAppTypography provides typography,
        LocalAppSpacing provides AppSpacing(),
        LocalAppShapes provides style.shapes,
        LocalAppElevation provides AppElevation(),
        LocalAppSizes provides sizes,
        LocalAppStyle provides style,
        LocalAppMotion provides motion,
        LocalContentColor provides resolved.contentPrimary,
        LocalTextStyle provides typography.bodyMedium,
        LocalTextSelectionColors provides selectionColors,
        LocalIndication provides indication,
        LocalAppHapticsEnabled provides hapticsEnabled,
        content = content,
    )
}

/**
 * The accessor every component and screen reads tokens through: `AppTheme.colors.accent`,
 * `AppTheme.spacing.lg`.
 */
object AppTheme {

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
        get() = LocalAppElevation.current

    val sizes: AppSizes
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSizes.current

    val motion: AppMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalAppMotion.current

    val style: AppStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalAppStyle.current
}
