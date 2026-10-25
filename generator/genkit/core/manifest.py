"""
A template's description of itself: `template.toml`, one `features/<key>.toml` per optional feature,
one `options/<key>.toml` per spec-level option, `presets.toml` and `removed.toml`, all beside the
template's own files.

Everything here is checked when it is loaded, so a typo in a feature file fails the generator's
tests rather than a user's generation.
"""

from __future__ import annotations

import re
import tomllib
from dataclasses import dataclass
from pathlib import Path
from typing import TYPE_CHECKING, Any

from .markers import KEY_PATTERN, TemplateError
from .spec import COMMON_FIELDS

if TYPE_CHECKING:
    from .spec import Spec

STATUSES: tuple[str, ...] = ("available", "beta", "coming_soon", "hidden")
BOUNDARIES: tuple[str, ...] = ("dotted", "path", "prefix", "snake")

_FEATURE_FIELDS = {
    "key", "title", "headline", "description", "group", "default", "requires", "implied_only",
    "owns", "legacy", "licence", "platforms",
}
_PRESET_FIELDS = {"title", "description", "features", "all"}
_OPTION_FIELDS = {
    "key", "title", "label", "headline", "description", "default", "variant", "owns", "disables", "hides", "locked",
    "feature_copy", "preset_copy",
}
_VARIANT_NAME = re.compile(r"[a-z][a-z0-9_-]*")


@dataclass(frozen=True)
class IdentityToken:
    """One piece of the template's own identity, and what replaces it in a project."""

    source: str
    #: A format string over the spec's names: `{package}`, `{package_path}`, `{pascal}`, …
    replacement: str
    boundary: str


@dataclass(frozen=True)
class LegacyKey:
    """An older feature key that now means this feature."""

    key: str
    #: Only when upgrading a spec of this schema. None: in any spec.
    schema: int | None = None


@dataclass(frozen=True)
class Licence:
    name: str
    spdx: str
    url: str


@dataclass(frozen=True)
class Feature:
    key: str
    title: str
    headline: str
    description: str
    group: str
    default: bool
    requires: tuple[str, ...]
    #: Hidden from the wizard and the website; enabled only through another feature's `requires`.
    implied_only: bool
    #: Template paths removed when the feature is off. `{pkg_path}` is the template's package path.
    owns: tuple[str, ...]
    legacy: tuple[LegacyKey, ...]
    licence: Licence | None
    #: Targets whose website card shows this feature. Empty: the manifest's own target.
    platforms: tuple[str, ...]


@dataclass(frozen=True)
class FeatureCopy:
    """What a feature's card and question say about it."""

    headline: str
    description: str


@dataclass(frozen=True)
class OptionLock:
    """A deployment that serves only one value of an option, and what it tells whoever asks for the other."""

    value: bool
    note: str


@dataclass(frozen=True)
class Option:
    """
    A spec-level switch with a default, such as leaving the UI components out. The marker engine,
    `owns` and the variants treat it as one more key, on unless the spec turns it off.
    """

    key: str
    title: str
    #: The checkbox the wizard and the website show for it, ticked for the value that is not the default.
    label: str
    headline: str
    description: str
    default: bool
    #: `generator/variants/<variant>/` replaces and adds files when the option is off. None: no variant.
    variant: str | None
    #: Template paths removed when the option is off. `{pkg_path}` is the template's package path.
    owns: tuple[str, ...]
    #: Feature → why it cannot be had while the option is off, in catalogue order.
    disables: dict[str, str]
    #: Spec field → why the website does not ask for it while the option is off.
    hides: dict[str, str]
    locked: OptionLock | None
    #: Feature → how it reads while the option is off, for a kept feature whose own copy names what the
    #: project then lacks; in catalogue order.
    feature_copy: dict[str, FeatureCopy]
    #: Preset → its description while the option is off, likewise.
    preset_copy: dict[str, str]


