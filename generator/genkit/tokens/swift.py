"""
The resolved tokens as Swift: the public `AppDesignStyle` and `AppMotionStyle` choices, and an
internal `enum DesignTokens` holding the values the SwiftUI design system reads. Sizes are points.

Every type is `nonisolated`: the design system's target is main-actor by default, and the tokens are
read from nonisolated code such as environment defaults and geometry callbacks.
"""

from __future__ import annotations

import json
import re
from typing import Any

from . import load, number

HEADER = (
    "// Generated from tokens/*.json by generator/genkit/tokens. "
    "Do not edit; run python3 -m genkit.tokens --write."
)

RELATIVE_PATH = "Packages/Core/Sources/DesignSystem/Tokens/DesignTokens.swift"

#: The asset catalog's accent colour, relative to the app target's folder.
ACCENT_COLOR_PATH = "Assets.xcassets/AccentColor.colorset/Contents.json"

#: The motion constants the SwiftUI design system reads, as (Swift name, Swift type, group and key
#: in motion.json, divisor). Milliseconds become seconds, as SwiftUI takes them.
_MOTION_CONSTANTS: tuple[tuple[str, str, str, str, int], ...] = (
    ("fadeShort", "TimeInterval", "fades", "shortMs", 1000),
    ("pressScale", "CGFloat", "press", "scale", 1),
    ("pressScaleLarge", "CGFloat", "press", "scaleLarge", 1),
    ("pressScaleIcon", "CGFloat", "press", "scaleIcon", 1),
    ("pressDimAlpha", "Double", "press", "dimAlpha", 1),
    ("highlightFadeOut", "TimeInterval", "press", "highlightFadeOutMs", 1000),
)


def emit(tokens: dict[str, Any]) -> str:
    lines = [HEADER, "import SwiftUI", ""]
    lines += _choice(
        "AppDesignStyle",
        "A design style: corner radii, container treatments, surface tones and type voice.",
        {name: style["description"] for name, style in tokens["styles"].items()},
    )
    lines.append("")
    # Resolving keeps a motion style's values and drops its description, which only documents it,
    # so the description comes from the token files themselves.
    lines += _choice(
        "AppMotionStyle",
        "A motion style: how much the springs overshoot, and how quickly they travel.",
        {name: style["description"] for name, style in load()["motion"]["styles"].items()},
    )
    lines += ["", "nonisolated enum DesignTokens {"]
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
    for index, section in enumerate(sections):
        if index:
            lines.append("")
        lines.extend(f"    {line}" if line else "" for line in section)
    lines.append("}")
    return "\n".join(lines) + "\n"


def accent_colorset(tokens: dict[str, Any]) -> str:
    """
    The asset catalog's `AccentColor`, in light and dark: the accent of the design style the project
    was generated with. Xcode makes it the app's default tint, so what is drawn outside the themed
    SwiftUI root carries the brand colour too.
    """
    tone = tokens["palette"]["tones"][tokens["styles"][tokens["defaults"]["designStyle"]]["surfaces"]]
    colors = []
    for theme in ("light", "dark"):
        alpha, red, green, blue = _argb(tone[theme]["accent"])
        entry: dict[str, Any] = {}
        if theme == "dark":
            entry["appearances"] = [{"appearance": "luminosity", "value": "dark"}]
        components = {"alpha": alpha, "blue": blue, "green": green, "red": red}
        entry["color"] = {"color-space": "srgb", "components": {key: f"{value:.3f}" for key, value in components.items()}}
        entry["idiom"] = "universal"
        colors.append(entry)
    catalog = {"colors": colors, "info": {"author": "xcode", "version": 1}}
    # The layout Xcode writes asset catalogs in.
    return json.dumps(catalog, indent=2, separators=(",", " : ")) + "\n"


def case_name(token: str) -> str:
    """A token's name as a Swift enum case: `BrandTint` → `brandTint`, `Outlined` → `outlined`."""
    return token[:1].lower() + token[1:]


def _choice(name: str, summary: str, cases: dict[str, str]) -> list[str]:
    lines = [f"/// {summary}", f"public nonisolated enum {name}: String, CaseIterable, Sendable {{"]
    for index, (token, description) in enumerate(cases.items()):
        if index:
            lines.append("")
        lines += [f"    /// {_sentence(description)}", f'    case {case_name(token)} = "{token}"']
    lines.append("}")
    return lines


