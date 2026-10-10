"""
The SwiftUI target, checked without a Swift compiler (MASTER-PLAN A12.4).

Every iOS preset, a scaffolded spec and each feature switched off on its own are generated and read
back: what is left of the template, the XcodeGen project, the package graph and its isolation, the
imports, balanced delimiters, forbidden patterns, String Catalogs, previews and folder rules. The
macOS CI job compiles the same projects; these checks catch what goes wrong between two of its runs.
"""

from __future__ import annotations

import json
import plistlib
import re
import tempfile
import unittest
from collections.abc import Callable
from dataclasses import dataclass, field
from pathlib import Path

from genkit import tokens
from genkit.core import pipeline, translations
from genkit.core.copy import is_under
from genkit.core.spec import SpecError
from genkit.targets import get
from genkit.targets.ios import scaffold, tabs
from genkit.targets.ios.target import bundle_id
from genkit.tokens import swift

try:
    import yaml
except ImportError:
    yaml = None

IOS = get("ios")
TEMPLATE = IOS.template_dir
FIXTURES = Path(__file__).resolve().parent / "fixtures" / "v2"

#: Modules any target may import without declaring them.
SYSTEM_MODULES = frozenset({
    "Accessibility", "CoreGraphics", "Foundation", "Network", "OSLog", "Observation", "SwiftData", "SwiftUI", "Testing",
    "UIKit", "os",
})

#: Apple's required-reason API categories the template's code uses, how each shows in Swift, and the
#: reason that matches that use. App Store Connect refuses a build that uses one of these APIs
#: without the category and a reason in the app's privacy manifest.
REQUIRED_REASON_APIS = {
    "NSPrivacyAccessedAPICategoryUserDefaults": (r"\bUserDefaults\b|@AppStorage\b", "CA92.1"),
}

#: Targets whose sources are interface code: main actor by default.
CORE_UI_TARGETS = frozenset({"DesignSystem", "Navigation"})
CORE_TARGETS = frozenset({"AppCore", "DesignSystem", "Navigation", "Networking", "Persistence"})


def raw_spec(**fields) -> dict:
    return {"schema": 2, "platform": "ios", "app_name": "Trail Notes", "package_name": "com.example.trail", **fields}


def read(**fields):
    return pipeline.read_spec(raw_spec(**fields), IOS)


def generate(directory: Path, raw: dict) -> tuple[Path, object]:
    parsed, _ = pipeline.read_spec(raw, IOS)
    project = directory / raw["app_name"].replace(" ", "")
    pipeline.build(parsed.spec, IOS, project, zip_output=False, commit="test")
    return project, parsed.spec


# Swift, read well enough to check it ----------------------------------------------------------

_STRING_START = re.compile(r'(#*)("""|")')


class SwiftSyntaxError(ValueError):
    pass


class Lexer:
    """
    Splits Swift into code and string literals. Comments (nested block comments too) are dropped and
    each literal becomes `"§n"` in the code, where `n` indexes `literals`; interpolations inside a
    literal are skipped with it.
    """

    def __init__(self, text: str):
        self.text = text
        self.i = 0
        self.out: list[str] = []
        self.literals: list[str] = []

    def run(self) -> tuple[str, list[str]]:
        self._code(self.out, interpolation=False)
        return "".join(self.out), self.literals

    def _code(self, out: list[str], interpolation: bool) -> None:
        text, depth = self.text, 0
        while self.i < len(text):
            if text.startswith("//", self.i):
                end = text.find("\n", self.i)
                self.i = len(text) if end < 0 else end
                continue
            if text.startswith("/*", self.i):
                self._block_comment()
                out.append(" ")
                continue
            match = _STRING_START.match(text, self.i)
            if match:
                self.i = match.end()
                # Numbered before it is read, so the strings of its interpolations come after it.
                index = len(self.literals)
                self.literals.append("")
                self.literals[index] = self._string(len(match.group(1)), match.group(2))
                out.append(f'"§{index}"')
                continue
            char = text[self.i]
            if interpolation:
                if char == "(":
                    depth += 1
                elif char == ")":
                    if depth == 0:
                        self.i += 1
                        return
                    depth -= 1
            out.append(char)
            self.i += 1
        if interpolation:
            raise SwiftSyntaxError("an interpolation is never closed")

    def _block_comment(self) -> None:
        depth = 0
        while self.i < len(self.text):
            if self.text.startswith("/*", self.i):
                depth += 1
                self.i += 2
            elif self.text.startswith("*/", self.i):
                depth -= 1
                self.i += 2
                if depth == 0:
                    return
            else:
                self.i += 1
        raise SwiftSyntaxError("a block comment is never closed")

    def _string(self, hashes: int, quote: str) -> str:
        text, close, escape = self.text, quote + "#" * hashes, "\\" + "#" * hashes
        value: list[str] = []
        while self.i < len(text):
            if text.startswith(close, self.i):
                self.i += len(close)
                return "".join(value)
            if quote == '"' and text[self.i] == "\n":
                raise SwiftSyntaxError("a string literal runs past the end of its line")
            if text.startswith(escape, self.i):
                self.i += len(escape)
                if text.startswith("(", self.i):
                    self.i += 1
                    self._code([], interpolation=True)
                    value.append("\\(…)")
                elif self.i < len(text):
                    value.append(text[self.i])
                    self.i += 1
                continue
            value.append(text[self.i])
            self.i += 1
        raise SwiftSyntaxError("a string literal is never closed")


