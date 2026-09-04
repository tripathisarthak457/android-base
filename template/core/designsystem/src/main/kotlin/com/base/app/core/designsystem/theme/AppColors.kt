package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour the app is allowed to use, named for what it means rather than what it looks like.
 *
 * A screen asks for `colors.danger.content`, never for a red. That is the difference between a
 * dark theme being one more instance of this class and being a search-and-replace through every
 * feature — and it is why a status pill, an error banner and a destructive button are guaranteed
 * to agree with each other.
 *
 * `@Immutable` is load-bearing: it tells the Compose compiler this can be compared by reference,
 * so a composable taking it as a parameter can skip. Without it, every component in the design
 * system recomposes whenever anything in the theme is read.
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
 *
 * Grouped so a caller picks a meaning and physically cannot pair a red foreground with an amber
 * background — the drift that turns four status styles into eleven.
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
    // Near-black rather than white: at this lightness the accent needs dark text on it to stay
    // legible, and white-on-light-blue is the single most common contrast failure in dark themes.
    onAccent = Color(0xFF06101F),

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
