"""
The design tokens in `tokens/*.json`: loaded, checked, and resolved against a spec into the one tree
every emitter (Kotlin, Swift, the website's catalogue) writes out.

    foundation.json   spacing, radii, sizes, borders, elevation
    typography.json   the role scale and each voice's weight per role
    motion.json       springs as (response, bounce scale), motion styles, fades, press, navigation
    palette.json      ramps, the template brand, status hues, roles per theme, surface tones
    styles.json       design styles: corners, treatments, surface tone, type voice

A palette value is a hex (`#RRGGBB` or `#AARRGGBB`), a ramp step (`grey.50`), another role
(`@surface`), or `{ "ref": …, "alpha": 0.4 }`.
"""

from __future__ import annotations

import json
import math
import re
from functools import cache
from pathlib import Path
from typing import TYPE_CHECKING, Any

from .. import brand
from ..core.markers import TemplateError
from ..core.spec import DEFAULT_DESIGN_STYLE, DEFAULT_FONT, DEFAULT_MONO_FONT, DEFAULT_MOTION_STYLE

if TYPE_CHECKING:
    from ..core.spec import Spec

TOKENS_DIR = Path(__file__).resolve().parents[3] / "tokens"
FILES: tuple[str, ...] = ("foundation", "typography", "motion", "palette", "styles")

PLATFORMS: tuple[str, ...] = ("android", "ios", "web")
THEMES: tuple[str, ...] = ("light", "dark")
BRAND_COLOURS: tuple[str, ...] = ("accent", "secondary", "tertiary")
_BRAND_FIELDS = ("base", "pressed", "subtleLight", "dark", "darkPressed", "subtleDark", "onLight", "onDark")
_STYLE_RADII = ("xs", "sm", "md", "lg", "xl", "sheet")

