#!/usr/bin/env python3
"""
Add a component to the design system of an Android project that already exists.

    py add_component.py Tag --project ../MyApp                  # component/tag/AppTag.kt and its test
    py add_component.py SearchBar --group fields --project .    # into the design system's component/fields
    py add_component.py Tag --project ../MyApp --dry-run        # what it would write, writing nothing

The component and its test are the two templates in the project's own docs/DESIGN-SYSTEM.md, renamed. That page is
where a component is learned, and the build's rules hold both templates to themselves (DesignSystemDocTest), so the
page, this scaffold and the guard cannot disagree. Projects with and without UI components get the same component, each
with the previews its own page shows.
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path

from genkit.core import prompts
from genkit.core.copy import write_text

DOC = "docs/DESIGN-SYSTEM.md"
DESIGN_SYSTEM = "core/designsystem"
TESTING = "core/testing/compose"
#: The component the page's templates build. Its name is what a new component's name replaces.
TEMPLATE_NAME = "Tag"

NAME = re.compile(r"(?:[A-Z][a-z0-9]+)+")
GROUP = re.compile(r"[a-z][a-z0-9]*")

#: Where each name the templates use comes from. `{designsystem}` and `{testing}` are the namespaces the project's own
#: modules declare. The templates are checked against this list before anything is written, so a template that
#: starts using a name not here stops this script with that name, rather than writing a file that does not compile.
IMPORTS = {
    "Alignment": "androidx.compose.ui.Alignment",
    "AppComponentPreviews": "{designsystem}.preview.AppComponentPreviews",
    "AppPreviewTheme": "{designsystem}.preview.AppPreviewTheme",
    "AppTheme": "{designsystem}.theme.AppTheme",
    "BasicText": "androidx.compose.foundation.text.BasicText",
    "Box": "androidx.compose.foundation.layout.Box",
    "Color": "androidx.compose.ui.graphics.Color",
    "Composable": "androidx.compose.runtime.Composable",
    "Config": "org.robolectric.annotation.Config",
    "Configuration": "android.content.res.Configuration",
    "Immutable": "androidx.compose.runtime.Immutable",
    "Modifier": "androidx.compose.ui.Modifier",
    "Preview": "androidx.compose.ui.tooling.preview.Preview",
    "PreviewWrapper": "androidx.compose.ui.tooling.preview.PreviewWrapper",
    "RecompositionCounter": "{testing}.recomposition.RecompositionCounter",
    "RobolectricTestRunner": "org.robolectric.RobolectricTestRunner",
    "Role": "androidx.compose.ui.semantics.Role",
    "Rule": "org.junit.Rule",
    "RuleChain": "org.junit.rules.RuleChain",
    "RunWith": "org.junit.runner.RunWith",
    "TEST_SDK": "{testing}.TEST_SDK",
    "Test": "org.junit.Test",
    "assertHeightIsAtLeast": "androidx.compose.ui.test.assertHeightIsAtLeast",
    "assertIsOff": "androidx.compose.ui.test.assertIsOff",
    "assertIsOn": "androidx.compose.ui.test.assertIsOn",
    "assertQuietBetweenEnds": "{testing}.recomposition.assertQuietBetweenEnds",
    "background": "androidx.compose.foundation.background",
    "createComposeRule": "androidx.compose.ui.test.junit4.createComposeRule",
    "dp": "androidx.compose.ui.unit.dp",
    "framesUntilIdle": "{testing}.recomposition.framesUntilIdle",
    "height": "androidx.compose.foundation.layout.height",
    "mutableStateOf": "androidx.compose.runtime.mutableStateOf",
    "onNodeWithText": "androidx.compose.ui.test.onNodeWithText",
    "padding": "androidx.compose.foundation.layout.padding",
    "performClick": "androidx.compose.ui.test.performClick",
    "remember": "androidx.compose.runtime.remember",
    "setThemed": "{designsystem}.setThemed",
    "sizeIn": "androidx.compose.foundation.layout.sizeIn",
    "toggleable": "androidx.compose.foundation.selection.toggleable",
    "wrapContentHeight": "androidx.compose.foundation.layout.wrapContentHeight",
}
#: A property delegated with `by` needs the delegate's operators.
GET_VALUE = "androidx.compose.runtime.getValue"
SET_VALUE = "androidx.compose.runtime.setValue"

#: Names a Kotlin file uses without an import: keywords, and what the language imports by default.
_KOTLIN = frozenset("""
    as break class continue do else false for fun if in interface is null object package return super this throw true
    try typealias typeof val var when while by catch constructor delegate dynamic field file finally get import init
    param property receiver set setparam value where abstract actual annotation companion const crossinline data enum
    expect external final infix inline inner internal lateinit noinline open operator out override private protected
    public reified sealed suspend tailrec vararg it
    Any Array Boolean Byte Char Double Float Int Long Nothing Short String Unit List Map Set Pair
    also and apply check checkNotNull emptyList emptyMap emptySet error lazy let listOf mapOf maxOf minOf or repeat
    require requireNotNull run setOf takeIf to until with xor