@dataclass(frozen=True)
class Preset:
    key: str
    title: str
    description: str
    features: tuple[str, ...]
    #: Every feature that is not implied-only, so new features join the preset automatically.
    all: bool


class TemplateManifest:
    """Everything one target's template says about itself."""

    def __init__(self, root: Path, target: dict[str, Any], data: dict[str, Any]):
        self.root = root
        self.key: str = target["key"]
        self.title: str = target["title"]
        self.status: str = target["status"]
        self.status_note: str = target.get("status_note", "")
        self.summary: str = target["summary"]
        self.identity: tuple[IdentityToken, ...] = data["identity"]
        self.required_regions: tuple[str, ...] = data["regions"]
        self.template_only: tuple[str, ...] = data["template_only"]
        self.binary_suffixes: frozenset[str] = data["binary_suffixes"]
        self.executable: tuple[str, ...] = data["executable"]
        #: Paths copied byte for byte: no renaming, no markers. Upstream files the project vendors.
        self.verbatim: tuple[str, ...] = data["verbatim"]
        self.syntax: dict[str, str] = data["syntax"]
        self.groups: tuple[str, ...] = data["groups"]
        self.features: dict[str, Feature] = data["features"]
        self.options: dict[str, Option] = data["options"]
        self.presets: dict[str, Preset] = data["presets"]
        self.default_preset: str = data["default_preset"]
        self.removed: dict[str, str] = data["removed"]

    @classmethod
    def load(cls, root: Path) -> "TemplateManifest":
        source = root / "template.toml"
        document = _read_toml(source)
        _expect_tables(source, document, {"target", "identity", "regions", "files", "groups"})

        target = document["target"]
        _expect_keys(source, "[target]", target, {"key", "title", "status", "summary"}, {"status_note"})
        if target["status"] not in STATUSES:
            raise TemplateError(source, None, f"[target] status must be one of {', '.join(STATUSES)}.")
        if target["status"] == "beta" and not target.get("status_note"):
            raise TemplateError(source, None, "A beta target needs a status_note saying why.")

        identity = tuple(_identity(source, entry) for entry in document["identity"].get("tokens", []))
        files = document["files"]
        _expect_keys(
            source, "[files]", files, set(), {"template_only", "binary_suffixes", "executable", "verbatim", "syntax"}
        )
        groups = tuple(document["groups"].get("order", []))
        if not groups:
            raise TemplateError(source, None, "[groups] order lists no groups.")

        path_token = next((token.source for token in identity if token.boundary == "path"), "")
        features = _load_features(root, groups, path_token)
        presets, default_preset = _load_presets(root / "presets.toml", features)
        options = _load_options(root, features, presets, path_token)
        removed = _load_removed(root / "removed.toml", features)

        return cls(
            root,
            target,
            {
                "identity": identity,
                "regions": tuple(document["regions"].get("required", [])),
                "template_only": tuple(files.get("template_only", [])),
                "binary_suffixes": frozenset(suffix.lower() for suffix in files.get("binary_suffixes", [])),
                "executable": tuple(files.get("executable", [])),
                "verbatim": tuple(files.get("verbatim", [])),
                "syntax": dict(files.get("syntax", {})),
                "groups": groups,
                "features": features,
                "options": options,
                "presets": presets,
                "default_preset": default_preset,
                "removed": removed,
            },
        )

    @property
    def package_path_token(self) -> str:
        return next(token.source for token in self.identity if token.boundary == "path")

    @property
    def selectable(self) -> list[Feature]:
        return [feature for feature in self.features.values() if not feature.implied_only]

    def resolve(self, selected: frozenset[str] | set[str]) -> frozenset[str]:
        """`selected` plus everything it requires, transitively."""
        resolved = set(selected)
        pending = list(selected)
        while pending:
            for required in self.features[pending.pop()].requires:
                if required not in resolved:
                    resolved.add(required)
                    pending.append(required)
        return frozenset(resolved)

    def preset_features(self, key: str) -> frozenset[str]:
        preset = self.presets[key]
        chosen = {feature.key for feature in self.selectable} if preset.all else set(preset.features)
        return self.resolve(chosen)

    @property
    def marker_keys(self) -> frozenset[str]:
        """Every key a marker may name: the features and the options."""
        return frozenset(self.features) | frozenset(self.options)

    def enabled_keys(self, spec: "Spec") -> frozenset[str]:
        """The marker keys that hold for `spec`: its features, and every option it leaves on."""
        return spec.features | {key for key in self.options if spec.option(key)}

    def option_values(self, given: dict[str, bool]) -> dict[str, bool]:
        """Every option's value: the one in `given`, else its default."""
        return {key: given.get(key, option.default) for key, option in self.options.items()}

    def disabled_features(self, values: dict[str, bool]) -> dict[str, str]:
        """Feature → why it is left out, for every option `values` turns off, in catalogue order."""
        return {
            feature: reason
            for key, option in self.options.items()
            if not values[key]
            for feature, reason in option.disables.items()
        }

    def feature_copy(self, feature: Feature, values: dict[str, bool]) -> FeatureCopy:
        """How `feature` reads for a project with these option values: an option that is off has the last word."""
        for key, option in self.options.items():
            if not values[key] and feature.key in option.feature_copy:
                return option.feature_copy[feature.key]
        return FeatureCopy(feature.headline, feature.description)

    def preset_description(self, preset: Preset, values: dict[str, bool]) -> str:
        """How `preset` reads for a project with these option values, likewise."""
        for key, option in self.options.items():
            if not values[key] and preset.key in option.preset_copy:
                return option.preset_copy[preset.key]
        return preset.description

    def owned_paths(self, enabled: frozenset[str]) -> list[str]:
        """Template-relative paths that belong to features and options which are off."""
        token = self.package_path_token
        owners: list[Feature | Option] = [*self.features.values(), *self.options.values()]
        return sorted(
            path.format(pkg_path=token)
            for owner in owners
            if owner.key not in enabled
            for path in owner.owns
        )

    def upgrade_keys(self, keys: tuple[str, ...], schema: int) -> tuple[list[str], list[str], list[str]]:
        """
        Feature keys from a spec of `schema` → today's keys. Returns (keys, a note per change,
        keys nobody knows). A key that maps to another is followed again, so a chain of renames
        across releases still lands on today's key.
        """
        result: list[str] = []
        notes: list[str] = []
        unknown: list[str] = []
        pending = list(keys)
        seen: set[str] = set()
        while pending:
            key = pending.pop(0)
            if key in seen:
                continue
            seen.add(key)

            versioned = self._legacy_targets(LegacyKey(key, schema))
            if versioned:
                notes.append(self._renamed(key, versioned))
                pending.extend(versioned)
            elif key in self.features:
                result.append(key)
            elif plain := self._legacy_targets(LegacyKey(key)):
                notes.append(self._renamed(key, plain))
                pending.extend(plain)
            elif key in self.removed:
                notes.append(self.removed[key])
            else:
                unknown.append(key)
        return result, notes, unknown

    def _legacy_targets(self, legacy: LegacyKey) -> list[str]:
        return [feature.key for feature in self.features.values() if legacy in feature.legacy]

    def _renamed(self, old: str, new: list[str]) -> str:
        quoted = [f"'{key}'" for key in new]
        joined = quoted[0] if len(quoted) == 1 else ", ".join(quoted[:-1]) + " and " + quoted[-1]
        # Several old keys folded into one feature: each of them is now only part of it.
        merged = len(new) == 1 and len(self.features[new[0]].legacy) > 1
        return f"'{old}' is now {'part of ' if merged else ''}{joined}."


