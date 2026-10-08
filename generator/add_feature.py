#!/usr/bin/env python3
"""
Add a feature module to an Android project that already exists.

    python3 add_feature.py orders                      # in the current project
    python3 add_feature.py orders profile --tab        # two modules, both as bottom-bar tabs
    python3 add_feature.py orders --project ../MyApp
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections.abc import Callable
from pathlib import Path

from genkit.core import prompts, translations
from genkit.core.record import RECORD_NAME
from genkit.core.spec import Spec, SpecError, validate_module_name
from genkit.targets.android import scaffold, strings, tabs
from genkit.targets.android.target import AndroidTarget


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="add_feature",
        description="Scaffold a data + feature module pair into an existing project.",
    )
    parser.add_argument("names", nargs="+", help="Module names, lower_snake_case.")
    parser.add_argument("--project", type=Path, default=Path.cwd(), help="The project root. Defaults to here.")
    parser.add_argument("--tab", action="store_true", help="Also add each module as a bottom-bar tab.")
    args = parser.parse_args(argv)

    project = args.project.expanduser().resolve()
    try:
        names = tuple(normalise(name) for name in args.names)
        spec = inspect_project(project, names)
    except SpecError as error:
        print(prompts.red(str(error)))
        return 1

    existing = [name for name in names if (project / "feature" / name).exists()]
    if existing:
        print(prompts.red(f"Already present: {', '.join(existing)}. Delete them first, or rename."))
        return 1

    scaffold.write_feature_modules(project, spec)
    strings.write(project, spec, {f"feature/{name}" for name in names})
    register_modules(project, names)
    register_dependencies(project, names)
    if args.tab:
        register_tabs(project, spec.package_name, names)

    report(names, spec.languages, tabs_added=args.tab)
    return 0


def normalise(name: str) -> str:
    cleaned = name.strip().lower().replace("-", "_").replace(" ", "_")
    validate_module_name(cleaned, AndroidTarget().reserved_module_names)
    return cleaned


def inspect_project(project: Path, names: tuple[str, ...]) -> Spec:
    """Just enough of the original spec to scaffold against, with `names` as its modules."""
    settings = project / "settings.gradle.kts"
    if not settings.is_file():
        raise SpecError(f"No settings.gradle.kts in {project}. Is that the project root?")
    text = settings.read_text(encoding="utf-8")
    match = re.search(r'rootProject\.name\s*=\s*"([^"]+)"', text)
    if match is None:
        raise SpecError("Could not read rootProject.name from settings.gradle.kts.")

    return Spec(
        app_name=match.group(1),
        package_name=find_package_name(project),
        features=frozenset({"network"} if ":core:network" in text else set()),
        feature_modules=names,
        languages=shipped_languages(project),
    )


def find_package_name(project: Path) -> str:
    """The app module's namespace, which is the package every scaffolded file is written under."""
    build_file = project / "app" / "build.gradle.kts"
    if not build_file.is_file():
        raise SpecError(f"No app/build.gradle.kts in {project}. Is that the project root?")
    match = re.search(r'namespace\s*=\s*"([^"]+)"', build_file.read_text(encoding="utf-8"))
    if match is None:
        raise SpecError("Could not read the app module's namespace from app/build.gradle.kts.")
    return match.group(1)


def shipped_languages(project: Path) -> tuple[str, ...]:
    """
    The languages the project ships besides English. Lint fails a module that lacks one, so a new
    module needs every one of them.
    """
    record = project / RECORD_NAME
    if record.is_file():
        languages = json.loads(record.read_text(encoding="utf-8")).get("languages", [])
        if isinstance(languages, list):
            return tuple(languages)
    by_qualifier = {strings.resource_qualifier(tag): tag for tag in translations.available("android")}
    found = {
        by_qualifier[path.parent.name.removeprefix("values-")]
        for path in project.glob("*/src/main/res/values-*/strings.xml")
        if path.parent.name.removeprefix("values-") in by_qualifier
    }
    return tuple(sorted(found))


def register_modules(project: Path, names: tuple[str, ...]) -> None:
    """Appends the includes to settings.gradle.kts, under the section each belongs to."""
    path = project / "settings.gradle.kts"
    text = path.read_text(encoding="utf-8")
    for prefix in (":data:", ":feature:"):
        additions = "".join(f'include("{prefix}{name}")\n' for name in names)
        text = insert_after_last(text, f'include("{prefix}', additions)
    path.write_text(text, encoding="utf-8")


