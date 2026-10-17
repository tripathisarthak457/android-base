"""
The engine's whole output over `fixtures/mini-template/`, byte for byte, against `golden/`.

The mini template exercises every marker form, every identity boundary and every kind of file, and
belongs to these tests alone, so a change to a real template never churns them. After a deliberate
engine change, regenerate the expected trees and review the diff:

    GENKIT_UPDATE_GOLDEN=1 python3 -m unittest tests.test_golden
"""

from __future__ import annotations

import errno
import os
import shutil
import stat
import subprocess
import tempfile
import unittest
import zipfile
from functools import cached_property
from pathlib import Path
from types import SimpleNamespace
from typing import Any
from unittest import mock

from genkit import tokens
from genkit.core import pipeline
from genkit.core.copy import write_text
from genkit.core.manifest import TemplateManifest
from genkit.core.spec import Spec, Vocabulary
from genkit.core.target import BuildContext, RenderError

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
        write_text(project / "transformed.txt", f"{spec.package_name} BaseApp\n")
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


def generate(
    case: str, destination: Path, *, zip_output: bool = False, git_init: bool = False, replace: bool = False
) -> pipeline.Generated:
    target = MiniTarget()
    parsed, _ = pipeline.read_spec(CASES[case], target)
    return pipeline.build(
        parsed.spec, target, destination, zip_output=zip_output, git_init=git_init, commit="golden", replace=replace
    )


def tree(root: Path) -> dict[str, bytes]:
    return {
        path.relative_to(root).as_posix(): path.read_bytes()
        for path in sorted(root.rglob("*"))
        if path.is_file() and path.name != "generator-spec.json"
    }


def pipeline_os(**functions: Any) -> Any:
    """Patches `functions` into the `os` module as the pipeline sees it, and nowhere else."""
    return mock.patch.object(pipeline, "os", SimpleNamespace(**{**vars(os), **functions}))


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

    @unittest.skipIf(os.name == "nt", "Windows has no executable bit: os.stat derives one from the file name")
    def test_the_wrapper_script_is_executable_and_nothing_else_is(self):
        project = self.root / "all"
        generate("all", project)
        executable = {
            path.relative_to(project).as_posix()
            for path in project.rglob("*")
            if path.is_file() and path.stat().st_mode & stat.S_IXUSR
        }
        self.assertEqual({"gradlew"}, executable)

    def test_the_zip_marks_the_wrapper_script_executable_and_nothing_else(self):
        archive = self.root / "all.zip"
        generate("all", archive, zip_output=True)
        with zipfile.ZipFile(archive) as opened:
            executable = {info.filename for info in opened.infolist() if (info.external_attr >> 16) & stat.S_IXUSR}
        self.assertEqual({"TrailNotes/gradlew"}, executable)

    @unittest.skipIf(shutil.which("git") is None, "git is not installed")
    def test_the_commit_marks_the_wrapper_script_executable_and_nothing_else(self):
        project = self.root / "all"
        result = generate("all", project, git_init=True)
        self.assertEqual(["a warning"], result.warnings)

        def git(*arguments: str) -> str:
            return subprocess.run(["git", *arguments], cwd=project, capture_output=True, text=True, check=True).stdout

        entries = [line.split(maxsplit=3) for line in git("ls-tree", "-r", "HEAD").splitlines()]
        self.assertEqual({"gradlew"}, {path for mode, _, _, path in entries if mode == "100755"})
        self.assertEqual("", git("status", "--porcelain"))
        # The mini template has no .gitattributes, so each blob is the file as generated, CRLF run.bat too.
        for _, _, blob, path in entries:
            committed = subprocess.run(["git", "cat-file", "blob", blob], cwd=project, capture_output=True, check=True).stdout
            self.assertEqual((project / path).read_bytes(), committed, path)

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


