"""
Which of a project's classes a baseline profile names, and whether a generated project has each.

A profile entry is a class, or a member of one: `Lcom/acme/ui/AppRootKt;` or
`HSPLcom/acme/ui/AppRootKt;->AppRoot(...)V`. Its owner is in the project when the project declares
that class, compiles it from a file (`AppRootKt` from `AppRoot.kt`), or generates it for one that is
declared: Hilt's `Hilt_MainActivity`, Dagger's `DaggerAcmeApplication_HiltComponents_SingletonC`, a
ViewModel's `SignInViewModel_HiltModules`, Compose's `ComposableSingletons$AppRootKt`.
"""

from __future__ import annotations

import re
from pathlib import Path

from tests.consistency.project import Project

_OWNER = re.compile(r"^[HSP]*L([\w/$]+);")


def classes_of(root: Path) -> set[tuple[str, str]]:
    """(package path, name) for everything the project's Kotlin declares or compiles a file class for."""
    project = Project(root)
    known = {
        (package.replace(".", "/"), name)
        for package, names in project.declared.items()
        for name in names
    }
    known |= {(project.package_of[path].replace(".", "/"), f"{path.stem}Kt") for path in project.kotlin}
    known |= {(namespace.replace(".", "/"), generated) for namespace in project.namespaces for generated in ("R", "BuildConfig")}
    return known


def owner(line: str, package_path: str) -> tuple[str, str] | None:
    """The project class an entry belongs to, as (package path, source name), or None for another library's."""
    match = _OWNER.match(line.strip())
    if not match or not match.group(1).startswith(package_path + "/"):
        return None
    package, _, name = match.group(1).rpartition("/")
    parts = name.split("$")
    top = parts[1] if parts[0] == "ComposableSingletons" and len(parts) > 1 else parts[0]
    for prefix in ("Hilt_", "Dagger"):
        top = top.removeprefix(prefix)
    return package, top.split("_")[0]


def kept(line: str, known: set[tuple[str, str]], package_path: str = "com/base/app") -> bool:
    """Whether the entry on `line` still names something the project has."""
    found = owner(line, package_path)
    return found is None or found in known
