"""
add_component.py writes a design-system component and its test from the project's own docs/DESIGN-SYSTEM.md, in
projects with and without UI components.

    py -m unittest tests.test_add_component
"""

from __future__ import annotations

import contextlib
import io
import re
import tempfile
import unittest
from pathlib import Path

import add_component
from genkit.core import pipeline
from genkit.targets import get

ANDROID = get("android")
PACKAGE = "com.acme.parts"
PKG = PACKAGE.replace(".", "/")
MAIN = f"core/designsystem/src/main/kotlin/{PKG}/core/designsystem/component"
TEST = f"core/designsystem/src/test/kotlin/{PKG}/core/designsystem/component"

_temp: tempfile.TemporaryDirectory | None = None
_projects: dict[str, Path] = {}


def setUpModule():
    global _temp
    _temp = tempfile.TemporaryDirectory()
    for name, fields in (("ui", {}), ("no-ui", {"ui_components": False})):
        raw = {
            "schema": 2, "platform": "android", "app_name": "Acme Parts", "package_name": PACKAGE,
            "features": sorted(ANDROID.manifest.preset_features("lean")), **fields,
        }
        project = Path(_temp.name) / name
        pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
        _projects[name] = project


def tearDownModule():
    _temp.cleanup()


def run(*argv: str) -> tuple[int, str]:
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        code = add_component.main(list(argv))
    return code, out.getvalue()


def files(project: Path) -> set[str]:
    return {path.relative_to(project).as_posix() for path in project.rglob("*") if path.is_file()}


def block(project: Path, tag: str) -> str:
    doc = (project / "docs/DESIGN-SYSTEM.md").read_text(encoding="utf-8")
    return re.search(rf"(?ms)^```kotlin {tag}\n(.*?)^```$", doc).group(1)


def body(source: str) -> str:
    """A written file after its package line and imports."""
    return re.sub(r"\Apackage [\w.]+\n\n(?:import [\w.]+\n)+\n", "", source)


class WritesTest(unittest.TestCase):

    def test_the_component_and_its_test_are_the_project_s_own_templates(self):
        for mode, project in _projects.items():
            with self.subTest(mode=mode):
                code, out = run("Tag", "--project", str(project))
                self.assertEqual(0, code, out)
                component = (project / MAIN / "tag/AppTag.kt").read_text(encoding="utf-8")
                test = (project / TEST / "tag/AppTagTest.kt").read_text(encoding="utf-8")
                self.assertTrue(component.startswith(f"package {PACKAGE}.core.designsystem.component.tag\n\n"))
                self.assertTrue(test.startswith(f"package {PACKAGE}.core.designsystem.component.tag\n\n"))
                self.assertEqual(block(project, "component-template"), body(component))
                self.assertEqual(block(project, "component-test-template"), body(test))

    def test_every_import_is_used_and_each_of_the_project_s_own_names_is_declared_where_it_is_imported_from(self):
        for mode, project in _projects.items():
            code, out = run("Badger", "--project", str(project))
            self.assertEqual(0, code, out)
            sources = {
                path.as_posix(): path.read_text(encoding="utf-8")
                for path in project.rglob("*.kt") if "/build/" not in path.as_posix()
            }
            for written in (project / MAIN / "badger/AppBadger.kt", project / TEST / "badger/AppBadgerTest.kt"):
                text = written.read_text(encoding="utf-8")
                for imported in re.findall(r"(?m)^import ([\w.]+)$", text):
                    package, _, name = imported.rpartition(".")
                    with self.subTest(mode=mode, file=written.name, imported=imported):
                        # A delegate's operators are used by `by`, not by name.
                        used = r"\bby\b" if name in ("getValue", "setValue") else rf"\b{name}\b"
                        self.assertRegex(body(text), used)
                        if package.startswith(PACKAGE):
                            self.assertTrue(any(
                                re.search(rf"(?m)^package {re.escape(package)}$", source)
                                and re.search(rf"\b(?:class|object|fun|val)\b[^\n(=]*\b{name}\b", source)
                                for source in sources.values()
                            ))

    def test_a_name_of_several_words_renames_every_name_and_the_comments(self):
        project = _projects["no-ui"]
        code, out = run("SearchBar", "--project", str(project))
        self.assertEqual(0, code, out)
        component = (project / MAIN / "searchbar/AppSearchBar.kt").read_text(encoding="utf-8")
        test = (project / TEST / "searchbar/AppSearchBarTest.kt").read_text(encoding="utf-8")
        for name in ("AppSearchBarColors", "object AppSearchBarDefaults", "fun AppSearchBar(", "fun AppSearchBarPreview("):
            self.assertIn(name, component)
        self.assertIn("class AppSearchBarTest", test)
        self.assertNotRegex(component + test, r"AppTag|\btag\b")
        self.assertIn("/** A search bar the person turns on and off.", component)

    def test_a_group_puts_it_in_that_package(self):
        project = _projects["ui"]
        code, out = run("Pennant", "--group", "fields", "--project", str(project))
        self.assertEqual(0, code, out)
        component = (project / MAIN / "fields/AppPennant.kt").read_text(encoding="utf-8")
        self.assertTrue(component.startswith(f"package {PACKAGE}.core.designsystem.component.fields\n"))
        self.assertTrue((project / TEST / "fields/AppPennantTest.kt").is_file())

    def test_a_dry_run_says_what_it_would_write_and_writes_nothing(self):
        for mode, project in _projects.items():
            with self.subTest(mode=mode):
                before = files(project)
                code, out = run("Ribbon", "--project", str(project), "--dry-run")
                self.assertEqual(0, code, out)
                self.assertIn(f"{MAIN}/ribbon/AppRibbon.kt", out)
                self.assertIn(f"{TEST}/ribbon/AppRibbonTest.kt", out)
                self.assertEqual(before, files(project))


