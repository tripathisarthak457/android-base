"""
The languages a generated app ships in besides English.

Each file under translations/ covers every translatable string in the template, keyed by module,
plus the strings the scaffolder writes into a new feature module. Only strings that survive into
the generated project are written, so a feature that is off leaves nothing behind. Lint fails a
build that ships a language with a string missing, and the generator's tests fail first.
"""

from __future__ import annotations

import json
import re
import xml.etree.ElementTree as ET
from functools import cache
from pathlib import Path

from . import scaffold
from .spec import ProjectSpec

TRANSLATIONS_DIR = Path(__file__).resolve().parent.parent / "translations"

#: The template's own language, which is always shipped and never listed in a spec.
SOURCE_LANGUAGE = "en"


class TranslationError(ValueError):
    """A string in the generated project with no translation, which lint would refuse later."""


@cache
def available() -> dict[str, str]:
    """Every language there is a translation for, as tag → its own name for itself."""
    return {
        path.stem: json.loads(path.read_text(encoding="utf-8"))["language"]
        for path in sorted(TRANSLATIONS_DIR.glob("*.json"))
    }


@cache
def load(tag: str) -> dict:
    return json.loads((TRANSLATIONS_DIR / f"{tag}.json").read_text(encoding="utf-8"))


def resource_qualifier(tag: str) -> str:
    """`pt-BR` → `pt-rBR`, the folder suffix Android expects for a language with a region."""
    language, _, region = tag.partition("-")
    return f"{language}-r{region}" if region else language


def translatable_names(strings_xml: Path) -> list[str]:
    """The strings and plurals in a values/strings.xml that a translation has to provide."""
    root = ET.parse(strings_xml).getroot()
    return [
        element.get("name")
        for element in root
        if element.tag in ("string", "plurals") and element.get("translatable") != "false"
    ]


def apply(destination: Path, spec: ProjectSpec) -> None:
    """Writes values-<lang>/strings.xml beside every values/strings.xml, and lists the languages."""
    if not spec.languages:
        return
    for tag in spec.languages:
        translation = load(tag)
        entries: dict[str, dict] = dict(translation["strings"])
        for name in spec.feature_modules:
            texts = {key.replace("{name}", name): value for key, value in translation["scaffold"].items()}
            # The feature's name as the user typed it, which is also its tab label: a name, so it
            # reads the same in every language until someone chooses otherwise.
            texts[f"{name}_title"] = scaffold.title(name)
            entries[f"feature/{name}/src/main/res"] = texts
        for module, texts in entries.items():
            source = destination / module / "values" / "strings.xml"
            if not source.is_file():
                continue
            names = translatable_names(source)
            missing = [name for name in names if name not in texts]
            if missing:
                raise TranslationError(
                    f"No {tag} translation for {', '.join(missing)} in {module}. Add them to "
                    f"generator/translations/{tag}.json."
                )
            target = destination / module / f"values-{resource_qualifier(tag)}" / "strings.xml"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(_strings_xml(names, texts), encoding="utf-8")
    _list_languages(destination, spec)


def _list_languages(destination: Path, spec: ProjectSpec) -> None:
    """Puts the languages in AppLocales.supported, which is what the language picker offers."""
    for path in (destination / "core/ui/src/main/kotlin").rglob("AppLocales.kt"):
        tags = ", ".join(f'"{tag}"' for tag in (SOURCE_LANGUAGE, *spec.languages))
        text = path.read_text(encoding="utf-8")
        updated = re.sub(
            r"(val supported: List<String> = listOf\()[^)]*(\))",
            lambda m: m.group(1) + tags + m.group(2),
            text,
            count=1,
        )
        path.write_text(updated, encoding="utf-8")


def _strings_xml(names: list[str], texts: dict) -> str:
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for name in names:
        value = texts[name]
        if isinstance(value, dict):
            lines.append(f'    <plurals name="{name}">')
            lines.extend(
                f'        <item quantity="{quantity}">{_escape(text)}</item>'
                for quantity, text in value.items()
            )
            lines.append("    </plurals>")
        else:
            lines.append(f'    <string name="{name}">{_escape(value)}</string>')
    lines.append("</resources>")
    return "\n".join(lines) + "\n"


def _escape(text: str) -> str:
    """XML first, then aapt's own rules: quotes are escaped, and a leading @ or ? is literal."""
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    if text[:1] in ("@", "?"):
        text = "\\" + text
    return text
