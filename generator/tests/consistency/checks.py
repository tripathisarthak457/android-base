"""
Static checks over a generated Android project, no Gradle: what would fail the first build, found in
a second. Each issue is one line, `CATEGORY path: detail`.
"""

from __future__ import annotations

import json
import re
import tomllib
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

from genkit.core.record import RECORD_NAME
from genkit.targets import get

from .project import IMPORT, Project, project_files

MARKER = re.compile(r"</?(?:opt|opt-else|generated):")
IDENTITY = re.compile(r"(?<![\w.])com\.base\.app(?!\w)|com/base/app(?!\w)|(?<!\w)BaseApp|(?<![A-Za-z0-9])base_?app(?![A-Za-z0-9])")
BINARY = {".jar", ".png", ".jpg", ".jpeg", ".webp", ".jks", ".ttf", ".otf", ".ico"}
#: Operator and delegate names a Kotlin import provides without the name appearing in the code.
_IMPLICIT = {
    "getValue", "setValue", "provideDelegate", "invoke", "plus", "minus", "times", "div", "contains",
    "compareTo", "rangeTo", "iterator", "component1", "component2",
}


#: What a project without UI components must never reach: the design system's components and
#: primitives, and the navigation shell.
NO_UI_FORBIDDEN = re.compile(
    r"\bcore\.designsystem\.(?:component|foundation\.(?:content|draw|glass|interaction|layout|scroll)|animation|icon)\b"
    r"|\b(?:AppShell|ShellState|StackHost)\b"
)


def check(root: Path, package: str, reference: Project | None, identity: bool = True) -> list[str]:
    project = Project(root)
    components = _has_ui_components(root)
    issues: list[str] = []
    issues += _leftovers(root, identity)
    included = _settings(root, issues)
    issues += _project_references(project, included)
    issues += _imports(project, package)
    issues += _unused_imports(project)
    if reference is not None:
        issues += _dangling(project, reference)
    issues += _catalogue(root, project)
    issues += _conventions(root, project, package)
    issues += _manifests(root, project, package)
    issues += _resources(root, project)
    issues += _unused_resources(root, project, copy_kept=not components)
    issues += _translations(root)
    issues += _hollow(root, project)
    if not components:
        issues += _without_ui_components(root, project, package)
    return issues


def _has_ui_components(root: Path) -> bool:
    record = root / RECORD_NAME
    data = json.loads(record.read_text(encoding="utf-8")) if record.is_file() else {}
    return data.get("ui_components", True) is not False


def _without_ui_components(root: Path, project: Project, package: str) -> list[str]:
    """
    A project without UI components holds none of what the option owns, and no source reaches the
    components, the primitives or the shell. That each placeholder's title is a string of its own
    module is `_resources`' check, as for any other `R.string`.
    """
    manifest = get("android").manifest
    path = package.replace(".", "/")
    issues = [
        f"NOUI-OWNED {owned.format(pkg_path=path)} is still there"
        for owned in manifest.options["ui_components"].owns
        if (root / owned.format(pkg_path=path)).exists()
    ]
    for source in project.kotlin + project.gradle:
        for number, line in enumerate(project.code[source].splitlines(), 1):
            if NO_UI_FORBIDDEN.search(line):
                issues.append(f"NOUI-REACH {project.relative(source)}:{number}: {line.strip()[:120]}")
    return issues


def _leftovers(root: Path, identity: bool) -> list[str]:
    issues = []
    for path in sorted(project_files(root, "*")):
        if path.is_dir() or path.suffix in BINARY:
            continue
        relative = path.relative_to(root).as_posix()
        if identity and IDENTITY.search(relative):
            issues.append(f"IDENTITY-PATH {relative}")
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        for number, line in enumerate(text.splitlines(), 1):
            if MARKER.search(line):
                issues.append(f"MARKER {relative}:{number}: {line.strip()[:120]}")
            if identity and IDENTITY.search(line):
                issues.append(f"IDENTITY {relative}:{number}: {line.strip()[:120]}")
    return issues


