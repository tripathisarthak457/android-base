"""The Android target: `templates/android`, its `android.*` settings, and what it generates."""

from __future__ import annotations

import re
from functools import cached_property
from pathlib import Path
from typing import Any

from ... import tokens
from ...tokens import kotlin
from ...core import translations
from ...core.copy import write_text
from ...core.manifest import TemplateManifest
from ...core.spec import (
    HOST_PATTERN,
    HTTP_URL_PATTERN,
    MAX_KEYSTORES,
    SCHEME_PATTERN,
    SOCKET_URL_PATTERN,
    VERSION_NAME_PATTERN,
    AndroidSettings,
    Spec,
    SpecError,
    Vocabulary,
)
from ...core.target import BuildContext
from . import icons, keystores, look, modes, scaffold, settings, strings, tabs, variants
from .settings import FLAVOURS

TEMPLATE_DIR = Path(__file__).resolve().parents[4] / "templates" / "android"

#: API level → (Android version, codename), for the minSdk choice: nobody remembers which number
#: is which, and picking minSdk blind is how a project excludes a quarter of its market.
API_LEVELS: dict[int, tuple[str, str]] = {
    24: ("7.0", "Nougat"),
    25: ("7.1", "Nougat"),
    26: ("8.0", "Oreo"),
    27: ("8.1", "Oreo"),
    28: ("9", "Pie"),
    29: ("10", "Quince Tart"),
    30: ("11", "Red Velvet Cake"),
    31: ("12", "Snow Cone"),
    32: ("12L", "Snow Cone v2"),
    33: ("13", "Tiramisu"),
    34: ("14", "Upside Down Cake"),
    35: ("15", "Vanilla Ice Cream"),
    36: ("16", "Baklava"),
    37: ("17", "Cinnamon Bun"),
}

#: Below this, `java.time` needs the desugaring library, which the build turns on by itself.
DESUGARING_THRESHOLD = 26

#: The lowest compileSdk the template's libraries accept. AndroidX publishes the level each release
#: needs in its AAR metadata, and compiling below it fails `checkAarMetadata` on the first build.
MIN_COMPILE_SDK = 37

#: Play's ceiling for versionCode.
MAX_VERSION_CODE = 2_100_000_000

#: Module names that name the template's structure rather than a feature directory.
_STRUCTURAL_NAMES = frozenset({"app", "core", "data", "feature", "build", "catalog", "benchmark"})

_SCHEME = re.compile(SCHEME_PATTERN)
_HOST = re.compile(HOST_PATTERN)
_HTTP_URL = re.compile(HTTP_URL_PATTERN)
_SOCKET_URL = re.compile(SOCKET_URL_PATTERN)
_VERSION_NAME = re.compile(VERSION_NAME_PATTERN)


def describe_api_level(level: int) -> str:
    version, codename = API_LEVELS.get(level, ("?", "unknown"))
    return f"API {level} · Android {version} ({codename})"


