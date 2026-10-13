"""
Kotlin import order. ktlint's `*` layout (the `formatting` feature reads it from `.editorconfig`) and
Android Studio's Optimize Imports both keep one block in ASCII order by path. The template's order holds
only for `com.base.app` with every feature on: once the package is renamed, `io.acme.app` belongs after
`dagger`, and once a feature's fenced imports are dropped the rest no longer interleave, so a project's
sources are put back in order after both.
"""

from __future__ import annotations

import re
from pathlib import Path

_IMPORT = re.compile(r"import [\w.]+(?: as \w+)?")


def sort(text: str) -> str:
    """
    The import block in ASCII order by path, with blank lines and duplicates dropped. A `//` comment line
    moves with the import below it. Anything else inside the block leaves `text` unchanged.
    """
    lines = text.splitlines(keepends=True)
    positions = [index for index, line in enumerate(lines) if line.startswith("import ")]
    if not positions:
        return text
    first, last = positions[0], positions[-1]

    units: dict[str, list[str]] = {}
    comments: list[str] = []
    for line in lines[first:last + 1]:
        content = line.rstrip("\n")
        if _IMPORT.fullmatch(content):
            units.setdefault(content, []).extend(comments)
            comments = []
        elif content.lstrip().startswith("//"):
            comments.append(content.strip())
        elif content.strip():
            return text

    block = [
        line
        for statement in sorted(units, key=lambda statement: statement.split()[1])
        for line in (*units[statement], statement)
    ]
    return "".join(lines[:first]) + "".join(f"{line}\n" for line in block) + "".join(lines[last + 1:])


def sort_sources(project: Path) -> None:
    """Sorts every Kotlin source set's files under `project`, leaving the vendored dot-directories alone."""
    for path in project.glob("**/src/**/*.kt"):
        if any(part.startswith(".") for part in path.relative_to(project).parts):
            continue
        text = path.read_text(encoding="utf-8")
        ordered = sort(text)
        if ordered != text:
            path.write_text(ordered, encoding="utf-8")