def _read_toml(path: Path) -> dict[str, Any]:
    try:
        return tomllib.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        raise TemplateError(path, None, "missing") from None
    except tomllib.TOMLDecodeError as error:
        raise TemplateError(path, None, f"not valid TOML: {error}") from None


def _expect_tables(path: Path, document: dict[str, Any], tables: set[str]) -> None:
    missing = sorted(tables - set(document))
    if missing:
        raise TemplateError(path, None, f"missing [{'], ['.join(missing)}]")
    extra = sorted(set(document) - tables)
    if extra:
        raise TemplateError(path, None, f"unknown table(s): {', '.join(extra)}")


def _expect_keys(path: Path, where: str, table: dict[str, Any], required: set[str], optional: set[str]) -> None:
    missing = sorted(required - set(table))
    if missing:
        raise TemplateError(path, None, f"{where} is missing {', '.join(missing)}")
    extra = sorted(set(table) - required - optional)
    if extra:
        raise TemplateError(path, None, f"{where} has unknown key(s): {', '.join(extra)}")


def _identity(path: Path, entry: dict[str, Any]) -> IdentityToken:
    _expect_keys(path, "an [identity] token", entry, {"from", "to", "boundary"}, set())
    if entry["boundary"] not in BOUNDARIES:
        raise TemplateError(
            path, None, f"identity token '{entry['from']}' has an unknown boundary '{entry['boundary']}'"
        )
    return IdentityToken(entry["from"], entry["to"], entry["boundary"])


