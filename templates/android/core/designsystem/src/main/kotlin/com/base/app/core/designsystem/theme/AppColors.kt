package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour the app is allowed to use, named for what it means rather than what it looks like.
 */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceInverse: Color,

    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
    val contentDisabled: Color,
    val contentInverse: Color,

    val accent: Color,
    val accentPressed: Color,
    val accentSubtle: Color,
    val onAccent: Color,

    /** Two supporting brand colours, each with the same three roles as the accent. */
    val secondary: Color,
    val secondaryPressed: Color,
    val secondarySubtle: Color,
    val onSecondary: Color,

    val tertiary: Color,
    val tertiaryPressed: Color,
    val tertiarySubtle: Color,
    val onTertiary: Color,

    val border: Color,
    val borderStrong: Color,
    val divider: Color,

    val success: StatusColors,
    val warning: StatusColors,
    val danger: StatusColors,
    val info: StatusColors,
    val neutral: StatusColors,

    val skeleton: Color,
    val scrim: Color,
    val focusRing: Color,

    /** Drives the decisions that cannot be expressed as a token — see `AppSurface`. */
    val isLight: Boolean,
)

/**
 * One meaning, three roles: [content] for text and icons, [subtle] for the pill or panel behind
 * them, [border] for the outline when the block needs one.
 */
@Immutable
data class StatusColors(
    val content: Color,
    val subtle: Color,
    val border: Color,
)

val LightColors = AppColors(
    background = Grey50,
    surface = White,
    surfaceVariant = Grey100,
    surfaceInverse = Grey900,

    contentPrimary = Grey900,
    contentSecondary = Grey700,
    contentTertiary = Grey500,
    contentDisabled = Grey400,
    contentInverse = White,

    accent = Accent,
    accentPressed = AccentPressed,
    accentSubtle = AccentSubtleLight,
    onAccent = White,

    secondary = Secondary,
    secondaryPressed = SecondaryPressed,
    secondarySubtle = SecondarySubtleLight,
    onSecondary = Ink900,

    tertiary = Tertiary,
    tertiaryPressed = TertiaryPressed,
    tertiarySubtle = TertiarySubtleLight,
    onTertiary = White,

    border = Grey200,
    borderStrong = Grey300,
    divider = Grey150,

    success = StatusColors(SuccessLight, SuccessSubtleLight, SuccessBorderLight),
    warning = StatusColors(WarningLight, WarningSubtleLight, WarningBorderLight),
    danger = StatusColors(DangerLight, DangerSubtleLight, DangerBorderLight),
    info = StatusColors(InfoLight, InfoSubtleLight, InfoBorderLight),
    neutral = StatusColors(Grey600, Grey100, Grey200),

    skeleton = Grey150,
    scrim = Grey900.copy(alpha = 0.45f),
    focusRing = Accent.copy(alpha = 0.40f),

    isLight = true,
)

val DarkColors = AppColors(
    background = Ink900,
    surface = Ink800,
    surfaceVariant = Ink700,
    surfaceInverse = Ink100,

    contentPrimary = Ink100,
    contentSecondary = Ink200,
    contentTertiary = Ink300,
    contentDisabled = Ink400,
    contentInverse = Ink900,

    accent = AccentDark,
    accentPressed = AccentDarkPressed,
    accentSubtle = AccentSubtleDark,
    // Whichever of near-black or white reads better on this lighter shade. For a light accent it is
    // near-black: white on light blue is the most common contrast failure in dark themes.
    onAccent = Color(0xFF06101F),

    secondary = SecondaryDark,
    secondaryPressed = SecondaryDarkPressed,
    secondarySubtle = SecondarySubtleDark,
    onSecondary = Color(0xFF06101F),

    tertiary = TertiaryDark,
    tertiaryPressed = TertiaryDarkPressed,
    tertiarySubtle = TertiarySubtleDark,
    onTertiary = Color(0xFF06101F),

    border = Ink600,
    borderStrong = Ink500,
    divider = Color(0xFF1E2531),

    success = StatusColors(SuccessDark, SuccessSubtleDark, SuccessBorderDark),
    warning = StatusColors(WarningDark, WarningSubtleDark, WarningBorderDark),
    danger = StatusColors(DangerDark, DangerSubtleDark, DangerBorderDark),
    info = StatusColors(InfoDark, InfoSubtleDark, InfoBorderDark),
    neutral = StatusColors(Ink200, Color(0xFF1A202B), Color(0xFF2C3441)),

    skeleton = Ink700,
    scrim = Color(0xFF000000).copy(alpha = 0.60f),
    focusRing = AccentDark.copy(alpha = 0.45f),

    isLight = false,
)

internal val LocalAppColors = staticCompositionLocalOf { LightColors }
