"""
The project's own settings, as the lines of the template's `generated` regions: identity and SDK
levels in `AppConfig.kt`, the API URLs of each environment on both platforms, the launcher label,
colour and languages, and the iOS project's identity.
"""

from __future__ import annotations

import json
from xml.sax.saxutils import escape as xml_escape

from ... import brand
from ...core.spec import AndroidSettings, IosSettings, Spec
from ...core.translations import SOURCE_LANGUAGE
from ..android.strings import escape as android_string

Regions = dict[str, list[str] | None]


def regions(spec: Spec) -> Regions:
    android = spec.android or AndroidSettings()
    urls = android.api_base_urls
    return {
        "app-config": [
            f'    const val APPLICATION_ID = "{spec.package_name}"\n',
            f'    const val BASE_PACKAGE = "{spec.package_name}"\n',
            "\n",
            f"    const val COMPILE_SDK = {android.compile_sdk}\n",
            f"    const val MIN_SDK = {android.min_sdk}\n",
            f"    const val TARGET_SDK = {android.target_sdk}\n",
        ],
        "app-version": [
            f"    const val VERSION_CODE = {android.version_code}\n",
            f'    const val VERSION_NAME = "{android.version_name}"\n',
        ],
        # One region per URL, so an environment the spec leaves out keeps the template's placeholder.
        "api-url-dev": [f'        apiBaseUrl = "{urls["dev"]}",\n'] if "dev" in urls else None,
        "api-url-prod": [f'        apiBaseUrl = "{urls["prod"]}",\n'] if "prod" in urls else None,
        "app-name": [f'    <string name="app_name" translatable="false">{android_string(spec.label)}</string>\n'],
        "launcher-background": _launcher_background(spec),
        "supported-locales": [
            f'    <locale android:name="{tag}" />\n' for tag in (SOURCE_LANGUAGE, *spec.languages)
        ],
    }


def ios_regions(spec: Spec, bundle_id: str) -> Regions:
    ios = spec.ios or IosSettings()
    urls = (spec.android or AndroidSettings()).api_base_urls
    return {
        "ios-deployment-target": ["  deploymentTarget:\n", f"    iOS: {_yaml_string(ios.deployment_target)}\n"],
        "ios-config": [
            f"        PRODUCT_BUNDLE_IDENTIFIER: {bundle_id}\n",
            f"        DEVELOPMENT_TEAM: {_yaml_string(ios.team_id)}\n",
        ],
        "ios-api-url-dev": [f"API_BASE_URL = {_xcconfig_url(urls['dev'])}\n"] if "dev" in urls else None,
        "ios-api-url-prod": [f"API_BASE_URL = {_xcconfig_url(urls['prod'])}\n"] if "prod" in urls else None,
        "ios-display-name": [
            "    <key>CFBundleDisplayName</key>\n",
            f"    <string>{xml_escape(spec.label)}</string>\n",
        ],
        "ios-localizations": [
            "    <key>CFBundleLocalizations</key>\n",
            "    <array>\n",
            *(f"        <string>{tag}</string>\n" for tag in (SOURCE_LANGUAGE, *spec.languages)),
            "    </array>\n",
        ],
    }


def _launcher_background(spec: Spec) -> list[str] | None:
    """The adaptive icon's background, so the launcher matches the app it opens."""
    if not (spec.accent_colour or spec.secondary_colour or spec.tertiary_colour):
        return None
    accent = brand.brand_ramps(spec.accent_colour, spec.secondary_colour, spec.tertiary_colour)["Accent"].base
    return [f'    <color name="launcher_background">#FF{brand.to_hex(accent)[1:]}</color>\n']


def _yaml_string(text: str) -> str:
    """`text` as a double-quoted YAML scalar. JSON's string syntax is a subset of YAML's."""
    return json.dumps(text, ensure_ascii=False)


def _xcconfig_url(url: str) -> str:
    """An xcconfig reads `//` as the start of a comment; an empty `$()` between the slashes is not."""
    return url.replace("//", "/$()/")
