"""
The resolved tokens as Kotlin: `internal object DesignTokens` in `:core:designsystem`, plain values
and small classes, with every spring precomputed per motion style so nothing is derived at runtime.
"""

from __future__ import annotations

from typing import Any

from . import number

HEADER = (
    "// Generated from tokens/*.json by generator/genkit/tokens. "
    "Do not edit; run python3 -m genkit.tokens --write."
)

#: Where the Android copy lives, under the module's source root and the package path.
RELATIVE_PATH = "core/designsystem/src/main/kotlin/{package_path}/core/designsystem/theme/tokens/DesignTokens.kt"


def emit(tokens: dict[str, Any], package: str) -> str:
    theme = f"{package}.core.designsystem.theme"
    lines = [
        HEADER,
        f"package {theme}.tokens",
        "",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.unit.Dp",
        "import androidx.compose.ui.unit.TextUnit",
        "import androidx.compose.ui.unit.dp",
        "import androidx.compose.ui.unit.sp",
        *(
            f"import {theme}.{name}"
            for name in (
                "AppDesignStyle", "AppMotionStyle", "BarTreatment", "CardTreatment", "FieldTreatment",
                "SurfaceTone", "TypeVoice",
            )
        ),
        "",
        "internal object DesignTokens {",
    ]
    sections = [
        _dp_object("Spacing", tokens["spacing"]),
        _dp_object("Radii", tokens["radii"]),
        _dp_object("Sizes", tokens["sizes"]),
        _borders(tokens["borders"]),
        _dp_object("Elevation", tokens["elevation"]),
        _type(tokens["type"]),
        _motion(tokens["motion"]),
        _palette(tokens["palette"]),
        _styles(tokens["styles"]),
        _defaults(tokens["defaults"]),
    ]
    for section in sections:
        lines.append("")
        lines.extend(_indent(section))
    lines.append("}")
    return "\n".join(lines) + "\n"


def _indent(lines: list[str]) -> list[str]:
    return [f"    {line}" if line else "" for line in lines]


def _float(value: float) -> str:
    return f"{repr(round(float(value), 4))}f"


def _dp(value: float) -> str:
    text = number(value)
    return f"({text}).dp" if text.startswith("-") else f"{text}.dp"


def _sp(value: float) -> str:
    text = number(value)
    return f"({text}).sp" if text.startswith("-") else f"{text}.sp"


def _color(hex_argb: str) -> str:
    return f"Color(0x{hex_argb.lstrip('#')})"


def _const(name: str) -> str:
    return name[:1].lower() + name[1:]


def _dp_object(name: str, values: dict[str, float]) -> list[str]:
    return [f"object {name} {{", *(f"    val {key}: Dp = {_dp(value)}" for key, value in values.items()), "}"]


def _borders(borders: dict[str, float]) -> list[str]:
    return [
        "object Borders {",
        "    /** Physical pixels, not dp: a hairline is one device pixel at every density. */",
        f"    const val hairlinePx: Int = {int(borders['hairlinePx'])}",
        f"    val width: Dp = {_dp(borders['width'])}",
        f"    val widthStrong: Dp = {_dp(borders['widthStrong'])}",
        "}",
    ]


