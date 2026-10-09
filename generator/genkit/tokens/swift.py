"""
The resolved tokens as Swift: `enum DesignTokens`, mirroring the Kotlin object. Springs keep
SwiftUI's own model, `Spring(duration: response, bounce:)`; sizes are points.
"""

from __future__ import annotations

from typing import Any

HEADER = (
    "// Generated from tokens/*.json by generator/genkit/tokens. "
    "Do not edit; run python3 -m genkit.tokens --write."
)

RELATIVE_PATH = "Packages/Core/Sources/DesignSystem/Tokens/DesignTokens.swift"


def emit(tokens: dict[str, Any]) -> str:
    lines = [HEADER, "import SwiftUI", "", "enum DesignTokens {"]
    sections = [
        _points("Spacing", tokens["spacing"]),
        _points("Radii", tokens["radii"]),
        _points("Sizes", tokens["sizes"]),
        _points("Borders", tokens["borders"]),
        _points("Elevation", tokens["elevation"]),
        _type(tokens["type"]),
        _motion(tokens["motion"]),
        _palette(tokens["palette"]),
        _styles(tokens["styles"]),
        _defaults(tokens["defaults"]),
    ]
    for section in sections:
        lines.append("")
        lines.extend(f"    {line}" if line else "" for line in section)
    lines.append("}")
    return "\n".join(lines) + "\n"


def _number(value: float) -> str:
    return str(int(value)) if float(value).is_integer() else repr(round(float(value), 4))


def _lower(name: str) -> str:
    return name[:1].lower() + name[1:]


def _color(hex_argb: str) -> str:
    digits = hex_argb.lstrip("#")
    alpha, red, green, blue = (int(digits[i : i + 2], 16) / 255 for i in (0, 2, 4, 6))
    return (
        f"Color(.sRGB, red: {red:.4f}, green: {green:.4f}, blue: {blue:.4f}, opacity: {alpha:.4f})"
    )


def _points(name: str, values: dict[str, float]) -> list[str]:
    return [f"enum {name} {{", *(f"    static let {key}: CGFloat = {_number(value)}" for key, value in values.items()), "}"]


def _type(type_tokens: dict[str, Any]) -> list[str]:
    voices = type_tokens["voices"]
    lines = [
        "struct TypeRole {",
        "    let size: CGFloat",
        "    let lineHeight: CGFloat",
        "    let tracking: CGFloat",
        *(f"    let weight{voice}: Int" for voice in voices),
        "}",
        "",
        "enum Typography {",
    ]
    for name, role in type_tokens["roles"].items():
        weights = ", ".join(f"weight{voice}: {role['weight'][voice]}" for voice in voices)
        lines.append(
            f"    static let {name} = TypeRole(size: {_number(role['size'])}, lineHeight: "
            f"{_number(role['lineHeight'])}, tracking: {_number(role['tracking'])}, {weights})"
        )
    lines.append("}")
    return lines


def _motion(motion: dict[str, Any]) -> list[str]:
    spring_names = list(next(iter(motion["styles"].values()))["springs"])
    lines = [
        "struct SpringToken {",
        "    let response: Double",
        "    let bounce: Double",
        "    var spring: Spring { Spring(duration: response, bounce: bounce) }",
        "}",
        "",
        "struct MotionStyle {",
        *(f"    let {name}: SpringToken" for name in spring_names),
        "}",
        "",
        "enum Motion {",
    ]
    for style, values in motion["styles"].items():
        springs = ", ".join(
            f"{name}: SpringToken(response: {_number(spring['response'])}, bounce: {_number(spring['bounce'])})"
            for name, spring in values["springs"].items()
        )
        lines.append(f"    static let {_lower(style)} = MotionStyle({springs})")
    navigation = motion["navigation"]
    lines += [
        f"    static let fadeShort: Double = {_number(motion['fades']['shortMs'] / 1000)}",
        f"    static let fadeLong: Double = {_number(motion['fades']['longMs'] / 1000)}",
        f"    static let staggerStep: Double = {_number(motion['stagger']['stepMs'] / 1000)}",
        f"    static let staggerMaxItems: Int = {int(motion['stagger']['maxItems'])}",
        f"    static let pressScale: CGFloat = {_number(motion['press']['scale'])}",
        f"    static let pressScaleLarge: CGFloat = {_number(motion['press']['scaleLarge'])}",
        f"    static let pressScaleIcon: CGFloat = {_number(motion['press']['scaleIcon'])}",
        f"    static let pressDimAlpha: Double = {_number(motion['press']['dimAlpha'])}",
        f"    static let highlightFadeOut: Double = {_number(motion['press']['highlightFadeOutMs'] / 1000)}",
        f"    static let navigationParallax: CGFloat = {_number(navigation['parallax'])}",
        f"    static let navigationScrimAlphaLight: Double = {_number(navigation['scrimAlpha']['light'])}",
        f"    static let navigationScrimAlphaDark: Double = {_number(navigation['scrimAlpha']['dark'])}",
        f"    static let navigationEdgeShadow: CGFloat = {_number(navigation['edgeShadow'])}",
        "}",
    ]
    return lines


