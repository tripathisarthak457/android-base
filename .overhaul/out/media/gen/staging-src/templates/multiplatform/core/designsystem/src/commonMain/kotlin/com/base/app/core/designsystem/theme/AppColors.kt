package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.base.app.core.designsystem.theme.tokens.DesignTokens

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
    /** Tracks and wells: a search field's background. */
    val fill: Color,
    /** Hairlines between rows. Decorative, so not held to a contrast ratio. */
    val separator: Color,
    /** A row under the finger. */
    val highlight: Color,
    /** The placeholder shapes drawn where content is still loading. */
    val skeleton: Color,

    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
    /** Disabled content. Exempt from contrast, as WCAG allows. */
    val contentDisabled: Color,

    val accent: Color,
    val accentPressed: Color,
    val accentSubtle: Color,
    /** Text and icons on [accent]. */
    val onAccent: Color,
    /** The accent as text or an icon on a surface, at 4.5:1 or more whatever the brand chose. */
    val accentContent: Color,

    /** Destructive actions and error text. */
    val danger: Color,
    val dangerSubtle: Color,

    val isLight: Boolean,
)

private fun DesignTokens.Roles.toColors(isLight: Boolean) = AppColors(
    background = background,
    backgroundGrouped = backgroundGrouped,
    surface = surface,
    surfaceGrouped = surfaceGrouped,
    fill = fill,
    separator = separator,
    highlight = highlight,
    skeleton = skeleton,
    contentPrimary = contentPrimary,
    contentSecondary = contentSecondary,
    contentTertiary = contentTertiary,
    contentDisabled = contentDisabled,
    accent = accent,
    accentPressed = accentPressed,
    accentSubtle = accentSubtle,
    onAccent = onAccent,
    accentContent = accentContent,
    danger = danger.content,
    dangerSubtle = danger.subtle,
    isLight = isLight,
)

private val Light = SurfaceTone.entries.associateWith { it.roles(dark = false).toColors(isLight = true) }
private val Dark = SurfaceTone.entries.associateWith { it.roles(dark = true).toColors(isLight = false) }

/** The palette this surface tone draws with, in the light or the dark theme. */
fun SurfaceTone.colors(dark: Boolean): AppColors = (if (dark) Dark else Light).getValue(this)

private fun SurfaceTone.roles(dark: Boolean): DesignTokens.Roles = when (this) {
    SurfaceTone.Cool -> if (dark) DesignTokens.Palette.Cool.dark else DesignTokens.Palette.Cool.light
    SurfaceTone.BrandTint ->
        if (dark) DesignTokens.Palette.BrandTint.dark else DesignTokens.Palette.BrandTint.light
    SurfaceTone.Paper -> if (dark) DesignTokens.Palette.Paper.dark else DesignTokens.Palette.Paper.light
    SurfaceTone.Cream -> if (dark) DesignTokens.Palette.Cream.dark else DesignTokens.Palette.Cream.light
}

internal val LocalAppColors = staticCompositionLocalOf { SurfaceTone.Cool.colors(dark = false) }