def _sentence(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def _argb(hex_argb: str) -> tuple[float, float, float, float]:
    """`#AARRGGBB` as alpha, red, green and blue from 0 to 1."""
    digits = hex_argb.lstrip("#")
    alpha, red, green, blue = (int(digits[i : i + 2], 16) / 255 for i in (0, 2, 4, 6))
    return alpha, red, green, blue


def _color(hex_argb: str) -> str:
    alpha, red, green, blue = _argb(hex_argb)
    return (
        f"Color(.sRGB, red: {red:.4f}, green: {green:.4f}, blue: {blue:.4f}, opacity: {alpha:.4f})"
    )


def _points(name: str, values: dict[str, float]) -> list[str]:
    return [
        f"nonisolated enum {name} {{",
        *(f"    static let {key}: CGFloat = {number(value)}" for key, value in values.items()),
        "}",
    ]


def _borders(borders: dict[str, float]) -> list[str]:
    widths = {key: value for key, value in borders.items() if key != "hairlinePx"}
    return [
        "nonisolated enum Borders {",
        "    /// Physical pixels, not points: a hairline is `hairlinePx / displayScale` points on every screen.",
        f"    static let hairlinePx: Int = {int(borders['hairlinePx'])}",
        *_points("Borders", widths)[1:],
    ]


def _type(type_tokens: dict[str, Any]) -> list[str]:
    voices = type_tokens["voices"]
    lines = [
        "nonisolated struct TypeRole {",
        "    let size: CGFloat",
        "    let lineHeight: CGFloat",
        "    let tracking: CGFloat",
        *(f"    let weight{voice}: Int" for voice in voices),
        "}",
        "",
        # Not "Type": that is a Swift metatype name.
        "nonisolated enum Typography {",
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
    styles = motion["styles"]
    spring_names = list(next(iter(styles.values()))["springs"])
    lines = [
        "/// A spring as response and damping ratio, the pair the Android template's springs use, so",
        "/// both apps run the same curve whatever the bounce.",
        "nonisolated struct SpringToken {",
        "    let response: Double",
        "    let dampingRatio: Double",
        "",
        "    var spring: Spring {",
        "        Spring(response: response, dampingRatio: dampingRatio)",
        "    }",
        "}",
        "",
        "nonisolated struct MotionStyle {",
        *(f"    let {name}: SpringToken" for name in spring_names),
        "}",
        "",
        "nonisolated enum Motion {",
    ]
    for style, values in styles.items():
        lines.append(f"    static let {case_name(style)} = MotionStyle(")
        arguments = [
            f"        {name}: SpringToken(response: {number(spring['response'])}, "
            f"dampingRatio: {number(spring['dampingRatio'])})"
            for name, spring in values["springs"].items()
        ]
        lines += [f"{argument}," for argument in arguments[:-1]] + arguments[-1:]
        lines.append("    )")
    lines += ["", "    static func style(_ style: AppMotionStyle) -> MotionStyle {", "        switch style {"]
    lines += [f"        case .{case_name(style)}: {case_name(style)}" for style in styles]
    lines += ["        }", "    }", ""]
    for name, swift_type, group, key, divisor in _MOTION_CONSTANTS:
        lines.append(f"    static let {name}: {swift_type} = {number(motion[group][key] / divisor)}")
    lines.append("}")
    return lines


def _palette(palette: dict[str, Any]) -> list[str]:
    sample = palette["tones"][next(iter(palette["tones"]))]["light"]
    plain = [name for name, value in sample.items() if not isinstance(value, dict)]
    statuses = [name for name, value in sample.items() if isinstance(value, dict)]
    status_fields = list(sample[statuses[0]]) if statuses else []
    lines = [
        "nonisolated struct Status {",
        *(f"    let {field}: Color" for field in status_fields),
        "}",
        "",
        "nonisolated struct Roles {",
        *(f"    let {name}: Color" for name in plain),
        *(f"    let {name}: Status" for name in statuses),
        "}",
        "",
        "/// The surface tones a design style is drawn on, resolved per appearance.",
        "nonisolated enum Palette {",
    ]
    for index, (tone, themes) in enumerate(palette["tones"].items()):
        if index:
            lines.append("")
        lines.append(f"    nonisolated enum {tone} {{")
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
        "nonisolated struct Style {",
        *(f"    let radius{key.capitalize()}: CGFloat" for key in radii),
        "    /// Nil: a capsule, as round as the control is tall.",
        "    let buttonRadius: CGFloat?",
        "    let chipRadius: CGFloat?",
        "    let card: CardTreatment",
        "    let field: FieldTreatment",
        "    let borderWidth: CGFloat",
        "    let borderWidthStrong: CGFloat",
        "    let uppercaseLabels: Bool",
        "    let offsetShadow: CGFloat",
        "    let light: Roles",
        "    let dark: Roles",
        "    let voice: TypeVoice",
        "}",
        "",
        "nonisolated enum Styles {",
    ]
    for name, style in styles.items():
        arguments = [f"radius{key.capitalize()}: {number(style['radii'][key])}" for key in radii]
        arguments += [
            f"{key}: {'nil' if style[key] == 'pill' else number(style[key])}" for key in ("buttonRadius", "chipRadius")
        ]
        arguments += [f"{key}: .{case_name(style[key])}" for key in ("card", "field")]
        arguments += [f"{key}: {number(style[key])}" for key in ("borderWidth", "borderWidthStrong")]
        arguments.append(f"uppercaseLabels: {'true' if style['uppercaseLabels'] else 'false'}")
        arguments.append(f"offsetShadow: {number(style['offsetShadow'])}")
        arguments += [f"{theme}: Palette.{style['surfaces']}.{theme}" for theme in ("light", "dark")]
        arguments.append(f"voice: .{case_name(style['voice'])}")
        lines.append(f"    static let {case_name(name)} = Style(")
        lines += [f"        {argument}," for argument in arguments[:-1]] + [f"        {arguments[-1]}"]
        lines.append("    )")
    lines += ["", "    static func style(_ style: AppDesignStyle) -> Style {", "        switch style {"]
    lines += [f"        case .{case_name(name)}: {case_name(name)}" for name in styles]
    lines += ["        }", "    }", "}"]
    return lines


def _defaults(defaults: dict[str, Any]) -> list[str]:
    return [
        "/// The choices the project was generated with.",
        "nonisolated enum Defaults {",
        f"    static let designStyle = AppDesignStyle.{case_name(defaults['designStyle'])}",
        f"    static let motionStyle = AppMotionStyle.{case_name(defaults['motionStyle'])}",
        f"    static let hapticsEnabled = {'true' if defaults['hapticsEnabled'] else 'false'}",
        "}",
    ]
