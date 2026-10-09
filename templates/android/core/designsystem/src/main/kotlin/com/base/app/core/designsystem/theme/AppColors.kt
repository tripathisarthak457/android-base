package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * Every colour the app is allowed to use, named for what it means rather than what it looks like.
 * Read through `AppTheme.colors`.
 */
@Immutable
data class AppColors(
    /** Behind a screen's content. */
    val background: Color,
    /** Behind inset-grouped sections. */
    val backgroundGrouped: Color,
    /** Cards, sheets and docked bars. */
    val surface: Color,
    /** The rows of an inset-grouped section. */
    val surfaceGrouped: Color,
    /**
     * Floating chrome, menus and popovers. Lighter than [surface] in dark theme, where a shadow
     * cannot show height.
     */
    val surfaceElevated: Color,
    /** Toasts and anything else drawn in the opposite theme. */
    val surfaceInverse: Color,
    /** Tracks and wells: a slider's rail, a segmented control's background, the off switch. */
    val fill: Color,
    /** Hairlines between rows and around cards. Decorative, so not held to a contrast ratio. */
    val separator: Color,
    /**
     * The edge of a control that has to be seen to be used (a checkbox ring, a field outline, the
     * off switch), at 3:1 or more against every surface.
     */
    val outline: Color,

    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
    /** Disabled content. Exempt from contrast, as WCAG allows. */
    val contentDisabled: Color,
    val contentInverse: Color,

    val accent: Color,
    val accentPressed: Color,
    val accentSubtle: Color,
    /** Text and icons on [accent]. */
    val onAccent: Color,
    /**
     * The accent as text or an icon on a surface: a link, a plain button, a selected tab. At least
     * 4.5:1 on every background, whatever accent the brand chose.
     */
    val accentContent: Color,

    /** Two supporting brand colours, each with the same roles as the accent. */
    val secondary: Color,
    val secondaryPressed: Color,
    val secondarySubtle: Color,
    val onSecondary: Color,

    val tertiary: Color,
    val tertiaryPressed: Color,
    val tertiarySubtle: Color,
    val onTertiary: Color,

    val success: StatusColors,
    val warning: StatusColors,
    val danger: StatusColors,
    val info: StatusColors,
    val neutral: StatusColors,

    /** A row under the finger. Translucent, so it reads on whatever surface the row sits on. */
    val highlight: Color,
    val scrim: Color,
    val focusRing: Color,
    val shadow: Color,
    /** The tab bar's selection pill. Content on it uses [onAccent]. */
    val barIndicator: Color,
    val skeleton: Color,
    /** The sweep across a skeleton, a visible step lighter than [skeleton] in both themes. */
    val skeletonHighlight: Color,

    /** Drives the decisions that cannot be expressed as a token, such as shadows. */
    val isLight: Boolean,
) {
    /** The surfaces text is drawn on, for checks that must hold on all of them. */
    val backgrounds: List<Color>
        get() = listOf(background, backgroundGrouped, surface, surfaceGrouped, surfaceElevated)
}

/**
 * One meaning in four roles: [base] for a solid fill, [content] for text and icons on a surface,
 * [subtle] for the panel behind them, [on] for text and icons on [base].
 */
@Immutable
data class StatusColors(
    val base: Color,
    val content: Color,
    val subtle: Color,
    val on: Color,
)

val LightColors: AppColors = AppColors(
    background = Grey50,
    backgroundGrouped = Grey75,
    surface = White,
    surfaceGrouped = White,
    surfaceElevated = White,
    surfaceInverse = Grey900,
    fill = Grey150,
    separator = Grey200,
    outline = Grey450,

    contentPrimary = Grey900,
    contentSecondary = Grey700,
    contentTertiary = Grey550,
    contentDisabled = Grey400,
    contentInverse = White,

    accent = Accent,
    accentPressed = AccentPressed,
    accentSubtle = AccentSubtleLight,
    onAccent = readableOn(Accent, Ink900),
    accentContent = Accent,

    secondary = Secondary,
    secondaryPressed = SecondaryPressed,
    secondarySubtle = SecondarySubtleLight,
    onSecondary = readableOn(Secondary, Ink900),

    tertiary = Tertiary,
    tertiaryPressed = TertiaryPressed,
    tertiarySubtle = TertiarySubtleLight,
    onTertiary = readableOn(Tertiary, Ink900),

    success = status(SuccessLight, SuccessSubtleLight, Ink900),
    warning = status(WarningLight, WarningSubtleLight, Ink900),
    danger = status(DangerLight, DangerSubtleLight, Ink900),
    info = status(InfoLight, InfoSubtleLight, Ink900),
    neutral = status(Grey600, Grey100, Ink900),

    highlight = Grey900.copy(alpha = 0.07f),
    scrim = Grey900.copy(alpha = 0.40f),
    focusRing = Accent,
    shadow = Grey900.copy(alpha = 0.14f),
    barIndicator = Accent,
    skeleton = Grey150,
    skeletonHighlight = Grey50,

    isLight = true,
).withDerivedRoles()

