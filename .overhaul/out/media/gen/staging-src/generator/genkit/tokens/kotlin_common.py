"""
The resolved tokens for the Compose Multiplatform template's `commonMain`, in the shape that
template's design system reads. It is not yet the Android design system: its colours are built
from `Roles` and its springs from Compose's stiffness and damping ratio, and it declares fewer
theme types (no bar indicator, sheet material or status colour class). So the values are the ones
the Android copy carries, read from the same token files, and the classes are the multiplatform
template's own. Only plain values, `Color`, `Dp` and `TextUnit` are used, which Compose
Multiplatform keeps in the same packages.

The shared file takes the Android values of the platform-specific sizes: the 48 dp touch target is
above iOS's 44 pt minimum, so it satisfies both.
"""

from __future__ import annotations

from typing import Any

from . import kotlin

#: Where the multiplatform copy lives, under the module's source root and the package path.
RELATIVE_PATH = (
    "core/designsystem/src/commonMain/kotlin/{package_path}/core/designsystem/theme/tokens/DesignTokens.kt"
)

#: The platform whose values the shared file takes where the tokens differ per platform.
PLATFORM = "android"

#: The theme types the emitted file names, all declared in the multiplatform design system's `theme` package.
THEME_TYPES = (
    "AppDesignStyle", "AppMotionStyle", "BarTreatment", "CardTreatment", "FieldTreatment", "SurfaceTone", "TypeVoice",
)


def emit(tokens: dict[str, Any], package: str) -> str:
    theme = f"{package}.core.designsystem.theme"
    lines = [
        kotlin.HEADER,
        f"package {theme}.tokens",
        "",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.unit.Dp",
        "import androidx.compose.ui.unit.TextUnit",
        "import androidx.compose.ui.unit.dp",
        "import androidx.compose.ui.unit.sp",
        *(f"import {theme}.{name}" for name in THEME_TYPES),
        "",
        "internal object DesignTokens {",
    ]
    sections = [
        kotlin._dp_object("Spacing", tokens["spacing"]),
        kotlin._dp_object("Radii", tokens["radii"]),
        kotlin._dp_object("Sizes", tokens["sizes"]),
        kotlin._borders(tokens["borders"]),
        kotlin._dp_object("Elevation", tokens["elevation"]),
        kotlin._type(tokens["type"]),
        _motion(tokens["motion"]),
        _palette(tokens["palette"]),
        _styles(tokens["styles"]),
        kotlin._defaults(tokens["defaults"]),
    ]
    for section in sections:
        lines.append("")
        lines.extend(kotlin._indent(section))
    lines.append("}")
    return "\n".join(lines) + "\n"


def _motion(motion: dict[str, Any]) -> list[str]:
    spring_names = list(next(iter(motion["styles"].values()))["springs"])
    lines = [
        "/** A spring as Compose takes it: mass 1. */",
        "class Spring(val stiffness: Float, val dampingRatio: Float)",
        "",
        "class MotionStyle(",
        *(f"    val {name}: Spring," for name in spring_names),
        ")",
        "",
        "object Motion {",
    ]
    for style, values in motion["styles"].items():
        lines.append(f"    val {kotlin._const(style)} = MotionStyle(")
        for name, spring in values["springs"].items():
            stiffness, damping = kotlin._float(spring["stiffness"]), kotlin._float(spring["dampingRatio"])
            lines.append(f"        {name} = Spring({stiffness}, {damping}),")
        lines.append("    )")
    navigation = motion["navigation"]
    lines += [
        "",
        f"    const val visibilityThreshold: Float = {kotlin._float(motion['visibilityThreshold']['float'])}",
        f"    const val offsetVisibilityThresholdPx: Int = {int(motion['visibilityThreshold']['offsetPx'])}",
        f"    const val fadeShortMs: Int = {int(motion['fades']['shortMs'])}",
        f"    const val fadeLongMs: Int = {int(motion['fades']['longMs'])}",
        f"    const val staggerStepMs: Int = {int(motion['stagger']['stepMs'])}",
        f"    const val staggerMaxItems: Int = {int(motion['stagger']['maxItems'])}",
        f"    const val pressScale: Float = {kotlin._float(motion['press']['scale'])}",
        f"    const val pressScaleLarge: Float = {kotlin._float(motion['press']['scaleLarge'])}",
        f"    const val pressScaleIcon: Float = {kotlin._float(motion['press']['scaleIcon'])}",
        f"    const val pressDimAlpha: Float = {kotlin._float(motion['press']['dimAlpha'])}",
        f"    const val highlightFadeOutMs: Int = {int(motion['press']['highlightFadeOutMs'])}",
        f"    const val navigationParallax: Float = {kotlin._float(navigation['parallax'])}",
        f"    const val navigationScrimAlphaLight: Float = {kotlin._float(navigation['scrimAlpha']['light'])}",
        f"    const val navigationScrimAlphaDark: Float = {kotlin._float(navigation['scrimAlpha']['dark'])}",
        f"    val navigationEdgeShadow: Dp = {kotlin._dp(navigation['edgeShadow'])}",
        "}",
    ]
    return lines


