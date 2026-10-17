"""
Catalogue v2: everything a client needs to render the configurator, for every target, as JSON.

    python3 -m genkit.core.catalogue --json     # print it, with what this machine can do
    python3 -m genkit.core.catalogue --write    # refresh web/frontend/lib/catalogue.json
    python3 -m genkit.core.catalogue --check    # fail when that copy is stale

The website builds from the checked-in copy (a Vercel preview has no Python), so any change to a
feature, a preset, a token or a rule is followed by `--write` and the file is committed. `--check`
ignores `generator_commit`, which changes with every commit.
"""

from __future__ import annotations

import argparse
import difflib
import json
import re
import sys
from typing import Any

from .. import targets, tokens
from ..tokens import web
from . import record, translations
from .copy import write_text
from .manifest import Feature, TemplateManifest
from .spec import (
    APP_NAME_PATTERN,
    DEFAULT_DESIGN_STYLE,
    DEFAULT_FONT,
    DEFAULT_MONO_FONT,
    DEFAULT_MOTION_STYLE,
    FONT_PATTERN,
    FORBIDDEN_FIRST_SEGMENTS,
    HEX_PATTERN,
    HOST_PATTERN,
    HTTP_URL_PATTERN,
    JAVA_RESERVED,
    KOTLIN_KEYWORDS,
    MAX_APP_NAME,
    MAX_DISPLAY_NAME,
    MAX_FEATURE_MODULES,
    MAX_FEATURES,
    MAX_KEYSTORES,
    MAX_LANGUAGES,
    MAX_MODULE_NAME,
    MAX_PACKAGE,
    MAX_PACKAGE_SEGMENTS,
    MAX_SEGMENT,
    MAX_SPEC_BYTES,
    MODULE_NAME_PATTERN,
    PACKAGE_SEGMENT_PATTERN,
    SCHEME_PATTERN,
    SOCKET_URL_PATTERN,
    VERSION_NAME_PATTERN,
)
from .target import Target

WEBSITE_COPY = record.REPOSITORY / "web" / "frontend" / "lib" / "catalogue.json"

#: Offered beside the typeface field. Not a list of what is allowed: any Google Fonts family works.
FONT_SUGGESTIONS: tuple[tuple[str, str], ...] = (
    ("DM Sans", "geometric, large x-height, reads well from 11sp to 32sp"),
    ("Inter", "the safe choice; designed for screens, very neutral"),
    ("Plus Jakarta Sans", "a little more character, good for consumer apps"),
    ("Manrope", "rounder and friendlier, slightly less good at small sizes"),
    ("Figtree", "warm and open; strong at display sizes"),
    ("Outfit", "geometric and clean, at its best in headlines"),
    ("Space Grotesk", "technical, with quirky details"),
    ("Nunito", "soft and rounded"),
    ("Sora", "wide and modern"),
    ("Fraunces", "a soft serif with personality"),
    ("Libre Baskerville", "a classic book serif"),
    ("IBM Plex Sans", "corporate and very legible"),
)


def catalogue(environment: bool = False) -> dict[str, Any]:
    """
    The whole catalogue. `environment` adds what this machine can do (whether keytool exists to
    generate signing keys), which differs per machine and so stays out of the checked-in copy.
    """
    registry = targets.REGISTRY
    own = {key: target.catalogue() for key, target in registry.items()}
    data: dict[str, Any] = {
        "version": 2,
        "generator_commit": record.generator_commit(),
        "targets": [_target(target, own[key]) for key, target in registry.items()],
        "rules": _rules(own),
        "tokens": web.catalogue_tokens(tokens.resolve(None, "web")),
    }
    if environment:
        data["environment"] = {key: target.environment() for key, target in registry.items()}
    return data