def lex(path: Path) -> tuple[str, list[str]]:
    return Lexer(path.read_text(encoding="utf-8")).run()


def unbalanced(code: str) -> str | None:
    """The first delimiter that does not pair up, or None."""
    pairs, stack = {")": "(", "]": "[", "}": "{"}, []
    for line, text in enumerate(code.splitlines(), 1):
        for char in text:
            if char in "([{":
                stack.append((char, line))
            elif char in pairs:
                if not stack or stack[-1][0] != pairs[char]:
                    return f"'{char}' on line {line}"
                stack.pop()
    return f"'{stack[-1][0]}' from line {stack[-1][1]}" if stack else None


def imports(code: str) -> set[str]:
    return set(re.findall(r"^\s*(?:@testable\s+)?import\s+(?:(?:struct|class|enum|protocol|func)\s+)?(\w+)", code, re.M))


def call_arguments(code: str, start: int) -> str:
    """The text between the parenthesis at `start` and the one that closes it."""
    depth = 0
    for index in range(start, len(code)):
        if code[index] == "(":
            depth += 1
        elif code[index] == ")":
            depth -= 1
            if depth == 0:
                return code[start + 1 : index]
    raise SwiftSyntaxError("a call is never closed")


# Package.swift ---------------------------------------------------------------------------------

@dataclass
class PackageTarget:
    name: str
    test: bool
    dependencies: list[str] = field(default_factory=list)
    products: list[str] = field(default_factory=list)
    settings: str | None = None
    resources: bool = False


@dataclass
class Package:
    name: str
    products: dict[str, list[str]]
    targets: dict[str, PackageTarget]
    #: Each `let` of SwiftSettings → whether it sets the main actor as the default isolation.
    main_actor_settings: dict[str, bool]


def read_package(path: Path) -> Package:
    code, literals = lex(path)

    def literal(token: str) -> str:
        return literals[int(token)]

    settings = {
        name: ".defaultIsolation(MainActor.self)" in body
        for name, body in re.findall(r"let\s+(\w+):\s*\[SwiftSetting\]\s*=\s*\[(.*?)\n\]", code, re.S)
    }
    products = {
        literal(name): [literal(target) for target in re.findall(r'"§(\d+)"', targets)]
        for name, targets in re.findall(r'\.library\(\s*name:\s*"§(\d+)",\s*targets:\s*\[([^\]]*)\]', code)
    }
    targets: dict[str, PackageTarget] = {}
    for match in re.finditer(r"\.(target|testTarget)\(", code):
        body = call_arguments(code, match.end() - 1)
        name = literal(re.search(r'name:\s*"§(\d+)"', body).group(1))
        target = PackageTarget(name, test=match.group(1) == "testTarget")
        dependencies = re.search(r"dependencies:\s*\[", body)
        if dependencies:
            depth, end = 1, dependencies.end()
            for index in range(dependencies.end(), len(body)):
                depth += {"[": 1, "]": -1}.get(body[index], 0)
                if depth == 0:
                    end = index
                    break
            listed = body[dependencies.end() : end]
            target.products = [
                literal(name) for name, _ in re.findall(r'\.product\(\s*name:\s*"§(\d+)",\s*package:\s*"§(\d+)"\s*\)', listed)
            ]
            without_products = re.sub(r"\.product\([^)]*\)", "", listed)
            target.dependencies = [literal(token) for token in re.findall(r'"§(\d+)"', without_products)]
        setting = re.search(r"swiftSettings:\s*(\w+)", body)
        target.settings = setting.group(1) if setting else None
        target.resources = ".process(" in body
        targets[name] = target
    name = literals[int(re.search(r'Package\(\s*name:\s*"§(\d+)"', code).group(1))]
    return Package(name, products, targets, settings)


def main_actor_by_default(package: Package, target: PackageTarget) -> bool:
    return bool(target.settings and package.main_actor_settings.get(target.settings))


# The checks, over one generated project --------------------------------------------------------

def swift_files(directory: Path) -> list[Path]:
    return sorted(directory.rglob("*.swift")) if directory.is_dir() else []


