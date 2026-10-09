"""Which template files a project gets, and the file-system chores around writing them."""

from __future__ import annotations

import os
from collections.abc import Iterator
from dataclasses import dataclass
from fnmatch import fnmatch
from pathlib import Path

#: Never copied out of a template: build output, IDE state, machine-local settings and keys.
SKIP_DIRS = frozenset({
    "build", ".gradle", ".kotlin", ".idea", ".git", "keys", ".cxx", "__pycache__",
    "DerivedData", ".build", "xcuserdata", "Pods", ".swiftpm",
})
SKIP_FILES = frozenset({"local.properties", "keystore.properties", ".DS_Store"})


@dataclass(frozen=True)
class SourceFile:
    #: Where the file sits in the template, before renaming.
    relative: str
    #: Where its bytes come from: the template, or a file a target substitutes for it.
    source: Path


def is_under(relative: str, prefixes: list[str] | tuple[str, ...]) -> bool:
    return any(relative == prefix or relative.startswith(prefix + "/") for prefix in prefixes)


def template_files(template: Path, excluded: list[str], overlays: dict[str, Path]) -> Iterator[SourceFile]:
    """
    Every file a project gets from `template`, in a stable order. `excluded` are template-relative
    paths left out with everything under them; `overlays` replace or add files.
    """
    seen: set[str] = set()
    root = str(template)
    for directory, subdirectories, filenames in os.walk(template):
        relative_dir = os.path.relpath(directory, root).replace(os.sep, "/")
        subdirectories[:] = sorted(
            name for name in subdirectories
            if name not in SKIP_DIRS and not is_under(_join(relative_dir, name), excluded)
        )
        for filename in sorted(filenames):
            relative = _join(relative_dir, filename)
            if filename in SKIP_FILES or is_under(relative, excluded):
                continue
            seen.add(relative)
            yield SourceFile(relative, overlays.get(relative) or Path(directory, filename))
    for relative in sorted(set(overlays) - seen):
        if not is_under(relative, excluded):
            yield SourceFile(relative, overlays[relative])


def _join(directory: str, name: str) -> str:
    return name if directory == "." else f"{directory}/{name}"


def is_executable(relative: str, patterns: tuple[str, ...]) -> bool:
    return any(fnmatch(relative, pattern) for pattern in patterns)


def mark_executable(path: Path) -> None:
    mode = path.stat().st_mode
    # Whoever can read it can run it, which is the mode git records for an executable file.
    path.chmod(mode | ((mode & 0o444) >> 2))


def prune_empty_directories(root: Path) -> None:
    for path in sorted(root.rglob("*"), key=lambda p: len(p.parts), reverse=True):
        if path.is_dir() and not any(path.iterdir()):
            path.rmdir()
