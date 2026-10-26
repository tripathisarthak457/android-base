#!/usr/bin/env python3
"""
Add a feature module to an Android project that already exists.

    py add_feature.py orders                      # in the current project
    py add_feature.py orders profile --tab         # two modules, both as bottom-nav tabs
    py add_feature.py orders --project ../MyApp
    py add_feature.py --domain pricing            # a pure-Kotlin :domain:pricing, modular projects only

In a project generated without UI components, a module gets its logic and a placeholder route, never
UI components, and there is no tab bar to add it to.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections.abc import Callable
from pathlib import Path
from string import Template

from genkit import targets
from genkit.core import prompts, translations
from genkit.core.copy import write_text
from genkit.core.record import RECORD_NAME
from genkit.core.spec import ARCHITECTURES, Spec, SpecError, validate_module_name
from genkit.targets.android import imports as kotlin_imports
from genkit.targets.android import scaffold, strings, tabs


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="add_feature",
        description="Scaffold a data + feature module pair into an existing project.",
    )
    parser.add_argument("names", nargs="*", help="Module names, lower_snake_case.")
    parser.add_argument(
        "--domain",
        metavar="NAME",
        help="Add a pure-Kotlin :domain:NAME module instead, for rules several features share. Modular projects only.",
    )
    parser.add_argument(
        "--project",
        type=Path,
        default=Path.cwd(),
        help="The project root. Defaults to the current directory.",
    )
    parser.add_argument(
        "--tab",
        action="store_true",
        help="Also add each module as a bottom-navigation tab. Not in a project without UI components.",
    )
    args = parser.parse_args(argv)

    project = args.project.expanduser().resolve()
    if args.domain is not None:
        if args.names or args.tab:
            parser.error("--domain adds one domain module and takes no feature names or --tab.")
        return add_domain(project, args.domain)
    if not args.names:
        parser.error("name at least one module, or use --domain.")

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
    components = spec.option(scaffold.UI_COMPONENTS)
    if args.tab and not components:
        print(prompts.red(
            "This project has no tab bar (it was generated without UI components). The module is reachable by "
            "its keys and deep links; add it to your own navigation once you build one."
        ))
        return 1

    scaffold.write_feature_modules(project, spec)
    strings.write(project, spec, {f"feature/{name}" for name in names})

    register_modules(project, names)
    register_dependencies(project, names)
    if args.tab:
        register_tabs(project, spec.package_name, names)

    report(names, spec.languages, tabs_added=args.tab, components=components)
    return 0


def add_domain(project: Path, raw_name: str) -> int:
    """Writes `domain/<name>`, which only the modular shape has a place for (MASTER-PLAN A2.4)."""
    try:
        name = normalise(raw_name)
        package_name = find_package_name(project)
    except SpecError as error:
        print(prompts.red(str(error)))
        return 1
    if not is_modular(project):
        print(prompts.red(
            "Domain modules belong to modular projects, where features see data only through :api modules. "
            "In a compact project, put shared rules in the data module that owns them."
        ))
        return 1
    root = project / "domain" / name
    if root.exists():
        print(prompts.red(f"Already present: domain/{name}."))
        return 1

    snippet = (Path(scaffold.__file__).parent / "snippets" / "domain.build.gradle.kts").read_text(encoding="utf-8")
    (root / "src/main/kotlin" / package_name.replace(".", "/") / "domain" / name).mkdir(parents=True)
    write_text(root / "build.gradle.kts", Template(snippet).substitute(package=package_name))

    settings = project / "settings.gradle.kts"
    text = settings.read_text(encoding="utf-8")
    include = f'include(":domain:{name}")\n'
    write_text(settings, insert_include(text, ('include(":domain:', 'include(":data:'), "data", include))

    print()
    print(prompts.green(prompts.bold("  Done.")))
    print(f"    :domain:{name}")
    print(prompts.dim("    Add the data :api modules it reads to its build file, and depend on it from the features that use it."))
    print()
    return 0


def is_modular(project: Path) -> bool:
    """
    The architecture the project's record names; without one, whether a module is split into :api
    and :impl. A modular project without UI components may have no module to split yet.
    """
    record = read_record(project)
    if record is not None and record.get("architecture") in ARCHITECTURES:
        return record["architecture"] == "modular"
    return any(project.glob("feature/*/impl/build.gradle.kts")) or any(project.glob("data/*/impl/build.gradle.kts"))


def inspect_project(project: Path, names: tuple[str, ...]) -> Spec:
    """Just enough of the original spec to scaffold against, with `names` as its modules."""
    settings = project / "settings.gradle.kts"
    if not settings.is_file():
        raise SpecError(f"No settings.gradle.kts in {project}. Is that the project root?")

    text = settings.read_text(encoding="utf-8")
    match = re.search(r'rootProject\.name\s*=\s*"([^"]+)"', text)
    if match is None:
        raise SpecError("Could not read rootProject.name from settings.gradle.kts.")

    package_name = find_package_name(project)
    manifest = targets.get("android").manifest
    return Spec(
        app_name=match.group(1),
        package_name=package_name,
        features=frozenset({"network"} if ":core:network" in text else set()),
        feature_modules=names,
        languages=shipped_languages(project),
        options=project_options(project, package_name),
        option_defaults={key: option.default for key, option in manifest.options.items()},
    )


def read_record(project: Path) -> dict | None:
    """The project's `generator-spec.json`, or None when it has none or it is not a JSON object."""
    record = project / RECORD_NAME
    if not record.is_file():
        return None
    try:
        data = json.loads(record.read_text(encoding="utf-8"))
    except ValueError:
        return None
    return data if isinstance(data, dict) else None