def check_project(case: unittest.TestCase, project: Path, spec) -> None:
    """Every static check of A12.4, ADAPTIVE-PLAN §14 and STANDARDS-PLAN §2.12 on one project."""
    app = spec.pascal_name
    core = read_package(project / "Packages" / "Core" / "Package.swift")
    features = read_package(project / "Packages" / "Features" / "Package.swift")

    with case.subTest(check="no template text is left"):
        for path in sorted(project.rglob("*")):
            relative = path.relative_to(project).as_posix()
            case.assertNotRegex(relative, r"BaseApp|baseapp", relative)
            if path.is_file() and path.suffix not in (".png",):
                text = path.read_text(encoding="utf-8")
                for leftover in ("<opt:", "</opt", "<opt-else", "<generated:", "BaseApp", "com.base.app", "baseapp"):
                    case.assertNotIn(leftover, text, f"{relative} still has {leftover}")
                case.assertNotRegex(text, r"(?m)^\s*(//|#)~", f"{relative} has an else line left")

    with case.subTest(check="every Swift file is balanced"):
        for path in sorted(project.rglob("*.swift")):
            code, _ = lex(path)
            case.assertIsNone(unbalanced(code), path.relative_to(project).as_posix())

    with case.subTest(check="packages have the targets their folders have"):
        for package, root in ((core, project / "Packages" / "Core"), (features, project / "Packages" / "Features")):
            on_disk = {path.name for folder in ("Sources", "Tests") for path in (root / folder).iterdir() if path.is_dir()}
            case.assertEqual(on_disk, set(package.targets), f"{package.name}: folders and targets differ")
            for target in package.targets.values():
                folder = root / ("Tests" if target.test else "Sources") / target.name
                case.assertTrue(swift_files(folder), f"{target.name} has no sources")
                case.assertEqual((folder / "Resources").is_dir(), target.resources, f"{target.name}: Resources and .process differ")
                for product, members in package.products.items():
                    case.assertTrue(set(members) <= set(package.targets), f"{product} names a missing target")

    with case.subTest(check="the module graph"):
        for target in core.targets.values():
            if target.test:
                continue
            case.assertFalse(target.products)
            case.assertTrue(set(target.dependencies) <= CORE_TARGETS, target.name)
            if target.name not in CORE_UI_TARGETS:
                case.assertFalse(set(target.dependencies) & CORE_UI_TARGETS, f"{target.name} depends on interface code")
        core_products = set(core.products)
        for target in features.targets.values():
            case.assertTrue(set(target.products) <= core_products, f"{target.name} uses a product Core does not have")
            if target.test:
                continue
            if target.name.endswith("Data"):
                case.assertEqual([], target.dependencies, f"{target.name} depends on another Features target")
            else:
                own = f"{target.name.removesuffix('Feature')}Data"
                case.assertTrue(set(target.dependencies) <= {own}, f"{target.name} depends on {target.dependencies}")

    with case.subTest(check="only interface targets are main-actor by default"):
        for package in (core, features):
            for target in package.targets.values():
                subject = target.name.removesuffix("Tests")
                interface = subject in CORE_UI_TARGETS or subject.endswith("Feature")
                case.assertEqual(interface, main_actor_by_default(package, target), target.name)
        background = [project / "Packages" / "Core" / "Sources" / name for name in ("Networking", "Persistence")]
        background += [path for path in (project / "Packages" / "Features" / "Sources").iterdir() if path.name.endswith("Data")]
        for folder in background:
            for path in swift_files(folder):
                case.assertNotIn("@MainActor", lex(path)[0], path.relative_to(project).as_posix())

    with case.subTest(check="every import is declared, and every dependency imported"):
        for package, root in ((core, project / "Packages" / "Core"), (features, project / "Packages" / "Features")):
            for target in package.targets.values():
                declared = set(target.dependencies) | set(target.products)
                imported: set[str] = set()
                for path in swift_files(root / ("Tests" if target.test else "Sources") / target.name):
                    found = imports(lex(path)[0])
                    imported |= found
                    undeclared = found - SYSTEM_MODULES - {target.name} - declared
                    case.assertFalse(undeclared, f"{path.relative_to(project)} imports {sorted(undeclared)}")
                case.assertFalse(declared - imported, f"{target.name} never imports {sorted(declared - imported)}")

    with case.subTest(check="forbidden patterns"):
        forbidden = {
            "ObservableObject": r"\bObservableObject\b",
            "@Published": r"@Published\b",
            "try!": r"\btry!",
            "print(": r"(?<![\w.])print\(",
            "AnyView": r"\bAnyView\b",
            "UIScreen.main": r"\bUIScreen\.main\b",
            "DispatchQueue.main.async": r"\bDispatchQueue\.main\.async",
            "UIRequiresFullScreen": r"\bUIRequiresFullScreen\b",
        }
        for path in sorted(project.rglob("*.swift")):
            code = lex(path)[0]
            relative = path.relative_to(project).as_posix()
            for name, pattern in forbidden.items():
                case.assertNotRegex(code, pattern, f"{relative} uses {name}")
            if relative.startswith("Packages/Features/Sources/"):
                case.assertNotRegex(code, r"\buserInterfaceIdiom\b", f"{relative} decides layout by device")
                for width in re.findall(r"\.frame\(\s*(?:width|minWidth):\s*(\d+)", code):
                    case.assertLess(int(width), 320, f"{relative} sets a width a window can be narrower than")

    with case.subTest(check="the XcodeGen project"):
        check_project_yml(case, project, spec, features)

    with case.subTest(check="the privacy manifest"):
        check_privacy_manifest(case, project, app)

    with case.subTest(check="String Catalogs"):
        check_catalogs(case, project, spec)

    with case.subTest(check="every feature view has an iPad preview"):
        for path in sorted((project / "Packages" / "Features" / "Sources").rglob("*View.swift")):
            code, _ = lex(path)
            widths = [int(width) for width in re.findall(r"#Preview\([^)]*traits:\s*\.fixedLayout\(width:\s*(\d+)", code)]
            case.assertTrue(any(width >= 744 for width in widths), f"{path.relative_to(project)} has no iPad #Preview")

    with case.subTest(check="folders are concepts, stores sit beside their views"):
        check_folders(case, project)

    with case.subTest(check="the tokens carry the spec's choices"):
        emitted = (project / swift.RELATIVE_PATH).read_text(encoding="utf-8")
        case.assertIn(f"static let designStyle = AppDesignStyle.{spec.design_style[0].lower()}{spec.design_style[1:]}\n", emitted)
        case.assertIn(f"static let motionStyle = AppMotionStyle.{spec.motion_style[0].lower()}{spec.motion_style[1:]}\n", emitted)
        accent = (project / app / swift.ACCENT_COLOR_PATH).read_text(encoding="utf-8")
        case.assertEqual(swift.accent_colorset(tokens.resolve(spec, "ios")), accent, "the asset catalog's AccentColor")

    app_code = "\n".join(lex(path)[0] for path in swift_files(project / app))
    case.assertIn(f"struct {app}App: App", app_code)
    if yaml is not None:
        with case.subTest(check="the app links what it imports"):
            linked = {
                product
                for dependency in project_yaml(project)["targets"][app]["dependencies"]
                for product in dependency.get("products", [])
            }
            case.assertFalse(imports(app_code) - SYSTEM_MODULES - linked)