class AndroidTarget:
    key = "android"
    template_dir = TEMPLATE_DIR

    @cached_property
    def manifest(self) -> TemplateManifest:
        return TemplateManifest.load(self.template_dir)

    @cached_property
    def reserved_module_names(self) -> frozenset[str]:
        """The template's own module names, which a scaffolded module would collide with."""
        names = set(_STRUCTURAL_NAMES)
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
        android = spec.android or AndroidSettings()
        validate_android(android)

        offered = translations.available(self.key)
        for tag in spec.languages:
            if tag not in offered:
                raise SpecError(
                    f"No translation for '{tag}'. Choose from: {', '.join(offered)}. English is "
                    "always included.",
                    "languages",
                )
        modes.check(spec)
        return []

    def identity(self, spec: Spec) -> dict[str, str]:
        return {
            "package": spec.package_name,
            "package_path": spec.package_path,
            "pascal": spec.pascal_name,
            "snake": spec.snake_name,
            "lower": spec.lower_name,
        }

    def regions(self, spec: Spec) -> dict[str, list[str] | None]:
        return {**settings.regions(spec), **tabs.regions(spec), **scaffold.regions(spec)}

    def overlays(self, spec: Spec) -> dict[str, Path]:
        return variants.overlays(spec)

    def transform(self, project: Path, spec: Spec) -> list[str]:
        scaffold.write_feature_modules(project, spec)
        return []

    def finish(self, project: Path, spec: Spec, context: BuildContext) -> list[str]:
        # The template ships its token copy emitted for its own defaults; the project's carries
        # the user's brand and choices.
        design_tokens = project / kotlin.RELATIVE_PATH.format(package_path=spec.package_path)
        if design_tokens.is_file():
            write_text(design_tokens, kotlin.emit(tokens.resolve(spec), spec.package_name))
        look.apply(project, spec)
        strings.write(project, spec)
        warnings = keystores.write(project, (spec.android or AndroidSettings()).keystores, context.summary)
        if context.icon_source is not None:
            warnings += icons.generate(context.icon_source, project, spec)
        return warnings

    def outline(self, spec: Spec) -> dict[str, list[str]]:
        return {
            "modules": [f":{tier}:{name}" for tier in ("data", "feature") for name in spec.feature_modules],
            "tabs": tabs.labels(spec),
        }

    def environment(self) -> dict[str, Any]:
        # A deployment without a JDK answers false and the website hides the offer, rather than
        # taking passwords and returning a zip with no keys in it.
        return {"keytool": keystores.find_keytool() is not None}

    def catalogue(self) -> dict[str, Any]:
        defaults = AndroidSettings()
        levels = [
            {
                "value": level,
                "label": describe_api_level(level),
                "version": version,
                "codename": codename,
                "needs_desugaring": level < DESUGARING_THRESHOLD,
            }
            for level, (version, codename) in sorted(API_LEVELS.items())
        ]
        return {
            "defaults": {
                "architecture": "compact",
                "android": {
                    "min_sdk": defaults.min_sdk,
                    "target_sdk": defaults.target_sdk,
                    "compile_sdk": defaults.compile_sdk,
                    "version_name": defaults.version_name,
                    "version_code": defaults.version_code,
                },
            },
            "fields": [
                {"key": "android.min_sdk", "type": "int", "default": defaults.min_sdk, "choices": levels},
                {"key": "android.target_sdk", "type": "int", "default": defaults.target_sdk, "choices": levels},
                {
                    "key": "android.compile_sdk", "type": "int", "default": defaults.compile_sdk,
                    "choices": [choice for choice in levels if choice["value"] >= MIN_COMPILE_SDK],
                },
                {"key": "android.version_name", "type": "string", "default": defaults.version_name, "rule": "version_name"},
                {"key": "android.version_code", "type": "int", "default": defaults.version_code, "min": 1, "max": MAX_VERSION_CODE},
                {"key": "android.api_base_urls", "type": "map", "keys": list(FLAVOURS), "rule": "http_url", "requires": ["network"]},
                {"key": "android.web_socket_urls", "type": "map", "keys": list(FLAVOURS), "rule": "socket_url", "requires": ["websocket"]},
                {"key": "android.deeplink_scheme", "type": "string", "rule": "scheme", "requires": ["deeplink"]},
                {"key": "android.deeplink_host", "type": "string", "rule": "host", "requires": ["deeplink"]},
                {
                    "key": "android.keystores", "type": "keystores", "max": MAX_KEYSTORES,
                    "names": list(keystores.KEYSTORE_NAMES),
                },
            ],
            "architectures": [
                {
                    "key": "compact",
                    "title": "Compact",
                    "description": "One module per feature and per data source. Simple, and what most apps need.",
                    "available": True,
                },
                {
                    "key": "modular",
                    "title": "Modular",
                    "description": "Each feature and data module split into :api and :impl, for larger teams.",
                    "available": modes.MODULAR_AVAILABLE,
                },
            ],
            "reserved_module_names": sorted(self.reserved_module_names),
            "rules": {
                "min_compile_sdk": MIN_COMPILE_SDK,
                "version_code_max": MAX_VERSION_CODE,
                "flavours": list(FLAVOURS),
                "keystore_names": list(keystores.KEYSTORE_NAMES),
                "keystore_password_min": keystores.MIN_KEYSTORE_PASSWORD,
                "keystore_generated_password": keystores.GENERATED_PASSWORD_PATTERN,
                "keystore_alias": keystores.ALIAS_PATTERN,
                "keystore_country": keystores.COUNTRY_PATTERN,
                "keystore_dname_forbidden": keystores.DNAME_SPECIALS,
                "keystore_validity_max_days": keystores.MAX_VALIDITY_DAYS,
            },
        }


