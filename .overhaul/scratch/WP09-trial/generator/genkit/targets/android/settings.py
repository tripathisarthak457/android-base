"""
The project's own settings, as the lines of the template's `generated` regions: identity and SDK
levels in `AppConfig.kt`, the launcher label and colour, deep-link strings, the language list, and
the computed parts of the README.
"""

from __future__ import annotations

from ... import brand
from ...core.spec import AndroidSettings, Spec
from ...core.translations import SOURCE_LANGUAGE
from .strings import escape

#: The build flavours a URL can be set for. Anything else would be silently ignored.
FLAVOURS: tuple[str, ...] = ("dev", "staging", "prod", "playstore")

Regions = dict[str, list[str] | None]


def regions(spec: Spec) -> Regions:
    android = spec.android or AndroidSettings()
    return {
        **_app_config(spec, android),
        **_flavour_urls(android),
        "app-name": [f'    <string name="app_name" translatable="false">{escape(spec.label)}</string>\n'],
        "deeplink-strings": [
            f'    <string name="deeplink_scheme" translatable="false">{android.deeplink_scheme or spec.lower_name or "app"}</string>\n',
            f'    <string name="deeplink_host" translatable="false">{android.deeplink_host or "example.com"}</string>\n',
        ],
        "launcher-background": _launcher_background(spec),
        "supported-locales": [
            "    val supported: List<String> = listOf("
            + ", ".join(f'"{tag}"' for tag in (SOURCE_LANGUAGE, *spec.languages))
            + ")\n"
        ],
        **_readme(spec, android),
    }


def _app_config(spec: Spec, android: AndroidSettings) -> Regions:
    return {
        "app-config": [
            f'    const val APPLICATION_ID = "{spec.package_name}"\n',
            f'    const val APP_NAME = "{spec.pascal_name}"\n',
            "\n",
            f"    const val COMPILE_SDK = {android.compile_sdk}\n",
            f"    const val MIN_SDK = {android.min_sdk}\n",
            f"    const val TARGET_SDK = {android.target_sdk}\n",
        ],
        "app-version": [
            f"    const val VERSION_CODE = {android.version_code}\n",
            f'    const val VERSION_NAME = "{android.version_name}"\n',
        ],
    }


def _flavour_urls(android: AndroidSettings) -> Regions:
    """One region per URL, so a flavour the spec leaves out keeps the template's placeholder."""
    lines: Regions = {}
    for flavour in FLAVOURS:
        api = android.api_base_urls.get(flavour)
        socket = android.web_socket_urls.get(flavour)
        lines[f"api-url-{flavour}"] = [f'        apiBaseUrl = "{api}",\n'] if api else None
        lines[f"socket-url-{flavour}"] = [f'        webSocketUrl = "{socket}",\n'] if socket else None
    return lines


def _launcher_background(spec: Spec) -> list[str] | None:
    """The adaptive icon's background, so the launcher matches the app it opens."""
    if not (spec.accent_colour or spec.secondary_colour or spec.tertiary_colour):
        return None
    accent = brand.brand_ramps(spec.accent_colour, spec.secondary_colour, spec.tertiary_colour)["Accent"].base
    return [f'    <color name="launcher_background">#FF{brand.to_hex(accent)[1:]}</color>\n']


def _readme(spec: Spec, android: AndroidSettings) -> Regions:
    modules = ", ".join(f"`:feature:{name}`" for name in spec.feature_modules) or "none yet"
    version = android.version_name
    return {
        "readme-title": [f"# {spec.label}\n"],
        "readme-styles": [
            f"This project was generated with the **{spec.design_style}** design style and the "
            f"**{spec.motion_style}**\n"
        ],
        "readme-theme-call": [
            f"    motionStyle = AppMotionStyle.{spec.motion_style},\n",
            f"    designStyle = AppDesignStyle.{spec.design_style},\n",
        ],
        "readme-version": [
            f"`{version}-devDebug` and so on; `prodRelease` and `playstoreRelease` stay a bare `{version}`,\n"
        ],
        "readme-modules": [f"Feature modules generated: {modules}\n"],
        # Both paragraphs are in the README template; the project keeps the one that is true.
        "readme-signing-generated": None if android.keystores else [],
        "readme-signing-manual": [] if android.keystores else None,
        "readme-features": [f"- {name}\n" for name in sorted(spec.features)],
    }