def project_yaml(project: Path) -> dict:
    return yaml.safe_load((project / "project.yml").read_text(encoding="utf-8"))


def check_project_yml(case: unittest.TestCase, project: Path, spec, features: Package) -> None:
    text = (project / "project.yml").read_text(encoding="utf-8")
    # Without it, ProMotion iPhones hold custom SwiftUI animations to 60 Hz.
    case.assertRegex(text, r"(?m)^\s+CADisableMinimumFrameDurationOnPhone: true$")
    case.assertIsNone(re.search(r"(?m)^\s*UIRequiresFullScreen:", text), "project.yml opts out of resizable windows")
    if yaml is None:
        print("PyYAML is not installed: project.yml was checked as text only.")
        return
    data = yaml.safe_load(text)
    app = spec.pascal_name
    ios = spec.ios
    case.assertEqual(app, data["name"])
    case.assertEqual({app, f"{app}Tests"}, set(data["targets"]))
    case.assertEqual({"Core": {"path": "Packages/Core"}, "Features": {"path": "Packages/Features"}}, data["packages"])
    case.assertEqual(ios.deployment_target, data["options"]["deploymentTarget"]["iOS"])
    case.assertEqual(ios.team_id, data["settings"]["base"]["DEVELOPMENT_TEAM"])

    target = data["targets"][app]
    case.assertEqual("application", target["type"])
    case.assertEqual([{"path": app, "type": "syncedFolder"}], target["sources"])
    case.assertEqual(bundle_id(spec), target["settings"]["base"]["PRODUCT_BUNDLE_IDENTIFIER"])
    case.assertEqual({"Debug": "Config/Dev.xcconfig", "Release": "Config/Prod.xcconfig"}, target["configFiles"])
    case.assertFalse(target["info"]["path"].startswith(f"{app}/"), "Info.plist inside the synced folder is copied as a resource too")

    properties = target["info"]["properties"]
    case.assertIs(True, properties["CADisableMinimumFrameDurationOnPhone"])
    case.assertEqual({}, properties["UILaunchScreen"])
    case.assertNotIn("UIRequiresFullScreen", properties)
    case.assertEqual(
        {"UIInterfaceOrientationPortrait", "UIInterfaceOrientationPortraitUpsideDown",
         "UIInterfaceOrientationLandscapeLeft", "UIInterfaceOrientationLandscapeRight"},
        set(properties["UISupportedInterfaceOrientations~ipad"]),
    )
    case.assertEqual(spec.label, properties["CFBundleDisplayName"])
    case.assertEqual(["en", *spec.languages], properties["CFBundleLocalizations"])
    case.assertEqual(spec.lower_name, properties["CFBundleURLTypes"][0]["CFBundleURLSchemes"][0])

    linked = {dependency["package"]: dependency["products"] for dependency in target["dependencies"]}
    case.assertEqual(scaffold.feature_products(spec), linked["Features"])
    case.assertTrue(set(linked["Features"]) <= set(features.products))
    case.assertEqual("Networking" in linked["Core"], spec.has("network"))
    case.assertEqual([{"target": app}], data["targets"][f"{app}Tests"]["dependencies"])


def check_privacy_manifest(case: unittest.TestCase, project: Path, app: str) -> None:
    manifest = plistlib.loads((project / app / "PrivacyInfo.xcprivacy").read_bytes())
    case.assertIs(False, manifest["NSPrivacyTracking"])
    declared = {
        entry["NSPrivacyAccessedAPIType"]: entry["NSPrivacyAccessedAPITypeReasons"]
        for entry in manifest["NSPrivacyAccessedAPITypes"]
    }
    shipped = [project / app, project / "Packages" / "Core" / "Sources", project / "Packages" / "Features" / "Sources"]
    code = "\n".join(lex(path)[0] for folder in shipped for path in swift_files(folder))
    for category, (pattern, reason) in REQUIRED_REASON_APIS.items():
        if re.search(pattern, code):
            case.assertIn(reason, declared.get(category, []), f"the privacy manifest does not declare {category}")


def catalog_keys_used(code: str, literals: list[str]) -> set[tuple[str, str]]:
    """(table, key) for every lookup in `bundle: .module` form."""
    used = set()
    pattern = r'(?:Text|String)\((?:localized:\s*)?"§(\d+)"(?:,\s*tableName:\s*"§(\d+)")?,\s*bundle:\s*\.module\)'
    for key, table in re.findall(pattern, code):
        used.add((literals[int(table)] if table else "Localizable", literals[int(key)]))
    return used


