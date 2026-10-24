"""Kotlin import order in generated projects: ASCII by path, whatever the package sorts as."""

from __future__ import annotations

import re
import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get
from genkit.targets.android import imports

ANDROID = get("android")


class SortTest(unittest.TestCase):

    def test_the_block_is_ordered_by_path_without_duplicates_or_blank_lines(self):
        text = (
            "package io.acme.app\n\n"
            "import io.acme.app.core.common.LoadState\n"
            "import androidx.compose.ui.draw.clip\n\n"
            "import androidx.compose.ui.Modifier\n"
            "import dagger.Module\n"
            "import io.acme.app.core.common.LoadState\n"
            "import io.acme.app.R as AppR\n\n"
            "fun f() = Unit\n"
        )
        self.assertEqual(
            "package io.acme.app\n\n"
            "import androidx.compose.ui.Modifier\n"
            "import androidx.compose.ui.draw.clip\n"
            "import dagger.Module\n"
            "import io.acme.app.R as AppR\n"
            "import io.acme.app.core.common.LoadState\n\n"
            "fun f() = Unit\n",
            imports.sort(text),
        )

    def test_a_comment_line_moves_with_the_import_below_it(self):
        text = "package a\n\nimport b.Z\n// Needed by two optional blocks.\nimport b.Y\nimport b.A\n\nval x = 1\n"
        self.assertEqual(
            "package a\n\nimport b.A\n// Needed by two optional blocks.\nimport b.Y\nimport b.Z\n\nval x = 1\n",
            imports.sort(text),
        )

    def test_anything_else_inside_the_block_leaves_the_file_as_written(self):
        text = "package a\n\nimport b.Z\n/* why */\nimport b.A\n\nval x = 1\n"
        self.assertEqual(text, imports.sort(text))

    def test_a_file_without_imports_is_unchanged(self):
        self.assertEqual("package a\n\nval x = 1\n", imports.sort("package a\n\nval x = 1\n"))


class GeneratedProjectTest(unittest.TestCase):

    def test_every_source_is_in_order_for_a_package_that_sorts_after_dagger(self):
        raw = {
            "schema": 2, "platform": "android", "app_name": "Acme", "package_name": "io.acme.app",
            "features": ["analytics", "network", "sample", "settings", "workmanager"], "feature_modules": ["orders"],
        }
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp) / "p"
            pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="t")
            checked = 0
            for path in project.glob("**/src/**/*.kt"):
                block = re.search(r"^import .*(?:\n(?:import .*|//.*|\s*))*", path.read_text(encoding="utf-8"), re.MULTILINE)
                if block is None:
                    continue
                paths = [line.split()[1] for line in block.group(0).splitlines() if line.startswith("import ")]
                with self.subTest(file=path.relative_to(project).as_posix()):
                    self.assertEqual(sorted(paths), paths)
                checked += 1
            self.assertGreater(checked, 100)


if __name__ == "__main__":
    unittest.main()