def _settings(root: Path, issues: list[str]) -> set[str]:
    text = re.sub(r"(?m)^\s*//.*$", "", (root / "settings.gradle.kts").read_text(encoding="utf-8"))
    included = set(re.findall(r'include\("([^"]+)"\)', text))
    for path in sorted(included):
        if not (root / path.strip(":").replace(":", "/") / "build.gradle.kts").is_file():
            issues.append(f"SETTINGS include {path} has no build.gradle.kts")
    for build in sorted(project_files(root, "build.gradle.kts")):
        if "build-logic" in build.parts or build.parent == root:
            continue
        path = ":" + build.parent.relative_to(root).as_posix().replace("/", ":")
        if path not in included:
            issues.append(f"SETTINGS module {path} is not included")
    return included


def _accessor(path: str) -> str:
    """`:feature:order_history` → `feature.orderHistory`, as Gradle's type-safe accessors spell it."""
    return ".".join(re.sub(r"[-_](\w)", lambda m: m.group(1).upper(), part) for part in path.strip(":").split(":"))


def _project_references(project: Project, included: set[str]) -> list[str]:
    accessors = {_accessor(path) for path in included}
    issues = []
    for path in project.gradle:
        for match in re.finditer(r'project\("(:[^"]+)"\)', project.text[path]):
            if match.group(1) not in included:
                issues.append(f"PROJECT-REF {project.relative(path)} -> {match.group(1)} is not included")
        for match in re.finditer(r"(?<![\w.])projects\.((?:\w+\.)*\w+)", project.code[path]):
            if match.group(1) not in accessors and not any(a.startswith(match.group(1) + ".") for a in accessors):
                issues.append(f"PROJECT-REF {project.relative(path)} -> projects.{match.group(1)} is not included")
    return issues


def _imports(project: Project, package: str) -> list[str]:
    issues = []
    for path in project.kotlin:
        for match in IMPORT.finditer(project.text[path]):
            full = match.group(1)
            if not full.startswith(package + ".") or full.endswith(".*"):
                continue
            owner, _, name = full.rpartition(".")
            if name in ("R", "BuildConfig"):
                if owner not in project.namespaces:
                    issues.append(f"IMPORT {project.relative(path)}: {full} (no module with namespace {owner})")
            elif name not in project.declared.get(owner, ()):
                parent, _, outer = owner.rpartition(".")
                if outer not in project.declared.get(parent, ()):
                    issues.append(f"IMPORT {project.relative(path)}: {full} is not declared")
    return issues


def _unused_imports(project: Project) -> list[str]:
    issues = []
    for path in project.kotlin:
        body = IMPORT.sub("", project.code[path])
        for match in IMPORT.finditer(project.text[path]):
            full, alias = match.group(1), match.group(2)
            if full.endswith(".*"):
                continue
            name = alias or full.rpartition(".")[2].strip("`")
            if name in _IMPLICIT or re.search(rf"(?<!\w){re.escape(name)}(?!\w)", body):
                continue
            # A KDoc link is a use to the IDE and none to the compiler.
            kind = "UNUSED-IMPORT(kdoc-only)" if re.search(rf"\[{re.escape(name)}[\].]", project.text[path]) else "UNUSED-IMPORT"
            issues.append(f"{kind} {project.relative(path)}: {full}")
    return issues


def _dangling(project: Project, reference: Project) -> list[str]:
    """
    Same-package references to a top-level declaration that exists with every feature on and was
    stripped here. A cross-package use needs an import, which `_imports` already checks.
    """
    gone = {
        (package, name) for package, name in reference.top_level
        if len(name) > 2 and name not in project.declared.get(package, ())
    }
    by_package: dict[str, set[str]] = defaultdict(set)
    for package, name in gone:
        by_package[package].add(name)
    issues = []
    for path in project.kotlin:
        names = by_package.get(project.package_of[path])
        if not names:
            continue
        code = IMPORT.sub("", project.code[path])
        for name in sorted(names):
            if re.search(rf"(?<![\w.]){re.escape(name)}(?!\w)", code):
                issues.append(f"DANGLING {project.relative(path)}: {name}")
    return issues