def check_catalogs(case: unittest.TestCase, project: Path, spec) -> None:
    from genkit.targets.ios import strings

    user_names = scaffold.user_names(spec)
    catalogs = {}
    for path in sorted(project.rglob("*.xcstrings")):
        data = json.loads(path.read_text(encoding="utf-8"))
        case.assertEqual("en", data["sourceLanguage"])
        identity = strings.catalog_id(project, path)
        for key, entry in data["strings"].items():
            shipped = set(entry.get("localizations", {}))
            expected = set() if key in user_names.get(identity, set()) else set(spec.languages)
            case.assertEqual(expected, shipped, f"{identity}: '{key}'")
        catalogs[path] = set(data["strings"])

    for package in ("Core", "Features"):
        for target in (project / "Packages" / package / "Sources").iterdir():
            used = set()
            for path in swift_files(target):
                used |= catalog_keys_used(*lex(path))
            for table, key in used:
                catalog = target / "Resources" / f"{table}.xcstrings"
                case.assertIn(key, catalogs.get(catalog, set()), f"{target.name}: '{key}' is not in {table}.xcstrings")

    app_catalog = catalogs[project / spec.pascal_name / "Localizable.xcstrings"]
    case.assertEqual(set(tabs.app_strings(spec)), app_catalog)


_FEATURE_FOLDERS_BANNED = {
    "Views", "ViewModels", "Stores", "Models", "Utils", "Utilities", "Helpers", "Common", "UI",
    "Presentation", "Screens", "Extensions", "Managers",
}


def check_folders(case: unittest.TestCase, project: Path) -> None:
    sources = project / "Packages" / "Core" / "Sources"
    allowed = {
        sources / "DesignSystem": {"Theme", "Tokens", "Foundation", "Components", "Resources"},
        sources / "DesignSystem" / "Foundation": {"Interaction", "Scroll", "Adaptive", "Glass"},
        sources / "Navigation": {"Shell", "Router", "DeepLinks"},
    }
    for folder, names in allowed.items():
        present = {path.name for path in folder.iterdir() if path.is_dir()}
        case.assertTrue(present <= names, f"{folder.relative_to(project)}: {sorted(present - names)}")
        case.assertFalse([path for path in folder.iterdir() if path.suffix == ".swift"], f"loose files in {folder.name}")

    for target in (project / "Packages" / "Features" / "Sources").iterdir():
        folders = {path.relative_to(target).as_posix() for path in target.rglob("*") if path.is_dir()}
        if target.name.endswith("Data"):
            case.assertTrue(folders <= {"API", "Remote", "Remote/DTO", "Local"}, f"{target.name}: {sorted(folders)}")
            continue
        case.assertFalse({Path(folder).name for folder in folders} & _FEATURE_FOLDERS_BANNED, f"{target.name}: {sorted(folders)}")
        for store in target.rglob("*Store.swift"):
            view = store.with_name(store.name.removesuffix("Store.swift") + "View.swift")
            case.assertTrue(view.is_file(), f"{store.relative_to(project)} has no view beside it")

    for path in sorted((project / "Packages").rglob("*.swift")):
        if path.parent.name == "Tokens":
            continue
        code, _ = lex(path)
        public_types = re.findall(r"(?m)^public\s+(?:nonisolated\s+|final\s+)*(?:struct|class|enum|protocol|actor)\s", code)
        case.assertLessEqual(len(public_types), 1, f"{path.relative_to(project)} declares {len(public_types)} public types")


# The tests -------------------------------------------------------------------------------------

class LexerTest(unittest.TestCase):

    def test_comments_and_strings_do_not_count_as_code(self):
        code, literals = Lexer('let a = "(" // )\n/* { /* nested } */ */ let b = #"\\("x")"#\n').run()
        self.assertIsNone(unbalanced(code))
        self.assertEqual(["(", '\\("x")'], literals)

    def test_interpolations_hold_strings_of_their_own(self):
        code, literals = Lexer('Text("a \\(f("b)")) c")').run()
        self.assertEqual('Text("§0")', code)
        self.assertEqual(["a \\(…) c", "b)"], literals)

    def test_multi_line_strings_and_raw_interpolation(self):
        code, _ = Lexer('let s = """\n  ) "quoted" \\(x)\n  """\nlet r = #"\\#(y) )"#').run()
        self.assertIsNone(unbalanced(code))

    def test_it_reports_what_does_not_pair_up(self):
        self.assertEqual("')' on line 2", unbalanced("f(\n))"))
        self.assertEqual("'(' from line 1", unbalanced("{ ("))
        with self.assertRaises(SwiftSyntaxError):
            Lexer('let s = "never closed\n').run()


