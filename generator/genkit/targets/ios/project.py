"""The regions of `project.yml` and the README: identity, platform settings and linked features."""

from __future__ import annotations

import json

from ...core.manifest import TemplateManifest
from ...core.spec import IosSettings, Spec
from .scaffold import feature_products


def regions(spec: Spec, manifest: TemplateManifest) -> dict[str, list[str] | None]:
    ios = spec.ios or IosSettings()
    return {
        "display-name": [f"        CFBundleDisplayName: {yaml_string(spec.label)}\n"],
        "deployment-target": ["  deploymentTarget:\n", f"    iOS: {yaml_string(ios.deployment_target)}\n"],
        "development-team": [f"    DEVELOPMENT_TEAM: {yaml_string(ios.team_id)}\n"],
        "localizations": [
            "        CFBundleLocalizations:\n",
            *(f"          - {tag}\n" for tag in ("en", *spec.languages)),
        ],
        "app-dependencies": [
            "      - package: Features\n",
            "        products:\n",
            *(f"          - {product}\n" for product in feature_products(spec)),
        ],
        "readme-features": [
            f"- `{key}`: {feature.title}\n"
            for key, feature in manifest.features.items()
            if key in spec.features
        ],
    }


def yaml_string(text: str) -> str:
    """`text` as a double-quoted YAML scalar. JSON's string syntax is a subset of YAML's."""
    return json.dumps(text, ensure_ascii=False)