def _catalogue(root: Path, project: Project) -> list[str]:
    path = root / "gradle/libs.versions.toml"
    try:
        toml = tomllib.loads(path.read_text(encoding="utf-8"))
    except (OSError, tomllib.TOMLDecodeError) as error:
        return [f"TOML {path.name}: {error}"]

    def normal(key: str) -> str:
        return re.sub(r"[-_]", ".", key)

    pools = {kind: {normal(key) for key in toml.get(kind, {})} for kind in ("libraries", "plugins", "bundles", "versions")}
    issues = []
    for kind in ("libraries", "plugins"):
        for name, entry in toml.get(kind, {}).items():
            version = entry.get("version") if isinstance(entry, dict) else None
            reference = version.get("ref") if isinstance(version, dict) else None
            if reference and normal(reference) not in pools["versions"]:
                issues.append(f"TOML {kind[:-1]} {name} refers to missing version {reference}")
    for name, members in toml.get("bundles", {}).items():
        for member in members:
            if normal(member) not in pools["libraries"]:
                issues.append(f"TOML bundle {name} names missing library {member}")

    prefixes = {"plugins.": "plugins", "bundles.": "bundles", "versions.": "versions"}
    for source in project.gradle:
        for match in re.finditer(r"(?<![\w])libs\.((?:\w+\.)*\w+)", project.code[source]):
            reference = match.group(1)
            kind = next((pool for prefix, pool in prefixes.items() if reference.startswith(prefix)), "libraries")
            key = reference.split(".", 1)[1] if kind != "libraries" else reference
            key = re.sub(r"\.(get|asProvider)$", "", key)
            if key not in pools[kind] and not any(item.startswith(key + ".") for item in pools[kind]):
                issues.append(f"LIBS {project.relative(source)}: libs.{reference}")
    for source in project.kotlin:
        if "build-logic" not in source.parts:
            continue
        for match in re.finditer(r'libs\.(findLibrary|findPlugin|findVersion|findBundle|library|plugin|version|bundle)\("([^"]+)"\)', project.text[source]):
            kind = next(pool for word, pool in (("ibrary", "libraries"), ("lugin", "plugins"), ("ersion", "versions"), ("undle", "bundles")) if word in match.group(1))
            if normal(match.group(2)) not in pools[kind]:
                issues.append(f"LIBS {project.relative(source)}: {match.group(0)}")
    return issues


def _conventions(root: Path, project: Project, package: str) -> list[str]:
    build = (root / "build-logic/convention/build.gradle.kts").read_text(encoding="utf-8")
    registered = set(re.findall(r'id\s*=\s*"([^"]+)"', build))
    classes = set(re.findall(r'implementationClass\s*=\s*"([^"]+)"', build))
    issues = []
    for name in sorted(classes):
        if name not in project.declared_in:
            issues.append(f"CONVENTION implementationClass {name} is not declared")
    for path in sorted((root / "build-logic/convention/src/main/kotlin").glob("*ConventionPlugin.kt")):
        if path.stem not in classes:
            issues.append(f"CONVENTION {path.stem} is never registered")
    applied = re.compile(r'(?:id|apply)\("(%s\.[^"]+)"\)' % re.escape(package))
    for path in project.gradle + [p for p in project.kotlin if "build-logic" in p.parts]:
        if path.name == "build.gradle.kts" and "build-logic" in path.parts:
            continue
        for match in applied.finditer(project.text[path]):
            if match.group(1) not in registered:
                issues.append(f"CONVENTION {project.relative(path)} applies unregistered {match.group(1)}")
    return issues


def _manifests(root: Path, project: Project, package: str) -> list[str]:
    issues = []
    for manifest in sorted(project_files(root, "AndroidManifest.xml")):
        module = manifest.parents[2]
        build = module / "build.gradle.kts"
        namespace = None
        if module.name == "app":
            namespace = package
        elif build.is_file():
            match = re.search(r'namespace\s*=\s*"([^"]+)"', build.read_text(encoding="utf-8"))
            namespace = match.group(1) if match else None
        text = manifest.read_text(encoding="utf-8")
        for match in re.finditer(
            r'<(activity|service|receiver|provider|application|activity-alias)\b[^>]*?android:name="([^"]+)"', text, re.S
        ):
            name = match.group(2)
            if name.startswith("."):
                if namespace is None:
                    continue
                name = namespace + name
            if not name.startswith(package):
                continue
            owner, _, simple = name.rpartition(".")
            if simple not in project.declared.get(owner, ()):
                issues.append(f"MANIFEST {project.relative(manifest)}: {match.group(1)} {name} is not declared")
    return issues


