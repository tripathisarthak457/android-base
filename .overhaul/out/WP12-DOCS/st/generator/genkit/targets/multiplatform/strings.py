"""
Translations written into a Compose Multiplatform project: `composeResources/values-<qualifier>/
strings.xml` beside every module's `composeResources/values/strings.xml`, holding exactly the
strings that survived into the project. A missing one is an error: Compose would show the English
text in the middle of a translated screen without saying so.
"""

from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

from ...core import translations
from ...core.copy import write_text
from ...core.spec import Spec
from ...core.translations import Text, TranslationError, module_slug
from . import modules

_RESOURCES = "src/commonMain/composeResources"


def resource_qualifier(tag: str) -> str:
    """`pt-BR` → `pt-rBR`, the folder suffix Compose resources expect for a language with a region."""
    language, _, region = tag.partition("-")
    return f"{language}-r{region}" if region else language


def translatable_names(strings_xml: Path) -> list[str]:
    root = ET.parse(strings_xml).getroot()
    return [
        element.get("name")
        for element in root
        if element.tag in ("string", "plurals") and element.get("translatable") != "false"
    ]


def write(project: Path, spec: Spec) -> None:
    if not spec.languages:
        return
    sources = [
        (source, translatable_names(source))
        for source in sorted(project.glob(f"**/{_RESOURCES}/values/strings.xml"))
    ]
    for tag in spec.languages:
        translation = translations.load("multiplatform", tag)
        entries: dict[str, dict[str, Text]] = {slug: dict(texts) for slug, texts in translation.modules.items()}
        for name in modules.modules(spec):
            entries[module_slug(f"feature/{name}")] = {
                key.replace("{name}", name): value for key, value in translation.scaffold.items()
            }

        for source, names in sources:
            if not names:
                continue
            module = source.parents[4].relative_to(project).as_posix()
            texts = entries.get(module_slug(module), {})
            missing = [name for name in names if name not in texts]
            if missing:
                raise TranslationError(
                    f"No {tag} translation for {', '.join(missing)} in {module}. Add them to "
                    f"generator/translations/multiplatform/{tag}/{module_slug(module)}.json."
                )
            target = source.parent.parent / f"values-{resource_qualifier(tag)}" / "strings.xml"
            target.parent.mkdir(parents=True, exist_ok=True)
            write_text(target, strings_xml(names, texts))


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
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
