package com.base.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * What the components look like, as one choice.
 *
 * The same eighty components suit very different apps once their corners, borders and bars
 * change together. Picking those one at a time produces a pill button beside a sharp card and a
 * floating bar over an underlined field; naming the combinations keeps them agreeing.
 *
 * ```
 * AppTheme(designStyle = AppDesignStyle.Social) { … }
 * ```
 *
 * These are starting points. Every component still takes a `shape` and friends, and the values
 * below are the place to push a style towards your own.
 */
enum class AppDesignStyle {
    /** Hairline outlines, modest corners, a docked bar. Dense and calm: tools, finance, admin. */
    Utility,

    /** Pill buttons, soft raised cards, filled fields, a floating bar. Feeds, chat, communities. */
    Social,

    /** Near-square corners, underlined fields, uppercase labels. Reading, news, portfolios. */
    Editorial,

    /** Thick outlines and hard offset shadows. Games, kids, anything that should feel like a toy. */
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

/**
 * The tokens a design style decides. Read through `AppTheme.style`.
 *
 * [offsetShadow] above zero turns on the hard shadow that [CardTreatment.Offset] and the
 * filled buttons draw; it is a distance, so a style can make it subtler without a new enum.
 */
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
)

internal val LocalAppStyle = staticCompositionLocalOf { AppStyle() }