class RefusesTest(unittest.TestCase):

    def assert_refused(self, project: Path, *argv: str, says: str) -> None:
        before = files(project)
        code, out = run(*argv, "--project", str(project))
        self.assertEqual(1, code)
        self.assertIn(says, out)
        self.assertEqual(before, files(project))

    def test_a_name_that_is_not_pascal_case(self):
        for name in ("tag", "search_bar", "Search-Bar", "SEARCH", "Qr2D!", "QRCode"):
            with self.subTest(name=name):
                self.assert_refused(_projects["no-ui"], name, says="is not a PascalCase name")

    def test_a_name_with_the_app_prefix_says_the_name_to_use(self):
        self.assert_refused(_projects["no-ui"], "AppTagline", says="add_component.py Tagline")

    def test_a_group_that_is_not_a_package_name(self):
        self.assert_refused(_projects["ui"], "Lantern", "--group", "Fields", says="is not a package name")

    def test_a_component_the_design_system_has(self):
        # The theme in both modes; a component only a project with UI components has.
        for mode, name, found in (("no-ui", "Theme", "theme/AppTheme.kt"), ("ui", "Theme", "theme/AppTheme.kt"),
                                  ("ui", "Badge", "AppBadge.kt")):
            with self.subTest(mode=mode, name=name):
                self.assert_refused(_projects[mode], name, says=found)
        project = _projects["no-ui"]
        self.assertEqual(0, run("Lozenge", "--project", str(project))[0])
        self.assert_refused(project, "Lozenge", says="AppLozenge is already in the design system")

    def test_a_project_without_a_design_system(self):
        with tempfile.TemporaryDirectory() as directory:
            project = Path(directory)
            (project / "settings.gradle.kts").write_text('rootProject.name = "Bare"\ninclude(":app")\n', encoding="utf-8")
            self.assert_refused(project, "Tag", says="has no :core:designsystem")

    def test_a_folder_that_is_not_a_project(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assert_refused(Path(directory), "Tag", says="No settings.gradle.kts")

    def test_a_template_that_uses_a_name_it_cannot_import(self):
        source = add_component.renamed("@Composable\nfun AppTag(modifier: Modifier = Modifier) { Spacer(modifier) }\n", "Tag")
        with self.assertRaisesRegex(add_component.ComponentError, r"use Spacer, which add_component\.py cannot import"):
            add_component.source(PACKAGE, source, frozenset(), {"designsystem": PACKAGE, "testing": PACKAGE})


if __name__ == "__main__":
    unittest.main()