def register_dependencies(project: Path, names: tuple[str, ...]) -> None:
    path = project / "app" / "build.gradle.kts"
    text = path.read_text(encoding="utf-8")
    additions = "".join(f'    implementation(project(":feature:{name}"))\n' for name in names)
    text = insert_after_last(
        text, '    implementation(project(":feature:', additions, fallback=insert_into_dependencies_block
    )
    path.write_text(text, encoding="utf-8")


def insert_into_dependencies_block(text: str, addition: str) -> str:
    """Puts `addition` just before the closing brace of the `dependencies { }` block."""
    lines = text.splitlines(keepends=True)
    try:
        start = next(index for index, line in enumerate(lines) if line.startswith("dependencies {"))
    except StopIteration:
        return text.rstrip("\n") + "\n\ndependencies {\n" + addition + "}\n"
    end = next((index for index in range(start + 1, len(lines)) if lines[index].rstrip("\n") == "}"), len(lines))
    lines.insert(end, addition)
    return "".join(lines)


_MODULE_TAB = re.compile(r"^\s*ShellTab\(key = \w+ListKey, label = \w+R\.string\.")
_TRAILING_TAB = re.compile(r"^\s*ShellTab\(key = (ProfileKey|SettingsKey),")


def register_tabs(project: Path, package_name: str, names: tuple[str, ...]) -> None:
    """
    Adds each module to `AppDestinations.tabs` where generation would have put it: after the
    module tabs already there, before profile and settings. The first tab is the start
    destination, so a new tab never goes in front.
    """
    path = project / "app/src/main/kotlin" / package_name.replace(".", "/") / "ui/AppDestinations.kt"
    if not path.is_file():
        print(prompts.yellow(f"  ! No AppDestinations.kt at {path}; add the tabs by hand."))
        return

    text = path.read_text(encoding="utf-8")
    imports = "".join(
        line
        for name in names
        if f".feature.{name}." not in text
        for line in tabs.shell_tab_imports(package_name, name)
    )
    text = insert_after_last(text, f"import {package_name}.", imports)

    lines = text.splitlines(keepends=True)
    try:
        opening = next(index for index, line in enumerate(lines) if "val tabs: List<ShellTab> = listOf(" in line)
    except StopIteration:
        print(prompts.yellow(f"  ! No tab list in {path}; add the tabs by hand."))
        path.write_text(text, encoding="utf-8")
        return
    closing = next(index for index in range(opening + 1, len(lines)) if lines[index].strip() == ")")
    position = closing
    for index in range(opening + 1, closing):
        if _MODULE_TAB.match(lines[index]):
            position = index + 1
        elif _TRAILING_TAB.match(lines[index]) and position == closing:
            position = index
            # A comment above the trailing tab is about that tab, so the new one goes above both.
            while position > opening + 1 and lines[position - 1].strip().startswith("//"):
                position -= 1
    lines.insert(position, "".join(tabs.shell_tab(name) for name in names))
    path.write_text("".join(lines), encoding="utf-8")


def insert_after_last(
    text: str,
    prefix: str,
    addition: str,
    fallback: Callable[[str, str], str] | None = None,
) -> str:
    """Inserts `addition` after the last line starting with `prefix`."""
    if not addition:
        return text
    lines = text.splitlines(keepends=True)
    last = max((index for index, line in enumerate(lines) if line.startswith(prefix)), default=None)
    if last is None:
        return fallback(text, addition) if fallback else text.rstrip("\n") + "\n" + addition
    lines.insert(last + 1, addition)
    return "".join(lines)


def report(names: tuple[str, ...], languages: tuple[str, ...], tabs_added: bool) -> None:
    print()
    print(prompts.green(prompts.bold("  Done.")))
    print()
    for name in names:
        print(f"    :data:{name}")
        print(f"    :feature:{name}")
    if languages:
        print(prompts.dim(f"    strings in {', '.join(languages)} as well as English"))
    print()
    print(prompts.bold("  Next"))
    print("    1. Sync Gradle.")
    if not tabs_added:
        print("    2. Register the screen where it belongs, or re-run with --tab.")
    print()


if __name__ == "__main__":
    sys.exit(main())
