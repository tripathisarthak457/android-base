"""
The design-system look a spec chooses (typefaces, motion and design style, haptics, brand colours),
written into the template's theme files by substitution.

These appliers stay until the theme reads the emitted DesignTokens file instead. Each one counts
its substitutions and fails when an expected one did not happen, so a renamed template line fails
the build instead of silently dropping a user's choice.
"""

from __future__ import annotations

import re
from pathlib import Path

from ... import brand
from ...core.copy import write_text
from ...core.spec import Spec
from ...core.target import RenderError
from .strings import escape

_THEME = "core/designsystem/src/main/kotlin/{pkg}/core/designsystem/theme"


def apply(project: Path, spec: Spec) -> None:
    apply_fonts(project, spec)
    apply_accent(project, spec)
    apply_feel(project, spec)
    apply_catalog_identity(project, spec)


def apply_fonts(project: Path, spec: Spec) -> None:
    """The chosen typeface names, in `AppFontNames`."""
    path = _theme_file(project, spec, "AppFonts.kt")
    text = path.read_text(encoding="utf-8")
    text = _substitute(path, text, r'(const val Sans = )"[^"]*"', lambda m: f'{m.group(1)}"{spec.font_name}"')
    text = _substitute(path, text, r'(const val Mono = )"[^"]*"', lambda m: f'{m.group(1)}"{spec.mono_font_name}"')
    write_text(path, text)


def apply_feel(project: Path, spec: Spec) -> None:
    """The motion style, the design style and the haptics default, in the one `AppTheme` call."""
    path = project / "app/src/main/kotlin" / spec.package_path / "ui/AppRoot.kt"
    text = _read(path)
    haptics = "settings.hapticsEnabled" if spec.haptics_enabled else "false"
    call = "\n".join([
        "AppTheme(",
        "        mode = themeMode,",
        f"        motionStyle = AppMotionStyle.{spec.motion_style},",
        f"        designStyle = AppDesignStyle.{spec.design_style},",
        f"        hapticsEnabled = {haptics},",
        "    ) {",
    ])
    original = "AppTheme(mode = themeMode, hapticsEnabled = settings.hapticsEnabled) {"
    if text.count(original) != 1:
        raise RenderError(f"{path}: expected the AppTheme call `{original}` exactly once.")
    text = text.replace(original, call, 1)

    theme_import = f"import {spec.package_name}.core.designsystem.theme.AppTheme\n"
    if theme_import not in text:
        raise RenderError(f"{path}: expected `{theme_import.strip()}`.")
    for name in ("AppDesignStyle", "AppMotionStyle"):
        style_import = f"import {spec.package_name}.core.designsystem.theme.{name}\n"
        if style_import not in text:
            text = text.replace(theme_import, style_import + theme_import, 1)
    write_text(path, text)


def apply_accent(project: Path, spec: Spec) -> None:
    """Three full colour ramps from the brand colours, and the text colour that reads on each."""
    if not (spec.accent_colour or spec.secondary_colour or spec.tertiary_colour):
        return
    ramps = brand.brand_ramps(spec.accent_colour, spec.secondary_colour, spec.tertiary_colour)

    path = _theme_file(project, spec, "Palette.kt")
    text = path.read_text(encoding="utf-8")
    for prefix, ramp in ramps.items():
        for name, rgb in (
            (prefix, ramp.base),
            (f"{prefix}Pressed", ramp.pressed),
            (f"{prefix}SubtleLight", ramp.subtle_light),
            (f"{prefix}Dark", ramp.dark),
            (f"{prefix}DarkPressed", ramp.dark_pressed),
            (f"{prefix}SubtleDark", ramp.subtle_dark),
        ):
            value = brand.to_argb(rgb)
            text = _substitute(
                path, text, rf"(internal val {name} = Color\()0x[0-9A-Fa-f]{{8}}(\))",
                lambda m, v=value: f"{m.group(1)}{v}{m.group(2)}",
            )
    write_text(path, text)

    path = _theme_file(project, spec, "AppColors.kt")
    text = path.read_text(encoding="utf-8")
    for prefix, ramp in ramps.items():
        role = prefix[0].lower() + prefix[1:]
        on_light = "White" if ramp.on_light == brand.WHITE else "Ink900"
        on_dark = "White" if ramp.on_dark == brand.WHITE else "Color(0xFF06101F)"
        text = _substitute(
            path, text, rf"({role}Subtle = {prefix}SubtleLight,\s*\n\s*on{prefix} = )\w+",
            lambda m, on=on_light: m.group(1) + on,
        )
        # Comment lines may sit between the subtle shade and the text colour in the dark palette.
        text = _substitute(
            path, text, rf"({role}Subtle = {prefix}SubtleDark,\s*\n(?:\s*//[^\n]*\n)*\s*on{prefix} = )[^,\n]+",
            lambda m, on=on_dark: m.group(1) + on,
        )
    write_text(path, text)


def apply_catalog_identity(project: Path, spec: Spec) -> None:
    """The catalog app's label and launcher colour, which follow the app's own."""
    if not spec.has("catalog"):
        return
    strings = project / "catalog/src/main/res/values/strings.xml"
    label = escape(f"{spec.label} Catalog")
    text = _substitute(
        strings, _read(strings), r'(<string name="app_name"[^>]*>).*?(</string>)',
        lambda m: m.group(1) + label + m.group(2),
    )
    write_text(strings, text)

    if not (spec.accent_colour or spec.secondary_colour or spec.tertiary_colour):
        return
    accent = brand.to_hex(brand.brand_ramps(spec.accent_colour, spec.secondary_colour, spec.tertiary_colour)["Accent"].base)
    colors = project / "catalog/src/main/res/values/colors.xml"
    text = _substitute(
        colors, _read(colors), r'(<color name="launcher_background">)#[0-9A-Fa-f]{6,8}(</color>)',
        lambda m: f"{m.group(1)}#FF{accent[1:]}{m.group(2)}",
    )
    write_text(colors, text)


def _theme_file(project: Path, spec: Spec, name: str) -> Path:
    path = project / _THEME.format(pkg=spec.package_path) / name
    if not path.is_file():
        raise RenderError(f"{name} is not where the design system keeps it: {path}")
    return path


def _read(path: Path) -> str:
    if not path.is_file():
        raise RenderError(f"Expected {path} in the generated project.")
    return path.read_text(encoding="utf-8")


def _substitute(path: Path, text: str, pattern: str, replacement) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1)
    if count != 1:
        raise RenderError(f"{path.name}: nothing matched `{pattern}`; the template changed under the generator.")
    return updated
