"""
The resolved tokens as Swift: `enum DesignTokens`, mirroring the Kotlin object. Springs keep
SwiftUI's own model, `Spring(duration: response, bounce:)`; sizes are points.
"""

from __future__ import annotations

from typing import Any

from . import number

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
        _borders(tokens["borders"]),
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


def _lower(name: str) -> str:
    return name[:1].lower() + name[1:]


def _color(hex_argb: str) -> str:
    digits = hex_argb.lstrip("#")
    alpha, red, green, blue = (int(digits[i : i + 2], 16) / 255 for i in (0, 2, 4, 6))
    return (
        f"Color(.sRGB, red: {red:.4f}, green: {green:.4f}, blue: {blue:.4f}, opacity: {alpha:.4f})"
    )


def _points(name: str, values: dict[str, float]) -> list[str]:
    return [f"enum {name} {{", *(f"    static let {key}: CGFloat = {number(value)}" for key, value in values.items()), "}"]


def _borders(borders: dict[str, float]) -> list[str]:
    widths = {key: value for key, value in borders.items() if key != "hairlinePx"}
    return [
        "enum Borders {",
        "    /// Physical pixels, not points: a hairline is `hairlinePx / displayScale` points on every screen.",
        f"    static let hairlinePx: Int = {int(borders['hairlinePx'])}",
        *_points("Borders", widths)[1:],
    ]


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
            f"    static let {name} = TypeRole(size: {number(role['size'])}, lineHeight: "
            f"{number(role['lineHeight'])}, tracking: {number(role['tracking'])}, {weights})"
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
            f"{name}: SpringToken(response: {number(spring['response'])}, bounce: {number(spring['bounce'])})"
            for name, spring in values["springs"].items()
        )
        lines.append(f"    static let {_lower(style)} = MotionStyle({springs})")
    navigation, skeleton = motion["navigation"], motion["skeleton"]
    easing = skeleton["easing"]
    lines += [
        f"    static let fadeShort: Double = {number(motion['fades']['shortMs'] / 1000)}",
        f"    static let fadeLong: Double = {number(motion['fades']['longMs'] / 1000)}",
        f"    static let staggerStep: Double = {number(motion['stagger']['stepMs'] / 1000)}",
        f"    static let staggerMaxItems: Int = {int(motion['stagger']['maxItems'])}",
        f"    static let pressScale: CGFloat = {number(motion['press']['scale'])}",
        f"    static let pressScaleLarge: CGFloat = {number(motion['press']['scaleLarge'])}",
        f"    static let pressScaleIcon: CGFloat = {number(motion['press']['scaleIcon'])}",
        f"    static let pressDimAlpha: Double = {number(motion['press']['dimAlpha'])}",
        f"    static let highlightFadeOut: Double = {number(motion['press']['highlightFadeOutMs'] / 1000)}",
        f"    static let navigationParallax: CGFloat = {number(navigation['parallax'])}",
        f"    static let navigationScrimAlphaLight: Double = {number(navigation['scrimAlpha']['light'])}",
        f"    static let navigationScrimAlphaDark: Double = {number(navigation['scrimAlpha']['dark'])}",
        f"    static let navigationEdgeShadow: CGFloat = {number(navigation['edgeShadow'])}",
        f"    static let skeletonBandWidth: CGFloat = {number(skeleton['bandWidth'])}",
        f"    static let skeletonTiltDegrees: Double = {number(skeleton['tiltDegrees'])}",
        f"    static let skeletonSweep: Double = {number(skeleton['sweepMs'] / 1000)}",
        f"    static let skeletonPause: Double = {number(skeleton['pauseMs'] / 1000)}",
        "    static let skeletonEasing = UnitCurve.bezier(",
        f"        startControlPoint: UnitPoint(x: {number(easing[0])}, y: {number(easing[1])}),",
        f"        endControlPoint: UnitPoint(x: {number(easing[2])}, y: {number(easing[3])})",
        "    )",
        f"    static let skeletonReferenceTravel: CGFloat = {number(skeleton['referenceTravel'])}",
        f"    static let skeletonMaxSlowdown: Double = {number(skeleton['maxSlowdown'])}",
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
        arguments = [f"radius{key.capitalize()}: {number(style['radii'][key])}" for key in radii]
        arguments += [
            f"{key}: {'nil' if style[key] == 'pill' else number(style[key])}" for key in ("buttonRadius", "chipRadius")
        ]
        arguments += [f'{key}: "{style[key]}"' for key in ("card", "field", "bar")]
        arguments += [f"{key}: {number(style[key])}" for key in ("borderWidth", "borderWidthStrong")]
        arguments.append(f"uppercaseLabels: {'true' if style['uppercaseLabels'] else 'false'}")
        arguments.append(f"offsetShadow: {number(style['offsetShadow'])}")
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
