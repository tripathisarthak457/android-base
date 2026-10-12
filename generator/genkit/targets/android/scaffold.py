"""
The feature modules a spec names, each a `:feature:<name>` with a list and a detail screen over its
own `:data:<name>`, in the shape of the template's sample (STANDARDS-PLAN 2.1). The Kotlin lives in
`snippets/scaffold/` with `__TOKEN__` placeholders, so it reads as the code it becomes.
"""

from __future__ import annotations

import re
from pathlib import Path

from ...core.spec import Spec, module_title, pascal_case

SNIPPETS = Path(__file__).parent / "snippets" / "scaffold"
TEMPLATE_DIR = Path(__file__).resolve().parents[4] / "templates" / "android"

#: The module scaffolded when nothing else gives the shell a tab: the shell needs at least one.
HOME = "home"

#: The tab sources the template has of its own.
TEMPLATE_TABS = ("sample", "paging", "search", "profile", "settings")

#: The design system's multi-size preview annotation, once the template has it.
_SCREEN_PREVIEWS = TEMPLATE_DIR / "core/designsystem/src/main/kotlin/com/base/app/core/designsystem/preview/AppScreenPreviews.kt"

_BASELINE = "config/code-rules-baseline.txt"

#: Snippet → path inside the module's package directory, for the feature module.
_FEATURE_SOURCES = {
    "Keys.kt": "api/{P}Keys.kt",
    "Navigation.kt": "navigation/{P}Navigation.kt",
    "ListContract.kt": "list/{P}ListContract.kt",
    "ListViewModel.kt": "list/{P}ListViewModel.kt",
    "ListRoute.kt": "list/{P}ListRoute.kt",
    "ListScreen.kt": "list/{P}ListScreen.kt",
    "DetailContract.kt": "detail/{P}DetailContract.kt",
    "DetailViewModel.kt": "detail/{P}DetailViewModel.kt",
    "DetailRoute.kt": "detail/{P}DetailRoute.kt",
    "DetailScreen.kt": "detail/{P}DetailScreen.kt",
}
_FEATURE_TESTS = {
    "ListViewModelTest.kt": "list/{P}ListViewModelTest.kt",
    "RecompositionTest.kt": "{P}RecompositionTest.kt",
}
_SCREENS = ("list/{P}ListScreen.kt", "detail/{P}DetailScreen.kt")


def modules(spec: Spec) -> tuple[str, ...]:
    """The modules to scaffold: the spec's, plus `home` when no feature or module would give a tab."""
    if spec.feature_modules or any(spec.has(feature) for feature in TEMPLATE_TABS):
        return spec.feature_modules
    return (HOME,)


def notes(spec: Spec) -> list[str]:
    if modules(spec) == (HOME,) and HOME not in spec.feature_modules:
        return [
            "No feature gives the app a tab, so a 'home' module was scaffolded under feature/home and "
            "data/home. Its repository has no source yet: give it one, or rename the module."
        ]
    return []


def regions(spec: Spec) -> dict[str, list[str] | None]:
    """Each module's settings includes and its dependency from :app."""
    names = modules(spec)
    return {
        "data-modules": [f'include(":data:{name}")\n' for name in names],
        "feature-modules": [f'include(":feature:{name}")\n' for name in names],
        "app-feature-dependencies": [f'    implementation(project(":feature:{name}"))\n' for name in names],
    }


def write_feature_modules(project: Path, spec: Spec) -> None:
    for name in modules(spec):
        tokens = _tokens(spec, name)
        _write_data_module(project, spec, name, tokens)
        _write_feature_module(project, spec, name, tokens)


def _tokens(spec: Spec, name: str) -> dict[str, str]:
    pascal = pascal_case(name)
    title = module_title(name)
    return {
        "__PKG__": spec.package_name,
        "__NAME__": name,
        "__P__": pascal,
        "__CAMEL__": pascal[0].lower() + pascal[1:],
        "__TITLE__": title,
        "__TITLE_LOWER__": title.lower(),
    }


