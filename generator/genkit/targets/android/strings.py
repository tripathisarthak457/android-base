"""
Translations written into an Android project: `values-<qualifier>/strings.xml` beside every module's
`values/strings.xml`, holding exactly the strings that survived into the project. Lint fails a
build that ships a language with a string missing, so a gap here is an error, not a warning.
"""

from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

from ...core import translations
from ...core.spec import Spec, module_title
from ...core.translations import Text, TranslationError, module_slug
from . import scaffold

_RES = "src/main/res"


def resource_qualifier(tag: str) -> str:
    """`pt-BR` → `pt-rBR`, the folder suffix Android expects for a language with a region."""
    language, _, region = tag.partition("-")
    return f"{language}-r{region}" if region else language


def translatable_names(strings_xml: Path) -> list[str]:
    """The strings and plurals a translation has to provide."""
    root = ET.parse(strings_xml).getroot()
    return [
        element.get("name")
        for element in root
        if element.tag in ("string", "plurals") and element.get("translatable") != "false"
    ]


def write(project: Path, spec: Spec, modules: set[str] | None = None) -> None:
    """Every module's translations, or only those of `modules` (paths like `feature/orders`)."""
    if not spec.languages:
        return
    sources = [
        (source, translatable_names(source))
        for source in sorted(project.glob(f"**/{_RES}/values/strings.xml"))
        if modules is None or _module(project, source) in modules
    ]
    for tag in spec.languages:
        translation = translations.load("android", tag)
        entries: dict[str, dict[str, Text]] = dict(translation.modules)
        for name in scaffold.modules(spec):
            texts = {key.replace("{name}", name): value for key, value in translation.scaffold.items()}
            # The feature's name as the user typed it, which is also its tab label: a name, so it
            # reads the same in every language until someone chooses otherwise.
            texts[f"{name}_title"] = module_title(name)
            entries[module_slug(f"feature/{name}")] = texts

        for source, names in sources:
            if not names:
                continue
            module = _module(project, source)
            texts = entries.get(module_slug(module), {})
            missing = [name for name in names if name not in texts]
            if missing:
                raise TranslationError(
                    f"No {tag} translation for {', '.join(missing)} in {module}. Add them to "
                    f"generator/translations/android/{tag}/{module_slug(module)}.json."
                )
            target = source.parent.parent / f"values-{resource_qualifier(tag)}" / "strings.xml"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(strings_xml(names, texts), encoding="utf-8")


def _module(project: Path, strings: Path) -> str:
    return strings.parent.parent.relative_to(project).as_posix().removesuffix(f"/{_RES}")


def strings_xml(names: list[str], texts: dict[str, Text]) -> str:
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for name in names:
        value = texts[name]
        if isinstance(value, dict):
            lines.append(f'    <plurals name="{name}">')
            lines.extend(
                f'        <item quantity="{quantity}">{escape(text)}</item>' for quantity, text in value.items()
            )
            lines.append("    </plurals>")
        else:
            lines.append(f'    <string name="{name}">{escape(value)}</string>')
    lines.append("</resources>")
    return "\n".join(lines) + "\n"


def escape(text: str) -> str:
    """XML first, then aapt's own rules: quotes are escaped, and a leading @ or ? is literal."""
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    if text[:1] in ("@", "?"):
        text = "\\" + text
    return text
