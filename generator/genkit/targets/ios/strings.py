"""
String Catalogs (`.xcstrings`) in an iOS project: one per package target and one for the app.

The template's catalogs hold the English keys only, which is what the code looks up. Generation
adds a translation of every key for every language the spec ships, from
`generator/translations/ios/<tag>/<catalog>.json`, where a catalog is named for its target
(`SampleFeature`), its target and table (`SettingsFeature.Language`), or `app`. A key left without
a translation would show English in the middle of a translated screen, so a gap is an error.
"""

from __future__ import annotations

import json
from collections.abc import Iterable
from pathlib import Path

from ...core import translations
from ...core.spec import Spec
from ...core.translations import TranslationError

SOURCE_LANGUAGE = "en"

#: The catalog id of the app target's own strings.
APP_CATALOG = "app"


def render(keys: Iterable[str], localizations: dict[str, dict[str, str]] | None = None) -> str:
    """
    A String Catalog holding `keys`, each translated into the languages of `localizations`
    (language → key → text) that have it. Formatted as Xcode writes catalogs, so opening the
    project does not rewrite them.
    """
    strings: dict[str, dict] = {}
    for key in keys:
        units = {
            tag: {"stringUnit": {"state": "translated", "value": texts[key]}}
            for tag, texts in sorted((localizations or {}).items())
            if key in texts
        }
        strings[key] = {"localizations": units} if units else {}
    catalog = {"sourceLanguage": SOURCE_LANGUAGE, "strings": strings, "version": "1.0"}
    return json.dumps(catalog, ensure_ascii=False, indent=2, separators=(",", " : "), sort_keys=True) + "\n"


def keys(catalog: Path) -> list[str]:
    """The keys a catalog holds."""
    return list(json.loads(catalog.read_text(encoding="utf-8"))["strings"])


def catalog_id(project: Path, catalog: Path) -> str:
    """`app`, a package target's name, or `Target.Table` for a table other than `Localizable`."""
    relative = catalog.relative_to(project)
    if relative.parts[0] != "Packages":
        return APP_CATALOG
    target = relative.parent.parent.name
    return target if catalog.stem == "Localizable" else f"{target}.{catalog.stem}"


def translate(project: Path, spec: Spec, user_names: dict[str, set[str]], scaffolded: frozenset[str]) -> None:
    """
    Adds every shipped language to every catalog in `project`.

    `user_names` maps a catalog id to its keys that are names the user typed, such as a scaffolded
    module's title: they read as typed in every language until someone translates them. The
    catalogs in `scaffolded` take their other translations from each language's `_scaffold.json`.
    """
    if not spec.languages:
        return
    loaded = {tag: translations.load("ios", tag) for tag in spec.languages}
    for catalog in sorted(project.rglob("*.xcstrings")):
        identity = catalog_id(project, catalog)
        names = user_names.get(identity, set())
        all_keys = keys(catalog)
        wanted = [key for key in all_keys if key not in names]
        localizations: dict[str, dict[str, str]] = {}
        for tag, translation in loaded.items():
            if identity in scaffolded:
                texts, source = translation.scaffold, "_scaffold.json"
            else:
                texts, source = translation.modules.get(identity, {}), f"{identity}.json"
            missing = [key for key in wanted if key not in texts]
            if missing:
                raise TranslationError(
                    f"generator/translations/ios/{tag}/{source} has no translation for "
                    f"{', '.join(repr(key) for key in missing)} ({catalog.relative_to(project)})."
                )
            localizations[tag] = {key: texts[key] for key in wanted}
        catalog.write_text(render(all_keys, localizations), encoding="utf-8", newline="\n")
