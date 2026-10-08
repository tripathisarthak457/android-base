"""
The languages a generated app ships in besides English, as each target's translation files say.

    generator/translations/<target>/<tag>/_language.json     {"name": "Deutsch"}
    generator/translations/<target>/<tag>/_scaffold.json     strings a scaffolded module gets
    generator/translations/<target>/<tag>/<module-slug>.json  name → text, one file per module

A module's slug is its path with `/` turned into `-`: `feature/settings` → `feature-settings`. One
file per module means a package adding a string to its own module never touches a line another
package edits. Writing the files into a project is the target's job.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from functools import cache
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / "translations"

#: The templates' own language, always shipped and never listed in a spec.
SOURCE_LANGUAGE = "en"


class TranslationError(ValueError):
    """A string in the generated project with no translation, which lint would refuse later."""


#: A string, or a plural as quantity → text.
Text = str | dict[str, str]


@dataclass(frozen=True)
class Translation:
    tag: str
    #: The language's name for itself, as the language picker shows it.
    name: str
    #: Module slug → string name → text.
    modules: dict[str, dict[str, Text]]
    #: Strings for a scaffolded module, with `{name}` standing for the module name.
    scaffold: dict[str, Text]


def module_slug(module: str) -> str:
    return module.replace("/", "-")


@cache
def available(target: str) -> dict[str, str]:
    """Every language with a translation for `target`, as tag → its own name for itself."""
    root = ROOT / target
    if not root.is_dir():
        return {}
    return {
        directory.name: _read(directory / "_language.json")["name"]
        for directory in sorted(root.iterdir())
        if (directory / "_language.json").is_file()
    }


@cache
def load(target: str, tag: str) -> Translation:
    directory = ROOT / target / tag
    modules = {
        path.stem: _read(path)
        for path in sorted(directory.glob("*.json"))
        if not path.name.startswith("_")
    }
    scaffold_path = directory / "_scaffold.json"
    return Translation(
        tag=tag,
        name=_read(directory / "_language.json")["name"],
        modules=modules,
        scaffold=_read(scaffold_path) if scaffold_path.is_file() else {},
    )


def _read(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))