""".split())


class ComponentError(Exception):
    """Why the component cannot be added, in words for the person who asked for it."""


@dataclass(frozen=True)
class Planned:
    path: Path
    text: str


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="add_component",
        description="Scaffold a design-system component and its test into an existing Android project, "
        "from the templates in its docs/DESIGN-SYSTEM.md.",
    )
    parser.add_argument("name", help="The component's name in PascalCase, without the App prefix: Tag, SearchBar.")
    parser.add_argument(
        "--project",
        type=Path,
        default=Path.cwd(),
        help="The project root. Defaults to the current directory.",
    )
    parser.add_argument(
        "--group",
        help="The package under the design system's `component` package. Defaults to the name in lower case.",
    )
    parser.add_argument("--dry-run", action="store_true", help="Say what would be written, and write nothing.")
    args = parser.parse_args(argv)

    project = args.project.expanduser().resolve()
    try:
        planned = plan(project, args.name, args.group)
    except ComponentError as error:
        print(prompts.red(str(error)))
        return 1

    if args.dry_run:
        print()
        print(prompts.bold("  Would write"))
        for item in planned:
            print(f"    {item.path.relative_to(project).as_posix()}")
        print()
        return 0

    for item in planned:
        item.path.parent.mkdir(parents=True, exist_ok=True)
        write_text(item.path, item.text)
    report(project, args.name, planned)
    return 0


def plan(project: Path, name: str, group: str | None) -> list[Planned]:
    """The component's file and its test, as they would be written, or ComponentError saying why not."""
    check_name(name)
    group = group if group is not None else name.lower()
    if not GROUP.fullmatch(group):
        raise ComponentError(f"'{group}' is not a package name: use lower-case letters and digits, such as 'fields'.")

    settings = project / "settings.gradle.kts"
    if not settings.is_file():
        raise ComponentError(f"No settings.gradle.kts in {project}. Is that the project root?")
    if f'include(":{DESIGN_SYSTEM.replace("/", ":")}")' not in settings.read_text(encoding="utf-8"):
        raise ComponentError(f"{project.name} has no :core:designsystem, so there is no design system to add a component to.")
    designsystem = namespace(project, DESIGN_SYSTEM)
    testing = namespace(project, TESTING)

    doc = project / DOC
    if not doc.is_file():
        raise ComponentError(f"No {DOC} in {project}: the component is written from its templates.")
    text = doc.read_text(encoding="utf-8").replace("\r\n", "\n")

    component = f"App{name}"
    main_root = project / DESIGN_SYSTEM / "src/main/kotlin"
    existing = declared_in(main_root, component)
    if existing is not None:
        raise ComponentError(f"{component} is already in the design system: {existing.relative_to(project).as_posix()}.")

    package = f"{designsystem}.component.{group}"
    package_path = package.replace(".", "/")
    modules = {"designsystem": designsystem, "testing": testing}
    body = renamed(block(text, "component-template"), name)
    test = renamed(block(text, "component-test-template"), name)
    planned = [
        Planned(main_root / package_path / f"{component}.kt", source(package, body, frozenset(), modules)),
        # The test is in the component's package, so it sees what the component's file declares.
        Planned(
            project / DESIGN_SYSTEM / "src/test/kotlin" / package_path / f"{component}Test.kt",
            source(package, test, declarations(code_only(body)), modules),
        ),
    ]
    for item in planned:
        if item.path.exists():
            raise ComponentError(f"{item.path.relative_to(project).as_posix()} already exists.")
    return planned


def check_name(name: str) -> None:
    if re.fullmatch(r"App[A-Z]\w*", name):
        raise ComponentError(f"Give the name without its App prefix, which every component gets: add_component.py {name[3:]}")
    if not NAME.fullmatch(name):
        raise ComponentError(
            f"'{name}' is not a PascalCase name: each word starts with a capital and goes on in lower case "
            "(Tag, SearchBar, QrCode)."
        )


def namespace(project: Path, module: str) -> str:
    """The namespace `module`'s build file declares, which is the package its sources are written under."""
    build_file = project / module / "build.gradle.kts"
    if not build_file.is_file():
        raise ComponentError(f"{project.name} has no {module}/build.gradle.kts; the component and its test need it.")
    match = re.search(r'namespace\s*=\s*"([^"]+)"', build_file.read_text(encoding="utf-8"))
    if match is None:
        raise ComponentError(f"Could not read the namespace from {module}/build.gradle.kts.")
    return match.group(1)


