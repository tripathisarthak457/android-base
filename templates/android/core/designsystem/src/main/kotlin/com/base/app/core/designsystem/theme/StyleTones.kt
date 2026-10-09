package com.base.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/** The palette with a style's neutrals in place of the template's. */
fun AppColors.withSurfaces(tone: SurfaceTone): AppColors = when (tone) {
    SurfaceTone.Cool -> this
    SurfaceTone.BrandTint -> brandTinted()
    SurfaceTone.Paper -> if (isLight) PaperLight.applyTo(this) else PaperDark.applyTo(this)
    SurfaceTone.Cream -> if (isLight) CreamLight.applyTo(this) else CreamDark.applyTo(this)
}.withDerivedRoles()

/*
 * A few percent of the accent, never more. Enough that a feed in a pink brand reads as pink at a
 * glance; any more and grey text on it starts to lose contrast and every surface looks selected.
 */
private fun AppColors.brandTinted(): AppColors {
    fun tint(base: Color, amount: Float) = lerp(base, accent, amount)
    return copy(
        background = tint(background, 0.045f),
        backgroundGrouped = tint(backgroundGrouped, 0.05f),
        surface = if (isLight) surface else tint(surface, 0.05f),
        surfaceGrouped = if (isLight) surfaceGrouped else tint(surfaceGrouped, 0.05f),
        surfaceElevated = if (isLight) surfaceElevated else tint(surfaceElevated, 0.05f),
        fill = tint(fill, 0.07f),
        separator = tint(separator, 0.1f),
        skeleton = tint(skeleton, 0.08f),
    )
}

private class Neutrals(
    val background: Color,
    val backgroundGrouped: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val fill: Color,
    val separator: Color,
    val outline: Color,
    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
    val skeletonHighlight: Color,
) {
    fun applyTo(colors: AppColors) = colors.copy(
        background = background,
        backgroundGrouped = backgroundGrouped,
        surface = surface,
        surfaceGrouped = surface,
        surfaceElevated = surfaceElevated,
        fill = fill,
        separator = separator,
        outline = outline,
        contentPrimary = contentPrimary,
        contentSecondary = contentSecondary,
        contentTertiary = contentTertiary,
        skeleton = fill,
        skeletonHighlight = skeletonHighlight,
    )
}

private val PaperLight = Neutrals(
    background = Color(0xFFF7F4EE),
    backgroundGrouped = Color(0xFFF1EDE5),
    surface = Color(0xFFFFFDF9),
    surfaceElevated = Color(0xFFFFFDF9),
    fill = Color(0xFFEAE4DA),
    separator = Color(0xFFE2DBCF),
    outline = Color(0xFF8C8273),
    contentPrimary = Color(0xFF15130F),
    contentSecondary = Color(0xFF3F3931),
    contentTertiary = Color(0xFF655D51),
    skeletonHighlight = Color(0xFFF7F4EE),
)

private val PaperDark = Neutrals(
    background = Color(0xFF12110F),
    backgroundGrouped = Color(0xFF12110F),
    surface = Color(0xFF1B1916),
    surfaceElevated = Color(0xFF25221E),
    fill = Color(0xFF2E2A25),
    separator = Color(0xFF34302A),
    outline = Color(0xFF7A7164),
    contentPrimary = Color(0xFFF4EFE6),
    contentSecondary = Color(0xFFCBC2B4),
    contentTertiary = Color(0xFFA0978A),
    skeletonHighlight = Color(0xFF443F37),
)

private val CreamLight = Neutrals(
    background = Color(0xFFFFF7EA),
    backgroundGrouped = Color(0xFFFCF1DE),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    fill = Color(0xFFF6E8D0),
    separator = Color(0xFFF0DFC2),
    outline = Color(0xFF94855F),
    contentPrimary = Color(0xFF1A1206),
    contentSecondary = Color(0xFF45392A),
    contentTertiary = Color(0xFF6A5B43),
    skeletonHighlight = Color(0xFFFFF7EA),
)

private val CreamDark = Neutrals(
    background = Color(0xFF15120D),
    backgroundGrouped = Color(0xFF15120D),
    surface = Color(0xFF1F1A13),
    surfaceElevated = Color(0xFF2A2419),
    fill = Color(0xFF332C20),
    separator = Color(0xFF3A3224),
    outline = Color(0xFF7E715A),
    contentPrimary = Color(0xFFFBF3E4),
    contentSecondary = Color(0xFFD6C8B0),
    contentTertiary = Color(0xFFA99A80),
    skeletonHighlight = Color(0xFF4A4030),
)

/**
 * The type scale in a style's voice. Only weights move: sizes and line heights stay on the scale,
 * so a style change never reflows a layout, and the fonts download only the weights listed here.
 */
fun AppTypography.withVoice(voice: TypeVoice): AppTypography = when (voice) {
    TypeVoice.Neutral -> this

    TypeVoice.Bold -> copy(
        largeTitle = largeTitle.weight(FontWeight.ExtraBold),
        title1 = title1.weight(FontWeight.ExtraBold),
        title2 = title2.weight(FontWeight.ExtraBold),
        title3 = title3.weight(FontWeight.Bold),
        headline = headline.weight(FontWeight.Bold),
    )

    TypeVoice.Editorial -> copy(
        largeTitle = largeTitle.weight(FontWeight.Medium),
        title1 = title1.weight(FontWeight.Medium),
        title2 = title2.weight(FontWeight.Medium),
    )

    TypeVoice.Rounded -> copy(
        largeTitle = largeTitle.weight(FontWeight.Black),
        title1 = title1.weight(FontWeight.Black),
        title2 = title2.weight(FontWeight.ExtraBold),
        title3 = title3.weight(FontWeight.ExtraBold),
        headline = headline.weight(FontWeight.Bold),
        caption2 = caption2.weight(FontWeight.SemiBold),
    )
}

private fun TextStyle.weight(weight: FontWeight) = copy(fontWeight = weight)
