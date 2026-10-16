#!/usr/bin/env python3
"""WP03's design-system strings, applied to the generator's translation files.

Re-runnable: adds what WP03 added to core/designsystem's strings.xml and removes what it deleted,
in whichever layout the tree has: generator/translations/<lang>.json (before WP01) or
generator/translations/android/<lang>/<module-slug>.json (after WP01 split them).

Usage: wp03_translations.py <repo root>
"""
import json
import sys
from pathlib import Path

MODULE = "core/designsystem/src/main/res"

ADD = {
    "designsystem_close": {"de": "Schließen", "es": "Cerrar", "fr": "Fermer", "hi": "बंद करें", "pt-BR": "Fechar"},
    "designsystem_expanded": {"de": "Ausgeklappt", "es": "Expandido", "fr": "Développé", "hi": "विस्तृत", "pt-BR": "Expandido"},
    "designsystem_collapsed": {"de": "Eingeklappt", "es": "Contraído", "fr": "Réduit", "hi": "संक्षिप्त", "pt-BR": "Recolhido"},
    "designsystem_expand": {"de": "Ausklappen", "es": "Expandir", "fr": "Développer", "hi": "विस्तृत करें", "pt-BR": "Expandir"},
    "designsystem_collapse": {"de": "Einklappen", "es": "Contraer", "fr": "Réduire", "hi": "संक्षिप्त करें", "pt-BR": "Recolher"},
    "designsystem_page_of": {
        "de": "Seite %1$d von %2$d", "es": "Página %1$d de %2$d", "fr": "Page %1$d sur %2$d",
        "hi": "पृष्ठ %1$d / %2$d", "pt-BR": "Página %1$d de %2$d",
    },
    "designsystem_loading": {"de": "Wird geladen", "es": "Cargando", "fr": "Chargement", "hi": "लोड हो रहा है", "pt-BR": "Carregando"},
    "designsystem_people_count": {
        "de": {"one": "%1$d Person", "other": "%1$d Personen"},
        "es": {"one": "%1$d persona", "other": "%1$d personas"},
        "fr": {"one": "%1$d personne", "other": "%1$d personnes"},
        "hi": {"one": "%1$d व्यक्ति", "other": "%1$d लोग"},
        "pt-BR": {"one": "%1$d pessoa", "other": "%1$d pessoas"},
    },
    "designsystem_total": {"de": "Gesamt", "es": "Total", "fr": "Total", "hi": "कुल", "pt-BR": "Total"},
    "designsystem_move_up": {"de": "Nach oben", "es": "Mover arriba", "fr": "Monter", "hi": "ऊपर ले जाएं", "pt-BR": "Mover para cima"},
    "designsystem_move_down": {"de": "Nach unten", "es": "Mover abajo", "fr": "Descendre", "hi": "नीचे ले जाएं", "pt-BR": "Mover para baixo"},
}

REMOVE = [
    "designsystem_select_date",
    "designsystem_select_time",
    "designsystem_confirm",
    "designsystem_retry",
    "designsystem_change_country_code",
    "designsystem_add_tag",
    "designsystem_not_set",
]


def apply_to(strings: dict, lang: str) -> bool:
    changed = False
    for key, texts in ADD.items():
        if strings.get(key) != texts[lang]:
            strings[key] = texts[lang]
            changed = True
    for key in REMOVE:
        if key in strings:
            del strings[key]
            changed = True
    return changed


def main(root: Path) -> None:
    translations = root / "generator" / "translations"
    split = translations / "android"
    if split.is_dir():
        for lang_dir in sorted(p for p in split.iterdir() if p.is_dir()):
            matches = [p for p in lang_dir.glob("*.json") if "designsystem" in p.stem]
            for path in matches:
                data = json.loads(path.read_text(encoding="utf-8"))
                strings = data.get("strings", data)
                if apply_to(strings, lang_dir.name):
                    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
                    print("updated", path.relative_to(root))
        return
    for path in sorted(translations.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        strings = data["strings"].setdefault(MODULE, {})
        if apply_to(strings, path.stem):
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            print("updated", path.relative_to(root))


if __name__ == "__main__":
    main(Path(sys.argv[1]).resolve())
