"""
Projects generated without UI components ("ui_components": false): what they hold and lack, and the
project scripts working on them.
"""

from __future__ import annotations

import io
import json
import re
import shutil
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

from genkit.core import pipeline
from genkit.core.record import RECORD_NAME
from genkit.targets import get

ANDROID = get("android")
PACKAGE = "com.acme.noui"
PKG = PACKAGE.replace(".", "/")


def generate(directory: Path, name: str, **fields) -> Path:
    raw = {"schema": 2, "platform": "android", "app_name": "Acme NoUi", "package_name": PACKAGE, **fields}
    project = directory / name
    pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
    return project


def snapshot(project: Path) -> dict[str, bytes]:
    return {path.relative_to(project).as_posix(): path.read_bytes() for path in project.rglob("*") if path.is_file()}


#: Every preset in both architectures, and no features with a module: the shapes a No-UI project takes.
SHAPES = {
    **{
        f"{preset}-{architecture}": {
            "features": sorted(ANDROID.manifest.preset_features(preset)), "architecture": architecture,
        }
        for preset in ("lean", "standard", "everything")
        for architecture in ("compact", "modular")
    },
    "none-modules": {"features": [], "feature_modules": ["orders"]},
}

OPTION = ANDROID.manifest.options["ui_components"]

#: The names in the rules' advice that libraries declare, not the project (and a word that only looks
#: like code). Every other name the advice cites must be the project's own, so a rule never sends a
#: reader to a component, token or helper the project does not have.
ADVICE_LIBRARY_NAMES = frozenset({
    # Kotlin and coroutines
    "checkNotNull", "runCatching", "GlobalScope",
    # Compose
    "BoxWithConstraints", "CompositingStrategy", "ModulateAlpha", "FlexBox", "LazyVerticalGrid", "MediaQuery",
    "ModifierNodeElement", "collectAsState", "contentPadding", "contentType", "drawLine", "drawRect", "fillMax",
    "flingBehavior", "horizontalScroll", "stringResource", "verticalScroll", "widthIn",
    # Lifecycle, Navigation 3, navigationevent, Material 3 adaptive, Coil
    "collectAsStateWithLifecycle", "ViewModel", "ViewModels", "NavDisplay", "SceneStrategy", "NavigationBackHandler",
    "NavigationSuiteScaffold", "AsyncImage", "SubcomposeAsyncImage",
    # Words
    "iOS",
})
_ADVICE = re.compile(r'advice\s*=\s*((?:"(?:[^"\\]|\\.)*"\s*\+?\s*)+)')
_ADVICE_PART = re.compile(r'"((?:[^"\\]|\\.)*)"')
_RULE_SETS = re.compile(r"val ALL: List<Rule> =([^\n]*\n(?:[ \t]+[^\n]*\n)*)")
_THEME_MEMBER = re.compile(r"\bAppTheme\.(\w+)")
_ANNOTATION = re.compile(r"@([A-Z]\w*)")
#: Code-shaped names: two or more humps, PascalCase or camelCase; an acronym (UI, GPU, TODO) is not one.
_CODE_NAME = re.compile(r"(?<![\w@])(?=\w*[a-z])([A-Za-z][a-z0-9]*(?:[A-Z][a-z0-9]*)+)\b")


def active_advice(project: Path) -> list[str]:
    """The advice of every rule in the project's CodeRules.ALL."""
    guard = project / f"build-logic/convention/src/main/kotlin/{PKG}/buildlogic/guard"
    sets = _RULE_SETS.search((guard / "CodeRules.kt").read_text(encoding="utf-8")).group(1)
    return [
        "".join(_ADVICE_PART.findall(match.group(1)))
        for name in re.findall(r"(\w+)\.ALL", sets)
        for match in _ADVICE.finditer((guard / f"{name}.kt").read_text(encoding="utf-8"))
    ]