def _resources(root: Path, project: Project) -> list[str]:
    names: dict[str, set[str]] = defaultdict(set)
    issues = []
    for res in project_files(root, "src/*/res"):
        for directory in res.iterdir():
            if not directory.is_dir():
                continue
            kind = directory.name.split("-")[0]
            for path in directory.iterdir():
                if kind == "values":
                    try:
                        elements = ET.parse(path).getroot()
                    except ET.ParseError as error:
                        issues.append(f"XML {project.relative(path)}: {error}")
                        continue
                    for element in elements:
                        name = element.get("name")
                        if not name:
                            continue
                        tag = element.get("type", "item") if element.tag == "item" else element.tag
                        tag = "array" if tag in ("string-array", "integer-array", "array") else tag
                        if tag == "declare-styleable":
                            names["attr"].update(a.get("name").replace(".", "_") for a in element if a.get("name"))
                            tag = "styleable"
                        names[tag].add(name.replace(".", "_"))
                else:
                    names[kind].add(path.name.split(".")[0])
                    if path.suffix == ".xml" and kind != "raw":
                        try:
                            ET.parse(path)
                        except ET.ParseError as error:
                            issues.append(f"XML {project.relative(path)}: {error}")
    kinds = "string|plurals|drawable|mipmap|raw|color|dimen|font|xml|array|bool|integer|style|layout|anim"
    for path in project.kotlin:
        for match in re.finditer(rf"(?<![\w.])(?:\w+R|R)\.({kinds}|id)\.(\w+)", project.code[path]):
            if match.group(2) not in names.get(match.group(1), set()):
                issues.append(f"RES {project.relative(path)}: R.{match.group(1)}.{match.group(2)}")
    for path in project_files(root, "*.xml"):
        text = re.sub(r"<!--.*?-->", "", path.read_text(encoding="utf-8"), flags=re.S)
        for match in re.finditer(rf'"@(?!android:)\+?({kinds})/([\w.]+)"', text):
            name = match.group(2).replace(".", "_")
            # Glance's default layouts come from the library, not from any module here.
            if name not in names.get(match.group(1), set()) and not name.startswith("glance_"):
                issues.append(f"RES {project.relative(path)}: @{match.group(1)}/{match.group(2)}")
    return issues


#: Values tags whose resources the unused check covers, by the R class each lands in.
_TRACKED_VALUES = {
    "string": "string", "plurals": "plurals", "color": "color", "dimen": "dimen",
    "string-array": "array", "integer-array": "array", "array": "array",
}
_TRACKED_FILES = ("drawable", "raw", "xml", "font")
_TRACKED = "|".join(sorted(set(_TRACKED_VALUES.values()) | set(_TRACKED_FILES)))
_REFERENCES = (
    # Any `…R.kind.name`, since modules import each other's R under an alias. A stray match can
    # only hide an unused resource, never report a used one.
    re.compile(rf"R\.({_TRACKED})\.(\w+)"),
    re.compile(rf"@(?!android:)(?:[\w.]+:)?({_TRACKED})/([\w.]+)"),
)


def _references(text: str) -> set[tuple[str, str]]:
    return {
        (match.group(1), match.group(2).replace(".", "_"))
        for pattern in _REFERENCES
        for match in pattern.finditer(text)
    }


