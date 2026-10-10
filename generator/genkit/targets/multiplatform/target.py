"""The Compose Multiplatform target: `templates/multiplatform`, its `android.*` and `ios.*` settings."""

from __future__ import annotations

import re
from functools import cached_property
from pathlib import Path
from typing import Any

from ... import tokens
from ...core import translations
from ...core.manifest import TemplateManifest
from ...core.spec import AndroidSettings, IosSettings, Spec, SpecError, Vocabulary
from ...core.target import BuildContext
from ...tokens import kotlin_common
from ..android.target import API_LEVELS, MAX_VERSION_CODE, MIN_COMPILE_SDK, describe_api_level, validate_android
from . import modules, settings, strings

TEMPLATE_DIR = Path(__file__).resolve().parents[4] / "templates" / "multiplatform"

#: The Android app's flavours. The iOS app has the same two, as build configurations.
FLAVOURS: tuple[str, ...] = ("dev", "prod")

#: The iOS releases a project can target, oldest first. Compose Multiplatform runs from iOS 15; a
#: release past the newest has no SDK yet, so its first build would fail. Apple went from 18 to 26.
IOS_VERSIONS: tuple[int, ...] = (15, 16, 17, 18, 26, 27)

#: Letters, digits and hyphens between dots, as Apple allows in a bundle identifier.
BUNDLE_ID_PATTERN = r"^[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+$"
MAX_BUNDLE_ID = 155
DEPLOYMENT_TARGET_PATTERN = r"^\d{2}\.\d$"
TEAM_ID_PATTERN = r"^[A-Z0-9]{10}$"

#: Module names that name the project's structure rather than a feature directory.
_STRUCTURAL_NAMES = frozenset({"core", "data", "feature", "build", "shared", "resources"})

_BUNDLE_ID = re.compile(BUNDLE_ID_PATTERN)
_DEPLOYMENT_TARGET = re.compile(DEPLOYMENT_TARGET_PATTERN)
_TEAM_ID = re.compile(TEAM_ID_PATTERN)


def bundle_id(spec: Spec) -> str:
    """The spec's bundle id, or the package name with `_` turned into `-`, which bundle ids refuse."""
    ios = spec.ios or IosSettings()
    return ios.bundle_id or spec.package_name.replace("_", "-")


