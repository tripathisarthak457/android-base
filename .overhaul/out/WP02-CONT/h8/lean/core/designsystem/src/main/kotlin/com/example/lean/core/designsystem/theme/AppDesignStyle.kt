package com.example.lean.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.example.lean.core.designsystem.theme.tokens.DesignTokens

/**
 * What the components look like, as one choice. These are starting points: the values are in
 * `DesignTokens.Styles`, so changing a style there moves every component with it, and each component
 * still accepts its own `shape` and friends.
 *
 * ```
 * AppTheme(designStyle = AppDesignStyle.Social) { … }
 * ```
 */
enum class AppDesignStyle {
    /** Cool neutrals, hairline outlines, a floating tab bar. Dense and calm: tools, finance, admin. */
    Utility,

    /**
     * Surfaces faintly tinted with the brand, pill buttons, soft raised cards, filled fields, a
     * floating bar and heavier headlines. Feeds, chat, communities.
     */
    Social,

    /**
     * Warm paper and ink, near-square corners, underlined fields, uppercase labels and large, tight
     * display type. Reading, news, portfolios.
     */
    Editorial,

    /** Cream surfaces, thick outlines, hard offset shadows and extra-bold type. Games, kids. */
    Playful,
    ;

    /** The tokens this style decides, built once per process. */
    fun style(): AppStyle = StylesByDesign.getValue(this)

    internal fun tokens(): DesignTokens.Style = when (this) {
        Utility -> DesignTokens.Styles.utility
        Social -> DesignTokens.Styles.social
        Editorial -> DesignTokens.Styles.editorial
        Playful -> DesignTokens.Styles.playful
    }
}

private val StylesByDesign = AppDesignStyle.entries.associateWith { design ->
    val tokens = design.tokens()
    AppStyle(
        shapes = AppShapes(
            xs = RoundedCornerShape(tokens.radiusXs),
            sm = RoundedCornerShape(tokens.radiusSm),
            md = RoundedCornerShape(tokens.radiusMd),
            lg = RoundedCornerShape(tokens.radiusLg),
            xl = RoundedCornerShape(tokens.radiusXl),
            sheet = RoundedCornerShape(topStart = tokens.radiusSheet, topEnd = tokens.radiusSheet),
        ),
        buttonShape = tokens.buttonRadius.shape(),
        chipShape = tokens.chipRadius.shape(),
        card = tokens.card,
        field = tokens.field,
        bar = tokens.bar,
        indicator = tokens.indicator,
        borderWidth = tokens.borderWidth,
        borderWidthStrong = tokens.borderWidthStrong,
        uppercaseLabels = tokens.uppercaseLabels,
        offsetShadow = tokens.offsetShadow,
        sheetRadius = tokens.radiusSheet,
        sheetInset = tokens.sheetInset,
        sheetMaterial = tokens.sheetMaterial,
        surfaces = tokens.surfaces,
        voice = tokens.voice,
    )
}

/** A null radius is a pill: as round as the component is tall, whatever its width. */
private fun Dp?.shape(): Shape = if (this == null) RoundedCornerShape(percent = 50) else RoundedCornerShape(this)

enum class CardTreatment {
    /** A hairline outline and no shadow. */
    Outlined,

    /** No outline, a soft shadow in light theme and a lighter surface in dark. */
    Raised,

    /** A strong outline with a solid shadow offset down and to the right. */
    Offset,
}

enum class FieldTreatment {
    /** A box with an outline that thickens and takes the accent on focus. */
    Outlined,

    /** A tinted box with no outline until it has focus or an error. */
    Filled,

    /** No box at all: a line underneath, as on paper. */
    Underlined,
}

/** How the tab bar is drawn. Editing a style's treatment changes the bar app-wide. */
enum class BarTreatment {
    /** A capsule lifted off the bottom edge, on the elevated surface with a hairline and one soft shadow. */
    Floating,

    /** The capsule on the surface at 88 % opacity, so content shows faintly through at no GPU cost. */
    Translucent,

    /** Full width against the bottom edge with a hairline above, as iOS drew it before 26. */
    Docked,
}

/** How the bar marks the selected tab. */
enum class BarIndicator {
    /** A solid pill in `barIndicator` behind the selected item, its glyph and label in `onAccent`. */
    Pill,

    /** No pill: the selected glyph turns filled and takes `accentContent`, which carries selection alone. */
    Subtle,
}

/** What a sheet's surface is made of. A design style picks one of the last three for its sheets. */
enum class SheetMaterial {
    /** The design style's choice: Solid unless the style is made of glass. */
    Automatic,

    /** The opaque container. The cheapest, and what every material becomes under Reduce transparency. */
    Solid,

    /** Drawn translucency (the container at 92-94 %, a rim). It samples nothing: a few cached draws. */
    Frosted,

    /**
     * Live liquid glass at partial detents on the tiers that sample the scene, opaque by halfway to
     * the large detent; Frosted on the lower tiers and in projects without liquid glass.
     */
    Glass,
}

/** The neutrals a style sits on. The accent ramps are the brand's and never change here. */
enum class SurfaceTone {
    /** The template's cool greys. */
    Cool,

    /** The cool greys with a few percent of the accent mixed in, so the app reads as the brand's. */
    BrandTint,

    /** Warm off-white paper and warm ink. */
    Paper,

    /** Cream and butter, warmer and brighter than paper. */
    Cream,
}

/** How the type scale speaks. The typeface is still the one `AppTheme` was given. */
enum class TypeVoice {
    /** The scale as `appTypography` defines it. */
    Neutral,

    /** Headlines and titles a weight heavier. */
    Bold,

    /** Display sizes larger and tighter, labels more widely tracked. */
    Editorial,

    /** Everything above body text extra-bold, body a little airier. */
    Rounded,
}

/** The tokens a design style decides. Read through `AppTheme.style`. */
@Immutable
data class AppStyle(
    val shapes: AppShapes,
    val buttonShape: Shape,
    val chipShape: Shape,
    val card: CardTreatment,
    val field: FieldTreatment,
    val bar: BarTreatment,
    val indicator: BarIndicator,
    val borderWidth: Dp,
    val borderWidthStrong: Dp,
    val uppercaseLabels: Boolean,
    val offsetShadow: Dp,
    /** Every corner of a sheet; a sheet keeps one radius whether it floats or is attached. */
    val sheetRadius: Dp,
    /** The gap a sheet floats at, from the sides and bottom, at its partial detents; 0 keeps it attached. */
    val sheetInset: Dp,
    /** What [SheetMaterial.Automatic] sheets are made of; never Automatic itself. */
    val sheetMaterial: SheetMaterial,
    val surfaces: SurfaceTone,
    val voice: TypeVoice,
)

internal val LocalAppStyle = staticCompositionLocalOf { AppDesignStyle.Utility.style() }
