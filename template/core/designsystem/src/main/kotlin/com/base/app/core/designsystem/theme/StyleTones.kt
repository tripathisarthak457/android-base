package com.base.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** The palette with a style's neutrals in place of the template's. */
fun AppColors.withSurfaces(tone: SurfaceTone): AppColors = when (tone) {
    SurfaceTone.Cool -> this
    SurfaceTone.BrandTint -> brandTinted()
    SurfaceTone.Paper -> if (isLight) PaperLight.applyTo(this) else PaperDark.applyTo(this)
    SurfaceTone.Cream -> if (isLight) CreamLight.applyTo(this) else CreamDark.applyTo(this)
}

/*
 * A few percent of the accent, never more. Enough that a feed in a pink brand reads as pink at a
 * glance; any more and grey text on it starts to lose contrast and every surface looks selected.
 */
private fun AppColors.brandTinted(): AppColors {
    fun tint(base: Color, amount: Float) = lerp(base, accent, amount)
    return copy(
        background = tint(background, if (isLight) 0.045f else 0.05f),
        surfaceVariant = tint(surfaceVariant, if (isLight) 0.07f else 0.07f),
        surface = if (isLight) surface else tint(surface, 0.05f),
        border = tint(border, 0.1f),
        divider = tint(divider, 0.08f),
        skeleton = tint(skeleton, 0.08f),
    )
}

private class Neutrals(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
) {
    fun applyTo(colors: AppColors) = colors.copy(
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        contentPrimary = contentPrimary,
        contentSecondary = contentSecondary,
        contentTertiary = contentTertiary,
        border = border,
        borderStrong = borderStrong,
        divider = divider,
        skeleton = divider,
    )
}

private val PaperLight = Neutrals(
    background = Color(0xFFF7F4EE),
    surface = Color(0xFFFFFDF9),
    surfaceVariant = Color(0xFFEFEAE1),
    contentPrimary = Color(0xFF15130F),
    contentSecondary = Color(0xFF3F3931),
    contentTertiary = Color(0xFF6A6256),
    border = Color(0xFFE2DBCF),
    borderStrong = Color(0xFFCCC2B1),
    divider = Color(0xFFEAE4DA),
)

private val PaperDark = Neutrals(
    background = Color(0xFF12110F),
    surface = Color(0xFF1B1916),
    surfaceVariant = Color(0xFF25221E),
    contentPrimary = Color(0xFFF4EFE6),
    contentSecondary = Color(0xFFCBC2B4),
    contentTertiary = Color(0xFF999080),
    border = Color(0xFF34302A),
    borderStrong = Color(0xFF4A443B),
    divider = Color(0xFF27241F),
)

private val CreamLight = Neutrals(
    background = Color(0xFFFFF7EA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFEFD6),
    contentPrimary = Color(0xFF1A1206),
    contentSecondary = Color(0xFF45392A),
    contentTertiary = Color(0xFF6F6048),
    border = Color(0xFFF0DFC2),
    borderStrong = Color(0xFFDDC59D),
    divider = Color(0xFFF6E8D0),
)

private val CreamDark = Neutrals(
    background = Color(0xFF15120D),
    surface = Color(0xFF1F1A13),
    surfaceVariant = Color(0xFF2A2419),
    contentPrimary = Color(0xFFFBF3E4),
    contentSecondary = Color(0xFFD6C8B0),
    contentTertiary = Color(0xFFA3947B),
    border = Color(0xFF3A3224),
    borderStrong = Color(0xFF514634),
    divider = Color(0xFF2B2519),
)

/** The type scale in a style's voice. Sizes stay on the same grid; weight and tracking move. */
fun AppTypography.withVoice(voice: TypeVoice): AppTypography = when (voice) {
    TypeVoice.Neutral -> this

    TypeVoice.Bold -> copy(
        displayLarge = displayLarge.weight(FontWeight.Black),
        displayMedium = displayMedium.weight(FontWeight.Black),
        displaySmall = displaySmall.weight(FontWeight.ExtraBold),
        headingLarge = headingLarge.weight(FontWeight.ExtraBold),
        headingMedium = headingMedium.weight(FontWeight.ExtraBold),
        headingSmall = headingSmall.weight(FontWeight.ExtraBold),
        titleLarge = titleLarge.weight(FontWeight.Bold),
        titleMedium = titleMedium.weight(FontWeight.Bold),
        button = button.weight(FontWeight.ExtraBold),
    )

    TypeVoice.Editorial -> copy(
        displayLarge = displayLarge.grow(6, -0.035),
        displayMedium = displayMedium.grow(5, -0.03),
        displaySmall = displaySmall.grow(4, -0.03),
        headingLarge = headingLarge.grow(2, -0.02),
        headingMedium = headingMedium.grow(1, -0.015),
        label = label.copy(letterSpacing = 0.12.em),
        labelSmall = labelSmall.copy(letterSpacing = 0.14.em),
    )

    TypeVoice.Rounded -> copy(
        displayLarge = displayLarge.weight(FontWeight.Black),
        displayMedium = displayMedium.weight(FontWeight.Black),
        displaySmall = displaySmall.weight(FontWeight.Black),
        headingLarge = headingLarge.weight(FontWeight.ExtraBold),
        headingMedium = headingMedium.weight(FontWeight.ExtraBold),
        headingSmall = headingSmall.weight(FontWeight.ExtraBold),
        titleLarge = titleLarge.weight(FontWeight.ExtraBold),
        titleMedium = titleMedium.weight(FontWeight.Bold),
        titleSmall = titleSmall.weight(FontWeight.Bold),
        bodyLarge = bodyLarge.copy(lineHeight = (bodyLarge.lineHeight.value + 2).sp),
        bodyMedium = bodyMedium.copy(lineHeight = (bodyMedium.lineHeight.value + 2).sp),
        button = button.weight(FontWeight.ExtraBold),
    )
}

private fun TextStyle.weight(weight: FontWeight) = copy(fontWeight = weight)

private fun TextStyle.grow(bySp: Int, tracking: Double) = copy(
    fontSize = (fontSize.value + bySp).sp,
    lineHeight = (lineHeight.value + bySp).sp,
    letterSpacing = tracking.em,
)
