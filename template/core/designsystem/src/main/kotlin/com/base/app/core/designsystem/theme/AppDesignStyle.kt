package com.base.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * What the components look like, as one choice. These are starting points: edit [style] to move
 * a style towards your own, and every component still accepts its own `shape` and friends.
 *
 * ```
 * AppTheme(designStyle = AppDesignStyle.Social) { … }
 * ```
 */
enum class AppDesignStyle {
    /** Cool neutrals, hairline outlines, a docked bar. Dense and calm: tools, finance, admin. */
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

    fun style(): AppStyle = when (this) {
        Utility -> AppStyle()

        Social -> AppStyle(
            shapes = AppShapes(
                xs = RoundedCornerShape(10.dp),
                sm = RoundedCornerShape(14.dp),
                md = RoundedCornerShape(20.dp),
                lg = RoundedCornerShape(26.dp),
                xl = RoundedCornerShape(32.dp),
                sheet = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            ),
            buttonShape = RoundedCornerShape(percent = 50),
            chipShape = RoundedCornerShape(percent = 50),
            card = CardTreatment.Raised,
            field = FieldTreatment.Filled,
            bar = BarTreatment.Floating,
            surfaces = SurfaceTone.BrandTint,
            voice = TypeVoice.Bold,
        )

        Editorial -> AppStyle(
            shapes = AppShapes(
                xs = RoundedCornerShape(2.dp),
                sm = RoundedCornerShape(3.dp),
                md = RoundedCornerShape(4.dp),
                lg = RoundedCornerShape(6.dp),
                xl = RoundedCornerShape(8.dp),
                sheet = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            ),
            buttonShape = RoundedCornerShape(2.dp),
            chipShape = RoundedCornerShape(2.dp),
            card = CardTreatment.Outlined,
            field = FieldTreatment.Underlined,
            bar = BarTreatment.Minimal,
            uppercaseLabels = true,
            surfaces = SurfaceTone.Paper,
            voice = TypeVoice.Editorial,
        )

        Playful -> AppStyle(
            shapes = AppShapes(
                xs = RoundedCornerShape(8.dp),
                sm = RoundedCornerShape(12.dp),
                md = RoundedCornerShape(16.dp),
                lg = RoundedCornerShape(22.dp),
                xl = RoundedCornerShape(28.dp),
                sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ),
            buttonShape = RoundedCornerShape(14.dp),
            chipShape = RoundedCornerShape(12.dp),
            card = CardTreatment.Offset,
            field = FieldTreatment.Outlined,
            bar = BarTreatment.Chunky,
            borderWidth = 2.dp,
            borderWidthStrong = 2.5.dp,
            offsetShadow = 4.dp,
            surfaces = SurfaceTone.Cream,
            voice = TypeVoice.Rounded,
        )
    }
}

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

enum class BarTreatment {
    /** Full width, a hairline above, icon and label per tab. */
    Docked,

    /** A rounded capsule lifted off the bottom edge, the selected tab in a tinted pill. */
    Floating,

    /** Full width and flat, uppercase labels, a short rule over the selected tab. */
    Minimal,

    /** Full width with a heavy top rule, the selected icon sitting in a solid pill. */
    Chunky,
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
    val shapes: AppShapes = AppShapes(),
    val buttonShape: Shape = RoundedCornerShape(10.dp),
    val chipShape: Shape = RoundedCornerShape(percent = 50),
    val card: CardTreatment = CardTreatment.Outlined,
    val field: FieldTreatment = FieldTreatment.Outlined,
    val bar: BarTreatment = BarTreatment.Docked,
    val borderWidth: Dp = 1.dp,
    val borderWidthStrong: Dp = 1.5.dp,
    val uppercaseLabels: Boolean = false,
    val offsetShadow: Dp = 0.dp,
    val surfaces: SurfaceTone = SurfaceTone.Cool,
    val voice: TypeVoice = TypeVoice.Neutral,
)

internal val LocalAppStyle = staticCompositionLocalOf { AppStyle() }
