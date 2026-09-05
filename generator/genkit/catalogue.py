"""
The generator's own options, as JSON, for anything that is not a terminal.

The website renders a form from this. Duplicating the feature list in TypeScript would mean two
lists that agree until the day somebody adds a feature to one of them — and the failure would be
a checkbox on the site that does nothing, which nobody notices until a user asks why their
project has no Room.

    py -m genkit.catalogue > catalogue.json

Everything here is derived from `spec.py`. There is no second source.
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
    MOTION_STYLES,
    PRESETS,
    RESERVED_MODULE_NAMES,
    describe_api_level,
    resolve_features,
)

#: Which part of the wizard a feature belongs to. Purely presentational — the generator does not
#: care — but a flat list of twenty-four checkboxes is a list nobody reads to the end of.
GROUPS: dict[str, tuple[str, str]] = {
    "network": ("Data", "How the app talks to your backend and what it does when that fails."),
    "websocket": ("Data", ""),
    "room": ("Data", ""),
    "coil": ("Data", ""),
    "workmanager": ("Data", ""),
    "auth": ("Screens", "Whole flows, wired end to end. Delete any of them in one commit."),
    "settings": ("Screens", ""),
    "onboarding": ("Screens", ""),
    "sample": ("Screens", ""),
    "forms": ("Screens", ""),
    "media": ("Screens", ""),
    "deeplink": ("Screens", ""),
    "firebase": ("Google", "Nothing here is on by default. Each needs a Firebase project."),
    "analytics-firebase": ("Google", ""),
    "crashlytics": ("Google", ""),
    "push": ("Google", ""),
    "analytics": ("Google", ""),
    "googlefonts": ("Design", "The look, and the app that shows it to you."),
    "catalog": ("Design", ""),
    "staticanalysis": ("Tooling", "The parts that keep it healthy after the first week."),
    "leakcanary": ("Tooling", ""),
    "baselineprofile": ("Tooling", ""),
    "ci": ("Tooling", ""),
    "fastlane": ("Tooling", ""),
}

GROUP_ORDER: tuple[str, ...] = ("Data", "Screens", "Design", "Google", "Tooling")

#: What a feature adds that a user can see, in one line. `Feature.description` explains what it
#: *is*; this says why you would want it. Both are shown, the second as the headline.
HEADLINES: dict[str, str] = {
    "network": "Talk to a REST API",
    "websocket": "Keep a live connection open",
    "room": "Work offline",
    "coil": "Load images from URLs",
    "workmanager": "Run work in the background",
    "analytics": "Track events and crashes, vendor-free",
    "firebase": "Connect a Firebase project",
    "analytics-firebase": "Send events to Firebase",
    "crashlytics": "Report crashes to Crashlytics",
    "push": "Receive push notifications",
    "deeplink": "Open the app from a link",
    "catalog": "A second app showing every component",
    "sample": "A working screen to learn from",
    "media": "Pick and compress photos and video",
    "forms": "Validate forms properly",
    "auth": "Sign in, sign up, reset password",
    "settings": "A settings screen",
    "onboarding": "A first-run walkthrough",
    "googlefonts": "Use any Google Font",
    "staticanalysis": "Catch style problems in CI",
    "leakcanary": "Find memory leaks in debug builds",
    "baselineprofile": "Cut cold-start time",
    "ci": "Build and test every pull request",
    "fastlane": "Ship releases with one command",
}


def catalogue() -> dict[str, Any]:
    """Everything a client needs to render the form and label what it is offering."""
    return {
        "features": [
            {
                "key": feature.key,
                "title": feature.title,
                "headline": HEADLINES.get(feature.key, feature.title),
                "description": feature.description,
                "default": feature.default,
                "requires": list(feature.requires),
                # The full set this feature drags in, so the UI can tick them without knowing the
                # dependency graph.
                "implies": sorted(resolve_features({feature.key}) - {feature.key}),
                "group": GROUPS.get(feature.key, ("Tooling", ""))[0],
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
        ],
    }


if __name__ == "__main__":
    json.dump(catalogue(), sys.stdout, indent=2)
    sys.stdout.write("\n")