class ValidationTest(unittest.TestCase):

    def assertRefused(self, field: str, **fields):
        with self.assertRaises(SpecError) as caught:
            read(**fields)
        self.assertEqual(field, caught.exception.field, str(caught.exception))

    def test_bundle_ids(self):
        self.assertEqual("com.example.trail", bundle_id(read()[0].spec))
        self.assertEqual("com.example.my-trail", bundle_id(read(package_name="com.example.my_trail")[0].spec))
        self.assertEqual("com.Acme.trail-2", bundle_id(read(ios={"bundle_id": "com.Acme.trail-2"})[0].spec))
        for bad in ("com.example.my_trail", "trail", "com..trail", "com.example.trail.", f"com.{'a' * 160}"):
            with self.subTest(bundle_id=bad):
                self.assertRefused("ios.bundle_id", ios={"bundle_id": bad})

    def test_deployment_targets(self):
        for good in ("17.0", "17.4", "18.2", "26.0", "27.1"):
            with self.subTest(target=good):
                self.assertEqual(good, read(ios={"deployment_target": good})[0].spec.ios.deployment_target)
        for bad in ("16.4", "19.0", "28.0", "17", "17.0.1", "iOS 17"):
            with self.subTest(target=bad):
                self.assertRefused("ios.deployment_target", ios={"deployment_target": bad})

    def test_team_ids(self):
        self.assertEqual("ABCDE12345", read(ios={"team_id": "ABCDE12345"})[0].spec.ios.team_id)
        for bad in ("abcde12345", "ABCDE1234", "ABCDE-1234"):
            with self.subTest(team_id=bad):
                self.assertRefused("ios.team_id", ios={"team_id": bad})

    def test_names_swift_cannot_take(self):
        self.assertRefused("feature_modules", feature_modules=["repeat"])
        self.assertRefused("feature_modules", feature_modules=["core"])
        self.assertRefused("feature_modules", feature_modules=["sample"])
        self.assertRefused("app_name", app_name="SwiftUI")
        self.assertRefused("app_name", app_name="Design System")
        self.assertRefused("app_name", app_name="Orders Feature", feature_modules=["orders"])
        self.assertRefused("display_name", display_name="Cash $(HOME)")

    def test_settings_the_platform_does_not_have(self):
        self.assertRefused("architecture", architecture="modular")
        self.assertRefused("android", android={"min_sdk": 26})
        self.assertRefused("languages", languages=["ja"])

    def test_a_spec_without_a_tab_gets_a_home_module(self):
        parsed, warnings = read(features=["network"])
        self.assertEqual(1, len(warnings))
        self.assertEqual(("home",), tabs.tab_modules(parsed.spec))
        self.assertEqual(["Home"], tabs.labels(parsed.spec))
        self.assertEqual([], read(features=["network", "sample"])[1])

    def test_the_language_shortcut_without_a_second_language_is_flagged(self):
        features = ["network", "sample", "settings", "language"]
        self.assertRegex(" ".join(read(features=features)[1]), "ships more than one")
        self.assertEqual([], read(features=features, languages=["de"])[1])


class TemplateTest(unittest.TestCase):
    """The template as checked in: it is itself a project with every feature on."""

    def test_the_tabs_region_is_what_the_target_writes_with_every_feature_on(self):
        spec = read(features=sorted(IOS.manifest.features))[0].spec
        text = (TEMPLATE / "BaseApp" / "AppTabs.swift").read_text(encoding="utf-8")
        region = text.split("// <generated:shell-tabs>\n", 1)[1].split("// </generated:shell-tabs>", 1)[0]
        self.assertEqual("".join(tabs.app_tabs_swift(spec)), region)

    def test_springs_and_treatments_the_swift_reads_exist_in_the_tokens(self):
        data = tokens.load()
        springs_swift = (TEMPLATE / "Packages/Core/Sources/DesignSystem/Theme/AppSprings.swift").read_text(encoding="utf-8")
        self.assertTrue(set(re.findall(r"motion\.(\w+)\.spring", springs_swift)) <= set(data["motion"]["springs"]))
        theme = TEMPLATE / "Packages/Core/Sources/DesignSystem/Theme"
        for enum, key in (("CardTreatment", "card"), ("FieldTreatment", "field"), ("TypeVoice", "voice")):
            cases = set(re.findall(r'case \w+ = "(\w+)"', (theme / f"{enum}.swift").read_text(encoding="utf-8")))
            with self.subTest(enum=enum):
                self.assertTrue({style[key] for style in data["styles"].values()} <= cases)

    def test_the_accent_colour_is_emitted_from_the_tokens(self):
        checked_in = (TEMPLATE / "BaseApp" / swift.ACCENT_COLOR_PATH).read_text(encoding="utf-8")
        self.assertEqual(swift.accent_colorset(tokens.resolve(None, "ios")), checked_in)
        brand = json.loads(swift.accent_colorset(tokens.resolve(read(accent_colour="#1F7A5C")[0].spec, "ios")))
        light = brand["colors"][0]["color"]["components"]
        self.assertEqual(("0.122", "0.478", "0.361"), (light["red"], light["green"], light["blue"]))
        self.assertEqual([{"appearance": "luminosity", "value": "dark"}], brand["colors"][1]["appearances"])

    def test_every_language_translates_exactly_the_templates_keys(self):
        from genkit.targets.ios import strings

        catalogs = {strings.catalog_id(TEMPLATE, path): set(strings.keys(path)) for path in TEMPLATE.rglob("*.xcstrings")}
        for tag in translations.available("ios"):
            translation = translations.load("ios", tag)
            with self.subTest(language=tag):
                self.assertEqual(set(catalogs), set(translation.modules))
                for identity, keys in catalogs.items():
                    self.assertEqual(keys, set(translation.modules[identity]), identity)
                self.assertEqual(set(scaffold._LIST_STRINGS), set(translation.scaffold))

    def test_the_offered_languages_match_android(self):
        self.assertEqual(translations.available("android"), translations.available("ios"))

    def test_no_test_outlives_the_source_it_tests(self):
        """
        A feature that removes a source file has to take its tests with it: `<Name>Tests.swift` in
        `Tests/<Target>Tests/` tests `<Name>.swift` anywhere in `Sources/<Target>/`. Nothing here
        compiles a generated project, so a test left behind would first fail on the Mac.
        """
        manifest = IOS.manifest
        tests = []
        for path in sorted(TEMPLATE.glob("Packages/*/Tests/*Tests/**/*Tests.swift")):
            _, package, _, folder, *_ = path.relative_to(TEMPLATE).parts
            sources = TEMPLATE / "Packages" / package / "Sources" / folder.removesuffix("Tests")
            tested = [sources, *sources.rglob(path.name.removesuffix("Tests.swift") + ".swift")]
            tests.append((path.relative_to(TEMPLATE).as_posix(), [item.relative_to(TEMPLATE).as_posix() for item in tested]))
        self.assertTrue(tests)
        for key in manifest.features:
            enabled = frozenset(other for other in manifest.features if key not in manifest.resolve({other}))
            owned = manifest.owned_paths(enabled)
            for test, tested in tests:
                if is_under(test, owned):
                    continue
                with self.subTest(feature=key, test=test):
                    self.assertEqual([], [path for path in tested if is_under(path, owned)], f"'{key}' removes what this test reads")