val DarkColors: AppColors = AppColors(
    background = Ink900,
    backgroundGrouped = Ink900,
    surface = Ink800,
    surfaceGrouped = Ink800,
    surfaceElevated = Ink700,
    surfaceInverse = Ink100,
    fill = Ink600,
    separator = Ink600,
    outline = Ink350,

    contentPrimary = Ink100,
    contentSecondary = Ink200,
    contentTertiary = Ink300,
    contentDisabled = Ink400,
    contentInverse = Ink900,

    accent = AccentDark,
    accentPressed = AccentDarkPressed,
    accentSubtle = AccentSubtleDark,
    onAccent = readableOn(AccentDark, Ink900),
    accentContent = AccentDark,

    secondary = SecondaryDark,
    secondaryPressed = SecondaryDarkPressed,
    secondarySubtle = SecondarySubtleDark,
    onSecondary = readableOn(SecondaryDark, Ink900),

    tertiary = TertiaryDark,
    tertiaryPressed = TertiaryDarkPressed,
    tertiarySubtle = TertiarySubtleDark,
    onTertiary = readableOn(TertiaryDark, Ink900),

    success = status(SuccessDark, SuccessSubtleDark, Ink900),
    warning = status(WarningDark, WarningSubtleDark, Ink900),
    danger = status(DangerDark, DangerSubtleDark, Ink900),
    info = status(InfoDark, InfoSubtleDark, Ink900),
    neutral = status(Ink200, Ink700, Ink900),

    highlight = White.copy(alpha = 0.09f),
    scrim = Color.Black.copy(alpha = 0.56f),
    focusRing = AccentDark,
    shadow = Color.Black.copy(alpha = 0.40f),
    barIndicator = AccentDark,
    skeleton = Ink700,
    skeletonHighlight = Ink500,

    isLight = false,
).withDerivedRoles()

/**
 * The roles that must hold their contrast whatever the surfaces are: the accent and status colours
 * as text, and the outline of controls. Run again whenever the surfaces change, as a design style's
 * tone does: a brand tint a few percent into the background is enough to drop a mid-tone below AA.
 */
internal fun AppColors.withDerivedRoles(): AppColors = copy(
    accentContent = legibleOn(accent, backgrounds + accentSubtle, isLight),
    outline = legibleOn(outline, backgrounds, isLight, AA_NON_TEXT),
    success = success.legibleOnSurfaces(backgrounds, isLight),
    warning = warning.legibleOnSurfaces(backgrounds, isLight),
    danger = danger.legibleOnSurfaces(backgrounds, isLight),
    info = info.legibleOnSurfaces(backgrounds, isLight),
)

private fun StatusColors.legibleOnSurfaces(backgrounds: List<Color>, light: Boolean) =
    copy(content = legibleOn(content, backgrounds + subtle, light))

private fun status(base: Color, subtle: Color, ink: Color) =
    StatusColors(base = base, content = base, subtle = subtle, on = readableOn(base, ink))

/** WCAG 2 contrast ratio between two opaque colours. */
internal fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

/** White or [ink], whichever reads better on [background]. */
internal fun readableOn(background: Color, ink: Color): Color =
    if (contrast(White, background) >= contrast(ink, background)) White else ink

/**
 * [color], or the nearest shade of it that reaches [minimum] on every one of [backgrounds]: darker
 * in a light palette, lighter in a dark one. Brand colours are picked as fills, and a mid-tone that
 * looks right on a button can be unreadable as a link.
 */
internal fun legibleOn(color: Color, backgrounds: List<Color>, light: Boolean, minimum: Float = AA_TEXT): Color {
    val toward = if (light) Color.Black else Color.White
    var shade = color
    repeat(MAX_SHADE_STEPS) {
        if (backgrounds.all { contrast(shade, it) >= minimum }) return shade
        shade = lerp(shade, toward, SHADE_STEP)
    }
    return shade
}

internal const val AA_TEXT = 4.5f
internal const val AA_NON_TEXT = 3f
private const val MAX_SHADE_STEPS = 30
private const val SHADE_STEP = 0.05f

internal val LocalAppColors = staticCompositionLocalOf { LightColors }