def validate_android(android: AndroidSettings) -> None:
    if android.min_sdk not in API_LEVELS:
        raise SpecError(
            f"minSdk {android.min_sdk} is outside the supported range "
            f"({min(API_LEVELS)}–{max(API_LEVELS)}).",
            "android.min_sdk",
        )
    if android.target_sdk < android.min_sdk:
        raise SpecError("targetSdk cannot be lower than minSdk.", "android.target_sdk")
    newest = max(API_LEVELS)
    for label, key, level in (
        ("targetSdk", "target_sdk", android.target_sdk),
        ("compileSdk", "compile_sdk", android.compile_sdk),
    ):
        # An API level past the table has no SDK platform to install yet, so the first build fails.
        if level > newest:
            raise SpecError(
                f"{label} {level} is past API {newest}, the newest level this generator knows.", f"android.{key}"
            )
    if android.compile_sdk < android.target_sdk:
        raise SpecError("compileSdk cannot be lower than targetSdk.", "android.compile_sdk")
    if android.compile_sdk < MIN_COMPILE_SDK:
        raise SpecError(
            f"compileSdk must be at least {MIN_COMPILE_SDK}: the template's libraries are built "
            "against it, and an older one fails the project's first build.",
            "android.compile_sdk",
        )
    if not 1 <= android.version_code <= MAX_VERSION_CODE:
        raise SpecError(f"versionCode must be between 1 and {MAX_VERSION_CODE}.", "android.version_code")
    if not _VERSION_NAME.fullmatch(android.version_name):
        raise SpecError("versionName must look like 1.0.0.", "android.version_name")

    for label, key, urls, pattern, example in (
        ("API base URL", "api_base_urls", android.api_base_urls, _HTTP_URL, "https://api.example.com/"),
        ("WebSocket URL", "web_socket_urls", android.web_socket_urls, _SOCKET_URL, "wss://api.example.com/ws"),
    ):
        for flavour, url in urls.items():
            if flavour not in FLAVOURS:
                raise SpecError(
                    f"Unknown flavour '{flavour}' for an {label}. Choose from: {', '.join(FLAVOURS)}.",
                    f"android.{key}",
                )
            if not pattern.fullmatch(url):
                raise SpecError(
                    f"The {flavour} {label} must look like {example}, without quotes, spaces, "
                    "backslashes or $.",
                    f"android.{key}",
                )

    if android.deeplink_scheme and not _SCHEME.fullmatch(android.deeplink_scheme):
        raise SpecError(
            "The deep-link scheme must start with a lowercase letter and contain only lowercase "
            "letters, digits, +, . and -.",
            "android.deeplink_scheme",
        )
    if android.deeplink_host and not _HOST.fullmatch(android.deeplink_host):
        raise SpecError("The deep-link host must be a domain name, like example.com.", "android.deeplink_host")

    for keystore in android.keystores:
        keystores.validate(keystore)
    names = [keystore.name for keystore in android.keystores]
    if len(set(names)) != len(names):
        raise SpecError("Each signing key can only be described once.", "android.keystores")