def _load_features(root: Path, groups: tuple[str, ...], path_token: str) -> dict[str, Feature]:
    loaded: list[Feature] = []
    for source in sorted((root / "features").glob("*.toml")):
        data = _read_toml(source)
        _expect_keys(
            source, "a feature", data, {"key", "title", "headline", "description", "group", "default"},
            _FEATURE_FIELDS,
        )
        if data["key"] != source.stem:
            raise TemplateError(source, None, f"key '{data['key']}' does not match the file name")
        if data["group"] not in groups:
            raise TemplateError(source, None, f"group '{data['group']}' is not in template.toml [groups]")
        licence = data.get("licence")
        loaded.append(
            Feature(
                key=data["key"],
                title=data["title"],
                headline=data["headline"],
                description=" ".join(data["description"].split()),
                group=data["group"],
                default=bool(data["default"]),
                requires=tuple(data.get("requires", [])),
                implied_only=bool(data.get("implied_only", False)),
                owns=tuple(data.get("owns", [])),
                legacy=tuple(_legacy(source, entry) for entry in data.get("legacy", [])),
                licence=Licence(licence["name"], licence["spdx"], licence["url"]) if licence else None,
                platforms=tuple(data.get("platforms", [])),
            )
        )

    features = {
        feature.key: feature
        for feature in sorted(loaded, key=lambda feature: (groups.index(feature.group), feature.key))
    }
    for feature in features.values():
        source = root / "features" / f"{feature.key}.toml"
        for required in feature.requires:
            if required not in features:
                raise TemplateError(source, None, f"requires unknown feature '{required}'")
        for legacy in feature.legacy:
            if legacy.key == feature.key:
                raise TemplateError(source, None, "lists its own key as a legacy key")
            if legacy.schema is None and legacy.key in features:
                raise TemplateError(
                    source, None,
                    f"legacy key '{legacy.key}' is a current feature; give it a schema to say when it applies",
                )
        for owned in feature.owns:
            if not (root / owned.format(pkg_path=path_token)).exists():
                raise TemplateError(source, None, f"owns '{owned}', which is not in the template")
    _check_acyclic(root, features)
    return features


