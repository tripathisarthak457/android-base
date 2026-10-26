"""
A table in `:core:database` for each scaffolded module, when the project has the database.

Core may not depend on data, so a module's entity and DAO live in `core/database/<name>/`, and
`AppDatabase` lists them at its three generated points. A scaffolded table joins version 1: a new
app has shipped no schema to migrate from.

Room refuses a database without a table. A project with the database but neither the reference
feature nor a scaffolded module gets one starter table, `note`, in the same shape, to rename for
its first domain.
"""

from __future__ import annotations

import re
from pathlib import Path

from ...core.copy import write_text
from ...core.spec import Spec, module_title, pascal_case

SNIPPETS = Path(__file__).parent / "snippets" / "database"

#: The table a project gets when nothing else would give its database one.
STARTER_TABLE = "note"

#: Packages `:core:database` already has, which a scaffolded module's table would land in.
RESERVED_NAMES = frozenset({"di", "queue", "sample", STARTER_TABLE})


def regions(spec: Spec) -> dict[str, list[str] | None]:
    names = _tables(spec)
    pkg = spec.package_name
    return {
        "database-imports": [
            line
            for name in names
            for line in (
                f"import {pkg}.core.database.{name}.{pascal_case(name)}Dao\n",
                f"import {pkg}.core.database.{name}.{pascal_case(name)}Entity\n",
            )
        ],
        "database-entities": [f"        {pascal_case(name)}Entity::class,\n" for name in names],
        "database-daos": [f"    abstract fun {_camel(name)}(): {pascal_case(name)}Dao\n" for name in names],
    }


def write_tables(project: Path, spec: Spec) -> None:
    for name in _tables(spec):
        directory = project / "core/database/src/main/kotlin" / spec.package_path / "core/database" / name
        directory.mkdir(parents=True, exist_ok=True)
        for snippet, suffix in (("Entity.kt", "Entity"), ("Dao.kt", "Dao")):
            text = (SNIPPETS / snippet).read_text(encoding="utf-8")
            for token, value in (
                ("__PKG__", spec.package_name),
                ("__NAME__", name),
                ("__P__", pascal_case(name)),
                ("__TABLE__", name),
                ("__ABOUT__", _about(name)),
            ):
                text = text.replace(token, value)
            write_text(directory / f"{pascal_case(name)}{suffix}.kt", _one_line_when_it_fits(text))


#: The projects' line length (`max_line_length` in .editorconfig).
_MAX_LINE = 100
_CLASS_HEADER = re.compile(r"^(?P<head>data class \w+\()\n(?P<params>(?:    .+,\n)+)\)$", re.MULTILINE)


def _one_line_when_it_fits(text: str) -> str:
    """A class header on one line when it fits, as ktlint's class-signature rule wants it; otherwise as written."""

    def joined(match: re.Match[str]) -> str:
        params = [line.strip().rstrip(",") for line in match["params"].splitlines()]
        line = f"{match['head']}{', '.join(params)})"
        return line if len(line) <= _MAX_LINE else match.group(0)

    return _CLASS_HEADER.sub(joined, text)


def _tables(spec: Spec) -> list[str]:
    if not spec.has("database"):
        return []
    if not spec.feature_modules and not spec.has("sample"):
        return [STARTER_TABLE]
    return list(spec.feature_modules)


def _about(name: str) -> str:
    if name == STARTER_TABLE:
        return (
            "A starter table, because Room needs at least one. Rename it for the app's first domain,\n"
            " * or delete it once another table exists."
        )
    return f"One {module_title(name).lower()} row as last fetched; `data/{name}` maps it to its model."


def _camel(name: str) -> str:
    pascal = pascal_case(name)
    return pascal[:1].lower() + pascal[1:]