class ProjectShapeTest(unittest.TestCase):
    """What every shape of No-UI project holds and lacks."""

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.projects = {
            name: generate(Path(cls.temp.name), name, ui_components=False, **fields) for name, fields in SHAPES.items()
        }

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_nothing_the_option_owns_is_there(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                present = [owned for owned in OPTION.owns if (project / owned.format(pkg_path=PKG)).exists()]
                self.assertEqual([], present)

    def test_every_variant_file_is_there(self):
        variants = ANDROID.manifest.root.parent.parent / "generator" / "variants" / OPTION.variant
        files = [path.relative_to(variants) for path in variants.rglob("*") if path.is_file()]
        self.assertTrue(files)
        for name, project in self.projects.items():
            with self.subTest(project=name):
                names = {path.name for path in project.rglob("*") if path.is_file()}
                for relative in files:
                    module = project / relative.as_posix().split("/src/")[0]
                    # A variant of a feature's module comes only with that feature: devtools, performance, sample.
                    if module.exists() or (project / relative.parts[0] / relative.parts[1] / "impl").exists():
                        self.assertIn(relative.name, names, relative.as_posix())

    def test_no_source_reaches_the_components_the_primitives_or_the_shell(self):
        from tests.consistency.checks import NO_UI_FORBIDDEN
        from tests.consistency.project import Project

        for name, project in self.projects.items():
            with self.subTest(project=name):
                parsed = Project(project)
                reached = [
                    f"{parsed.relative(path)}:{number}"
                    for path in parsed.kotlin + parsed.gradle
                    for number, line in enumerate(parsed.code[path].splitlines(), 1)
                    if NO_UI_FORBIDDEN.search(line)
                ]
                self.assertEqual([], reached)

    def test_every_placeholder_is_titled_with_a_string_of_its_own_module(self):
        title = re.compile(r"PlaceholderScreen\(\s*title = R\.string\.(\w+)")
        for name, project in self.projects.items():
            with self.subTest(project=name):
                for path in project.rglob("*.kt"):
                    for name in title.findall(path.read_text(encoding="utf-8")):
                        module = path.as_posix().split("/src/")[0]
                        strings = Path(module, "src/main/res/values/strings.xml").read_text(encoding="utf-8")
                        self.assertIn(f'<string name="{name}">', strings, path.name)

    def test_the_app_opens_on_its_registered_start_screen(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                start = project / f"app/src/main/kotlin/{PKG}/ui/start"
                navigation = (start / "StartNavigation.kt").read_text(encoding="utf-8")
                self.assertIn("entry<StartKey> { StartScreen() }", navigation)
                self.assertIn("subclass(StartKey::class, StartKey.serializer())", navigation)
                self.assertIn('.testTag("start")', (start / "StartScreen.kt").read_text(encoding="utf-8"))
                destinations = (project / f"app/src/main/kotlin/{PKG}/ui/AppDestinations.kt").read_text(encoding="utf-8")
                self.assertIn("val start: AppNavKey = StartKey", destinations)
                self.assertNotIn("ShellTab", destinations)

    def test_the_theme_is_the_skeleton(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                theme = (project / f"core/designsystem/src/main/kotlin/{PKG}/core/designsystem/theme/AppTheme.kt").read_text(encoding="utf-8")
                self.assertNotIn("AppOverlayHost", theme)
                self.assertIn("LocalIndication provides NoIndication", theme)
                for slot in ("colors", "typography", "spacing", "shapes", "elevation", "sizes", "springs", "reduceMotion"):
                    self.assertIn(f"val {slot}: ", theme, slot)
                previews = project / f"core/designsystem/src/main/kotlin/{PKG}/core/designsystem/preview"
                self.assertTrue((previews / "ThemePreview.kt").is_file())

    def test_feature_modules_render_no_screenshots(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                plugin = (project / "build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt").read_text(encoding="utf-8")
                self.assertNotIn("android.screenshots", plugin)

    def test_the_record_says_so(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                record = json.loads((project / RECORD_NAME).read_text(encoding="utf-8"))
                self.assertIs(False, record["ui_components"])

    def test_no_home_module_and_no_tab(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                self.assertFalse((project / "feature/home").exists())

    def test_every_name_the_rules_advise_is_in_the_project(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                sources = "\n".join(
                    path.read_text(encoding="utf-8")
                    for path in project.rglob("*.kt")
                    if "/src/main/" in path.as_posix() and not path.is_relative_to(project / "build-logic")
                )
                theme = project / f"core/designsystem/src/main/kotlin/{PKG}/core/designsystem/theme/AppTheme.kt"
                members = set(re.findall(r"\b(?:val|fun)\s+(\w+)", theme.read_text(encoding="utf-8")))
                annotations = set(re.findall(r"\bannotation class (\w+)", sources))
                declared = set(re.findall(
                    r"\b(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)", sources,
                ))
                missing = set()
                advice = active_advice(project)
                self.assertTrue(advice)
                for line in advice:
                    missing |= {f"AppTheme.{m}" for m in _THEME_MEMBER.findall(line) if m not in members}
                    missing |= {f"@{a}" for a in _ANNOTATION.findall(line) if a not in annotations}
                    names = _CODE_NAME.findall(_THEME_MEMBER.sub("", line))
                    missing |= {n for n in names if n not in declared and n not in ADVICE_LIBRARY_NAMES}
                self.assertEqual(set(), missing)

    def test_the_profiles_name_only_classes_the_project_has(self):
        from tests.android_profile import classes_of, kept

        # A name other than the template's own, so every class the profile names has to have been renamed.
        with tempfile.TemporaryDirectory() as directory:
            project = generate(
                Path(directory), "everything",
                features=sorted(ANDROID.manifest.preset_features("everything")), ui_components=False,
            )
            known = classes_of(project)
            profiles = {
                module: (project / module / "src/main/baseline-prof.txt").read_text(encoding="utf-8").splitlines()
                for module in ("app", "core/navigation", "core/designsystem", "core/ui")
            }
        for module, profile in profiles.items():
            with self.subTest(module=module):
                self.assertTrue(profile)
                self.assertEqual([], [line for line in profile if not kept(line, known, PKG)][:10])
        # The cold start runs through the application class and the Hilt graph built for it.
        for name in ("AcmeNoUiApplication", "Hilt_AcmeNoUiApplication", "DaggerAcmeNoUiApplication_HiltComponents_SingletonC"):
            with self.subTest(name=name):
                self.assertTrue(any(f"L{PKG}/{name};" in line for line in profiles["app"]))


class UiProjectTest(unittest.TestCase):
    """The same spec with the box unticked is the UI project it always was."""

    def test_a_ui_project_has_the_shell_and_records_nothing_new(self):
        with tempfile.TemporaryDirectory() as directory:
            project = generate(Path(directory), "ui", features=sorted(ANDROID.manifest.preset_features("lean")))
            self.assertNotIn("ui_components", json.loads((project / RECORD_NAME).read_text(encoding="utf-8")))
            self.assertTrue((project / f"core/navigation/src/main/kotlin/{PKG}/core/navigation/AppShell.kt").is_file())
            self.assertFalse((project / f"app/src/main/kotlin/{PKG}/ui/start").exists())
            self.assertTrue((project / "feature/home").is_dir())


class ProjectScriptsTest(unittest.TestCase):
    """add_feature.py and remove_feature.py on a project without UI components."""

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.base = generate(
            Path(cls.temp.name), "lean", features=sorted(ANDROID.manifest.preset_features("lean")), languages=["de"],
            ui_components=False,
        )

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def copy(self) -> Path:
        target = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, target)
        shutil.copytree(self.base, target / "p")
        return target / "p"

    def add(self, *arguments: str) -> tuple[int, str]:
        from add_feature import main

        with redirect_stdout(io.StringIO()) as out:
            code = main(list(arguments))
        return code, out.getvalue()

    def test_a_module_brings_its_logic_and_a_placeholder_route_and_says_so(self):
        project = self.copy()
        code, out = self.add("orders", "--project", str(project))
        self.assertEqual(0, code, out)
        source = project / f"feature/orders/src/main/kotlin/{PKG}/feature/orders"
        self.assertTrue((source / "list/OrdersListViewModel.kt").is_file())
        self.assertIn("PlaceholderScreen(title = R.string.orders_title)", (source / "list/OrdersListRoute.kt").read_text(encoding="utf-8"))
        self.assertEqual([], sorted(path.name for path in project.rglob("Orders*Screen.kt")))
        self.assertEqual([], sorted(path.name for path in project.rglob("*RecompositionTest.kt") if "orders" in path.as_posix()))
        self.assertTrue((project / "feature/orders/src/main/res/values-de/strings.xml").is_file())
        self.assertIn('include(":feature:orders")', (project / "settings.gradle.kts").read_text(encoding="utf-8"))
        self.assertIn("Added :data:orders and :feature:orders with its ViewModel and a placeholder route.", out)
        self.assertIn("No UI components: build the screen on your design system.", out)

    def test_a_tab_is_refused_before_anything_is_written(self):
        project = self.copy()
        before = snapshot(project)
        code, out = self.add("billing", "--tab", "--project", str(project))
        self.assertEqual(1, code)
        self.assertIn("This project has no tab bar (it was generated without UI components).", out)
        self.assertEqual(before, snapshot(project))

    def test_remove_undoes_add(self):
        from remove_feature import main as remove

        project = self.copy()
        before = snapshot(project)
        self.assertEqual(0, self.add("orders", "--project", str(project))[0])
        with redirect_stdout(io.StringIO()):
            self.assertEqual(0, remove(["orders", "--yes", "--project", str(project)]))
        self.assertEqual(before, snapshot(project))

    def test_without_a_record_the_project_is_read_from_what_it_holds(self):
        project = self.copy()
        (project / RECORD_NAME).unlink()
        code, out = self.add("orders", "--project", str(project))
        self.assertEqual(0, code, out)
        route = project / f"feature/orders/src/main/kotlin/{PKG}/feature/orders/list/OrdersListRoute.kt"
        self.assertIn("PlaceholderScreen(", route.read_text(encoding="utf-8"))
        self.assertEqual(1, self.add("billing", "--tab", "--project", str(project))[0])

    def test_a_domain_module_joins_a_modular_project(self):
        temp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, temp)
        project = generate(temp, "modular", features=["network"], architecture="modular", ui_components=False)
        code, out = self.add("--domain", "pricing", "--project", str(project))
        self.assertEqual(0, code, out)
        self.assertTrue((project / "domain/pricing/build.gradle.kts").is_file())
        self.assertIn('include(":domain:pricing")', (project / "settings.gradle.kts").read_text(encoding="utf-8"))

    def test_a_compact_project_has_no_place_for_a_domain_module(self):
        project = self.copy()
        before = snapshot(project)
        code, out = self.add("--domain", "pricing", "--project", str(project))
        self.assertEqual(1, code)
        self.assertIn("Domain modules belong to modular projects", out)
        self.assertEqual(before, snapshot(project))

    def test_a_record_that_does_not_say_true_or_false_is_refused(self):
        project = self.copy()
        record = project / RECORD_NAME
        data = json.loads(record.read_text(encoding="utf-8"))
        record.write_text(json.dumps({**data, "ui_components": "no"}), encoding="utf-8")
        before = snapshot(project)
        code, out = self.add("orders", "--project", str(project))
        self.assertEqual(1, code)
        self.assertIn("'ui_components'", out)
        self.assertEqual(before, snapshot(project))


if __name__ == "__main__":
    unittest.main()
