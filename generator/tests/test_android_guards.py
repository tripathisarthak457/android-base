"""
The build guards' own configuration, in every shape a project takes with UI components and without: each rule's advice
sends its reader to something the project declares, and neither baseline accepts anything the project lacks.

A baseline entry for a file or a composable the project does not have is dead configuration that waits to accept the
next file or function given that name, without a word. So each entry sits in the region of what it accepts, and this
test holds every shape to it.
"""

from __future__ import annotations

import re
import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get
from tests.test_android_noui import SHAPES, active_advice, unreachable_advice

ANDROID = get("android")
PACKAGE = "com.acme.guards"
PKG = PACKAGE.replace(".", "/")

_PACKAGE = re.compile(r"^\s*package\s+([\w.]+)", re.MULTILINE)


def generate(directory: Path, name: str, **fields) -> Path:
    raw = {"schema": 2, "platform": "android", "app_name": "Acme Guards", "package_name": PACKAGE, **fields}
    project = directory / name
    pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
    return project


def baseline_lines(path: Path) -> list[str]:
    """A baseline's entries: every line that is neither blank nor a comment."""
    lines = (line.strip() for line in path.read_text(encoding="utf-8").splitlines())
    return [line for line in lines if line and not line.startswith("#")]


_SPLIT_MODULE = re.compile(r"^((?:feature|data)/[^/]+)/")


def missing_code_rule_paths(project: Path) -> list[str]:
    """
    The paths config/code-rules-baseline.txt accepts findings in that the project does not have. As in
    CodeRulesBaseline, a path names a feature or data module without its `api`/`impl` level, so the one list serves a
    project before and after it is split.
    """
    def present(path: str) -> bool:
        candidates = [path] + [_SPLIT_MODULE.sub(rf"\1/{half}/", path) for half in ("api", "impl")]
        return any((project / candidate).is_file() for candidate in candidates)

    return [line for line in baseline_lines(project / "config/code-rules-baseline.txt") if not present(line.split()[0])]


def undeclared_stability_entries(project: Path) -> list[str]:
    """
    The entries of config/compose-stability-baseline.txt whose composable no production source declares: the
    longest prefix of `com.acme.x.Card.Content(state)` that a source file names as its package is the package, and
    the last name before the parameter is the function one of that package's files declares.
    """
    files_by_package: dict[str, list[str]] = {}
    for path in project.rglob("*.kt"):
        if "/src/main/" not in path.as_posix() or path.is_relative_to(project / "build-logic"):
            continue
        text = path.read_text(encoding="utf-8")
        declared = _PACKAGE.search(text)
        if declared:
            files_by_package.setdefault(declared.group(1), []).append(text)

    undeclared = []
    for entry in baseline_lines(project / "config/compose-stability-baseline.txt"):
        name = entry.split("#")[0].strip().split("(")[0].split(":")[0]
        parts = name.split(".")
        owner = next((".".join(parts[:end]) for end in range(len(parts) - 1, 0, -1)
                      if ".".join(parts[:end]) in files_by_package), None)
        function = re.compile(rf"\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?{re.escape(parts[-1])}\s*\(")
        if owner is None or not any(function.search(text) for text in files_by_package[owner]):
            undeclared.append(entry.split("#")[0].strip())
    return undeclared


class GuardConfigurationTest(unittest.TestCase):
    """Every preset in both architectures, and a project of feature modules alone, with and without UI components."""

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.projects = {
            f"{mode}-{name}": generate(Path(cls.temp.name), f"{mode}-{name}", **fields, **extra)
            for mode, extra in (("ui", {}), ("noui", {"ui_components": False}))
            for name, fields in SHAPES.items()
        }

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_every_name_the_rules_advise_is_in_the_project(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                self.assertTrue(active_advice(project, PKG))
                self.assertEqual(set(), unreachable_advice(project, PKG))

    def test_every_code_rule_baseline_entry_is_for_a_file_the_project_has(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                self.assertEqual([], missing_code_rule_paths(project))

    def test_every_stability_baseline_entry_is_for_a_composable_the_project_declares(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                self.assertEqual([], undeclared_stability_entries(project))

    def test_the_checks_see_entries_at_all(self):
        # A path or package these helpers misread would make every list above empty and every test pass.
        ui = self.projects["ui-everything-compact"]
        self.assertGreater(len(baseline_lines(ui / "config/code-rules-baseline.txt")), 10)
        self.assertEqual(2, len(baseline_lines(ui / "config/compose-stability-baseline.txt")))
        self.assertGreater(len(active_advice(ui, PKG)), 40)


if __name__ == "__main__":
    unittest.main()
