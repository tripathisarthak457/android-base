"""
The module layout a spec asks for. `compact` is the template as it is authored: one module per
feature and per data domain, each with a public `api` package. `modular` moves every `api` package
into its own pure-Kotlin `:api` module and the rest of the module into `:impl`.

The split edits build files and settings only. Every source file keeps its text and its package, so
no import changes between the two shapes, and a compact project can be split at any time:

    python3 -m genkit.targets.android.modes <project>
"""

from __future__ import annotations

import re
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path
from string import Template

SNIPPETS = Path(__file__).parent / "snippets"
TIERS = ("feature", "data")

_NAMESPACE = re.compile(r'^\s*namespace\s*=\s*"([\w.]+)"', re.MULTILINE)
_INCLUDE = re.compile(r'^(\s*)include\("(:(?:feature|data):[a-z0-9_]+)"\)\s*$', re.MULTILINE)
_PROJECT = re.compile(r'project\(\s*"(:(feature|data):([a-z0-9_]+))"\s*\)')
_ACCESSOR = re.compile(r"\bprojects\.(feature|data)\.([A-Za-z0-9]+)\b(?![.\w])")
_PUBLIC_LIBRARY = re.compile(r"^\s*api\(libs\.[\w.]+\)\s*$", re.MULTILINE)
_DEPENDENCIES = re.compile(r"^dependencies\s*\{\s*$", re.MULTILINE)
_SKIPPED = frozenset({"build", ".gradle", ".kotlin"})


class SplitError(ValueError):
    """The project cannot be split as it stands."""


@dataclass(frozen=True)
class Module:
    tier: str
    name: str

    @property
    def path(self) -> str:
        return f":{self.tier}:{self.name}"

    @property
    def accessor(self) -> str:
        first, *rest = self.name.split("_")
        return first + "".join(part.capitalize() for part in rest)


def split(project: Path) -> list[str]:
    """Splits the compact project at `project` in place. Returns notes for the person running it."""
    package = _package(project)
    package_path = package.replace(".", "/")
    modules = [
        Module(tier, directory.name)
        for tier in TIERS
        if (project / tier).is_dir()
        for directory in sorted((project / tier).iterdir())
        if (directory / "build.gradle.kts").is_file()
    ]
    already = sorted(impl.parent for tier in TIERS for impl in (project / tier).glob("*/impl"))
    if already:
        raise SplitError(f"{already[0].relative_to(project)} is already split into :api and :impl.")
    if not modules:
        return ["Nothing to split: the project has no feature or data modules."]

    features = {module.name for module in modules if module.tier == "feature"}
    shared = _shared_key_folders(project, package_path, features)
    for module in modules:
        _split_module(project, package, package_path, module)
    owners = _move_shared_keys(project, package_path, shared)
    _depend_on_imported_apis(project, package, [m for m in modules if m.tier == "feature"])
    for build_file in sorted(project.glob("**/build.gradle.kts")):
        if not _SKIPPED.intersection(build_file.relative_to(project).parts):
            _rewrite_references(project, build_file, modules)
    _rewrite_settings(project / "settings.gradle.kts")
    _app_depends_on_data(project / "app/build.gradle.kts", [m for m in modules if m.tier == "data"])

    notes = [f"Split {len(modules)} modules into :api and :impl."]
    if owners:
        notes.append(f"Moved the shared keys of {', '.join(owners)} into their :api modules.")
    return notes


def _package(project: Path) -> str:
    build = project / "app/build.gradle.kts"
    match = _NAMESPACE.search(build.read_text(encoding="utf-8")) if build.is_file() else None
    if match is None:
        raise SplitError(f"{project} is not an Android project from this generator: app/build.gradle.kts has no namespace.")
    return match.group(1)


def _split_module(project: Path, package: str, package_path: str, module: Module) -> None:
    root = project / module.tier / module.name
    impl = root / "impl"
    impl.mkdir()
    for child in sorted(root.iterdir()):
        if child.name not in _SKIPPED and child != impl:
            child.rename(impl / child.name)

    compact_build = (impl / "build.gradle.kts").read_text(encoding="utf-8")
    api = root / "api"
    moved_sources = False
    for source_set in ("main", "test", "testFixtures"):
        package_dir = Path("src") / source_set / "kotlin" / package_path / module.tier / module.name / "api"
        if (impl / package_dir).is_dir():
            _move_tree(impl / package_dir, api / package_dir)
            _prune_empty(impl / package_dir, stop=impl)
            moved_sources = True

    imports = "\n".join(path.read_text(encoding="utf-8") for path in api.rglob("*.kt")) if moved_sources else ""
    extra = [line.strip() for line in _PUBLIC_LIBRARY.findall(compact_build)]
    if "import kotlinx.serialization" in imports and "api(libs.kotlinx.serialization.core)" not in extra:
        extra.append("api(libs.kotlinx.serialization.core)")
    api.mkdir(exist_ok=True)
    (api / "build.gradle.kts").write_text(_api_build_file(package, module, extra), encoding="utf-8")

    _add_dependency(impl / "build.gradle.kts", f'api(project("{module.path}:api"))')


def _api_build_file(package: str, module: Module, extra: list[str]) -> str:
    snippet = Template((SNIPPETS / f"{module.tier}-api.build.gradle.kts").read_text(encoding="utf-8"))
    text = snippet.substitute(package=package)
    present = set(text.splitlines())
    lines = [f"    {line}" for line in extra if f"    {line}" not in present]
    if not lines:
        return text
    head, _, tail = text.rpartition("}")
    return head + "\n".join(lines) + "\n}" + tail