def project_options(project: Path, package_name: str) -> dict[str, bool]:
    """
    Each of the template's options as the project was generated: from its record, which writes an
    option only when it is not the default; or, without a record, on when anything the option owns
    is there, since a project generated with it off has none of that.
    """
    record = read_record(project)
    values: dict[str, bool] = {}
    for key, option in targets.get("android").manifest.options.items():
        if record is not None:
            value = record.get(key, option.default)
            if not isinstance(value, bool):
                raise SpecError(f"{RECORD_NAME} gives '{key}' as {value!r}; it must be true or false.", key)
        elif option.owns:
            path = package_name.replace(".", "/")
            value = any((project / owned.format(pkg_path=path)).exists() for owned in option.owns)
        else:
            value = option.default
        values[key] = value
    return values


def find_package_name(project: Path) -> str:
    """The app module's namespace, which is the package every scaffolded file is written under."""
    build_file = project / "app" / "build.gradle.kts"
    if not build_file.is_file():
        raise SpecError(f"No app/build.gradle.kts in {project}. Is that the project root?")

    match = re.search(r'namespace\s*=\s*"([^"]+)"', build_file.read_text(encoding="utf-8"))
    if match is None:
        raise SpecError("Could not read the app module's namespace from app/build.gradle.kts.")
    return match.group(1)


def normalise(name: str) -> str:
    cleaned = name.strip().lower().replace("-", "_").replace(" ", "_")
    validate_module_name(cleaned, targets.get("android").reserved_module_names)
    return cleaned


def shipped_languages(project: Path) -> tuple[str, ...]:
    """
    The languages the project ships besides English. Lint fails a module that lacks one, so a new
    module needs every one of them.
    """
    record = read_record(project)
    if record is not None:
        languages = record.get("languages", [])
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
    """Adds the includes to settings.gradle.kts, under the section each belongs to."""
    path = project / "settings.gradle.kts"
    text = path.read_text(encoding="utf-8")

    for tier in ("data", "feature"):
        additions = "".join(f'include(":{tier}:{name}")\n' for name in names)
        text = insert_include(text, (f'include(":{tier}:',), tier, additions)

    write_text(path, text)


