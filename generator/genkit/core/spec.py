"""
The answers a project is generated from, in the schema-2 shape every target reads, and the rules
that hold whatever the target.

A spec arrives as JSON from the wizard, a file, the API or a project's own `generator-spec.json`.
`parse()` upgrades older shapes, checks every field's type, and returns a `Spec` with its feature
set resolved. Target-specific rules (SDK levels, signing, bundle ids) live with each target.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field, replace
from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:
    from .manifest import TemplateManifest

SCHEMA = 2
PLATFORMS: tuple[str, ...] = ("android", "multiplatform", "ios")
ARCHITECTURES: tuple[str, ...] = ("compact", "modular")

#: The key a generated project's record keeps its provenance under. Read back, it is ignored.
STAMP_KEY = "generator"

MAX_APP_NAME = 50
MAX_DISPLAY_NAME = 30
MAX_PACKAGE = 100
MAX_PACKAGE_SEGMENTS = 8
MAX_SEGMENT = 30
MAX_MODULE_NAME = 30
MAX_FEATURE_MODULES = 12
MAX_FEATURES = 64
MAX_LANGUAGES = 10
MAX_KEYSTORES = 4
MAX_SPEC_BYTES = 32 * 1024

DEFAULT_FONT = "DM Sans"
DEFAULT_MONO_FONT = "JetBrains Mono"
DEFAULT_DESIGN_STYLE = "Utility"
DEFAULT_MOTION_STYLE = "Standard"

DEFAULT_MIN_SDK = 26
DEFAULT_TARGET_SDK = 37
DEFAULT_COMPILE_SDK = 37

#: Hard keywords cannot name a package segment without backticks, which generated code never
#: writes. Soft and modifier keywords (`value`, `data`, `get`) are ordinary identifiers there.
KOTLIN_KEYWORDS: frozenset[str] = frozenset({
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
    "interface", "is", "null", "object", "package", "return", "super", "this", "throw", "true",
    "try", "typealias", "typeof", "val", "var", "when", "while",
})

JAVA_RESERVED: frozenset[str] = frozenset({
    "_", "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
    "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final",
    "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
    "interface", "long", "native", "new", "null", "package", "private", "protected", "public",
    "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
    "throws", "transient", "true", "try", "void", "volatile", "while",
})

#: ART refuses to load an app's classes from these, and Kotlin reserves its own.
FORBIDDEN_FIRST_SEGMENTS: tuple[str, ...] = ("java", "javax", "kotlin")

# Every pattern is matched with `fullmatch`. They are written anchored as well because the website
# reads them from the catalogue and JavaScript has no fullmatch.
APP_NAME_PATTERN = r"^[A-Za-z][A-Za-z0-9 ._-]{0,49}$"
PACKAGE_SEGMENT_PATTERN = r"^[a-z][a-z0-9_]*$"
#: No `__` and no trailing `_`: `order__history` and `order_history` would both be OrderHistory.
MODULE_NAME_PATTERN = r"^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$"
HEX_PATTERN = r"^#?[0-9A-Fa-f]{6}$"
VERSION_NAME_PATTERN = r"^\d+\.\d+\.\d+$"
#: A Google Fonts family name as the family page spells it: "Plus Jakarta Sans", "M PLUS 1p".
FONT_PATTERN = r"^[A-Za-z0-9][A-Za-z0-9 -]{0,59}$"
#: Narrower than RFC 3986 on purpose: no quote, backslash or `$`, because the value is written
#: into a Kotlin string literal in build-logic, which Gradle compiles and runs.
_URL_BODY = r"[A-Za-z0-9._~:/?#\[\]@!&'()*+,;=%-]+"
HTTP_URL_PATTERN = rf"^https?://{_URL_BODY}$"
SOCKET_URL_PATTERN = rf"^wss?://{_URL_BODY}$"
SCHEME_PATTERN = r"^[a-z][a-z0-9+.-]{0,31}$"
HOST_PATTERN = (
    r"^(?=.{1,253}$)[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$"
)

_APP_NAME = re.compile(APP_NAME_PATTERN)
_SEGMENT = re.compile(PACKAGE_SEGMENT_PATTERN)
_MODULE = re.compile(MODULE_NAME_PATTERN)
_HEX = re.compile(HEX_PATTERN)
_FONT = re.compile(FONT_PATTERN)

#: The fields every schema-2 spec may carry at the top level, besides `android` and `ios`.
_COMMON_FIELDS = (
    "schema", "platform", "app_name", "display_name", "package_name", "architecture", "features",
    "feature_modules", "design_style", "motion_style", "accent_colour", "secondary_colour",
    "tertiary_colour", "font_name", "mono_font_name", "haptics_enabled", "languages",
)

#: Fields that sat at the top level in schema 1 and live under `android` now.
ANDROID_FIELDS: tuple[str, ...] = (
    "min_sdk", "target_sdk", "compile_sdk", "version_name", "version_code", "keystores",
    "api_base_urls", "web_socket_urls", "deeplink_scheme", "deeplink_host",
)

IOS_FIELDS: tuple[str, ...] = ("bundle_id", "deployment_target", "team_id")

KEYSTORE_FIELDS: tuple[str, ...] = (
    "name", "alias", "store_password", "key_password", "existing_path", "common_name",
    "organisation", "country", "validity_days",
)

#: Which settings block each platform reads.
_SETTINGS_BLOCKS: dict[str, tuple[str, ...]] = {
    "android": ("android",),
    "multiplatform": ("android", "ios"),
    "ios": ("ios",),
}


class SpecError(ValueError):
    """A rejected answer. The message is meant for whoever typed it; `field` names where."""

    def __init__(self, message: str, field: str | None = None):
        super().__init__(message)
        self.field = field


@dataclass(frozen=True)
class KeystoreSpec:
    name: str
    alias: str
    store_password: str
    #: Only read for an existing keystore. A generated one is PKCS12, which has one password.
    key_password: str = ""
    #: When set, an existing .jks is copied instead of a new one being generated.
    existing_path: str | None = None
    common_name: str = "Unknown"
    organisation: str = "Unknown"
    country: str = "US"
    validity_days: int = 10_000

    @property
    def dname(self) -> str:
        return f"CN={self.common_name}, O={self.organisation}, C={self.country}"


@dataclass(frozen=True)
class AndroidSettings:
    min_sdk: int = DEFAULT_MIN_SDK
    target_sdk: int = DEFAULT_TARGET_SDK
    compile_sdk: int = DEFAULT_COMPILE_SDK
    version_name: str = "1.0.0"
    version_code: int = 1
    keystores: tuple[KeystoreSpec, ...] = ()
    api_base_urls: dict[str, str] = field(default_factory=dict)
    web_socket_urls: dict[str, str] = field(default_factory=dict)
    deeplink_scheme: str = ""
    deeplink_host: str = ""


@dataclass(frozen=True)
class IosSettings:
    #: Empty means the package name with `_` turned into `-`, which is what a bundle id allows.
    bundle_id: str = ""
    deployment_target: str = "17.0"
    team_id: str = ""


@dataclass(frozen=True)
class Spec:
    app_name: str
    package_name: str
    platform: str = "android"
    #: The launcher and home-screen label. Any Unicode; empty means the app name.
    display_name: str = ""
    architecture: str = "compact"
    features: frozenset[str] = frozenset()
    feature_modules: tuple[str, ...] = ()
    design_style: str = DEFAULT_DESIGN_STYLE
    motion_style: str = DEFAULT_MOTION_STYLE
    #: The primary brand hex. Blank keeps the template's own palette.
    accent_colour: str = ""
    #: Blank means "work one out from the primary", not "keep the template's".
    secondary_colour: str = ""
    tertiary_colour: str = ""
    font_name: str = DEFAULT_FONT
    mono_font_name: str = DEFAULT_MONO_FONT
    #: The default for haptics. The user's own device setting still applies on top.
    haptics_enabled: bool = True
    #: Languages shipped besides English, as tags: "es", "pt-BR".
    languages: tuple[str, ...] = ()
    android: AndroidSettings | None = None
    ios: IosSettings | None = None

    @property
    def pascal_name(self) -> str:
        """`My Great App` → `MyGreatApp`. Class names and the Gradle root project use it."""
        parts = re.split(r"[^A-Za-z0-9]+", self.app_name)
        return "".join(part[:1].upper() + part[1:] for part in parts if part)

    @property
    def lower_name(self) -> str:
        """`My Great App` → `mygreatapp`. Deep-link schemes and other lowercase identifiers."""
        return self.pascal_name.lower()

    @property
    def snake_name(self) -> str:
        """
        `My Great App` → `my_great_app`. Notification channel ids and the Keystore alias use it,
        so it must never change for a name that already generated: an app regenerated with a
        different alias could no longer decrypt what it stored.
        """
        return re.sub(r"(?<!^)(?=[A-Z])", "_", self.pascal_name).lower()

    @property
    def package_path(self) -> str:
        return self.package_name.replace(".", "/")

    @property
    def label(self) -> str:
        """What the launcher shows under the icon."""
        return self.display_name or self.app_name

    def has(self, feature: str) -> bool:
        return feature in self.features

    def with_features(self, features: frozenset[str]) -> "Spec":
        return replace(self, features=features)


@dataclass(frozen=True)
class Vocabulary:
    """The names a spec may use. They come from the tokens and the template, not from here."""

    #: Each design style and the features it needs.
    design_styles: dict[str, tuple[str, ...]]
    motion_styles: tuple[str, ...]
    #: Module names the template already uses.
    reserved_module_names: frozenset[str]


@dataclass(frozen=True)
class Parsed:
    """A spec with everything `parse` learned on the way."""

    spec: Spec
    #: One line per change made to an older spec, for whoever wrote it.
    notes: tuple[str, ...]
    #: Fields a schema-1 spec carried that nothing reads any more.
    ignored_fields: tuple[str, ...]
    #: The features asked for, before `requires` added anything.
    requested: frozenset[str]


def upgrade(raw: object, platform: str) -> tuple[dict[str, Any], list[str], list[str], int]:
    """
    Any accepted spec shape → a schema-2 dict. Returns (data, notes, ignored fields, the schema the
    spec was written in). Feature keys are upgraded later, against the target's manifest.
    """
    if not isinstance(raw, dict):
        raise SpecError("The spec must be a JSON object.")
    data = {key: value for key, value in raw.items() if value is not None}
    data.pop(STAMP_KEY, None)

    schema = data.pop("schema", 1)
    if isinstance(schema, bool) or not isinstance(schema, int) or schema < 1:
        raise SpecError("`schema` must be 1 or 2.", "schema")
    if schema > SCHEMA:
        raise SpecError(
            f"This spec is schema {schema}, written by a newer generator. This one reads up to "
            f"schema {SCHEMA}.",
            "schema",
        )

    notes: list[str] = []
    ignored: list[str] = []
    if schema == 1:
        # Schema 1 had no `platform`; every spec was Android, and the API dropped unknown fields.
        known = set(_COMMON_FIELDS) | set(ANDROID_FIELDS)
        ignored = sorted(key for key in data if key not in known)
        for key in ignored:
            data.pop(key)
        data.setdefault("platform", "android")
        data.setdefault("architecture", "compact")
        moved = _move_android_fields(data)
        notes.append(
            f"Upgraded a schema-1 spec: {_join(moved)} now live under \"android\"."
            if moved
            else "Upgraded a schema-1 spec (it has no \"schema\" field)."
        )
    else:
        data.setdefault("platform", platform)
        if data["platform"] in ("android", "multiplatform"):
            moved = _move_android_fields(data)
            if moved:
                notes.append(f"Moved {_join(moved)} under \"android\".")

    return data, notes, ignored, schema


def apply_overrides(data: dict[str, Any], overrides: list[tuple[str, Any]]) -> None:
    """
    `--set key=value` pairs, applied to an upgraded spec. Dotted keys reach into the settings
    blocks (`android.min_sdk`); a flat Android key means the one under "android". A null value
    removes the field, as null means absent in a spec file.
    """
    for key, value in overrides:
        parts = key.split(".")
        if parts[0] == "schema":
            # The spec is already upgraded to the current schema when overrides apply, so a schema
            # given here would be accepted and then never read.
            raise SpecError("--set cannot change the schema: write it in the spec file.", key)
        if parts[0] in ANDROID_FIELDS:
            parts = ["android", *parts]
        holder = data
        for part in parts[:-1]:
            holder = holder.setdefault(part, {})
            if not isinstance(holder, dict):
                raise SpecError(f"Cannot set '{key}': '{part}' is not an object.", key)
        if value is None:
            holder.pop(parts[-1], None)
        else:
            holder[parts[-1]] = value


def _move_android_fields(data: dict[str, Any]) -> list[str]:
    flat = [key for key in ANDROID_FIELDS if key in data]
    if not flat:
        return []
    block = data.setdefault("android", {})
    if not isinstance(block, dict):
        raise SpecError("`android` must be an object.", "android")
    for key in flat:
        if key in block:
            raise SpecError(
                f"'{key}' is given both at the top level and under \"android\"; keep one.",
                f"android.{key}",
            )
        block[key] = data.pop(key)
    return flat


def _join(names: list[str]) -> str:
    quoted = [f"'{name}'" for name in names]
    return quoted[0] if len(quoted) == 1 else ", ".join(quoted[:-1]) + " and " + quoted[-1]


def parse(
    raw: object,
    manifest: "TemplateManifest",
    vocabulary: Vocabulary,
    overrides: list[tuple[str, Any]] | None = None,
) -> Parsed:
    """
    Raw JSON → a validated `Spec` for `manifest`'s target, with features upgraded and resolved.
    `overrides` are applied after the upgrade. Target rules are applied separately.
    """
    data, notes, ignored, schema = upgrade(raw, manifest.key)
    if overrides:
        apply_overrides(data, overrides)
    allowed = set(_COMMON_FIELDS) | {"android", "ios"}
    for key in data:
        if key not in allowed:
            raise SpecError(f"Unknown field '{key}'.", key)
    platform = _text(data, "platform", "platform")
    if platform not in PLATFORMS:
        raise SpecError(f"Unknown platform '{platform}'. Choose one of: {', '.join(PLATFORMS)}.", "platform")
    if platform != manifest.key:
        raise SpecError(
            f"This spec is for '{platform}', but it was given to the '{manifest.key}' generator.",
            "platform",
        )

    features_raw = _text_list(data, "features", "features")
    if len(features_raw) > MAX_FEATURES:
        raise SpecError(f"At most {MAX_FEATURES} features can be listed.", "features")
    upgraded, feature_notes, unknown = manifest.upgrade_keys(features_raw, schema)
    notes.extend(feature_notes)
    if unknown:
        raise SpecError(f"Unknown feature(s): {', '.join(sorted(unknown))}.", "features")

    spec = Spec(
        app_name=_text(data, "app_name", "app_name"),
        package_name=_text(data, "package_name", "package_name"),
        platform=platform,
        display_name=_text(data, "display_name", "display_name"),
        architecture=_text(data, "architecture", "architecture") or "compact",
        features=frozenset(upgraded),
        feature_modules=_text_list(data, "feature_modules", "feature_modules"),
        design_style=_text(data, "design_style", "design_style") or DEFAULT_DESIGN_STYLE,
        motion_style=_text(data, "motion_style", "motion_style") or DEFAULT_MOTION_STYLE,
        accent_colour=_text(data, "accent_colour", "accent_colour"),
        secondary_colour=_text(data, "secondary_colour", "secondary_colour"),
        tertiary_colour=_text(data, "tertiary_colour", "tertiary_colour"),
        font_name=_text(data, "font_name", "font_name") or DEFAULT_FONT,
        mono_font_name=_text(data, "mono_font_name", "mono_font_name") or DEFAULT_MONO_FONT,
        haptics_enabled=_flag(data, "haptics_enabled", "haptics_enabled", True),
        languages=_text_list(data, "languages", "languages"),
        android=_android(data.get("android")) if "android" in _SETTINGS_BLOCKS[platform] else None,
        ios=_ios(data.get("ios")) if "ios" in _SETTINGS_BLOCKS[platform] else None,
    )
    for block in ("android", "ios"):
        if block in data and block not in _SETTINGS_BLOCKS[platform]:
            raise SpecError(f"A '{platform}' spec has no \"{block}\" settings.", block)

    validate(spec, manifest, vocabulary)
    resolved = manifest.resolve(spec.features)
    return Parsed(
        spec=spec.with_features(resolved),
        notes=tuple(notes),
        ignored_fields=tuple(ignored),
        requested=frozenset(upgraded),
    )


def _android(raw: object) -> AndroidSettings:
    if raw is None:
        return AndroidSettings()
    data = _block(raw, "android", ANDROID_FIELDS)
    keystores = data.get("keystores", [])
    if not isinstance(keystores, list):
        raise SpecError("`android.keystores` must be a list.", "android.keystores")
    if len(keystores) > MAX_KEYSTORES:
        raise SpecError(f"At most {MAX_KEYSTORES} signing keys can be described.", "android.keystores")
    return AndroidSettings(
        min_sdk=_number(data, "min_sdk", "android.min_sdk", DEFAULT_MIN_SDK),
        target_sdk=_number(data, "target_sdk", "android.target_sdk", DEFAULT_TARGET_SDK),
        compile_sdk=_number(data, "compile_sdk", "android.compile_sdk", DEFAULT_COMPILE_SDK),
        version_name=_text(data, "version_name", "android.version_name") or "1.0.0",
        version_code=_number(data, "version_code", "android.version_code", 1),
        keystores=tuple(_keystore(entry) for entry in keystores),
        api_base_urls=_text_map(data, "api_base_urls", "android.api_base_urls"),
        web_socket_urls=_text_map(data, "web_socket_urls", "android.web_socket_urls"),
        deeplink_scheme=_text(data, "deeplink_scheme", "android.deeplink_scheme"),
        deeplink_host=_text(data, "deeplink_host", "android.deeplink_host"),
    )


def _ios(raw: object) -> IosSettings:
    if raw is None:
        return IosSettings()
    data = _block(raw, "ios", IOS_FIELDS)
    return IosSettings(
        bundle_id=_text(data, "bundle_id", "ios.bundle_id"),
        deployment_target=_text(data, "deployment_target", "ios.deployment_target") or "17.0",
        team_id=_text(data, "team_id", "ios.team_id"),
    )


def _keystore(raw: object) -> KeystoreSpec:
    data = _block(raw, "android.keystores", KEYSTORE_FIELDS)
    existing = _text(data, "existing_path", "android.keystores") or None
    return KeystoreSpec(
        name=_text(data, "name", "android.keystores"),
        alias=_text(data, "alias", "android.keystores"),
        store_password=_text(data, "store_password", "android.keystores"),
        key_password=_text(data, "key_password", "android.keystores"),
        existing_path=existing,
        common_name=_text(data, "common_name", "android.keystores") or "Unknown",
        organisation=_text(data, "organisation", "android.keystores") or "Unknown",
        country=_text(data, "country", "android.keystores") or "US",
        validity_days=_number(data, "validity_days", "android.keystores", 10_000),
    )


def _block(raw: object, name: str, fields: tuple[str, ...]) -> dict[str, Any]:
    if not isinstance(raw, dict):
        raise SpecError(f"`{name}` must be an object.", name)
    data = {key: value for key, value in raw.items() if value is not None}
    for key in data:
        if key not in fields:
            raise SpecError(f"Unknown field '{name}.{key}'.", f"{name}.{key}")
    return data


def _text(data: dict[str, Any], key: str, where: str) -> str:
    value = data.get(key, "")
    if not isinstance(value, str):
        raise SpecError(f"`{where}` must be a string.", where)
    return value


def _number(data: dict[str, Any], key: str, where: str, default: int) -> int:
    value = data.get(key, default)
    if isinstance(value, bool) or not isinstance(value, int):
        raise SpecError(f"`{where}` must be a whole number.", where)
    return value


def _flag(data: dict[str, Any], key: str, where: str, default: bool) -> bool:
    value = data.get(key, default)
    if not isinstance(value, bool):
        raise SpecError(f"`{where}` must be true or false.", where)
    return value


def _text_list(data: dict[str, Any], key: str, where: str) -> tuple[str, ...]:
    value = data.get(key, [])
    # A bare string is iterable, so "home" would become the modules h, o, m and e.
    if not isinstance(value, list) or not all(isinstance(item, str) for item in value):
        raise SpecError(f"`{where}` must be a list of strings.", where)
    return tuple(value)


def _text_map(data: dict[str, Any], key: str, where: str) -> dict[str, str]:
    value = data.get(key, {})
    if not isinstance(value, dict) or not all(isinstance(item, str) for item in value.values()):
        raise SpecError(f"`{where}` must be an object of strings.", where)
    return dict(value)


def validate(spec: Spec, manifest: "TemplateManifest", vocabulary: Vocabulary) -> None:
    validate_app_name(spec.app_name)
    validate_display_name(spec.display_name)
    validate_package_name(spec.package_name)

    if spec.architecture not in ARCHITECTURES:
        raise SpecError(
            f"Unknown architecture '{spec.architecture}'. Choose one of: {', '.join(ARCHITECTURES)}.",
            "architecture",
        )

    validate_feature_modules(spec.feature_modules, vocabulary.reserved_module_names)

    if len(spec.languages) > MAX_LANGUAGES:
        raise SpecError(f"At most {MAX_LANGUAGES} languages can be added.", "languages")
    if len(set(spec.languages)) != len(spec.languages):
        raise SpecError("Each language can only be listed once.", "languages")

    resolved = manifest.resolve(spec.features)
    design_styles = vocabulary.design_styles
    if spec.design_style not in design_styles:
        raise SpecError(
            f"Unknown design style '{spec.design_style}'. Choose one of: {', '.join(design_styles)}.",
            "design_style",
        )
    missing = [key for key in design_styles[spec.design_style] if key not in resolved]
    if missing:
        raise SpecError(
            f"The {spec.design_style} design style needs {_join(missing)}.", "design_style"
        )
    if spec.motion_style not in vocabulary.motion_styles:
        raise SpecError(
            f"Unknown motion style '{spec.motion_style}'. "
            f"Choose one of: {', '.join(vocabulary.motion_styles)}.",
            "motion_style",
        )

    for label, key in (("typeface", "font_name"), ("monospace typeface", "mono_font_name")):
        value = getattr(spec, key)
        if not _FONT.fullmatch(value):
            raise SpecError(
                f"The {label} '{value}' is not a Google Fonts family name: letters, digits, "
                "spaces and hyphens, as fonts.google.com spells it.",
                key,
            )

    for label, key in (
        ("accent", "accent_colour"),
        ("secondary", "secondary_colour"),
        ("tertiary", "tertiary_colour"),
    ):
        value = getattr(spec, key)
        if value and not _HEX.fullmatch(value):
            raise SpecError(f"The {label} colour must be a six-digit hex, like #2C6BED.", key)


def validate_app_name(name: str) -> None:
    if not name:
        raise SpecError("An app name is required.", "app_name")
    if not _APP_NAME.fullmatch(name):
        raise SpecError(
            f"The app name must start with a letter and be at most {MAX_APP_NAME} letters, "
            "digits, spaces, dots, hyphens or underscores. Put anything else in the display name.",
            "app_name",
        )


def validate_display_name(name: str) -> None:
    if len(name) > MAX_DISPLAY_NAME:
        raise SpecError(
            f"The display name can be at most {MAX_DISPLAY_NAME} characters: launchers cut it "
            "off well before that.",
            "display_name",
        )
    if not name.isprintable():
        raise SpecError("The display name cannot contain control characters.", "display_name")


def validate_package_name(package_name: str) -> None:
    if not package_name:
        raise SpecError("A package name is required, like com.example.myapp.", "package_name")
    if len(package_name) > MAX_PACKAGE:
        raise SpecError(f"The package name can be at most {MAX_PACKAGE} characters.", "package_name")
    segments = package_name.split(".")
    if len(segments) < 2:
        raise SpecError(
            "The package name needs at least two segments, like com.example.myapp: Play rejects "
            "a single-segment application id.",
            "package_name",
        )
    if len(segments) > MAX_PACKAGE_SEGMENTS:
        raise SpecError(
            f"The package name can have at most {MAX_PACKAGE_SEGMENTS} segments.", "package_name"
        )
    if segments[0] in FORBIDDEN_FIRST_SEGMENTS:
        raise SpecError(
            f"A package cannot start with '{segments[0]}.': the runtime refuses to load an app's "
            "classes from it.",
            "package_name",
        )
    for segment in segments:
        if not _SEGMENT.fullmatch(segment):
            raise SpecError(
                f"Package segment '{segment}' is invalid: lowercase letters, digits and "
                "underscores only, starting with a letter.",
                "package_name",
            )
        if len(segment) > MAX_SEGMENT:
            raise SpecError(
                f"Package segment '{segment}' is longer than {MAX_SEGMENT} characters.", "package_name"
            )
        if is_reserved(segment):
            raise SpecError(
                f"Package segment '{segment}' is a Kotlin or Java keyword, so the generated "
                "sources would not compile.",
                "package_name",
            )


def validate_module_name(name: str, reserved: frozenset[str]) -> None:
    if not _MODULE.fullmatch(name):
        raise SpecError(
            f"Module name '{name}' is invalid: lowercase letters and digits, words joined by "
            "single underscores, starting with a letter. It becomes a Gradle path, a package "
            "segment and a class name.",
            "feature_modules",
        )
    if len(name) > MAX_MODULE_NAME:
        raise SpecError(
            f"Module name '{name}' is longer than {MAX_MODULE_NAME} characters.", "feature_modules"
        )
    if is_reserved(name):
        raise SpecError(
            f"Module name '{name}' is a Kotlin or Java keyword, so its package would not compile.",
            "feature_modules",
        )
    if name in reserved:
        raise SpecError(
            f"Module name '{name}' is taken: the template already ships a module or directory "
            "called that. Pick another name.",
            "feature_modules",
        )


def validate_feature_modules(names: tuple[str, ...], reserved: frozenset[str]) -> None:
    if len(names) > MAX_FEATURE_MODULES:
        raise SpecError(
            f"At most {MAX_FEATURE_MODULES} feature modules can be scaffolded at once; add more "
            "later with add_feature.py.",
            "feature_modules",
        )
    for name in names:
        validate_module_name(name, reserved)
    if len(set(names)) != len(names):
        raise SpecError("Feature module names must be unique.", "feature_modules")
    seen: dict[str, str] = {}
    for name in names:
        pascal = pascal_case(name)
        if pascal in seen:
            raise SpecError(
                f"Modules '{seen[pascal]}' and '{name}' would both generate classes named "
                f"{pascal}…; rename one.",
                "feature_modules",
            )
        seen[pascal] = name


def is_reserved(word: str) -> bool:
    return word in KOTLIN_KEYWORDS or word in JAVA_RESERVED


def pascal_case(name: str) -> str:
    """`order_history` → `OrderHistory`."""
    return "".join(part.capitalize() for part in name.split("_") if part)


def module_title(name: str) -> str:
    """`order_history` → `Order history`. What a scaffolded screen shows in its top bar."""
    words = [part for part in name.split("_") if part]
    return " ".join(words).capitalize() if words else name


def to_json(spec: Spec, *, keystores: bool) -> dict[str, Any]:
    """The schema-2 JSON for `spec`. Keystores carry passwords, so callers choose."""
    data: dict[str, Any] = {
        "schema": SCHEMA,
        "platform": spec.platform,
        "app_name": spec.app_name,
        "display_name": spec.display_name,
        "package_name": spec.package_name,
        "architecture": spec.architecture,
        "features": sorted(spec.features),
        "feature_modules": list(spec.feature_modules),
        "design_style": spec.design_style,
        "motion_style": spec.motion_style,
        "accent_colour": spec.accent_colour,
        "secondary_colour": spec.secondary_colour,
        "tertiary_colour": spec.tertiary_colour,
        "font_name": spec.font_name,
        "mono_font_name": spec.mono_font_name,
        "haptics_enabled": spec.haptics_enabled,
        "languages": list(spec.languages),
    }
    if spec.android is not None:
        android = spec.android
        block: dict[str, Any] = {
            "min_sdk": android.min_sdk,
            "target_sdk": android.target_sdk,
            "compile_sdk": android.compile_sdk,
            "version_name": android.version_name,
            "version_code": android.version_code,
            "api_base_urls": dict(sorted(android.api_base_urls.items())),
            "web_socket_urls": dict(sorted(android.web_socket_urls.items())),
            "deeplink_scheme": android.deeplink_scheme,
            "deeplink_host": android.deeplink_host,
        }
        if keystores:
            block["keystores"] = [
                {key: getattr(keystore, key) for key in KEYSTORE_FIELDS if getattr(keystore, key) is not None}
                for keystore in android.keystores
            ]
        data["android"] = block
    if spec.ios is not None:
        data["ios"] = {key: getattr(spec.ios, key) for key in IOS_FIELDS}
    return data