def declared_in(root: Path, component: str) -> Path | None:
    """The file under `root` that is named after `component` or declares it, if one does."""
    declaration = re.compile(rf"\b(?:fun|class|object|interface)\s+{component}\b")
    for path in sorted(root.rglob("*.kt")) if root.is_dir() else []:
        if path.stem == component or declaration.search(path.read_text(encoding="utf-8")):
            return path
    return None


def block(doc: str, tag: str) -> str:
    match = re.search(rf"(?ms)^```kotlin {tag}\n(.*?)^```$", doc)
    if match is None:
        raise ComponentError(f"{DOC} has no ```kotlin {tag} block to write the component from.")
    return match.group(1)


def source(package: str, body: str, siblings: frozenset[str], modules: dict[str, str]) -> str:
    """A Kotlin file of `body` in `package`: the package line, the imports `body` needs, then `body`."""
    code = code_only(body)
    unknown = sorted(undeclared(code) - siblings - set(IMPORTS))
    if unknown:
        raise ComponentError(
            f"The component templates in {DOC} use {', '.join(unknown)}, which add_component.py cannot import. "
            "Add each to IMPORTS in add_component.py."
        )
    used = set(re.findall(r"\b[A-Za-z_]\w*\b", code))
    imports = {IMPORTS[symbol].format(**modules) for symbol in used & set(IMPORTS)}
    if re.search(r"\b(?:val|var)\s+\w+\s+by\b", code):
        imports.add(GET_VALUE)
    if re.search(r"\bvar\s+\w+\s+by\b", code):
        imports.add(SET_VALUE)
    lines = "".join(f"import {line}\n" for line in sorted(imports, key=import_order))
    return f"package {package}\n\n{lines}\n{body.lstrip(chr(10))}"


_COMMENT = re.compile(r"/\*(?:.|\n)*?\*/|//[^\n]*")
_LITERAL = re.compile(r'"""(?:.|\n)*?"""|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])\'|`[^`\n]*`')


def renamed(template: str, name: str) -> str:
    """The template's `AppTag` names for the component `name`, and the word 'tag' in its comments."""
    words = " ".join(word.lower() for word in re.findall(r"[A-Z][a-z0-9]*", name))
    article = "an" if words[0] in "aeiou" else "a"
    word = TEMPLATE_NAME.lower()

    def prose(comment: re.Match[str]) -> str:
        text = re.sub(rf"\b([Aa]) {word}\b", lambda match: f"{match.group(1)}{article[1:]} {words}", comment.group(0))
        return re.sub(rf"\b{word}\b", words, text)

    return _COMMENT.sub(prose, re.sub(rf"\bApp{TEMPLATE_NAME}(?![a-z0-9])", f"App{name}", template))


def code_only(text: str) -> str:
    """`text` without its comments, string and character literals and backticked names."""
    return _COMMENT.sub(" ", _LITERAL.sub(" ", text))


def undeclared(code: str) -> set[str]:
    """
    The names `code` refers to that it neither declares nor gets from Kotlin: those not reached through a `.`, not a
    declaration, a parameter, a named argument or a lambda's parameter.
    """
    referenced = set(re.findall(r"(?<![\w.$])([A-Za-z_]\w*)", code))
    declared = set(declarations(code)) | set(re.findall(r"\b(?:val|var)\s+(\w+)", code))
    # Parameters, named arguments and lambda parameters start in lower case; a type before `=` or `:` is a reference.
    declared |= set(re.findall(r"(?<![\w.$])([a-z_]\w*)\s*(?::(?!:)|=(?!=)|->)", code))
    return referenced - declared - _KOTLIN


def declarations(code: str) -> frozenset[str]:
    """The classes, objects, interfaces and functions `code` declares."""
    return frozenset(re.findall(r"\b(?:class|object|interface|fun)\s+(?:<[^>]*>\s*)?(?:\w+\.)?(\w+)", code))


def import_order(path: str) -> tuple[int, str]:
    """ktlint's order for an Android Studio project: everything else, then java, javax and kotlin."""
    for rank, prefix in enumerate(("java.", "javax.", "kotlin."), start=1):
        if path.startswith(prefix):
            return rank, path
    return 0, path


def report(project: Path, name: str, planned: list[Planned]) -> None:
    print()
    print(prompts.green(prompts.bold("  Done.")))
    print()
    for item in planned:
        print(f"    {item.path.relative_to(project).as_posix()}")
    print()
    print(prompts.bold("  Next"))
    print(f"    1. Make it yours: what App{name} draws, its sizes and colours in App{name}Defaults, its KDoc and its test,")
    print("       keeping it stateless and every value from AppTheme (docs/DESIGN-SYSTEM.md).")
    print("    2. ./gradlew :core:designsystem:verifyCodeRules :core:designsystem:testDebugUnitTest")
    print()


if __name__ == "__main__":
    sys.exit(main())