class GeneratedProjectsTest(unittest.TestCase):
    """Every preset, the fixtures, and every feature switched off on its own."""

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.root = Path(cls.temp.name)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def check(self, name: str, raw: dict) -> tuple[Path, object]:
        project, spec = generate(self.root / name, raw)
        check_project(self, project, spec)
        return project, spec

    def test_every_preset(self):
        for preset in IOS.manifest.presets:
            with self.subTest(preset=preset):
                self.check(f"preset-{preset}", raw_spec(features=sorted(IOS.manifest.preset_features(preset)), languages=["de"]))

    def test_the_fixtures(self):
        for path in sorted(FIXTURES.glob("ios-*.json")):
            with self.subTest(fixture=path.name):
                self.check(path.stem, json.loads(path.read_text(encoding="utf-8")))

    def test_each_feature_switched_off_alone(self):
        every = set(IOS.manifest.features)
        for key in sorted(every):
            enabled = sorted(other for other in every if key not in IOS.manifest.resolve({other}))
            with self.subTest(without=key):
                self.check(f"without-{key}", raw_spec(features=enabled, languages=["fr"]))

    def test_nothing_but_a_scaffolded_module_without_networking(self):
        project, _ = self.check("bare", raw_spec(features=[], feature_modules=["field_notes"], languages=["hi"]))
        repository = project / "Packages/Features/Sources/FieldNotesData/DefaultFieldNotesRepository.swift"
        self.assertIn(".success([])", repository.read_text(encoding="utf-8"))
        self.assertNotIn("Networking", (project / "Packages/Core/Package.swift").read_text(encoding="utf-8"))

    def test_with_no_tab_feature_a_home_module_is_the_tab(self):
        project, _ = self.check("home", raw_spec(features=["network", "onboarding"]))
        app_tabs = (project / "TrailNotes" / "AppTabs.swift").read_text(encoding="utf-8")
        self.assertIn("case home\n", app_tabs)
        self.assertIn("HomeRootView(client: model.services.apiClient)", app_tabs)

    def test_scaffolded_tabs_each_get_their_own_symbol(self):
        modules = [f"module_{index}" for index in range(12)]
        project, spec = self.check("twelve", raw_spec(features=["network", "sample", "settings"], feature_modules=modules))
        symbols = re.findall(r'case \.\w+: "([a-z0-9.]+)"\n', (project / "TrailNotes" / "AppTabs.swift").read_text(encoding="utf-8"))
        self.assertEqual(len(symbols), len(set(symbols)))
        self.assertEqual(["Home", *(f"Module {index}" for index in range(12)), "Settings"], tabs.labels(spec))

    def test_a_display_name_with_quotes_and_emoji_reaches_the_home_screen(self):
        if yaml is None:
            self.skipTest("PyYAML is not installed")
        label = 'Café "Ünï" 🚀 #1: a'
        project, _ = self.check("label", raw_spec(features=["network", "sample"], display_name=label))
        self.assertEqual(label, project_yaml(project)["targets"]["TrailNotes"]["info"]["properties"]["CFBundleDisplayName"])

    def test_headless_generation_names_the_target(self):
        from genkit.core import headless
        import io

        out = io.StringIO()
        code = headless.run(
            ["--target", "ios", str(self.root / "headless.zip")],
            io.BytesIO(json.dumps(raw_spec(features=["network", "sample"])).encode()),
            out,
        )
        result = json.loads(out.getvalue())
        self.assertEqual(0, code, result)
        self.assertEqual("ios", result["target"])
        self.assertEqual("TrailNotes", result["project_name"])


def replacing(old: str, new: str) -> Callable[[str], str]:
    """An edit that swaps the one occurrence of `old` for `new`."""

    def edit(text: str) -> str:
        if text.count(old) != 1:
            raise AssertionError(f"expected {old!r} once, found it {text.count(old)} times")
        return text.replace(old, new)

    return edit


