"""
The engine's whole output over `fixtures/mini-template/`, byte for byte, against `golden/`.

The mini template exercises every marker form, every identity boundary and every kind of file, and
belongs to these tests alone, so a change to a real template never churns them. After a deliberate
engine change, regenerate the expected trees and review the diff:

    GENKIT_UPDATE_GOLDEN=1 python3 -m unittest tests.test_golden
"""

from __future__ import annotations

import os
import shutil
import stat
import tempfile
import unittest
from functools import cached_property
from pathlib import Path
from typing import Any

from genkit import tokens
from genkit.core import pipeline
from genkit.core.manifest import TemplateManifest
from genkit.core.spec import Spec, Vocabulary
from genkit.core.target import BuildContext

HERE = Path(__file__).resolve().parent
MINI = HERE / "fixtures" / "mini-template"
GOLDEN = HERE / "golden"


class MiniTarget:
    """A target for the mini template, written against the same protocol the real ones use."""

    key = "android"
    template_dir = MINI

    @cached_property
    def manifest(self) -> TemplateManifest:
        return TemplateManifest.load(MINI)

    def vocabulary(self) -> Vocabulary:
        return Vocabulary(
            design_styles=tokens.design_style_requirements(),
            motion_styles=tokens.motion_style_names(),
            reserved_module_names=frozenset({"app", "extra"}),
        )

    def validate(self, spec: Spec) -> list[str]:
        return []

    def identity(self, spec: Spec) -> dict[str, str]:
        return {
            "package": spec.package_name,
            "package_path": spec.package_path,
            "pascal": spec.pascal_name,
            "snake": spec.snake_name,
            "lower": spec.lower_name,
        }

    def regions(self, spec: Spec) -> dict[str, list[str] | None]:
        return {
            # Generated text is final: a module named after a template token is not renamed.
            "modules": [f'include(":feature:{name}")\n' for name in spec.feature_modules],
            "version": None if spec.has("extra") else ['    val version = "1.2.3" // com.base.app stays\n'],
            "readme-name": [f"# {spec.label}\n"],
        }

    def overlays(self, spec: Spec) -> dict[str, Path]:
        return {}

    def transform(self, project: Path, spec: Spec) -> list[str]:
        (project / "transformed.txt").write_text(f"{spec.package_name} BaseApp\n", encoding="utf-8")
        return []

    def finish(self, project: Path, spec: Spec, context: BuildContext) -> list[str]:
        context.summary.append("finished")
        return ["a warning"]

    def outline(self, spec: Spec) -> dict[str, list[str]]:
        return {"modules": list(spec.feature_modules), "tabs": []}

    def catalogue(self) -> dict[str, Any]:
        return {"defaults": {}, "fields": [], "architectures": [], "reserved_module_names": [], "rules": {}}

    def environment(self) -> dict[str, Any]:
        return {}


CASES: dict[str, dict[str, Any]] = {
    "all": {
        "schema": 2, "platform": "android", "app_name": "Trail Notes", "package_name": "com.example.trail",
        "features": ["net", "ui", "extra"], "feature_modules": ["baseapp", "database_app"],
    },
    "none": {"schema": 2, "platform": "android", "app_name": "Bare", "package_name": "io.bare.min"},
    "hostile": {
        "app_name": "Xbaseapp", "package_name": "com.base.apple", "features": ["screens", "network-v0", "old"],
        "display_name": "Café & Co",
    },
}


def generate(case: str, destination: Path) -> pipeline.Generated:
    target = MiniTarget()
    parsed, _ = pipeline.read_spec(CASES[case], target)
    return pipeline.build(parsed.spec, target, destination, zip_output=False, commit="golden")


def tree(root: Path) -> dict[str, bytes]:
    return {
        path.relative_to(root).as_posix(): path.read_bytes()
        for path in sorted(root.rglob("*"))
        if path.is_file() and path.name != "generator-spec.json"
    }


class GoldenTest(unittest.TestCase):

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)

    def test_every_case_matches_its_golden_tree(self):
        update = os.environ.get("GENKIT_UPDATE_GOLDEN") == "1"
        for case in CASES:
            with self.subTest(case=case):
                project = self.root / case
                generate(case, project)
                expected_dir = GOLDEN / f"mini-{case}"
                if update:
                    shutil.rmtree(expected_dir, ignore_errors=True)
                    shutil.copytree(project, expected_dir, ignore=shutil.ignore_patterns("generator-spec.json"))
                actual, expected = tree(project), tree(expected_dir)
                self.assertEqual(sorted(expected), sorted(actual))
                for name in expected:
                    self.assertEqual(expected[name], actual[name], name)

    def test_the_wrapper_script_is_executable_and_nothing_else_is(self):
        project = self.root / "all"
        generate("all", project)
        executable = {
            path.relative_to(project).as_posix()
            for path in project.rglob("*")
            if path.is_file() and path.stat().st_mode & stat.S_IXUSR
        }
        self.assertEqual({"gradlew"}, executable)

    def test_template_only_files_never_ship(self):
        project = self.root / "all"
        generate("all", project)
        for name in ("template.toml", "presets.toml", "removed.toml", "features"):
            self.assertFalse((project / name).exists(), name)

    def test_the_target_contributes_warnings_and_a_summary(self):
        result = generate("none", self.root / "none")
        self.assertEqual(["a warning"], result.warnings)
        self.assertEqual(["finished"], result.summary)

    def test_a_feature_owning_a_path_takes_it_away_when_off(self):
        project = self.root / "none"
        generate("none", project)
        self.assertFalse((project / "extra").exists())
        self.assertFalse((project / "src/main/kotlin/io/bare/min/ExtraHook.kt").exists())


if __name__ == "__main__":
    unittest.main()
