#!/usr/bin/env python3
"""
Add a feature module to a project that already exists.

    py add_feature.py orders                      # in the current project
    py add_feature.py orders profile --tab         # two modules, both as bottom-nav tabs
    py add_feature.py orders --project ../MyApp

Produces the same `:data:<name>` + `:feature:<name>` pair the generator scaffolds at creation
time, and performs the three edits that are otherwise done by hand and forgotten one at a time:
`settings.gradle.kts`, the app module's dependencies, and — with `--tab` — `AppDestinations`.

It reads the project's own package name out of its Gradle files rather than asking, so running it
in the wrong directory fails immediately instead of writing a module in the wrong namespace.
"""

from __future__ import annotations

import argparse
import re
import sys
from collections.abc import Callable
from pathlib import Path

from genkit import prompts, scaffold
from genkit.spec import ProjectSpec, SpecError, validate_module_name


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="add_feature",
        description="Scaffold a data + feature module pair into an existing project.",
    )
    parser.add_argument("names", nargs="+", help="Module names, lower_snake_case.")
    parser.add_argument(
        "--project",
        type=Path,
        default=Path.cwd(),
        help="The project root. Defaults to the current directory.",
    )
    parser.add_argument(
        "--tab",
        action="store_true",
        help="Also add each module as a bottom-navigation tab.",
    )
    args = parser.parse_args(argv)

    project = args.project.expanduser().resolve()

    try:
        spec = inspect_project(project)
        names = tuple(normalise(name) for name in args.names)
    except SpecError as error:
        print(prompts.red(str(error)))
        return 1

    existing = [name for name in names if (project / "feature" / name).exists()]
    if existing:
        print(prompts.red(f"Already present: {', '.join(existing)}. Delete them first, or rename."))
        return 1

    spec = spec_with_modules(spec, names)
    scaffold.write_feature_modules(project, spec)

    register_modules(project, names)
    register_dependencies(project, spec, names)
    if args.tab:
        register_tabs(project, spec, names)

    report(names, tabs=args.tab)
    return 0


# ─────────────────────────────────────────────────────────────────────────────
# Reading the project
# ─────────────────────────────────────────────────────────────────────────────


def inspect_project(project: Path) -> ProjectSpec:
    """
    Recovers just enough of the original spec to scaffold against.

    Only the package name, the app name and whether networking is present actually affect what is
    written; everything else on `ProjectSpec` is left at its default because the scaffold does not
    read it.
    """
    settings = project / "settings.gradle.kts"
    if not settings.is_file():
        raise SpecError(f"No settings.gradle.kts in {project}. Is that the project root?")

    text = settings.read_text(encoding="utf-8")
    match = re.search(r'rootProject\.name\s*=\s*"([^"]+)"', text)
    if match is None:
        raise SpecError("Could not read rootProject.name from settings.gradle.kts.")
    app_name = match.group(1)

    package_name = find_package_name(project)
    features = {"network"} if ":core:network" in text else set()

    return ProjectSpec(
        app_name=app_name,
        package_name=package_name,
        features=frozenset(features),
    )


def find_package_name(project: Path) -> str:
    """
    The app module's namespace, which is the package every scaffolded file is written under.

    Read from the build file rather than inferred from the directory layout: a module whose
    directories and namespace disagree is unusual but legal, and guessing would put new files in
    a package that does not compile.
    """
    build_file = project / "app" / "build.gradle.kts"
    if not build_file.is_file():
        raise SpecError(f"No app/build.gradle.kts in {project}. Is that the project root?")

    match = re.search(r'namespace\s*=\s*"([^"]+)"', build_file.read_text(encoding="utf-8"))
    if match is None:
        raise SpecError("Could not read the app module's namespace from app/build.gradle.kts.")
    return match.group(1)


def normalise(name: str) -> str:
    cleaned = name.strip().lower().replace("-", "_").replace(" ", "_")
    validate_module_name(cleaned)
    return cleaned


def spec_with_modules(spec: ProjectSpec, names: tuple[str, ...]) -> ProjectSpec:
    from dataclasses import replace

    return replace(spec, feature_modules=names)


# ─────────────────────────────────────────────────────────────────────────────
# The three edits
# ─────────────────────────────────────────────────────────────────────────────