def _palette(palette: dict[str, Any]) -> list[str]:
    sample = palette["tones"][next(iter(palette["tones"]))]["light"]
    plain = [name for name, value in sample.items() if not isinstance(value, dict)]
    statuses = [name for name, value in sample.items() if isinstance(value, dict)]
    status_fields = list(sample[statuses[0]]) if statuses else []
    lines = [
        "struct Status {",
        *(f"    let {field}: Color" for field in status_fields),
        "}",
        "",
        "struct Roles {",
        *(f"    let {name}: Color" for name in plain),
        *(f"    let {name}: Status" for name in statuses),
        "}",
        "",
        "enum Palette {",
    ]
    for ramp, steps in palette["ramps"].items():
        for step, value in steps.items():
            lines.append(f"    static let {ramp}{step} = {_color(value)}")
    for tone, themes in palette["tones"].items():
        lines += ["", f"    enum {tone} {{"]
        for theme, roles in themes.items():
            lines.append(f"        static let {theme} = Roles(")
            arguments = [f"            {name}: {_color(roles[name])}" for name in plain]
            arguments += [
                f"            {name}: Status("
                + ", ".join(f"{field}: {_color(roles[name][field])}" for field in status_fields)
                + ")"
                for name in statuses
            ]
            # One argument per line, so the section indent emit() adds reaches every one of them.
            lines += [f"{argument}," for argument in arguments[:-1]] + arguments[-1:]
            lines.append("        )")
        lines.append("    }")
    lines.append("}")
    return lines


def _styles(styles: dict[str, Any]) -> list[str]:
    radii = list(next(iter(styles.values()))["radii"])
    lines = [
        "struct Style {",
        *(f"    let radius{key.capitalize()}: CGFloat" for key in radii),
        "    /// Nil: a pill, as round as the component is tall.",
        "    let buttonRadius: CGFloat?",
        "    let chipRadius: CGFloat?",
        "    let card: String",
        "    let field: String",
        "    let bar: String",
        "    let borderWidth: CGFloat",
        "    let borderWidthStrong: CGFloat",
        "    let uppercaseLabels: Bool",
        "    let offsetShadow: CGFloat",
        "    let surfaces: String",
        "    let voice: String",
        "}",
        "",
        "enum Styles {",
    ]
    for name, style in styles.items():
        arguments = [f"radius{key.capitalize()}: {_number(style['radii'][key])}" for key in radii]
        arguments += [
            f"{key}: {'nil' if style[key] == 'pill' else _number(style[key])}" for key in ("buttonRadius", "chipRadius")
        ]
        arguments += [f'{key}: "{style[key]}"' for key in ("card", "field", "bar")]
        arguments += [f"{key}: {_number(style[key])}" for key in ("borderWidth", "borderWidthStrong")]
        arguments.append(f"uppercaseLabels: {'true' if style['uppercaseLabels'] else 'false'}")
        arguments.append(f"offsetShadow: {_number(style['offsetShadow'])}")
        arguments += [f'{key}: "{style[key]}"' for key in ("surfaces", "voice")]
        lines.append(f"    static let {_lower(name)} = Style({', '.join(arguments)})")
    lines.append("}")
    return lines


def _defaults(defaults: dict[str, Any]) -> list[str]:
    return [
        "enum Defaults {",
        f"    static let designStyle = \"{defaults['designStyle']}\"",
        f"    static let motionStyle = \"{defaults['motionStyle']}\"",
        f"    static let hapticsEnabled = {'true' if defaults['hapticsEnabled'] else 'false'}",
        f"    static let fontName = \"{defaults['fontName']}\"",
        f"    static let monoFontName = \"{defaults['monoFontName']}\"",
        "}",
    ]