class MultiplatformTarget:
    key = "multiplatform"
    template_dir = TEMPLATE_DIR

    @cached_property
    def manifest(self) -> TemplateManifest:
        return TemplateManifest.load(self.template_dir)

    @cached_property
    def reserved_module_names(self) -> frozenset[str]:
        """The template's own module names, which a scaffolded module would collide with."""
        names = set(_STRUCTURAL_NAMES) | {modules.HOME}
        for tier in ("data", "feature"):
            names.update(path.name for path in (self.template_dir / tier).iterdir() if path.is_dir())
        return frozenset(names)

    def vocabulary(self) -> Vocabulary:
        return Vocabulary(
            design_styles=tokens.design_style_requirements(),
            motion_styles=tokens.motion_style_names(),
            reserved_module_names=self.reserved_module_names,
        )

    def validate(self, spec: Spec) -> list[str]:
        if spec.architecture != "compact":
            raise SpecError(
                "The Compose Multiplatform template has one layout, compact. Modular is Android-only "
                "for now.",
                "architecture",
            )
        validate_android_settings(spec.android or AndroidSettings())
        validate_ios(spec.ios or IosSettings(), spec.package_name)

        offered = translations.available(self.key)
        for tag in spec.languages:
            if tag not in offered:
                raise SpecError(
                    f"No translation for '{tag}'. Choose from: {', '.join(offered)}. English is "
                    "always included.",
                    "languages",
                )

        # Xcode expands build settings written as $(NAME) or ${NAME} in Info.plist values, so such
        # text would not reach the home screen as typed.
        if "$(" in spec.label or "${" in spec.label:
            raise SpecError("The display name cannot contain $( or ${: Xcode reads them as build settings.", "display_name")

        # A module's package drops the underscores of its name, so two names that differ only in
        # them would compile into one package.
        packages: dict[str, str] = {}
        for name in spec.feature_modules:
            other = packages.setdefault(modules.package_segment(name), name)
            if other != name:
                raise SpecError(
                    f"Module names '{other}' and '{name}' both become the package "
                    f"'{modules.package_segment(name)}'. Rename one.",
                    "feature_modules",
                )
        return []

    def identity(self, spec: Spec) -> dict[str, str]:
        return {
            "package": spec.package_name,
            "package_path": spec.package_path,
            "pascal": spec.pascal_name,
            "lower": spec.lower_name,
        }

    def regions(self, spec: Spec) -> dict[str, list[str] | None]:
        return {
            **settings.regions(spec),
            **settings.ios_regions(spec, bundle_id(spec)),
            **modules.regions(spec),
        }

    def overlays(self, spec: Spec) -> dict[str, Path]:
        return {}

    def transform(self, project: Path, spec: Spec) -> list[str]:
        modules.write_feature_modules(project, spec)
        return []

    def finish(self, project: Path, spec: Spec, context: BuildContext) -> list[str]:
        # The template ships its token copy emitted for its own defaults; the project's carries
        # the user's brand and choices.
        design_tokens = project / kotlin_common.RELATIVE_PATH.format(package_path=spec.package_path)
        if design_tokens.is_file():
            design_tokens.write_text(
                kotlin_common.emit(tokens.resolve(spec, kotlin_common.PLATFORM), spec.package_name),
                encoding="utf-8",
            )
        strings.write(project, spec)
        return modules.notes(spec)

    def outline(self, spec: Spec) -> dict[str, list[str]]:
        return {
            "modules": [f":{tier}:{name}" for name in modules.modules(spec) for tier in ("data", "feature")],
            "tabs": modules.labels(spec),
        }

    def environment(self) -> dict[str, Any]:
        return {}

    def catalogue(self) -> dict[str, Any]:
        android = AndroidSettings()
        ios = IosSettings()
        levels = [
            {"value": level, "label": describe_api_level(level), "version": version, "codename": codename}
            for level, (version, codename) in sorted(API_LEVELS.items())
        ]
        return {
            "defaults": {
                "architecture": "compact",
                "android": {
                    "min_sdk": android.min_sdk,
                    "target_sdk": android.target_sdk,
                    "compile_sdk": android.compile_sdk,
                    "version_name": android.version_name,
                    "version_code": android.version_code,
                },
                "ios": {"deployment_target": ios.deployment_target},
            },
            "fields": [
                {"key": "android.min_sdk", "type": "int", "default": android.min_sdk, "choices": levels},
                {"key": "android.target_sdk", "type": "int", "default": android.target_sdk, "choices": levels},
                {
                    "key": "android.compile_sdk", "type": "int", "default": android.compile_sdk,
                    "choices": [choice for choice in levels if choice["value"] >= MIN_COMPILE_SDK],
                },
                {"key": "android.version_name", "type": "string", "default": android.version_name, "rule": "version_name"},
                {"key": "android.version_code", "type": "int", "default": android.version_code, "min": 1, "max": MAX_VERSION_CODE},
                {"key": "android.api_base_urls", "type": "map", "keys": list(FLAVOURS), "rule": "http_url", "requires": ["network"]},
                {"key": "ios.bundle_id", "type": "string", "rule": "ios_bundle_id", "max": MAX_BUNDLE_ID},
                {
                    "key": "ios.deployment_target", "type": "string", "default": ios.deployment_target,
                    "choices": [{"value": f"{major}.0", "label": f"iOS {major}"} for major in IOS_VERSIONS],
                },
                {"key": "ios.team_id", "type": "string", "rule": "ios_team_id"},
            ],
            "architectures": [
                {
                    "key": "compact",
                    "title": "Compact",
                    "description": "One module per feature and per data source, shared by the Android and iOS apps.",
                    "available": True,
                },
            ],
            "reserved_module_names": sorted(self.reserved_module_names),
            "rules": {
                "min_compile_sdk": MIN_COMPILE_SDK,
                "version_code_max": MAX_VERSION_CODE,
                "ios_bundle_id": BUNDLE_ID_PATTERN,
                "ios_bundle_id_max": MAX_BUNDLE_ID,
                "ios_deployment_target": DEPLOYMENT_TARGET_PATTERN,
                "ios_team_id": TEAM_ID_PATTERN,
                "multiplatform_ios_versions": list(IOS_VERSIONS),
            },
        }


def validate_android_settings(android: AndroidSettings) -> None:
    """The Android target's rules for what both templates share, and a refusal for what this one lacks."""
    validate_android(android)
    for flavour in android.api_base_urls:
        if flavour not in FLAVOURS:
            raise SpecError(
                f"Unknown flavour '{flavour}' for an API base URL. This template has {' and '.join(FLAVOURS)}.",
                "android.api_base_urls",
            )
    for key, given in (
        ("web_socket_urls", bool(android.web_socket_urls)),
        ("deeplink_scheme", bool(android.deeplink_scheme)),
        ("deeplink_host", bool(android.deeplink_host)),
        ("keystores", bool(android.keystores)),
    ):
        if given:
            raise SpecError(
                f"android.{key} is not offered by the Compose Multiplatform template yet. Leave it out.",
                f"android.{key}",
            )


def validate_ios(ios: IosSettings, package_name: str) -> None:
    if ios.bundle_id:
        if len(ios.bundle_id) > MAX_BUNDLE_ID or not _BUNDLE_ID.fullmatch(ios.bundle_id):
            raise SpecError(
                "The bundle id must look like com.example.app: letters, digits and hyphens between "
                f"dots, at most {MAX_BUNDLE_ID} characters. Underscores are not allowed.",
                "ios.bundle_id",
            )
    elif len(package_name) > MAX_BUNDLE_ID:
        raise SpecError(
            f"The package name is longer than the {MAX_BUNDLE_ID} characters a bundle id may have; give "
            "a shorter ios.bundle_id.",
            "ios.bundle_id",
        )

    if not _DEPLOYMENT_TARGET.fullmatch(ios.deployment_target) or int(
        ios.deployment_target.partition(".")[0]
    ) not in IOS_VERSIONS:
        raise SpecError(
            f"The deployment target must be an iOS release from {IOS_VERSIONS[0]}.0 to {IOS_VERSIONS[-1]}.x, "
            f"like 17.0: {', '.join(str(major) for major in IOS_VERSIONS)}.",
            "ios.deployment_target",
        )
    if ios.team_id and not _TEAM_ID.fullmatch(ios.team_id):
        raise SpecError("The team id is the 10 capital letters and digits Apple shows under Membership.", "ios.team_id")