def register_modules(project: Path, names: tuple[str, ...]) -> None:
    """
    Appends the includes to settings.gradle.kts, under the section each belongs to.

    Anchored on the existing `include(":data:` and `include(":feature:` lines rather than on the
    section comments, so it still works in a project where those comments have been edited away.
    """
    path = project / "settings.gradle.kts"
    text = path.read_text(encoding="utf-8")

    for prefix in (":data:", ":feature:"):
        additions = "".join(f'include("{prefix}{name}")\n' for name in names)
        text = insert_after_last(text, f'include("{prefix}', additions)

    path.write_text(text, encoding="utf-8")


def register_dependencies(project: Path, spec: ProjectSpec, names: tuple[str, ...]) -> None:
    path = project / "app" / "build.gradle.kts"
    text = path.read_text(encoding="utf-8")

    additions = "".join(
        f'    implementation(project(":feature:{name}"))\n' for name in names
    )
    text = insert_after_last(
        text,
        '    implementation(project(":feature:',
        additions,
        fallback=insert_into_dependencies_block,
    )
    path.write_text(text, encoding="utf-8")


def insert_into_dependencies_block(text: str, addition: str) -> str:
    """
    Puts [addition] just before the closing brace of the `dependencies { }` block.

    The fallback for a project that has no feature modules yet — which is every project the first
    time this runs. Appending at the end of the file instead produces a build script that fails to
    compile, with an error about `implementation` not resolving that says nothing about why.
    """
    lines = text.splitlines(keepends=True)
    try:
        start = next(
            index for index, line in enumerate(lines) if line.startswith("dependencies {")
        )
    except StopIteration:
        return text.rstrip("\n") + "\n\ndependencies {\n" + addition + "}\n"

    end = next(
        (index for index in range(start + 1, len(lines)) if lines[index].rstrip("\n") == "}"),
        len(lines),
    )
    lines.insert(end, addition)
    return "".join(lines)


def register_tabs(project: Path, spec: ProjectSpec, names: tuple[str, ...]) -> None:
    """
    Adds each module to `AppDestinations.tabs`, and imports its key.

    Inserted before the closing paren of the `tabs` list rather than after the last entry, because
    a project may legitimately have none — and the file still has to compile after the edit.
    """
    path = (
        project
        / "app/src/main/kotlin"
        / spec.package_path
        / "ui/AppDestinations.kt"
    )
    if not path.is_file():
        print(prompts.yellow(f"  ! No AppDestinations.kt at {path}; add the tabs by hand."))
        return

    text = path.read_text(encoding="utf-8")

    imports = "".join(
        f"import {spec.package_name}.feature.{name}.{scaffold.pascal(name)}ListKey\n"
        for name in names
        if f".feature.{name}." not in text
    )
    text = insert_after_last(text, f"import {spec.package_name}.", imports)

    entries = "".join(
        f'        ShellTab(key = {scaffold.pascal(name)}ListKey, '
        f'label = "{scaffold.title(name)}", icon = AppIcons.Grid),\n'
        for name in names
    )
    text = text.replace(
        "    val tabs: List<ShellTab> = listOf(\n",
        f"    val tabs: List<ShellTab> = listOf(\n{entries}",
        1,
    )

    path.write_text(text, encoding="utf-8")


def insert_after_last(
    text: str,
    prefix: str,
    addition: str,
    fallback: Callable[[str, str], str] | None = None,
) -> str:
    """
    Inserts [addition] after the last line starting with [prefix].

    A project with no feature modules yet has no such line — the normal case for the first run of
    this script, not an error. [fallback] says where to put it then; without one the lines are
    appended, which is correct for a file of top-level `include(...)` calls and wrong for anything
    with a block structure.
    """
    if not addition:
        return text

    lines = text.splitlines(keepends=True)
    last = max(
        (index for index, line in enumerate(lines) if line.startswith(prefix)),
        default=None,
    )
    if last is None:
        return fallback(text, addition) if fallback else text.rstrip("\n") + "\n" + addition

    lines.insert(last + 1, addition)
    return "".join(lines)


def report(names: tuple[str, ...], tabs: bool) -> None:
    print()
    print(prompts.green(prompts.bold("  Done.")))
    print()
    for name in names:
        print(f"    :data:{name}")
        print(f"    :feature:{name}")
    print()
    print(prompts.bold("  Next"))
    print("    1. Sync Gradle.")
    if not tabs:
        print("    2. Register the screen where it belongs, or re-run with --tab.")
    print()


if __name__ == "__main__":
    sys.exit(main())