class PlacementTest(unittest.TestCase):
    """The finished project moved into place: from staging on another volume, and over an older one."""

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)

    def assert_is_golden(self, case: str, project: Path) -> None:
        generated = {name: data for name, data in tree(project).items() if not name.startswith(".git/")}
        self.assertEqual(tree(GOLDEN / f"mini-{case}"), generated)
        self.assertEqual([project.name], [path.name for path in self.root.iterdir()], "left beside it")

    @unittest.skipIf(shutil.which("git") is None, "git is not installed")
    def test_a_git_project_moves_from_staging_on_another_volume(self):
        def other_volume(source, target):
            raise OSError(errno.EXDEV, os.strerror(errno.EXDEV), source, None, target)

        project = self.root / "all"
        with pipeline_os(rename=other_volume):
            generate("all", project, git_init=True)
        self.assert_is_golden("all", project)

    @unittest.skipIf(shutil.which("git") is None, "git is not installed")
    def test_a_git_project_is_replaced_whole(self):
        project = self.root / "all"
        generate("all", project, git_init=True)
        generate("none", project, git_init=True, replace=True)
        self.assert_is_golden("none", project)

    def test_the_old_project_stays_when_the_new_one_cannot_take_its_place(self):
        project = self.root / "all"
        generate("all", project)

        def refuse_the_new_one(source, target):
            if Path(target) == project and Path(source).name == project.name:
                raise PermissionError("in use")
            os.replace(source, target)

        with pipeline_os(replace=refuse_the_new_one), self.assertRaises(PermissionError):
            generate("none", project, replace=True)
        self.assert_is_golden("all", project)

    def test_the_old_project_is_kept_beside_when_it_can_be_neither_replaced_nor_put_back(self):
        project = self.root / "all"
        generate("all", project)

        def refuse_the_destination(source, target):
            if Path(target) == project:
                raise PermissionError("in use")
            os.replace(source, target)

        with pipeline_os(replace=refuse_the_destination), self.assertRaisesRegex(
            RenderError, "could not be replaced, and what was there could not be put back"
        ) as raised:
            generate("none", project, replace=True)
        [kept] = self.root.iterdir()
        self.assertIn(str(kept / project.name), str(raised.exception))
        self.assertEqual(tree(GOLDEN / "mini-all"), tree(kept / project.name))


@unittest.skipUnless(os.name == "nt", "the path length limit is Windows'")
class WindowsPathLimitTest(unittest.TestCase):

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        try:
            (self.root / ("x" * 240)).mkdir()
        except OSError:
            pass
        else:
            self.skipTest("long paths are on here")

    def test_a_staged_path_over_the_limit_is_explained(self):
        target = MiniTarget()
        raw = {**CASES["none"], "app_name": "A" * 50, "package_name": "io." + ".".join(["p" * 30] * 3)}
        parsed, _ = pipeline.read_spec(raw, target)
        # Staged here, a source file passes the limit first; one level deeper, its folder does.
        for staging in (self.root / "s", self.root / "s" / ("t" * 150)):
            with self.subTest(depth=len(str(staging))):
                staging.mkdir(parents=True)
                destination = self.root / "Long"
                with mock.patch.object(tempfile, "tempdir", str(staging)), self.assertRaisesRegex(
                    RenderError, "Windows refused a path of .* shorter app name or package name"
                ):
                    pipeline.build(parsed.spec, target, destination, zip_output=False, commit="golden")
                self.assertEqual([], [path for path in staging.iterdir() if path.name.startswith("android-gen-")])
                self.assertFalse(destination.exists())

    def test_a_destination_too_deep_for_the_folder_beside_it_is_explained(self):
        # 240 characters leave room for Bare.zip, not for the folder it is finished in beside it.
        deep = self.root / ("d" * (240 - len(str(self.root)) - 1))
        deep.mkdir()
        destination = deep / "Bare.zip"
        with self.assertRaisesRegex(RenderError, "Windows refused a path of .* shorter destination, app name"):
            generate("none", destination, zip_output=True)
        self.assertEqual([], list(deep.iterdir()))


@unittest.skipUnless(os.name == "nt", "the path length limit is Windows'")
class WindowsLongPathsTest(unittest.TestCase):

    def test_with_long_paths_on_a_long_missing_path_is_only_missing(self):
        for on, expected in ((True, FileNotFoundError), (False, RenderError)):
            with self.subTest(long_paths=on), mock.patch.object(pipeline, "_windows_long_paths_on", return_value=on):
                with self.assertRaises(expected), pipeline._windows_path_limit_explained("app name"):
                    raise FileNotFoundError(errno.ENOENT, "No such file or directory", "C:\\" + "x" * 300)


if __name__ == "__main__":
    unittest.main()
