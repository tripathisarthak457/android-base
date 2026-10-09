"""
Brand colours: the three ramps a project's palette draws from, derived from up to three hexes, and
the text colour that reads on each shade.

Every target and the website use these numbers, so they live in one place. The website ports the
same functions to TypeScript and a golden test compares the two.
"""

from __future__ import annotations

import colorsys
import math
from dataclasses import dataclass

Rgb = tuple[float, float, float]

#: The template's own primary, and what a supporting colour is derived from when none is given.
TEMPLATE_ACCENT = "#2C6BED"

WHITE: Rgb = (1.0, 1.0, 1.0)
#: The dark text each palette puts on a brand colour: Ink900, and the dark theme's near-black.
LIGHT_THEME_INK: Rgb = (0x0B / 255, 0x0E / 255, 0x14 / 255)
DARK_THEME_INK: Rgb = (0x06 / 255, 0x10 / 255, 0x1F / 255)
#: The near-black the dark palette's subtle shades are mixed from.
SUBTLE_INK: Rgb = (0.043, 0.063, 0.106)

#: WCAG AA for body text, which is what a label on a button is.
AA = 4.5

#: When only the primary is given: the same hue with most of the chroma taken out for the
#: secondary, and a sixth of a turn round the wheel for the tertiary.
SECONDARY_SATURATION = 0.45
TERTIARY_HUE_SHIFT = 60

#: How far `legible` darkens before giving up. Every hue reaches AA well within it.
MAX_DARKENING_STEPS = 40


def hex_to_rgb(value: str) -> Rgb:
    cleaned = value.strip().lstrip("#")
    if len(cleaned) != 6:
        raise ValueError(f"'{value}' is not a six-digit hex colour, like #2C6BED.")
    return tuple(int(cleaned[i : i + 2], 16) / 255 for i in (0, 2, 4))  # type: ignore[return-value]


def _channel(value: float) -> int:
    return round(max(0.0, min(1.0, value)) * 255)


def to_argb(rgb: Rgb) -> str:
    """`0xFFRRGGBB`, as Kotlin's `Color(…)` takes it."""
    return "0xFF" + "".join(f"{_channel(c):02X}" for c in rgb)


def to_hex(rgb: Rgb) -> str:
    """`#RRGGBB`."""
    return "#" + "".join(f"{_channel(c):02X}" for c in rgb)


def shift(rgb: Rgb, lightness: float, saturation: float = 1.0) -> Rgb:
    """Moves a colour along its own hue rather than towards black or white."""
    hue, existing, sat = colorsys.rgb_to_hls(*rgb)
    target = existing * lightness if lightness <= 1 else 1 - (1 - existing) / lightness
    return colorsys.hls_to_rgb(hue, max(0.0, min(1.0, target)), max(0.0, min(1.0, sat * saturation)))


def mix(rgb: Rgb, other: Rgb, amount: float) -> Rgb:
    return tuple(a + (b - a) * amount for a, b in zip(rgb, other))  # type: ignore[return-value]


def rotate_hue(rgb: Rgb, degrees: float) -> Rgb:
    """Moves a colour round the wheel, keeping how light and how saturated it is."""
    hue, lightness, saturation = colorsys.rgb_to_hls(*rgb)
    return colorsys.hls_to_rgb((hue + degrees / 360.0) % 1.0, lightness, saturation)


def relative_luminance(rgb: Rgb) -> float:
    channels = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in rgb]
    return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]


def contrast(a: Rgb, b: Rgb) -> float:
    light, dark = sorted((relative_luminance(a), relative_luminance(b)), reverse=True)
    return (light + 0.05) / (dark + 0.05)


def legible(colour: Rgb, ink: Rgb) -> Rgb:
    """
    `colour`, or the nearest darker shade that white or `ink` text on it reads at AA. Mid-tones sit
    in a band where neither reaches 4.5:1; darkening a few percent moves them into white's range.
    """
    for _ in range(MAX_DARKENING_STEPS):
        if max(contrast(colour, WHITE), contrast(colour, ink)) >= AA:
            return colour
        colour = shift(colour, 0.97)
    return colour


