"""
The generator's own options, as JSON, for anything that is not a terminal.

    py -m genkit.catalogue > catalogue.json
"""

from __future__ import annotations

import json
import sys
from typing import Any

from .render import find_keytool
from .spec import (
    API_LEVELS,
    DEFAULT_COMPILE_SDK,
    DEFAULT_MIN_SDK,
    DEFAULT_TARGET_SDK,
    DESUGARING_THRESHOLD,
    FEATURES,
    KEYSTORE_NAMES,
    MIN_KEYSTORE_PASSWORD,
    DESIGN_STYLES,
    MOTION_STYLES,
    PRESETS,
    RESERVED_MODULE_NAMES,
    describe_api_level,
    resolve_features,
)
from .translations import available as available_languages

#: Which part of the wizard a feature belongs to. Purely presentational — the generator does not
#: care — but a flat list of three dozen checkboxes is a list nobody reads to the end of.
GROUPS: dict[str, tuple[str, str]] = {
    "network": ("Data", "How the app talks to your backend and what it does when that fails."),
    "websocket": ("Data", ""),
    "room": ("Data", ""),
    "database": ("Data", ""),
    "coil": ("Data", ""),
    "paging": ("Data", ""),
    "workmanager": ("Data", ""),
    "auth": ("Screens", "Whole flows, wired end to end. Delete any of them in one commit."),
    "googlesignin": ("Screens", ""),
    "onboarding": ("Screens", ""),
    "profile": ("Screens", ""),
    "search": ("Screens", ""),
    "settings": ("Screens", ""),
    "language": ("Screens", ""),
    "sample": ("Screens", ""),
    "forms": ("Screens", ""),
    "media": ("Screens", ""),
    "applock": ("Screens", ""),
    "licenses": ("Screens", ""),
    "deeplink": ("Platform", "The parts of Android outside your own screens."),
    "browser": ("Platform", ""),
    "widget": ("Platform", ""),
    "playstore": ("Platform", ""),
    "firebase": ("Google", "Off by default. Each needs a Firebase project."),
    "push": ("Google", ""),
    "analytics": ("Google", ""),
    "flags": ("Google", ""),
    "googlefonts": ("Design", "The look, and the app that shows it to you."),
    "lottie": ("Design", ""),
    "catalog": ("Design", ""),
    "composemetrics": ("Design", ""),
    "leakcanary": ("Tooling", "The parts that keep it healthy after the first week."),
    "devtools": ("Tooling", ""),
    "coverage": ("Tooling", ""),
    "depsanalysis": ("Tooling", ""),
    "baselineprofile": ("Tooling", ""),
}

GROUP_ORDER: tuple[str, ...] = ("Data", "Screens", "Platform", "Design", "Google", "Tooling")

#: What a feature adds that a user can see, in one line. `Feature.description` explains what it
#: *is*; this says why you would want it. Both are shown, the second as the headline.
HEADLINES: dict[str, str] = {
    "network": "Talk to a REST API",
    "websocket": "Keep a live connection open",
    "room": "Work offline",
    "coil": "Load images from URLs",
    "lottie": "Play Lottie animations",
    "paging": "An endless feed that loads as you scroll",
    "workmanager": "Run work in the background",
    "analytics": "Track events and crashes, vendor-free",
    "firebase": "Analytics, Crashlytics and Remote Config",
    "push": "Receive push notifications",
    "deeplink": "Open the app from a link",
    "browser": "Open web pages without leaving the app",
    "widget": "A widget on the home screen",
    "catalog": "A second app showing every component",
    "sample": "A working screen to learn from",
    "media": "Pick and compress photos and video",
    "forms": "Validate forms properly",
    "auth": "Sign in, sign up, reset password",
    "googlesignin": "Continue with Google",
    "profile": "A profile tab with a photo",
    "search": "Search as you type",
    "settings": "A settings screen",
    "language": "Let people pick the app's language",
    "onboarding": "A first-run walkthrough",
    "googlefonts": "Use any Google Font",
    "leakcanary": "Find memory leaks in debug builds",
    "baselineprofile": "Cut cold-start time",
    "database": "Store your own data, not just a cache",
    "devtools": "See every request the app made",
    "licenses": "Show the licences you have to show",
    "flags": "Turn a feature on without shipping",
    "applock": "Ask for a fingerprint on resume",
    "playstore": "Offer updates and ask for ratings",
    "coverage": "Fail a build that drops coverage",
    "depsanalysis": "See which dependencies nobody uses",
    "composemetrics": "Catch a component that cannot skip",
}