def group_key(title: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")


def _target(target: Target, own: dict[str, Any]) -> dict[str, Any]:
    manifest = target.manifest
    styles = tokens.load()
    return {
        "key": target.key,
        "title": manifest.title,
        "status": manifest.status,
        "status_note": manifest.status_note,
        "summary": manifest.summary,
        "groups": [{"key": group_key(title), "title": title} for title in manifest.groups],
        "features": [_feature(manifest, feature) for feature in manifest.features.values()],
        "presets": [
            {
                "key": preset.key,
                "title": preset.title,
                "description": preset.description,
                "features": sorted(manifest.preset_features(preset.key)),
                "default": preset.key == manifest.default_preset,
            }
            for preset in manifest.presets.values()
        ],
        "defaults": {
            "preset": manifest.default_preset,
            "design_style": DEFAULT_DESIGN_STYLE,
            "motion_style": DEFAULT_MOTION_STYLE,
            "font_name": DEFAULT_FONT,
            "mono_font_name": DEFAULT_MONO_FONT,
            "accent_colour": styles["palette"]["templateAccent"],
            "haptics_enabled": True,
            "languages": [],
            **own["defaults"],
        },
        "fields": _common_fields() + own["fields"],
        "architectures": own["architectures"],
        "design_styles": [
            {"key": name, "title": name, "description": style["description"], "requires": list(style["requires"])}
            for name, style in styles["styles"].items()
        ],
        "motion_styles": [
            {"key": name, "title": name, "description": style["description"]}
            for name, style in styles["motion"]["styles"].items()
        ],
        "languages": [{"tag": tag, "name": name} for tag, name in translations.available(target.key).items()],
    }


def _feature(manifest: TemplateManifest, feature: Feature) -> dict[str, Any]:
    return {
        "key": feature.key,
        "title": feature.title,
        "headline": feature.headline,
        "description": feature.description,
        "group": group_key(feature.group),
        "default": feature.default,
        "requires": list(feature.requires),
        # The full set this feature brings in, so a client can tick them without the graph.
        "implies": sorted(manifest.resolve({feature.key}) - {feature.key}),
        "implied_only": feature.implied_only,
        "licence": (
            {"name": feature.licence.name, "spdx": feature.licence.spdx, "url": feature.licence.url}
            if feature.licence else None
        ),
        "platforms": list(feature.platforms),
    }


def _common_fields() -> list[dict[str, Any]]:
    return [
        {"key": "app_name", "type": "string", "rule": "app_name", "required": True},
        {"key": "display_name", "type": "string", "max": MAX_DISPLAY_NAME},
        {"key": "package_name", "type": "string", "rule": "package", "required": True},
        {"key": "architecture", "type": "choice"},
        {"key": "features", "type": "features", "max": MAX_FEATURES},
        {"key": "feature_modules", "type": "list", "rule": "module_name", "max": MAX_FEATURE_MODULES},
        {"key": "design_style", "type": "choice", "default": DEFAULT_DESIGN_STYLE},
        {"key": "motion_style", "type": "choice", "default": DEFAULT_MOTION_STYLE},
        {"key": "accent_colour", "type": "colour", "rule": "hex"},
        {"key": "secondary_colour", "type": "colour", "rule": "hex"},
        {"key": "tertiary_colour", "type": "colour", "rule": "hex"},
        {
            "key": "font_name", "type": "string", "rule": "font", "default": DEFAULT_FONT,
            "suggestions": [{"value": name, "note": note} for name, note in FONT_SUGGESTIONS],
        },
        {"key": "mono_font_name", "type": "string", "rule": "font", "default": DEFAULT_MONO_FONT},
        {"key": "haptics_enabled", "type": "bool", "default": True},
        {"key": "languages", "type": "list", "max": MAX_LANGUAGES},
    ]


def _rules(own_by_target: dict[str, dict[str, Any]]) -> dict[str, Any]:
    rules: dict[str, Any] = {
        "app_name": APP_NAME_PATTERN,
        "app_name_max": MAX_APP_NAME,
        "display_name_max": MAX_DISPLAY_NAME,
        "package_max": MAX_PACKAGE,
        "package_segments_min": 2,
        "package_segments_max": MAX_PACKAGE_SEGMENTS,
        "segment": PACKAGE_SEGMENT_PATTERN,
        "segment_max": MAX_SEGMENT,
        "package_first_segment_forbidden": list(FORBIDDEN_FIRST_SEGMENTS),
        "module_name": MODULE_NAME_PATTERN,
        "module_name_max": MAX_MODULE_NAME,
        "feature_modules_max": MAX_FEATURE_MODULES,
        "features_max": MAX_FEATURES,
        "languages_max": MAX_LANGUAGES,
        "keystores_max": MAX_KEYSTORES,
        "spec_bytes_max": MAX_SPEC_BYTES,
        "reserved_words": {"kotlin": sorted(KOTLIN_KEYWORDS), "java": sorted(JAVA_RESERVED)},
        "template_module_names": {},
        "hex": HEX_PATTERN,
        "version_name": VERSION_NAME_PATTERN,
        "http_url": HTTP_URL_PATTERN,
        "socket_url": SOCKET_URL_PATTERN,
        "font": FONT_PATTERN,
        "scheme": SCHEME_PATTERN,
        "host": HOST_PATTERN,
    }
    for key, own in own_by_target.items():
        rules["template_module_names"][key] = own["reserved_module_names"]
        for name, value in own.get("rules", {}).items():
            # The website reads one flat table, so a second target giving a rule a different value
            # would leave the first validating against the second's.
            if name in rules and rules[name] != value:
                raise ValueError(f"The {key} target's catalogue rule '{name}' clashes with one already set.")
            rules[name] = value
    return rules


def _static_json(data: dict[str, Any]) -> str:
    return json.dumps(data, indent=2, sort_keys=True, ensure_ascii=False) + "\n"


def _comparable(text: str) -> str:
    data = json.loads(text)
    data.pop("generator_commit", None)
    return _static_json(data)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="python3 -m genkit.core.catalogue", description=__doc__.strip().splitlines()[0])
    action = parser.add_mutually_exclusive_group(required=True)
    action.add_argument("--json", action="store_true", help="print the catalogue")
    action.add_argument("--write", action="store_true", help=f"write {WEBSITE_COPY.relative_to(record.REPOSITORY).as_posix()}")
    action.add_argument("--check", action="store_true", help="fail when the website's copy is stale")
    args = parser.parse_args(argv)

    if args.json:
        sys.stdout.write(_static_json(catalogue(environment=True)))
        return 0

    fresh = _static_json(catalogue())
    relative = WEBSITE_COPY.relative_to(record.REPOSITORY).as_posix()
    if args.write:
        WEBSITE_COPY.parent.mkdir(parents=True, exist_ok=True)
        write_text(WEBSITE_COPY, fresh)
        print(f"wrote {relative}")
        return 0

    if not WEBSITE_COPY.is_file():
        print(f"{relative} does not exist. Run: python3 -m genkit.core.catalogue --write")
        return 1
    current = _comparable(WEBSITE_COPY.read_text(encoding="utf-8"))
    expected = _comparable(fresh)
    if current != expected:
        sys.stdout.writelines(
            difflib.unified_diff(
                current.splitlines(keepends=True), expected.splitlines(keepends=True),
                f"{relative} (checked in)", f"{relative} (fresh)",
            )
        )
        print(f"\n{relative} is stale. Run: python3 -m genkit.core.catalogue --write")
        return 1
    print(f"{relative} is current.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
