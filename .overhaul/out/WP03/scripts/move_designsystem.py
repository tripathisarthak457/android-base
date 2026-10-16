#!/usr/bin/env python3
"""
Moves design-system files into STANDARDS-PLAN §2.4's packages and fixes every reference.

    move_designsystem.py <templates/android> <moves.tsv>

moves.tsv: one `old/relative.kt<TAB>new/relative.kt` per line, relative to the design system's
package directory (`test:` before both for a file under src/test). Files are moved with `git mv`, their `package` line rewritten, and then every
Kotlin file in the template is fixed: imports of a moved name point at its new package, and a
design-system file that used a name from its old package without an import (same package) gets
one. Imports are sorted only in files this changes.
"""

import re
import subprocess
import sys
from pathlib import Path

DS = Path("core/designsystem/src/main/kotlin/com/base/app/core/designsystem")
DS_TEST = Path("core/designsystem/src/test/kotlin/com/base/app/core/designsystem")
BASE = "com.base.app.core.designsystem"
DECLARATION = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|internal|inline|data|enum|sealed|value|abstract|open|annotation|const|fun)\s+)*"
    r"(?:fun|class|object|interface|val|var|typealias)\s+(?:<[^>]+>\s+)?(?:[\w.]+(?:<[^>]*>)?\.)?(\w+)",
    flags=re.MULTILINE,
)
STRING_OR_COMMENT = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/')


def package_of(relative: str) -> str:
    parts = Path(relative).parent.parts
    return ".".join((BASE, *parts)) if parts else BASE


def declarations(text: str) -> set[str]:
    """Top-level names a file declares that another package can see."""
    found = set()
    for match in DECLARATION.finditer(text):
        line_start = text.rfind("\n", 0, match.start()) + 1
        if text[line_start:match.start()].strip() or "private " in text[line_start:match.end()]:
            continue
        found.add(match.group(1))
    return found


def index(root: Path) -> dict[str, set[str]]:
    names: dict[str, set[str]] = {}
    for path in (root / DS).rglob("*.kt"):
        package = package_of(path.relative_to(root / DS).as_posix())
        for name in declarations(path.read_text(encoding="utf-8")):
            names.setdefault(name, set()).add(package)
    return names


def identifiers(text: str) -> set[str]:
    body = "\n".join(line for line in text.split("\n") if not line.startswith(("import ", "package ")))
    return set(re.findall(r"\b[A-Za-z_]\w*\b", STRING_OR_COMMENT.sub(" ", body)))


def add_import(text: str, path: str) -> str:
    line = f"import {path}"
    if re.search(rf"^{re.escape(line)}$", text, flags=re.MULTILINE):
        return text
    imports = list(re.finditer(r"^import .+$", text, flags=re.MULTILINE))
    if imports:
        at = imports[-1].end()
        return text[:at] + "\n" + line + text[at:]
    package = re.search(r"^package .+$", text, flags=re.MULTILINE)
    return text[:package.end()] + "\n\n" + line + text[package.end():]


def import_order(line: str):
    path = line.removeprefix("import ")
    group = 4 if " as " in path else 1 if path.startswith("java.") else 2 if path.startswith("javax.") else 3 if path.startswith("kotlin.") else 0
    return group, path


def sort_imports(text: str) -> str:
    """Sorts and de-duplicates each run of import lines; runs split by markers stay apart."""
    out, run = [], []
    for line in text.split("\n") + [None]:
        if line is not None and line.startswith("import "):
            run.append(line)
            continue
        out.extend(sorted(dict.fromkeys(run), key=import_order))
        run = []
        if line is not None:
            out.append(line)
    return "\n".join(out)


def main() -> int:
    root, table = Path(sys.argv[1]), Path(sys.argv[2])
    moves = [line.split("\t") for line in table.read_text().splitlines() if line.strip()]
    before = index(root)
    old_package_of_file: dict[Path, str] = {}

    for old, new in moves:
        base = DS_TEST if old.startswith("test:") else DS
        old, new = old.removeprefix("test:"), new.removeprefix("test:")
        source, target = root / base / old, root / base / new
        target.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(["git", "mv", str(source), str(target)], check=True)
        text = target.read_text(encoding="utf-8")
        text = re.sub(r"^package .+$", f"package {package_of(new)}", text, count=1, flags=re.MULTILINE)
        target.write_text(text, encoding="utf-8")
        old_package_of_file[target.resolve()] = package_of(old)

    after = index(root)
    moved = {
        name: next(iter(packages))
        for name, packages in after.items()
        if len(packages) == 1 and before.get(name) != packages
    }

    changed = 0
    for path in root.rglob("*.kt"):
        if "/build/" in path.as_posix():
            continue
        text = path.read_text(encoding="utf-8")
        updated = text

        def repoint(match: re.Match) -> str:
            package, name = match.group(1), match.group(2)
            if name in moved and package in before.get(name, set()) and package not in after.get(name, set()):
                return f"import {moved[name]}.{name}"
            return match.group(0)

        updated = re.sub(rf"^import ({re.escape(BASE)}[\w.]*)\.(\w+)$", repoint, updated, flags=re.MULTILINE)

        package_line = re.search(r"^package ([\w.]+)$", updated, flags=re.MULTILINE)
        if package_line and package_line.group(1).startswith(BASE):
            own = package_line.group(1)
            old_own = old_package_of_file.get(path.resolve(), own)
            imported = set(re.findall(r"^import [\w.]+\.(\w+)$", updated, flags=re.MULTILINE))
            for name in identifiers(updated) - imported - declarations(updated):
                packages = after.get(name, set())
                if len(packages) != 1 or own in packages:
                    continue
                home = next(iter(packages))
                # Visible without an import before the move only if it shared the file's old package.
                if old_own in before.get(name, set()):
                    updated = add_import(updated, f"{home}.{name}")

        package_line = re.search(r"^package ([\w.]+)$", updated, flags=re.MULTILINE)
        if package_line:
            # A name that moved into this file's own package needs no import any more.
            updated = re.sub(rf"^import {re.escape(package_line.group(1))}\.\w+\n", "", updated, flags=re.MULTILINE)
        if updated != text:
            path.write_text(sort_imports(updated), encoding="utf-8")
            changed += 1
    print(f"moved {len(moves)} files, {len(moved)} names changed package, fixed {changed} files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
