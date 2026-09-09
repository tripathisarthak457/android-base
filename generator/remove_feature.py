#!/usr/bin/env python3
"""
Remove a feature module from a project, and undo the three edits that added it.

    py remove_feature.py orders                    # in the current project
    py remove_feature.py orders profile            # two at once
    py remove_feature.py orders --project ../MyApp
    py remove_feature.py orders --dry-run          # say what would go, delete nothing

The exact inverse of `add_feature.py`: it deletes `:data:<name>` and `:feature:<name>` and takes
their lines back out of `settings.gradle.kts`, the app module's dependencies, and
`AppDestinations`. Doing it by hand means remembering all three, and the one people forget is the
Gradle include — which fails the next sync with an error about a missing project rather than about
the directory they deleted.

It refuses a module the template ships. `:feature:auth` and the rest are removed by generating
without them, not by deleting the directory: their code is referenced from the app module's
navigation and from files no name-based scan would find, and half-removing one leaves a project
that does not compile with no obvious way back.
"""

from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

from add_feature import inspect_project
from genkit import prompts, scaffold
from genkit.spec import RESERVED_MODULE_NAMES, SpecError, validate_module_name


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="remove_feature",
        description="Delete a scaffolded data + feature module pair and its three registrations.",
    )
    parser.add_argument("names", nargs="+", help="Module names, lower_snake_case.")
    parser.add_argument(
        "--project",
        type=Path,
        default=Path.cwd(),
        help="The project root. Defaults to the current directory.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print what would be deleted and edited, and delete nothing.",
    )
    parser.add_argument(
        "--yes",
        action="store_true",
        help="Skip the confirmation. For scripts; deleting source is not undoable.",
    )
    args = parser.parse_args(argv)

    project = args.project.expanduser().resolve()

    try:
        spec = inspect_project(project)
        names = tuple(normalise(name) for name in args.names)
    except SpecError as error:
        print(prompts.red(str(error)))
        return 1

    directories = [
        path
        for name in names
        for path in (project / "data" / name, project / "feature" / name)
    ]
    present = [path for path in directories if path.exists()]

    missing = [name for name in names if not (project / "feature" / name).exists()]
    if missing:
        print(prompts.red(f"Not in this project: {', '.join(missing)}."))
        print(prompts.dim("  Nothing was changed. Check the name, or the --project path."))
        return 1

    report_plan(project, names, present)

    if args.dry_run:
        print(prompts.dim("  Nothing was deleted. Drop --dry-run to do it."))
        print()
        return 0

    # Source files, not build output. Asked once, listing everything, rather than per module —
    # answering the same question four times is how somebody stops reading it.
    if not args.yes and not prompts.ask_yes_no("\nDelete these permanently?", False):
        print("Cancelled.")
        return 0

    for path in present:
        shutil.rmtree(path)

    unregister_modules(project, names)
    unregister_dependencies(project, names)
    unregister_tabs(project, spec.package_name, names)

    report_done(names)
    return 0


def normalise(name: str) -> str:
    cleaned = name.strip().lower().replace("-", "_").replace(" ", "_")
    validate_module_name(cleaned)
    if cleaned in RESERVED_MODULE_NAMES:
        # Unreachable via validate_module_name, which rejects these first. Kept as the place the
        # rule is explained, because the reason it is a rule is not the same reason it cannot be
        # scaffolded — see the module docstring.
        raise SpecError(f"'{cleaned}' is part of the template, not a scaffolded module.")
    return cleaned


# ─────────────────────────────────────────────────────────────────────────────
# Undoing the three edits
# ─────────────────────────────────────────────────────────────────────────────


def unregister_modules(project: Path, names: tuple[str, ...]) -> None:
    """Takes the `include(...)` lines back out of settings.gradle.kts."""
    path = project / "settings.gradle.kts"
    targets = {f'include(":{tier}:{name}")' for name in names for tier in ("data", "feature")}
    _drop_lines(path, lambda line: line.strip() in targets)


def unregister_dependencies(project: Path, names: tuple[str, ...]) -> None:
    """Takes the `implementation(project(":feature:x"))` lines out of app/build.gradle.kts."""
    path = project / "app" / "build.gradle.kts"
    targets = {f'implementation(project(":feature:{name}"))' for name in names}
    _drop_lines(path, lambda line: line.strip() in targets)


def unregister_tabs(project: Path, package_name: str, names: tuple[str, ...]) -> None:
    """
    Takes each module's tab and its import back out of `AppDestinations`.

    Matched on the module's own key class rather than on the whole line, because the label and the
    icon are the two things somebody is most likely to have edited since — and a tab left behind
    is a compile error naming a class that no longer exists.
    """
    path = project / "app/src/main/kotlin" / package_name.replace(".", "/") / "ui/AppDestinations.kt"
    if not path.is_file():
        print(prompts.yellow(f"  ! No AppDestinations.kt at {path}; check the tabs by hand."))
        return

    keys = {f"{scaffold.pascal(name)}ListKey" for name in names}
    imports = {f"import {package_name}.feature.{name}." for name in names}

    def is_ours(line: str) -> bool:
        stripped = line.strip()
        if any(stripped.startswith(prefix) for prefix in imports):
            return True
        return stripped.startswith("ShellTab(") and any(f"= {key}," in stripped for key in keys)

    _drop_lines(path, is_ours)


def _drop_lines(path: Path, matches) -> None:
    """
    Removes whole lines from [path]. Silent when the file does not have them.

    Line-based rather than a regex over the whole file, for the same reason `add_feature.py`
    inserts line by line: these are build scripts somebody is expected to have edited by hand, and
    a pattern that spans lines starts matching things it did not mean the moment the formatting
    changes.
    """
    if not path.is_file():
        return
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    kept = [line for line in lines if not matches(line)]
    if len(kept) != len(lines):
        path.write_text("".join(kept), encoding="utf-8")


# ─────────────────────────────────────────────────────────────────────────────
# Output
# ─────────────────────────────────────────────────────────────────────────────


def report_plan(project: Path, names: tuple[str, ...], present: list[Path]) -> None:
    prompts.heading("Removing")
    for path in present:
        print(f"  {prompts.red('delete')}  {path.relative_to(project).as_posix()}/")
    print()
    print(prompts.dim("  and the lines naming them in:"))
    print(prompts.dim("    settings.gradle.kts, app/build.gradle.kts, ui/AppDestinations.kt"))
    print()


def report_done(names: tuple[str, ...]) -> None:
    print()
    print(prompts.green(prompts.bold("  Done.")))
    print()
    print(f"  Removed: {', '.join(names)}")
    print()
    print(prompts.bold("  Next"))
    print("    1. Sync Gradle.")
    print("    2. Anything else that referenced these screens is now a compile error, which is")
    print("       the list of places to look.")
    print()


if __name__ == "__main__":
    sys.exit(main())