def _shared_key_folders(project: Path, package_path: str, features: set[str]) -> list[Path]:
    """The `core/navigation/…/feature/<owner>/api` folders, refused when an owner is not a feature module."""
    shared_root = project / "core/navigation/src/main/kotlin" / package_path / "feature"
    folders = sorted(shared_root.glob("*/api")) if shared_root.is_dir() else []
    for folder in folders:
        if folder.parent.name not in features:
            raise SplitError(
                f"{folder.relative_to(project)} holds keys owned by '{folder.parent.name}', which is not a "
                "feature module here."
            )
    return folders


def _move_shared_keys(project: Path, package_path: str, folders: list[Path]) -> list[str]:
    """Moves each shared-key folder into `:feature:<owner>:api`, where the owner's own keys are."""
    sources = project / "core/navigation/src/main/kotlin"
    for folder in folders:
        owner = folder.parent.name
        _move_tree(folder, project / "feature" / owner / "api/src/main/kotlin" / package_path / "feature" / owner / "api")
        _prune_empty(folder.parent, stop=sources)
    return [folder.parent.name for folder in folders]


def _depend_on_imported_apis(project: Path, package: str, features: list[Module]) -> None:
    """Each `:impl` that imports another feature's api package (a shared key) depends on that `:api`."""
    names = {module.name for module in features}
    imported = re.compile(rf"^import\s+{re.escape(package)}\.feature\.([a-z0-9_]+)\.api\b", re.MULTILINE)
    for module in features:
        impl = project / "feature" / module.name / "impl"
        owners = {
            match.group(1)
            for source in sorted((impl / "src").rglob("*.kt"))
            for match in imported.finditer(source.read_text(encoding="utf-8"))
        }
        for owner in sorted(owners & names - {module.name}):
            _add_dependency(impl / "build.gradle.kts", f'implementation(project(":feature:{owner}:api"))')


def _rewrite_references(project: Path, build_file: Path, modules: list[Module]) -> None:
    """Points each reference to a split module at the half the referring module may see."""
    parts = build_file.relative_to(project).parts
    if len(parts) == 4 and parts[0] in TIERS and parts[2] == "api":
        return
    # The app wires implementations; every other module sees a split module only through its api.
    half = "impl" if parts[0] == "app" else "api"
    split_paths = {module.path for module in modules}
    accessors = {(module.tier, module.accessor) for module in modules}

    def project_call(match: re.Match[str]) -> str:
        return f'project("{match.group(1)}:{half}")' if match.group(1) in split_paths else match.group(0)

    def accessor(match: re.Match[str]) -> str:
        return f"{match.group(0)}.{half}" if (match.group(1), match.group(2)) in accessors else match.group(0)

    text = build_file.read_text(encoding="utf-8")
    rewritten = _ACCESSOR.sub(accessor, _PROJECT.sub(project_call, text))
    if rewritten != text:
        build_file.write_text(rewritten, encoding="utf-8")


def _rewrite_settings(settings: Path) -> None:
    text = settings.read_text(encoding="utf-8")
    rewritten = _INCLUDE.sub(
        lambda match: f'{match.group(1)}include("{match.group(2)}:api")\n{match.group(1)}include("{match.group(2)}:impl")',
        text,
    )
    settings.write_text(rewritten, encoding="utf-8")


def _app_depends_on_data(build_file: Path, data: list[Module]) -> None:
    """
    In the compact shape the app reaches each data module through the feature that uses it. A
    feature's `:impl` now sees only the data `:api`, so the app names every data `:impl` itself, or
    their Hilt bindings would never reach the graph.
    """
    if not build_file.is_file():
        return
    for module in data:
        _add_dependency(build_file, f'implementation(project("{module.path}:impl"))', after=":impl\"))")


def _add_dependency(build_file: Path, line: str, after: str | None = None) -> None:
    """Adds `line` to the top-level dependencies block, after the last line containing `after` if any."""
    text = build_file.read_text(encoding="utf-8")
    if line in text:
        return
    lines = text.splitlines(keepends=True)
    anchors = [index for index, current in enumerate(lines) if after and after in current]
    if anchors:
        lines.insert(anchors[-1] + 1, f"    {line}\n")
    else:
        block = _DEPENDENCIES.search(text)
        if block is None:
            separator = "" if text.endswith("\n\n") or not text else "\n"
            build_file.write_text(f"{text}{separator}dependencies {{\n    {line}\n}}\n", encoding="utf-8")
            return
        index = text.count("\n", 0, block.end()) + 1
        lines.insert(index, f"    {line}\n")
    build_file.write_text("".join(lines), encoding="utf-8")


def _move_tree(source: Path, target: Path) -> None:
    for path in sorted(source.rglob("*")):
        if path.is_file():
            destination = target / path.relative_to(source)
            if destination.exists():
                raise SplitError(f"Both {source} and {target} hold {path.name}.")
            destination.parent.mkdir(parents=True, exist_ok=True)
            path.rename(destination)
    shutil.rmtree(source)


def _prune_empty(directory: Path, stop: Path) -> None:
    """Removes `directory` and then each parent below `stop` while they are empty."""
    current = directory
    while current != stop and stop in current.parents:
        if current.is_dir() and not any(current.iterdir()):
            current.rmdir()
        elif current.exists():
            return
        current = current.parent


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: python3 -m genkit.targets.android.modes <compact project directory>", file=sys.stderr)
        return 2
    try:
        notes = split(Path(argv[0]))
    except (SplitError, OSError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    for note in notes:
        print(note)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