class ViolationsTest(unittest.TestCase):
    """Each check fails on a project that breaks its rule, so no check can quietly stop firing."""

    ROW = "Packages/Features/Sources/SampleFeature/Components/SampleRow.swift"
    ROW_PADDING = ".padding(.vertical, AppSpacing.xxs)"
    FEATURES = "Packages/Features/Package.swift"

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.project, cls.spec = generate(Path(cls.temp.name), raw_spec(features=sorted(IOS.manifest.features)))

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def assertCaught(self, message: str, path: str, edit: Callable[[str], str]) -> None:
        """Applies `edit` to `path` (to empty text for a file that does not exist), checks the
        project, expects a failure matching `message`, and puts the project back."""
        file = self.project / path
        original = file.read_text(encoding="utf-8") if file.exists() else None
        created = [parent for parent in reversed(file.parents) if not parent.exists()]
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(edit(original or ""), encoding="utf-8")
        try:
            # Outside a running test, a sub-test's failure is raised rather than recorded.
            with self.assertRaisesRegex(AssertionError, message):
                check_project(unittest.TestCase(), self.project, self.spec)
        finally:
            if original is None:
                file.unlink()
                for folder in reversed(created):
                    folder.rmdir()
            else:
                file.write_text(original, encoding="utf-8")

    def test_the_unchanged_project_passes(self):
        check_project(self, self.project, self.spec)

    def test_layout_rules(self):
        padded = self.ROW_PADDING
        for message, path, edit in (
            ("sets a width", self.ROW, replacing(padded, f"{padded}\n        .frame(width: 400)")),
            ("sets a width", self.ROW, replacing(padded, f"{padded}\n        .frame(minWidth: 375)")),
            (
                "decides layout by device",
                self.ROW,
                replacing(padded, ".padding(.vertical, UIDevice.current.userInterfaceIdiom == .pad ? AppSpacing.xs : AppSpacing.xxs)"),
            ),
            (
                "has no iPad #Preview",
                "Packages/Features/Sources/SampleFeature/List/SampleListView.swift",
                replacing('#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834))', '#Preview("Wide")'),
            ),
            ("opts out of resizable windows", "project.yml", replacing("UILaunchScreen: {}", "UILaunchScreen: {}\n        UIRequiresFullScreen: true")),
            ("CADisableMinimumFrameDurationOnPhone", "project.yml", replacing("FrameDurationOnPhone: true", "FrameDurationOnPhone: false")),
            (
                "does not declare NSPrivacyAccessedAPICategoryUserDefaults",
                "TrailNotes/PrivacyInfo.xcprivacy",
                replacing("NSPrivacyAccessedAPICategoryUserDefaults", "NSPrivacyAccessedAPICategoryDiskSpace"),
            ),
        ):
            with self.subTest(rule=message, edit=path):
                self.assertCaught(message, path, edit)

    def test_every_ipad_orientation_stays(self):
        if yaml is None:
            self.skipTest("PyYAML is not installed")
        self.assertCaught(
            "UIInterfaceOrientationPortraitUpsideDown",
            "project.yml",
            replacing("          - UIInterfaceOrientationPortraitUpsideDown\n", ""),
        )

    def test_graph_and_isolation_rules(self):
        for message, path, edit in (
            ("SampleFeature depends on", self.FEATURES, replacing('name: "SampleFeature",\n            dependencies: [\n', 'name: "SampleFeature",\n            dependencies: [\n                "SettingsFeature",\n')),
            (r"imports \['SettingsFeature'\]", self.ROW, replacing("import SwiftUI\n", "import SettingsFeature\nimport SwiftUI\n")),
            ("SampleAPI.swift", "Packages/Features/Sources/SampleData/Remote/SampleAPI.swift", replacing("struct SampleAPI", "@MainActor\nstruct SampleAPI")),
            (
                "False != True : SampleData$",
                self.FEATURES,
                lambda text: text.replace("swiftSettings: dataSettings", "swiftSettings: featureSettings", 1),
            ),
        ):
            with self.subTest(rule=message):
                self.assertCaught(message, path, edit)

    def test_code_rules(self):
        store = "Packages/Features/Sources/SampleFeature/List/SampleListStore.swift"
        for message, path, edit in (
            ("uses ObservableObject", store, replacing("final class SampleListStore {", "final class SampleListStore: ObservableObject {")),
            (r"uses print\(", store, replacing("    func send(_ event: Event) {\n", "    func send(_ event: Event) {\n        print(event)\n")),
            ("is not in Localizable.xcstrings", self.ROW, replacing(f"{self.ROW_PADDING}\n", f"{self.ROW_PADDING}\n        .help(Text(\"Words no catalog has\", bundle: .module))\n")),
            ("declares 2 public types", "Packages/Features/Sources/SampleData/API/SampleItem.swift", lambda text: text + "\npublic struct SampleTag: Sendable {}\n"),
            ("'Views'", "Packages/Features/Sources/SampleFeature/Views/SampleBadge.swift", lambda _: "struct SampleBadge {}\n"),
            ("has no view beside it", "Packages/Features/Sources/SampleFeature/Detail/SampleShareStore.swift", lambda _: "final class SampleShareStore {}\n"),
        ):
            with self.subTest(rule=message):
                self.assertCaught(message, path, edit)


if __name__ == "__main__":
    unittest.main()
