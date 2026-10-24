"""
Projects generated without UI components ("ui_components": false): what they hold and lack, and the
project scripts working on them.
"""

from __future__ import annotations

import io
import json
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