def _write_data_module(project: Path, spec: Spec, name: str, tokens: dict[str, str]) -> None:
    root = project / "data" / name
    source = root / "src/main/kotlin" / spec.package_path / "data" / name
    tests = root / "src/test/kotlin" / spec.package_path / "data" / name
    fixtures = root / "src/testFixtures/kotlin" / spec.package_path / "data" / name
    remote = spec.has("network") and name != HOME

    _render("data/build.remote.gradle.kts" if remote else "data/build.local.gradle.kts", root / "build.gradle.kts", tokens)
    _render("data/Item.kt", source / "api" / f"{tokens['__P__']}Item.kt", tokens)
    _render("data/Repository.kt", source / "api" / f"{tokens['__P__']}Repository.kt", tokens)
    _render(
        "data/DefaultRepository.remote.kt" if remote else "data/DefaultRepository.local.kt",
        source / f"Default{tokens['__P__']}Repository.kt",
        tokens,
    )
    _render("data/DataModule.kt", source / "di" / f"{tokens['__P__']}DataModule.kt", tokens)
    # Beside the interface it fakes, so a modular split moves it into `:data:<name>:api` with it.
    _render("data/FakeRepository.kt", fixtures / "api" / f"Fake{tokens['__P__']}Repository.kt", tokens)
    if remote:
        _render("data/Dto.kt", source / "remote" / f"{tokens['__P__']}Dto.kt", tokens)
        _render("data/DtoTest.kt", tests / "remote" / f"{tokens['__P__']}DtoTest.kt", tokens)
        _render("data/fixture.json", root / "src/test/resources" / f"{name}.json", tokens)


def _write_feature_module(project: Path, spec: Spec, name: str, tokens: dict[str, str]) -> None:
    root = project / "feature" / name
    source = root / "src/main/kotlin" / spec.package_path / "feature" / name
    tests = root / "src/test/kotlin" / spec.package_path / "feature" / name
    annotated = _SCREEN_PREVIEWS.is_file()
    previews = {
        "__PREVIEW_IMPORTS__": (
            f"import {spec.package_name}.core.designsystem.preview.AppScreenPreviews"
            if annotated
            else "import androidx.compose.ui.tooling.preview.Preview"
        ),
        "__PREVIEW__": f"{'@AppScreenPreviews' if annotated else '@Preview(showBackground = true)'}\n@Composable",
    }

    _render("feature/build.gradle.kts", root / "build.gradle.kts", tokens)
    _render("feature/strings.xml", root / "src/main/res/values/strings.xml", tokens)
    for snippet, relative in _FEATURE_SOURCES.items():
        # `@AppScreenPreviews` applies the theme itself; a plain `@Preview` needs it written out.
        _render(f"feature/{snippet}", source / relative.format(P=tokens["__P__"]), {**tokens, **previews}, themed=not annotated)
    for snippet, relative in _FEATURE_TESTS.items():
        _render(f"feature/{snippet}", tests / relative.format(P=tokens["__P__"]), tokens)

    if not annotated:
        _accept_unpreviewed_screens(project, spec, name, tokens["__P__"])


def _accept_unpreviewed_screens(project: Path, spec: Spec, name: str, pascal: str) -> None:
    """
    Until the design system has `@AppScreenPreviews`, `adaptive-preview` cannot be met; the scaffolded
    screens join the template's own in the code-rule baseline, which shrinks as screens move to it.
    """
    baseline = project / _BASELINE
    if not baseline.is_file():
        return
    base = f"feature/{name}/src/main/kotlin/{spec.package_path}/feature/{name}"
    lines = [f"{base}/{screen.format(P=pascal)} adaptive-preview 1\n" for screen in _SCREENS]
    with baseline.open("a", encoding="utf-8") as file:
        file.writelines(lines)


def _render(snippet: str, destination: Path, tokens: dict[str, str], themed: bool = True) -> None:
    text = (SNIPPETS / snippet).read_text(encoding="utf-8")
    for token, value in tokens.items():
        text = text.replace(token, value)
    text = _theme_previews(text, themed)
    leftover = re.search(r"__[A-Z_]+__", text)
    if leftover:
        raise ValueError(f"{snippet} has an unfilled {leftover.group(0)}")
    if destination.suffix == ".kt":
        text = sort_imports(text)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(text, encoding="utf-8")


def _theme_previews(text: str, themed: bool) -> str:
    """Turns the `__THEMED_START__`/`__THEMED_END__` lines into `AppTheme { … }`, or drops them and outdents."""
    out: list[str] = []
    inside = False
    for line in text.splitlines(keepends=True):
        marker = line.strip()
        if marker in ("__THEMED_START__", "__THEMED_END__"):
            inside = marker == "__THEMED_START__"
            if themed:
                out.append(line.replace("__THEMED_START__", "AppTheme {").replace("__THEMED_END__", "}"))
            continue
        out.append(line[4:] if inside and not themed and line.startswith("    ") else line)
    return "".join(out)


_IMPORT = re.compile(r"^import [\w.]+(?: as \w+)?\n", re.MULTILINE)


def sort_imports(text: str) -> str:
    """Kotlin imports in ASCII order, the Android style guide's and `.editorconfig`'s `*` layout."""
    found = _IMPORT.findall(text)
    if not found:
        return text
    first = text.index(found[0])
    last = text.index(found[-1]) + len(found[-1])
    ordered = sorted(set(found), key=lambda line: line.split()[1])
    return text[:first] + "".join(ordered) + text[last:]