def insert_include(text: str, anchors: tuple[str, ...], section: str, addition: str) -> str:
    """
    Puts [addition] after the last include that starts with the first of [anchors] the settings
    have. With none of them, as in a project without UI components before its first module, it goes
    right under the comment that heads [section] (`// ── data ──`), and at the end only when the
    settings have lost that comment too.
    """
    lines = text.splitlines()
    anchor = next((anchor for anchor in anchors if any(line.startswith(anchor) for line in lines)), None)
    return insert_after_last(text, anchor or f"// ── {section} ", addition)


def register_dependencies(project: Path, names: tuple[str, ...]) -> None:
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
    write_text(path, text)


def insert_into_dependencies_block(text: str, addition: str) -> str:
    """Puts [addition] just before the closing brace of the `dependencies { }` block."""
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


_MODULE_TAB = re.compile(r"ShellTab\(\s*key = \w+ListKey,\s*label = \w+R\.string\.")
_HOME_TAB = re.compile(r"ShellTab\(\s*key = SampleListKey,")
_ICON = re.compile(r"\bAppIcons\.(\w+)")


def register_tabs(project: Path, package_name: str, names: tuple[str, ...]) -> None:
    """
    Adds each module to `AppDestinations.tabs` where generation would have put it: after the
    module tabs already there, or after Home. The first tab is the start destination, so a new tab
    never goes in front of it, and each gets a glyph no other tab uses.
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
    opening = next((index for index, line in enumerate(lines) if "val tabs: List<ShellTab> = listOf(" in line), None)
    closing = None
    if opening is not None:
        closing = next((index for index in range(opening + 1, len(lines)) if lines[index].strip() == ")"), None)
    if closing is None:
        print(prompts.yellow(f"  ! No tab list in {path}; add the tabs by hand."))
        write_text(path, text)
        return

    entries = tabs.tab_entries(lines, opening + 1, closing)

    def entry(span: tuple[int, int]) -> str:
        return "".join(lines[span[0]:span[1] + 1])

    modules = [span for span in entries if _MODULE_TAB.search(entry(span))]
    home = [span for span in entries if _HOME_TAB.search(entry(span))]
    anchor = (modules or home or entries[:1] or [(opening, opening)])[-1][1]

    taken = set(_ICON.findall("".join(lines[opening:closing])))
    chosen = tabs.icons(names, taken, tabs.available_glyphs(project))
    lines.insert(anchor + 1, "".join(tabs.shell_tab(name, *chosen[name]) for name in names))
    # The new imports went after the project's last one; ktlint wants the list in order.
    write_text(path, kotlin_imports.sort("".join(lines)))


def insert_after_last(
    text: str,
    prefix: str,
    addition: str,
    fallback: Callable[[str, str], str] | None = None,
) -> str:
    """Inserts [addition] after the last line starting with [prefix]."""
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


def report(names: tuple[str, ...], languages: tuple[str, ...], tabs_added: bool, components: bool) -> None:
    print()
    print(prompts.green(prompts.bold("  Done.")))
    print()
    for name in names:
        if components:
            print(f"    :data:{name}")
            print(f"    :feature:{name}")
        else:
            print(f"    Added :data:{name} and :feature:{name} with its ViewModel and a placeholder route.")
    if languages:
        print(prompts.dim(f"    strings in {', '.join(languages)} as well as English"))
    if not components:
        print(prompts.dim("    No UI components: build the screen on your design system."))
    print()
    print(prompts.bold("  Next"))
    print("    1. Sync Gradle.")
    if not components:
        print("    2. Open it from your own navigation by its keys, or by a deep link.")
    elif not tabs_added:
        print("    2. Register the screen where it belongs, or re-run with --tab.")
    print()


if __name__ == "__main__":
    sys.exit(main())
