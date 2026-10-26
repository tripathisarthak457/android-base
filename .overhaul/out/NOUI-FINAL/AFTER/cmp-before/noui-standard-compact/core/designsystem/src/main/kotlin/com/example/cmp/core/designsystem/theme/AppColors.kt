package com.example.cmp.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.cmp.core.designsystem.theme.tokens.DesignTokens

/**
 * Every colour the app is allowed to use, named for what it means rather than what it looks like.
 * Read through `AppTheme.colors`.
 */
@Immutable
public data class AppColors(
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
    /**
     * The raised thumb of a segmented control on [fill]. White in light theme, where its shadow
     * lifts it; in dark theme a step lighter than the track, as iOS draws it, never darker.
     */
    val segmentThumb: Color,
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
public data class StatusColors(
    val base: Color,
    val content: Color,
    val subtle: Color,
    val on: Color,
)

/**
 * The palette the theme draws with, light or [dark]. Every role is resolved when the tokens are
 * emitted, the brand's colours included, and the roles drawn over the surfaces (the accent as text,
 * status text, outlines) already darkened or lightened until they keep their contrast on every one
 * of them; `PaletteContrastTest` holds them to it.
 */
public fun appColors(dark: Boolean): AppColors = if (dark) DesignTokens.Palette.dark else DesignTokens.Palette.light

internal val LocalAppColors = staticCompositionLocalOf { appColors(dark = false) }