def _unused_resources(root: Path, project: Project, copy_kept: bool = False) -> list[str]:
    """
    Resources a module ships that nothing reaches: dead weight in the APK and, for strings, a line
    every language has to translate. A values resource that only another values resource names
    (the font-certificate arrays under their parent array, a colour aliased by another) counts as
    used only when that one is. With `copy_kept`, a project without UI components, strings and
    plurals are not counted: each screen's copy stays, translated, for the screen built over its
    placeholder, and release builds shrink away what is still unused.
    """
    declared: dict[tuple[str, str], str] = {}
    # What each values resource names, for every qualifier it is defined under.
    bodies: dict[tuple[str, str], list[str]] = defaultdict(list)
    roots = [project.code[path] for path in project.kotlin] + [project.text[path] for path in project.gradle]
    roots += [
        re.sub(r"<!--.*?-->", "", path.read_text(encoding="utf-8"), flags=re.S)
        for path in sorted(project_files(root, "*.xml"))
        if not (path.parent.parent.name == "res" and path.parent.name.split("-")[0] == "values")
    ]
    for res in sorted(project_files(root, "src/*/res")):
        main = res.parent.name == "main"
        for directory in sorted(d for d in res.iterdir() if d.is_dir()):
            kind = directory.name.split("-")[0]
            for path in sorted(p for p in directory.iterdir() if p.is_file()):
                if kind != "values":
                    if main and kind in _TRACKED_FILES:
                        declared.setdefault((kind, path.name.split(".")[0]), path.relative_to(root).as_posix())
                    continue
                try:
                    elements = ET.parse(path).getroot()
                except ET.ParseError:
                    continue  # `_resources` reports it.
                for element in elements:
                    body = ET.tostring(element, encoding="unicode")
                    tag = element.get("type", "item") if element.tag == "item" else element.tag
                    name = element.get("name")
                    if tag not in _TRACKED_VALUES or not name:
                        # Styles, attributes and the like are not checked here; what they name is used.
                        roots.append(body)
                        continue
                    key = (_TRACKED_VALUES[tag], name.replace(".", "_"))
                    bodies[key].append(body)
                    if main and directory.name == "values":
                        declared.setdefault(key, path.relative_to(root).as_posix())

    used: set[tuple[str, str]] = set()
    pending = [reference for text in roots for reference in _references(text)]
    while pending:
        key = pending.pop()
        if key not in used:
            used.add(key)
            pending.extend(reference for body in bodies.get(key, ()) for reference in _references(body))
    return [
        f"UNUSED-RES {where}: {kind}/{name}"
        for (kind, name), where in sorted(declared.items(), key=lambda item: (item[1], item[0]))
        if (kind, name) not in used and not (copy_kept and kind in ("string", "plurals"))
    ]


def _translations(root: Path) -> list[str]:
    issues = []
    for source in project_files(root, "src/main/res/values/strings.xml"):
        wanted = sorted(e.get("name") for e in ET.parse(source).getroot() if e.get("translatable") != "false")
        for translated in source.parent.parent.glob("values-*/strings.xml"):
            got = sorted(e.get("name") for e in ET.parse(translated).getroot())
            if got != wanted:
                issues.append(
                    f"TRANSLATION {translated.relative_to(root).as_posix()}: missing {sorted(set(wanted) - set(got))}, "
                    f"extra {sorted(set(got) - set(wanted))}"
                )
    return issues


def _hollow(root: Path, project: Project) -> list[str]:
    empty = (path for path in project_files(root, "*") if path.is_file() and path.stat().st_size == 0)
    issues = [f"EMPTY-FILE {project.relative(path)}" for path in empty]
    for path in project.gradle:
        for match in re.finditer(r"\b(dependencies|plugins|android)\s*\{\s*\}", project.code[path]):
            issues.append(f"EMPTY-BLOCK {project.relative(path)}: {match.group(1)} {{}}")
    # Every branch fenced out of a `when` over a sealed type leaves `when (x) {}`, which Kotlin
    # rejects as not exhaustive.
    for path in project.kotlin:
        for match in re.finditer(r"\bwhen\s*\((?:[^()]|\([^()]*\))*\)\s*\{\s*\}", project.code[path]):
            line = project.code[path].count("\n", 0, match.start()) + 1
            issues.append(f"EMPTY-WHEN {project.relative(path)}:{line}")
    return issues
