package com.base.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/**
 * The look of the whole app as one choice: corners, how cards, fields and the tab bar are drawn,
 * the surface tone and the type voice. The values are in `DesignTokens.Styles`.
 */
enum class AppDesignStyle {
    /** Cool neutrals, hairline outlines, a docked bar. Dense and calm: tools, finance, admin. */
    Utility,

    /** Brand-tinted surfaces, pill buttons, soft raised cards and heavier headlines. */
    Social,

    /** Paper and ink, square corners and small uppercase labels. Reading apps, publications. */
    Editorial,

    /** Cream surfaces, thick outlines with an offset shadow, rounded type. Games, kids, hobbies. */
    Playful,
    ;

    internal fun tokens(): DesignTokens.Style = when (this) {
        Utility -> DesignTokens.Styles.utility
        Social -> DesignTokens.Styles.social
        Editorial -> DesignTokens.Styles.editorial
        Playful -> DesignTokens.Styles.playful
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
    /** A box with an outline that takes the accent on focus. */
    Outlined,

    /** A tinted box with no outline until it has focus or an error. */
    Filled,

    /** No box at all: a line underneath, as on paper. */
    Underlined,
}

enum class BarTreatment {
    /** Full width, a hairline above, icon and label per tab. */
    Docked,

    /** A rounded capsule lifted off the bottom edge. */
    Floating,

    /** Full width and flat, uppercase labels. */
    Minimal,

    /** Full width, a thick outline above, heavier labels. */
    Chunky,
}

/** Which neutral palette the surfaces are drawn from. */
enum class SurfaceTone {
    /** The template's cool greys. */
    Cool,

    /** The cool greys with a few percent of the accent mixed in. */
    BrandTint,

    /** Warm off-white paper and warm ink. */
    Paper,

    /** A soft cream, for the playful style. */
    Cream,
}

/** How headlines and labels are weighted. Sizes never change between voices. */
enum class TypeVoice {
    Neutral,
    Bold,
    Editorial,
    Rounded,
}

/** What a design style resolves to for the components that read it. */
@Immutable
class AppStyle internal constructor(tokens: DesignTokens.Style) {
    /** Null corners are a pill: as round as the button is tall. */
    val buttonShape: Shape = tokens.buttonRadius?.let(::RoundedCornerShape) ?: RoundedCornerShape(percent = 50)
    val sectionShape: Shape = RoundedCornerShape(tokens.radiusMd)
    val fieldShape: Shape = RoundedCornerShape(tokens.radiusSm)
    val bar: BarTreatment = tokens.bar
    val surfaces: SurfaceTone = tokens.surfaces
    val voice: TypeVoice = tokens.voice
}

private val StylesByDesign = AppDesignStyle.entries.associateWith { AppStyle(it.tokens()) }

/** The resolved style for this design, built once per process. */
fun AppDesignStyle.style(): AppStyle = StylesByDesign.getValue(this)

internal val LocalAppStyle = staticCompositionLocalOf { AppDesignStyle.Utility.style() }