_HEX = re.compile(r"^#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
_IDENTIFIER = re.compile(r"^[A-Z][A-Za-z]*$")

#: (red, green, blue, alpha), each 0–1.
Rgba = tuple[float, float, float, float]


@cache
def load() -> dict[str, Any]:
    data = {}
    for name in FILES:
        path = TOKENS_DIR / f"{name}.json"
        try:
            data[name] = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise TemplateError(path, None, f"cannot read the tokens: {error}") from None
    validate(data)
    return data


def design_style_requirements() -> dict[str, tuple[str, ...]]:
    """Each design style, in display order, with the features it needs."""
    return {name: tuple(style["requires"]) for name, style in load()["styles"].items()}


def motion_style_names() -> tuple[str, ...]:
    return tuple(load()["motion"]["styles"])


def validate(data: dict[str, Any]) -> None:
    """Raises TemplateError naming the file and key of the first thing wrong."""
    _validate_foundation(data["foundation"])
    voices = _validate_typography(data["typography"])
    _validate_motion(data["motion"])
    tones = _validate_palette(data["palette"])
    _validate_styles(data["styles"], tones, voices)


def _fail(file: str, where: str, message: str) -> TemplateError:
    return TemplateError(TOKENS_DIR / f"{file}.json", None, f"{where}: {message}")


def _number(file: str, where: str, value: object, minimum: float | None = None) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise _fail(file, where, "must be a number")
    if minimum is not None and value < minimum:
        raise _fail(file, where, f"must be at least {minimum}")
    return value


def _table(file: str, where: str, value: object, keys: tuple[str, ...] | None = None) -> dict[str, Any]:
    if not isinstance(value, dict) or not value:
        raise _fail(file, where, "must be a non-empty object")
    if keys is not None:
        missing = [key for key in keys if key not in value]
        if missing:
            raise _fail(file, where, f"is missing {', '.join(missing)}")
    return value


def _validate_foundation(foundation: dict[str, Any]) -> None:
    for group in ("spacing", "radii", "borders", "elevation"):
        for key, value in _table("foundation", group, foundation.get(group)).items():
            _number("foundation", f"{group}.{key}", value, 0)
    for key, value in _table("foundation", "sizes", foundation.get("sizes")).items():
        if isinstance(value, dict):
            for platform in PLATFORMS:
                _number("foundation", f"sizes.{key}.{platform}", value.get(platform), 0)
        else:
            _number("foundation", f"sizes.{key}", value, 0)


def _validate_typography(typography: dict[str, Any]) -> tuple[str, ...]:
    voices = typography.get("voices")
    if not isinstance(voices, list) or not voices:
        raise _fail("typography", "voices", "must list the type voices")
    for name, role in _table("typography", "roles", typography.get("roles")).items():
        where = f"roles.{name}"
        _table("typography", where, role, ("size", "lineHeight", "tracking", "weight"))
        _number("typography", f"{where}.size", role["size"], 1)
        if _number("typography", f"{where}.lineHeight", role["lineHeight"], 1) < role["size"]:
            raise _fail("typography", where, "lineHeight is smaller than size")
        _number("typography", f"{where}.tracking", role["tracking"])
        weights = _table("typography", f"{where}.weight", role["weight"], tuple(voices))
        for voice, weight in weights.items():
            if weight not in range(100, 1000, 100):
                raise _fail("typography", f"{where}.weight.{voice}", "must be 100, 200, … 900")
    return tuple(voices)


def _validate_motion(motion: dict[str, Any]) -> None:
    for name, style in _table("motion", "styles", motion.get("styles")).items():
        _table("motion", f"styles.{name}", style, ("bounce", "speed", "description"))
        if not 0 <= _number("motion", f"styles.{name}.bounce", style["bounce"]) < 1:
            raise _fail("motion", f"styles.{name}.bounce", "must be at least 0 and below 1")
        if _number("motion", f"styles.{name}.speed", style["speed"]) <= 0:
            raise _fail("motion", f"styles.{name}.speed", "must be above 0")
    for name, spring in _table("motion", "springs", motion.get("springs")).items():
        _table("motion", f"springs.{name}", spring, ("response", "bounceScale", "bySpeed"))
        if _number("motion", f"springs.{name}.response", spring["response"]) <= 0:
            raise _fail("motion", f"springs.{name}.response", "must be above 0")
        _number("motion", f"springs.{name}.bounceScale", spring["bounceScale"], 0)
        if not isinstance(spring["bySpeed"], bool):
            raise _fail("motion", f"springs.{name}.bySpeed", "must be true or false")
    for group in ("visibilityThreshold", "fades", "stagger", "press"):
        for key, value in _table("motion", group, motion.get(group)).items():
            _number("motion", f"{group}.{key}", value, 0)
    navigation = _table("motion", "navigation", motion.get("navigation"), ("parallax", "scrimAlpha", "edgeShadow"))
    _number("motion", "navigation.parallax", navigation["parallax"], 0)
    _number("motion", "navigation.edgeShadow", navigation["edgeShadow"], 0)
    for theme in THEMES:
        _number("motion", f"navigation.scrimAlpha.{theme}", _table("motion", "navigation.scrimAlpha", navigation["scrimAlpha"], THEMES)[theme], 0)


def _validate_palette(palette: dict[str, Any]) -> tuple[str, ...]:
    if not _HEX.fullmatch(str(palette.get("templateAccent"))):
        raise _fail("palette", "templateAccent", "must be a hex colour")
    ramps = _table("palette", "ramps", palette.get("ramps"))
    for ramp, steps in ramps.items():
        for step, value in _table("palette", f"ramps.{ramp}", steps).items():
            if not _HEX.fullmatch(str(value)):
                raise _fail("palette", f"ramps.{ramp}.{step}", "must be a hex colour")
    brand_table = _table("palette", "templateBrand", palette.get("templateBrand"), BRAND_COLOURS)
    for colour in BRAND_COLOURS:
        for field, value in _table("palette", f"templateBrand.{colour}", brand_table[colour], _BRAND_FIELDS).items():
            if not _HEX.fullmatch(str(value)):
                raise _fail("palette", f"templateBrand.{colour}.{field}", "must be a hex colour")

    roles = _table("palette", "roles", palette.get("roles"), THEMES)
    names = list(_table("palette", "roles.light", roles["light"]))
    if list(_table("palette", "roles.dark", roles["dark"])) != names:
        raise _fail("palette", "roles", "light and dark must list the same roles in the same order")
    tones = _table("palette", "tones", palette.get("tones"))
    for tone, table in tones.items():
        for key, overrides in table.items():
            if key == "mix":
                for theme, amounts in _table("palette", f"tones.{tone}.mix", overrides, THEMES).items():
                    for role, amount in amounts.items():
                        _check_role(names, tone, role)
                        if not 0 <= _number("palette", f"tones.{tone}.mix.{theme}.{role}", amount) <= 1:
                            raise _fail("palette", f"tones.{tone}.mix.{theme}.{role}", "must be between 0 and 1")
            elif key in THEMES:
                for role in overrides:
                    _check_role(names, tone, role)
            else:
                raise _fail("palette", f"tones.{tone}", f"unknown key '{key}'")

    # Resolving every tone once proves every reference points somewhere and none loops.
    ramps_rgba = _ramps(palette)
    template_brand = _template_brand(palette)
    for tone in tones:
        for theme in THEMES:
            _resolve_roles(palette, ramps_rgba, template_brand, tone, theme)
    return tuple(tones)


def _check_role(names: list[str], tone: str, role: str) -> None:
    if role not in names:
        raise _fail("palette", f"tones.{tone}", f"'{role}' is not a role")


def _validate_styles(styles: dict[str, Any], tones: tuple[str, ...], voices: tuple[str, ...]) -> None:
    required = (
        "description", "requires", "radii", "buttonRadius", "chipRadius", "card", "field", "bar",
        "borderWidth", "borderWidthStrong", "uppercaseLabels", "offsetShadow", "surfaces", "voice",
    )
    for name, style in _table("styles", "styles", styles).items():
        if not _IDENTIFIER.fullmatch(name):
            raise _fail("styles", name, "a style's name must be a capitalised identifier")
        _table("styles", name, style, required)
        for key in _STYLE_RADII:
            _number("styles", f"{name}.radii.{key}", _table("styles", f"{name}.radii", style["radii"], _STYLE_RADII)[key], 0)
        for key in ("buttonRadius", "chipRadius"):
            if style[key] != "pill":
                _number("styles", f"{name}.{key}", style[key], 0)
        for key in ("card", "field", "bar"):
            if not _IDENTIFIER.fullmatch(str(style[key])):
                raise _fail("styles", f"{name}.{key}", "must name a treatment, like Outlined")
        for key in ("borderWidth", "borderWidthStrong", "offsetShadow"):
            _number("styles", f"{name}.{key}", style[key], 0)
        if not isinstance(style["uppercaseLabels"], bool):
            raise _fail("styles", f"{name}.uppercaseLabels", "must be true or false")
        if not isinstance(style["requires"], list):
            raise _fail("styles", f"{name}.requires", "must list feature keys")
        if style["surfaces"] not in tones:
            raise _fail("styles", f"{name}.surfaces", f"must be one of the palette's tones: {', '.join(tones)}")
        if style["voice"] not in voices:
            raise _fail("styles", f"{name}.voice", f"must be one of the type voices: {', '.join(voices)}")


def spring(response: float, bounce: float) -> tuple[float, float]:
    """
    An iOS `Spring(duration: response, bounce:)` as Compose sees it, with mass 1: stiffness and
    damping ratio.
    """
    return (2 * math.pi / response) ** 2, 1 - bounce


def resolve(spec: "Spec | None" = None, platform: str = "android") -> dict[str, Any]:
    """The full token tree for `spec`'s brand and choices; None resolves the template's defaults."""
    data = load()
    foundation = data["foundation"]
    return {
        "spacing": dict(foundation["spacing"]),
        "radii": dict(foundation["radii"]),
        "sizes": {
            key: value[platform] if isinstance(value, dict) else value
            for key, value in foundation["sizes"].items()
        },
        "borders": dict(foundation["borders"]),
        "elevation": dict(foundation["elevation"]),
        "type": {"voices": list(data["typography"]["voices"]), "roles": data["typography"]["roles"]},
        "motion": _motion(data["motion"]),
        "palette": _palette(data["palette"], spec),
        "styles": data["styles"],
        "defaults": {
            "designStyle": spec.design_style if spec else DEFAULT_DESIGN_STYLE,
            "motionStyle": spec.motion_style if spec else DEFAULT_MOTION_STYLE,
            "hapticsEnabled": spec.haptics_enabled if spec else True,
            "fontName": spec.font_name if spec else DEFAULT_FONT,
            "monoFontName": spec.mono_font_name if spec else DEFAULT_MONO_FONT,
        },
    }


def _motion(motion: dict[str, Any]) -> dict[str, Any]:
    styles = {}
    for name, style in motion["styles"].items():
        springs = {}
        for key, definition in motion["springs"].items():
            response = definition["response"] / (style["speed"] if definition["bySpeed"] else 1)
            bounce = style["bounce"] * definition["bounceScale"]
            stiffness, damping = spring(response, bounce)
            springs[key] = {
                "response": round(response, 4),
                "bounce": round(bounce, 4),
                "stiffness": round(stiffness, 2),
                "dampingRatio": round(damping, 4),
            }
        styles[name] = {"bounce": style["bounce"], "speed": style["speed"], "springs": springs}
    return {
        "styles": styles,
        **{key: motion[key] for key in ("visibilityThreshold", "fades", "stagger", "press", "navigation")},
    }


def _palette(palette: dict[str, Any], spec: "Spec | None") -> dict[str, Any]:
    ramps = _ramps(palette)
    if spec is not None and (spec.accent_colour or spec.secondary_colour or spec.tertiary_colour):
        computed = brand.brand_ramps(spec.accent_colour, spec.secondary_colour, spec.tertiary_colour)
        brand_rgba = {
            colour: {
                "base": ramp.base, "pressed": ramp.pressed, "subtleLight": ramp.subtle_light,
                "dark": ramp.dark, "darkPressed": ramp.dark_pressed, "subtleDark": ramp.subtle_dark,
                "onLight": ramp.on_light, "onDark": ramp.on_dark,
            }
            for colour, ramp in zip(BRAND_COLOURS, computed.values())
        }
        brand_rgba = {c: {k: (*v, 1.0) for k, v in fields.items()} for c, fields in brand_rgba.items()}
    else:
        brand_rgba = _template_brand(palette)

    return {
        "ramps": {
            name: {step: to_hex(colour) for step, colour in steps.items()} for name, steps in ramps.items()
        },
        "brand": {
            colour: {field: to_hex(value) for field, value in fields.items()} for colour, fields in brand_rgba.items()
        },
        "tones": {
            tone: {
                theme: _hexes(_resolve_roles(palette, ramps, brand_rgba, tone, theme)) for theme in THEMES
            }
            for tone in palette["tones"]
        },
    }


def _hexes(roles: dict[str, Any]) -> dict[str, Any]:
    return {name: _hexes(value) if isinstance(value, dict) else to_hex(value) for name, value in roles.items()}


def to_hex(colour: Rgba) -> str:
    """`#AARRGGBB`."""
    return "#" + "".join(f"{round(max(0.0, min(1.0, c)) * 255):02X}" for c in (colour[3], *colour[:3]))


def _parse_hex(value: str) -> Rgba:
    digits = value.lstrip("#")
    alpha = int(digits[:2], 16) / 255 if len(digits) == 8 else 1.0
    rgb = digits[-6:]
    return (int(rgb[0:2], 16) / 255, int(rgb[2:4], 16) / 255, int(rgb[4:6], 16) / 255, alpha)


def _ramps(palette: dict[str, Any]) -> dict[str, dict[str, Rgba]]:
    return {
        name: {step: _parse_hex(value) for step, value in steps.items()}
        for name, steps in palette["ramps"].items()
    }


def _template_brand(palette: dict[str, Any]) -> dict[str, dict[str, Rgba]]:
    return {
        colour: {field: _parse_hex(value) for field, value in fields.items()}
        for colour, fields in palette["templateBrand"].items()
    }


def _resolve_roles(
    palette: dict[str, Any],
    ramps: dict[str, dict[str, Rgba]],
    brand_rgba: dict[str, dict[str, Rgba]],
    tone: str,
    theme: str,
) -> dict[str, Any]:
    light = theme == "light"
    tone_table = palette["tones"][tone]
    definitions: dict[str, Any] = {**palette["roles"][theme], **tone_table.get(theme, {})}

    resolved: dict[str, Rgba] = {}
    for colour in BRAND_COLOURS:
        fields = brand_rgba[colour]
        resolved[colour] = fields["base" if light else "dark"]
        resolved[f"{colour}Pressed"] = fields["pressed" if light else "darkPressed"]
        resolved[f"{colour}Subtle"] = fields["subtleLight" if light else "subtleDark"]
        resolved[f"on{colour.capitalize()}"] = fields["onLight" if light else "onDark"]

    def value_of(spec: object, trail: tuple[str, ...]) -> Rgba:
        if isinstance(spec, dict):
            base = value_of(spec.get("ref"), trail)
            return (*base[:3], base[3] * _number("palette", f"{tone}.{theme}", spec.get("alpha"), 0))
        if not isinstance(spec, str):
            raise _fail("palette", f"tones.{tone}.{theme}", f"cannot read colour {spec!r}")
        if spec.startswith("@"):
            return role(spec[1:], trail)
        if spec.startswith("#") and _HEX.fullmatch(spec):
            return _parse_hex(spec)
        ramp, _, step = spec.partition(".")
        if ramp in ramps and step in ramps[ramp]:
            return ramps[ramp][step]
        raise _fail("palette", f"{tone}.{theme}", f"'{spec}' is not a hex, a ramp step or a role")

    def role(name: str, trail: tuple[str, ...]) -> Rgba:
        if name in resolved:
            return resolved[name]
        if name in trail:
            raise _fail("palette", f"{tone}.{theme}", f"roles refer to each other in a loop: {' → '.join(trail + (name,))}")
        if name not in definitions:
            raise _fail("palette", f"{tone}.{theme}", f"'@{name}' is not a role")
        resolved[name] = value_of(definitions[name], trail + (name,))
        return resolved[name]

    for name in palette["roles"][theme]:
        role(name, ())

    accent = resolved["accent"]
    for name, amount in tone_table.get("mix", {}).get(theme, {}).items():
        mixed = brand.lerp_oklab(resolved[name][:3], accent[:3], amount)
        resolved[name] = (*mixed, resolved[name][3])

    for colour in BRAND_COLOURS:
        shade = brand.readable_shade(resolved[colour][:3], resolved["background"][:3])
        resolved[f"{colour}Content"] = (*brand.readable_shade(shade, resolved["surface"][:3]), 1.0)

    ink = brand.LIGHT_THEME_INK if light else brand.DARK_THEME_INK
    roles: dict[str, Any] = {name: resolved[name] for name in palette["roles"][theme]}
    for colour in BRAND_COLOURS:
        for suffix in ("", "Pressed", "Subtle", "Content"):
            roles[f"{colour}{suffix}"] = resolved[f"{colour}{suffix}"]
        roles[f"on{colour.capitalize()}"] = resolved[f"on{colour.capitalize()}"]
    for status, themes in palette["status"].items():
        base = value_of(themes[theme]["base"], ())
        subtle = value_of(themes[theme]["subtle"], ())
        roles[status] = {
            "base": base,
            "content": (*brand.readable_shade(base[:3], subtle[:3]), 1.0),
            "subtle": subtle,
            "border": value_of(themes[theme]["border"], ()),
            "on": (*(brand.WHITE if brand.white_reads_better(base[:3], ink) else ink), 1.0),
        }
    return roles