def _load_options(
    root: Path, features: dict[str, Feature], presets: dict[str, Preset], path_token: str
) -> dict[str, Option]:
    options: dict[str, Option] = {}
    for source in sorted((root / "options").glob("*.toml")):
        data = _read_toml(source)
        _expect_keys(
            source, "an option", data, {"key", "title", "label", "headline", "description", "default"}, _OPTION_FIELDS
        )
        key = data["key"]
        if key != source.stem:
            raise TemplateError(source, None, f"key '{key}' does not match the file name")
        # The key is the spec field and the marker atom at once, so it has to be a valid marker key.
        if not isinstance(key, str) or not re.fullmatch(KEY_PATTERN, key):
            raise TemplateError(source, None, f"key '{key}' cannot be named in a marker")
        if key in features:
            raise TemplateError(source, None, f"key '{key}' is also a feature")
        if key in COMMON_FIELDS:
            raise TemplateError(source, None, f"key '{key}' is already a spec field")
        if not isinstance(data["default"], bool):
            raise TemplateError(source, None, "default must be true or false")
        variant = data.get("variant")
        if variant is not None and not (isinstance(variant, str) and _VARIANT_NAME.fullmatch(variant)):
            raise TemplateError(source, None, f"variant {variant!r} is not a directory name")

        disables = _reasons(source, data, "disables")
        for feature in disables:
            if feature not in features or features[feature].implied_only:
                raise TemplateError(source, None, f"[disables] names '{feature}', which is not a selectable feature")
        for feature in features.values():
            if feature.key in disables:
                continue
            # `resolve` would bring a disabled feature straight back through this one.
            brought = sorted(_requirements(features, feature.key) & set(disables))
            if brought:
                raise TemplateError(
                    source, None, f"'{feature.key}' requires {', '.join(brought)}, so it belongs in [disables] too"
                )
        hides = _reasons(source, data, "hides")
        for field in hides:
            if field not in COMMON_FIELDS:
                raise TemplateError(source, None, f"[hides] names '{field}', which is not a spec field")

        owns = tuple(data.get("owns", []))
        for owned in owns:
            if not (root / owned.format(pkg_path=path_token)).exists():
                raise TemplateError(source, None, f"owns '{owned}', which is not in the template")

        feature_copy = _feature_copy(source, data.get("feature_copy", {}), features, disables)
        preset_copy = _preset_copy(source, data.get("preset_copy", {}), presets)

        options[key] = Option(
            key=key,
            title=data["title"],
            label=data["label"],
            headline=data["headline"],
            description=" ".join(data["description"].split()),
            default=data["default"],
            variant=variant,
            owns=owns,
            disables=disables,
            hides=hides,
            locked=_lock(source, data.get("locked")),
            feature_copy=feature_copy,
            preset_copy=preset_copy,
        )
    return options


def _feature_copy(
    source: Path, raw: object, features: dict[str, Feature], disables: dict[str, str]
) -> dict[str, FeatureCopy]:
    """`[feature_copy.<feature>]` tables, in catalogue order; a missing headline keeps the feature's own."""
    if not isinstance(raw, dict):
        raise TemplateError(source, None, "[feature_copy] must hold a table per feature")
    for name, table in raw.items():
        where = f"[feature_copy.{name}]"
        if name not in features or features[name].implied_only:
            raise TemplateError(source, None, f"{where} names a feature nobody can choose")
        if name in disables:
            raise TemplateError(source, None, f"{where} describes a feature [disables] leaves out")
        if not isinstance(table, dict):
            raise TemplateError(source, None, f"{where} must be a table")
        _expect_keys(source, where, table, {"description"}, {"headline", "description"})
        if not all(isinstance(value, str) and value.strip() for value in table.values()):
            raise TemplateError(source, None, f"{where} needs words in its headline and description")
    return {
        name: FeatureCopy(
            " ".join(raw[name].get("headline", feature.headline).split()), " ".join(raw[name]["description"].split())
        )
        for name, feature in features.items()
        if name in raw
    }


def _preset_copy(source: Path, raw: object, presets: dict[str, Preset]) -> dict[str, str]:
    """`[preset_copy]`: a description per preset, in the presets' order."""
    if not isinstance(raw, dict) or not all(isinstance(text, str) and text.strip() for text in raw.values()):
        raise TemplateError(source, None, "[preset_copy] must give each preset a description")
    for name in raw:
        if name not in presets:
            raise TemplateError(source, None, f"[preset_copy] names '{name}', which is not a preset")
    return {name: " ".join(raw[name].split()) for name in presets if name in raw}