def _palette(palette: dict[str, Any]) -> list[str]:
    sample = palette["tones"][next(iter(palette["tones"]))]["light"]
    plain = [name for name, value in sample.items() if not isinstance(value, dict)]
    statuses = [name for name, value in sample.items() if isinstance(value, dict)]
    status_fields = list(sample[statuses[0]]) if statuses else []

    lines = [
        f"class Status({', '.join(f'val {field}: Color' for field in status_fields)})",
        "",
        "class Roles(",
        *(f"    val {name}: Color," for name in plain),
        *(f"    val {name}: Status," for name in statuses),
        ")",
        "",
        "object Palette {",
    ]
    for ramp, steps in palette["ramps"].items():
        for step, value in steps.items():
            lines.append(f"    val {ramp}{step} = {kotlin._color(value)}")
    for tone, themes in palette["tones"].items():
        lines += ["", f"    object {tone} {{"]
        for theme, roles in themes.items():
            lines.append(f"        val {theme} = Roles(")
            for name in plain:
                lines.append(f"            {name} = {kotlin._color(roles[name])},")
            for name in statuses:
                values = ", ".join(kotlin._color(roles[name][field]) for field in status_fields)
                lines.append(f"            {name} = Status({values}),")
            lines.append("        )")
        lines.append("    }")
    lines.append("}")
    return lines


def _styles(styles: dict[str, Any]) -> list[str]:
    radii = list(next(iter(styles.values()))["radii"])
    lines = [
        "class Style(",
        *(f"    val radius{key.capitalize()}: Dp," for key in radii),
        "    /** Null: a pill, as round as the component is tall. */",
        "    val buttonRadius: Dp?,",
        "    val chipRadius: Dp?,",
        "    val card: CardTreatment,",
        "    val field: FieldTreatment,",
        "    val bar: BarTreatment,",
        "    val borderWidth: Dp,",
        "    val borderWidthStrong: Dp,",
        "    val uppercaseLabels: Boolean,",
        "    val offsetShadow: Dp,",
        "    val surfaces: SurfaceTone,",
        "    val voice: TypeVoice,",
        ")",
        "",
        "object Styles {",
    ]
    for name, style in styles.items():
        lines.append(f"    val {kotlin._const(name)} = Style(")
        lines += [f"        radius{key.capitalize()} = {kotlin._dp(style['radii'][key])}," for key in radii]
        for key in ("buttonRadius", "chipRadius"):
            lines.append(f"        {key} = {'null' if style[key] == 'pill' else kotlin._dp(style[key])},")
        lines += [
            f"        card = CardTreatment.{style['card']},",
            f"        field = FieldTreatment.{style['field']},",
            f"        bar = BarTreatment.{style['bar']},",
            f"        borderWidth = {kotlin._dp(style['borderWidth'])},",
            f"        borderWidthStrong = {kotlin._dp(style['borderWidthStrong'])},",
            f"        uppercaseLabels = {'true' if style['uppercaseLabels'] else 'false'},",
            f"        offsetShadow = {kotlin._dp(style['offsetShadow'])},",
            f"        surfaces = SurfaceTone.{style['surfaces']},",
            f"        voice = TypeVoice.{style['voice']},",
            "    )",
        ]
    lines.append("}")
    return lines