def describe(feature_key: str) -> tuple[str, str]:
    """The group and headline for one feature, or [KeyError] naming what is missing."""
    missing = [name for name, table in (("GROUPS", GROUPS), ("HEADLINES", HEADLINES))
               if feature_key not in table]
    if missing:
        raise KeyError(
            f"Feature '{feature_key}' is missing from {' and '.join(missing)} in catalogue.py. "
            "Every feature needs a group to be filed under and a headline saying what it buys."
        )
    return GROUPS[feature_key][0], HEADLINES[feature_key]


def catalogue() -> dict[str, Any]:
    """Everything a client needs to render the form and label what it is offering."""
    return {
        "features": [
            {
                "key": feature.key,
                "title": feature.title,
                "headline": describe(feature.key)[1],
                "description": feature.description,
                "default": feature.default,
                "requires": list(feature.requires),
                # The full set this feature drags in, so the UI can tick them without knowing the
                # dependency graph.
                "implies": sorted(resolve_features({feature.key}) - {feature.key}),
                "group": describe(feature.key)[0],
            }
            for feature in FEATURES
            if not feature.implied_only
        ],
        "groups": [
            {
                "name": name,
                "caption": next(
                    (caption for _, (group, caption) in GROUPS.items() if group == name and caption),
                    "",
                ),
            }
            for name in GROUP_ORDER
        ],
        "presets": [
            {
                "key": preset.key,
                "title": preset.title,
                "description": preset.description,
                "features": sorted(resolve_features(set(preset.features))),
            }
            for preset in PRESETS
        ],
        "motionStyles": [
            {"key": name, "description": description} for name, description in MOTION_STYLES
        ],
        "designStyles": [
            {"key": name, "description": description} for name, description in DESIGN_STYLES
        ],
        # English is always shipped and is not listed.
        "languages": [{"tag": tag, "name": name} for tag, name in available_languages().items()],
        "apiLevels": [
            {
                "level": level,
                "label": describe_api_level(level),
                "version": version,
                "codename": codename,
                "needsDesugaring": level < DESUGARING_THRESHOLD,
            }
            for level, (version, codename) in sorted(API_LEVELS.items())
        ],
        "defaults": {
            "minSdk": DEFAULT_MIN_SDK,
            "targetSdk": DEFAULT_TARGET_SDK,
            "compileSdk": DEFAULT_COMPILE_SDK,
            "versionName": "1.0.0",
            "versionCode": 1,
            "fontName": "DM Sans",
            "monoFontName": "JetBrains Mono",
            "accentColour": "#2C6BED",
            "motionStyle": "Standard",
            "designStyle": "Utility",
            "hapticsEnabled": True,
            "preset": "standard",
        },
        "keystoreNames": list(KEYSTORE_NAMES),
        # Whether *this* server can create keys, not whether it should. A deployment with no JDK
        # on the image answers False and the site hides the offer, rather than taking passwords
        # and returning a zip with no keys in it.
        "keystoresAvailable": find_keytool() is not None,
        "minKeystorePassword": MIN_KEYSTORE_PASSWORD,
        # So the site rejects a taken module name in the form rather than after the round trip,
        # and cannot drift from the generator's own list.
        "reservedModuleNames": sorted(RESERVED_MODULE_NAMES),
        "fontSuggestions": [
            "DM Sans",
            "Inter",
            "Plus Jakarta Sans",
            "Manrope",
            "Figtree",
            "Outfit",
            "Space Grotesk",
            "Nunito",
            "Sora",
            "Fraunces",
            "Libre Baskerville",
            "IBM Plex Sans",
        ],
    }


if __name__ == "__main__":
    json.dump(catalogue(), sys.stdout, indent=2)
    sys.stdout.write("\n")