def _reasons(source: Path, data: dict[str, Any], table: str) -> dict[str, str]:
    reasons = data.get(table, {})
    if not isinstance(reasons, dict) or not all(isinstance(reason, str) and reason.strip() for reason in reasons.values()):
        raise TemplateError(source, None, f"[{table}] must give each name a one-line reason")
    return {name: " ".join(reason.split()) for name, reason in reasons.items()}


def _lock(source: Path, raw: object) -> OptionLock | None:
    if raw is None:
        return None
    if not isinstance(raw, dict):
        raise TemplateError(source, None, "[locked] must be a table")
    _expect_keys(source, "[locked]", raw, {"value", "note"}, set())
    if not isinstance(raw["value"], bool):
        raise TemplateError(source, None, "[locked] value must be true or false")
    if not isinstance(raw["note"], str) or not raw["note"].strip():
        raise TemplateError(source, None, "[locked] needs a note saying why")
    return OptionLock(raw["value"], " ".join(raw["note"].split()))


def _requirements(features: dict[str, Feature], key: str) -> set[str]:
    """What `key` requires, transitively."""
    found: set[str] = set()
    pending = [key]
    while pending:
        for required in features[pending.pop()].requires:
            if required not in found:
                found.add(required)
                pending.append(required)
    return found


def _legacy(source: Path, entry: object) -> LegacyKey:
    if isinstance(entry, str):
        return LegacyKey(entry)
    if isinstance(entry, dict) and set(entry) == {"key", "schema"}:
        return LegacyKey(entry["key"], int(entry["schema"]))
    raise TemplateError(source, None, f"a legacy entry must be a key or {{ key, schema }}: {entry!r}")


def _check_acyclic(root: Path, features: dict[str, Feature]) -> None:
    state: dict[str, int] = {}

    def visit(key: str, trail: list[str]) -> None:
        if state.get(key) == 2:
            return
        if state.get(key) == 1:
            cycle = " → ".join(trail[trail.index(key):] + [key])
            raise TemplateError(root / "features" / f"{key}.toml", None, f"requires form a cycle: {cycle}")
        state[key] = 1
        for required in features[key].requires:
            visit(required, trail + [key])
        state[key] = 2

    for key in features:
        visit(key, [])


def _load_presets(path: Path, features: dict[str, Feature]) -> tuple[dict[str, Preset], str]:
    document = _read_toml(path)
    default = document.pop("default", None)
    presets: dict[str, Preset] = {}
    for key, table in document.items():
        if not isinstance(table, dict):
            raise TemplateError(path, None, f"'{key}' must be a table")
        _expect_keys(path, f"[{key}]", table, {"title", "description"}, _PRESET_FIELDS)
        listed = tuple(table.get("features", []))
        everything = bool(table.get("all", False))
        if everything == bool(listed):
            raise TemplateError(path, None, f"[{key}] needs either features or all = true, not both")
        for feature in listed:
            if feature not in features:
                raise TemplateError(path, None, f"[{key}] lists unknown feature '{feature}'")
        presets[key] = Preset(key, table["title"], " ".join(table["description"].split()), listed, everything)
    if default not in presets:
        raise TemplateError(path, None, f"default = '{default}' is not a preset")
    return presets, default


def _load_removed(path: Path, features: dict[str, Feature]) -> dict[str, str]:
    removed = _read_toml(path) if path.exists() else {}
    for key, note in removed.items():
        if not isinstance(note, str):
            raise TemplateError(path, None, f"'{key}' must map to the note an old spec receives")
        if key in features:
            raise TemplateError(path, None, f"'{key}' is both removed and a current feature")
        # A legacy key without a schema always maps first, so its removal note could never be read.
        if any(legacy == LegacyKey(key) for feature in features.values() for legacy in feature.legacy):
            raise TemplateError(path, None, f"'{key}' is both removed and a legacy key without a schema")
    return dict(removed)