def _type(type_tokens: dict[str, Any]) -> list[str]:
    voices = type_tokens["voices"]
    lines = [
        "/** One role of the type scale. Each voice changes the weight only. */",
        "class TypeRole(",
        "    val size: TextUnit,",
        "    val lineHeight: TextUnit,",
        "    val tracking: TextUnit,",
        *(f"    val weight{voice}: Int," for voice in voices),
        ")",
        "",
        "object Type {",
    ]
    for name, role in type_tokens["roles"].items():
        weights = ", ".join(str(role["weight"][voice]) for voice in voices)
        lines.append(
            f"    val {name} = TypeRole({_sp(role['size'])}, {_sp(role['lineHeight'])}, "
            f"{_sp(role['tracking'])}, {weights})"
        )
    lines.append("}")
    return lines


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
        lines.append(f"    val {_const(style)} = MotionStyle(")
        for name, spring in values["springs"].items():
            lines.append(f"        {name} = Spring({_float(spring['stiffness'])}, {_float(spring['dampingRatio'])}),")
        lines.append("    )")
    navigation = motion["navigation"]
    lines += [
        "",
        f"    const val visibilityThreshold: Float = {_float(motion['visibilityThreshold']['float'])}",
        f"    const val offsetVisibilityThresholdPx: Int = {int(motion['visibilityThreshold']['offsetPx'])}",
        f"    const val fadeShortMs: Int = {int(motion['fades']['shortMs'])}",
        f"    const val fadeLongMs: Int = {int(motion['fades']['longMs'])}",
        f"    const val staggerStepMs: Int = {int(motion['stagger']['stepMs'])}",
        f"    const val staggerMaxItems: Int = {int(motion['stagger']['maxItems'])}",
        f"    const val pressScale: Float = {_float(motion['press']['scale'])}",
        f"    const val pressScaleLarge: Float = {_float(motion['press']['scaleLarge'])}",
        f"    const val pressScaleIcon: Float = {_float(motion['press']['scaleIcon'])}",
        f"    const val pressDimAlpha: Float = {_float(motion['press']['dimAlpha'])}",
        f"    const val highlightFadeOutMs: Int = {int(motion['press']['highlightFadeOutMs'])}",
        f"    const val navigationParallax: Float = {_float(navigation['parallax'])}",
        f"    const val navigationScrimAlphaLight: Float = {_float(navigation['scrimAlpha']['light'])}",
        f"    const val navigationScrimAlphaDark: Float = {_float(navigation['scrimAlpha']['dark'])}",
        f"    val navigationEdgeShadow: Dp = {_dp(navigation['edgeShadow'])}",
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
            lines.append(f"    val {ramp}{step} = {_color(value)}")
    for tone, themes in palette["tones"].items():
        lines += ["", f"    object {tone} {{"]
        for theme, roles in themes.items():
            lines.append(f"        val {theme} = Roles(")
            for name in plain:
                lines.append(f"            {name} = {_color(roles[name])},")
            for name in statuses:
                values = ", ".join(_color(roles[name][field]) for field in status_fields)
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
        lines.append(f"    val {_const(name)} = Style(")
        lines += [f"        radius{key.capitalize()} = {_dp(style['radii'][key])}," for key in radii]
        for key in ("buttonRadius", "chipRadius"):
            lines.append(f"        {key} = {'null' if style[key] == 'pill' else _dp(style[key])},")
        lines += [
            f"        card = CardTreatment.{style['card']},",
            f"        field = FieldTreatment.{style['field']},",
            f"        bar = BarTreatment.{style['bar']},",
            f"        borderWidth = {_dp(style['borderWidth'])},",
            f"        borderWidthStrong = {_dp(style['borderWidthStrong'])},",
            f"        uppercaseLabels = {'true' if style['uppercaseLabels'] else 'false'},",
            f"        offsetShadow = {_dp(style['offsetShadow'])},",
            f"        surfaces = SurfaceTone.{style['surfaces']},",
            f"        voice = TypeVoice.{style['voice']},",
            "    )",
        ]
    lines.append("}")
    return lines


def _defaults(defaults: dict[str, Any]) -> list[str]:
    return [
        "/** The project's own choices, which `AppTheme` falls back to. */",
        "object Defaults {",
        f"    val designStyle: AppDesignStyle = AppDesignStyle.{defaults['designStyle']}",
        f"    val motionStyle: AppMotionStyle = AppMotionStyle.{defaults['motionStyle']}",
        f"    const val hapticsEnabled: Boolean = {'true' if defaults['hapticsEnabled'] else 'false'}",
        f"    const val fontName: String = \"{defaults['fontName']}\"",
        f"    const val monoFontName: String = \"{defaults['monoFontName']}\"",
        "}",
    ]
