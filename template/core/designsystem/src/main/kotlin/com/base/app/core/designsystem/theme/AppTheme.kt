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

/**
 * Which palette to render with.
 *
 * [System] follows the device; the other two override it. Persisting the user's choice belongs to
 * the app module — this type is only the vocabulary for expressing it.
 */
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

/**
 * The single theme wrapper. Everything the app draws sits inside exactly one of these.
 *
 * There is no `MaterialTheme` underneath. That is deliberate and it is the reason this file has
 * to provide a few things Material would otherwise have supplied: the indication used by every
 * clickable, the default content colour and text style that untagged text inherits, and the
 * selection handle colours inside text fields. Missing any one of them is a visible defect —
 * black-on-black text, a default purple selection handle — rather than a subtle one, which is
 * why they are all here and not discovered one at a time.
 *
 * ## Haptics are one boolean
 *
 * `AppTheme(hapticsEnabled = …)` silences every control in the app at once. Wire it to a
 * preference; the device's own haptics setting still applies on top, so this cannot make a phone
 * buzz that its owner has asked to stay quiet.
 *
 * ## The feel is one enum
 *
 * `AppTheme(motionStyle = AppMotionStyle.Bouncy)` changes how every control in the app responds
 * to a finger. See [AppMotionStyle].
 *
 * ## The typeface is one string
 *
 * `AppTheme(fontName = "Manrope")` restyles every screen in the app. The name is a Google Fonts
 * family name; see [AppFonts] for the reasoning and for how to bundle a licensed font instead.
 */
@Composable
fun AppTheme(
    mode: ThemeMode = ThemeMode.System,
    colors: AppColors? = null,
    fontName: String = AppFontNames.Sans,
    monoFontName: String = AppFontNames.Mono,
    fonts: AppFonts? = null,
    motionStyle: AppMotionStyle = AppMotionStyle.Standard,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val resolved = colors ?: if (mode.isDark()) DarkColors else LightColors
    val resolvedFonts = fonts ?: rememberAppFonts(fontName, monoFontName)
    val typography = remember(resolvedFonts) { appTypography(resolvedFonts) }
    val motion = remember(motionStyle) { motionStyle.motion() }

    val selectionColors = remember(resolved) {
        TextSelectionColors(
            handleColor = resolved.accent,
            // The selection rectangle sits *behind* the glyphs, so it has to stay light enough
            // for them to read through it. Anything above roughly a third alpha and selected
            // text becomes harder to read than unselected text, which is backwards.
            backgroundColor = resolved.accent.copy(alpha = 0.28f),
        )
    }

    val indication = remember(resolved) { AppIndication(resolved.contentPrimary) }

    CompositionLocalProvider(
        LocalAppColors provides resolved,
        LocalAppTypography provides typography,
        LocalAppSpacing provides AppSpacing(),
        LocalAppShapes provides AppShapes(),
        LocalAppElevation provides AppElevation(),
        LocalAppSizes provides AppSizes(),
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
 *
 * An object with read-only composable properties rather than free functions, so the call sites
 * group visually and autocomplete after `AppTheme.` lists the whole vocabulary.
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
}