def dark_variant(base: Rgb) -> Rgb:
    """
    The shade the dark palette draws. The hex that reads as confident on white reads as muddy on
    near-black, and a fully saturated colour on a dark surface vibrates.
    """
    return shift(mix(base, WHITE, 0.22), 1.0, 0.92)


def white_reads_better(colour: Rgb, ink: Rgb) -> bool:
    return contrast(colour, WHITE) >= contrast(colour, ink)


def brand_colours(accent: str, secondary: str = "", tertiary: str = "") -> dict[str, Rgb]:
    """The three brand colours, with anything left blank worked out from the primary."""
    primary = hex_to_rgb(accent or TEMPLATE_ACCENT)
    return {
        "Accent": primary,
        "Secondary": hex_to_rgb(secondary) if secondary else shift(primary, 1.0, SECONDARY_SATURATION),
        "Tertiary": hex_to_rgb(tertiary) if tertiary else rotate_hue(primary, TERTIARY_HUE_SHIFT),
    }


@dataclass(frozen=True)
class BrandRamp:
    """One brand colour as both palettes draw it."""

    base: Rgb
    pressed: Rgb
    subtle_light: Rgb
    dark: Rgb
    dark_pressed: Rgb
    subtle_dark: Rgb
    #: Text on `base` in the light palette, and on `dark` in the dark one.
    on_light: Rgb
    on_dark: Rgb


def brand_ramps(accent: str, secondary: str = "", tertiary: str = "") -> dict[str, BrandRamp]:
    """Accent, Secondary and Tertiary, each legible under text in both palettes."""
    ramps = {}
    for name, colour in brand_colours(accent, secondary, tertiary).items():
        base = legible(colour, LIGHT_THEME_INK)
        dark = legible(dark_variant(base), DARK_THEME_INK)
        ramps[name] = BrandRamp(
            base=base,
            pressed=shift(base, 0.78),
            subtle_light=mix(base, WHITE, 0.90),
            dark=dark,
            dark_pressed=shift(mix(base, WHITE, 0.40), 1.0, 0.85),
            subtle_dark=mix(SUBTLE_INK, base, 0.14),
            on_light=WHITE if white_reads_better(base, LIGHT_THEME_INK) else LIGHT_THEME_INK,
            on_dark=WHITE if white_reads_better(dark, DARK_THEME_INK) else DARK_THEME_INK,
        )
    return ramps


def readable_shade(colour: Rgb, background: Rgb, ratio: float = AA) -> Rgb:
    """
    `colour` moved towards whichever end of the lightness scale contrasts with `background` until
    it reads at `ratio`: the accent used as text or an icon on a surface.
    """
    darken = relative_luminance(background) > 0.18
    for _ in range(MAX_DARKENING_STEPS):
        if contrast(colour, background) >= ratio:
            return colour
        colour = shift(colour, 0.94) if darken else shift(colour, 1.0 / 0.94)
    return colour


# Compose interpolates colours in Oklab (`androidx.compose.ui.graphics.lerp`), so a tint computed
# here matches what the same mix gives at runtime.


def _linear(c: float) -> float:
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _gamma(c: float) -> float:
    return 12.92 * c if c <= 0.0031308 else 1.055 * math.copysign(abs(c) ** (1 / 2.4), c) - 0.055


def to_oklab(rgb: Rgb) -> tuple[float, float, float]:
    r, g, b = (_linear(c) for c in rgb)
    l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
    m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
    s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
    l, m, s = (math.copysign(abs(v) ** (1 / 3), v) for v in (l, m, s))
    return (
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
    )


def from_oklab(lab: tuple[float, float, float]) -> Rgb:
    lightness, a, b = lab
    l = (lightness + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m = (lightness - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s = (lightness - 0.0894841775 * a - 1.2914855480 * b) ** 3
    return (
        _gamma(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
        _gamma(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
        _gamma(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s),
    )


def lerp_oklab(start: Rgb, stop: Rgb, fraction: float) -> Rgb:
    a, b = to_oklab(start), to_oklab(stop)
    return from_oklab(tuple(x + (y - x) * fraction for x, y in zip(a, b)))  # type: ignore[arg-type]
