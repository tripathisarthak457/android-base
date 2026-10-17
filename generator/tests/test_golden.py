"""
The engine's whole output over `fixtures/mini-template/`, byte for byte, against `golden/`.

The mini template exercises every marker form, every identity boundary and every kind of file, and
belongs to these tests alone, so a change to a real template never churns them. After a deliberate
engine change, regenerate the expected trees and review the diff:

    GENKIT_UPDATE_GOLDEN=1 python3 -m unittest tests.test_golden
"""

from __future__ import annotations

import contextlib
import errno
import os
import re
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
from genkit.core import pipeline, placement
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


def unzipped(archive: Path) -> dict[str, bytes]:
    """`tree` of the one project folder in `archive`."""
    with zipfile.ZipFile(archive) as opened:
        return {
            info.filename.split("/", 1)[1]: opened.read(info)
            for info in opened.infolist()
            if not info.is_dir() and not info.filename.endswith("/generator-spec.json")
        }


def seen_by_placement(module: Any, **functions: Any) -> Any:
    """Patches `functions` into `module` (`os`, `shutil`) as the placement module sees it, and nowhere else."""
    return mock.patch.object(placement, module.__name__, SimpleNamespace(**{**vars(module), **functions}))


def deepest_destination(case: str) -> int:
    """The length of the longest destination at which `case`'s project fits Windows' length limit."""
    root = GOLDEN / f"mini-{case}"
    room = [placement._WINDOWS_MAX_DIRECTORY]
    for path in root.rglob("*"):
        limit = placement._WINDOWS_MAX_DIRECTORY if path.is_dir() else placement._WINDOWS_MAX_FILE
        room.append(limit - (len(str(path)) - len(str(root))))
    return min(room)


def delete_tree(path: Path) -> None:
    """Deletes what a test made there, files past Windows' length limit included."""
    shutil.rmtree("\\\\?\\" + str(path) if os.name == "nt" else path, ignore_errors=True)


def at_length(root: Path, length: int, name: str) -> Path:
    """`root/d…d/name`, `length` characters long, its folder made."""
    folder = root / ("d" * (length - len(str(root)) - len(name) - 2))
    folder.mkdir()
    return folder / name


def staged_on_another_volume() -> Any:
    """
    Fails every rename of the staged project as a rename to another volume fails, wherever in the
    process it is made, so a shutil.move would meet it too and then fail to delete git's read-only
    objects on Windows. No other rename is affected.
    """
    rename = os.rename

    def other_volume(source, target):
        if Path(source).parent.name.startswith(f"{MiniTarget.key}-gen-"):
            raise OSError(errno.EXDEV, os.strerror(errno.EXDEV), source, None, target)
        rename(source, target)

    return mock.patch("os.rename", other_volume)


def refusing_the_new_output(destination: Path, error: OSError) -> Any:
    """Fails the new output's rename onto `destination`, the first one onto it, with `error`."""
    refused: list[str] = []

    def replace(source, target):
        if Path(target) == destination and not refused:
            refused.append(source)
            raise error
        os.replace(source, target)

    return seen_by_placement(os, replace=replace)


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
        project = self.root / "all"
        with staged_on_another_volume():
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
        with refusing_the_new_output(project, PermissionError("in use")), self.assertRaises(PermissionError):
            generate("none", project, replace=True)
        self.assert_is_golden("all", project)

    def test_a_destination_held_open_is_left_as_it_was(self):
        project = self.root / "all"
        generate("all", project)

        def held_open(source, target):
            if Path(source) == project:
                raise PermissionError(errno.EACCES, "in use", str(source))
            os.replace(source, target)

        with seen_by_placement(os, replace=held_open), self.assertRaisesRegex(
            RenderError, rf"^{re.escape(str(project))} could not be moved aside \(in use\); nothing was changed\."
        ):
            generate("none", project, replace=True)
        self.assert_is_golden("all", project)

    def test_the_new_output_is_named_when_a_failure_leaves_it_undeleted(self):
        # A note on the error, which a failed deletion must not take the place of. A success
        # leaves nothing of its own to delete.
        held_open = PermissionError(errno.EACCES, "in use")
        project = self.root / "all"
        with mock.patch.object(placement, "_delete_tree", side_effect=held_open):
            result = generate("all", project)
        self.assertEqual(["a warning"], result.warnings)
        self.assert_is_golden("all", project)

        with mock.patch.object(placement, "_delete_tree", side_effect=held_open), refusing_the_new_output(
            project, PermissionError(errno.EACCES, "refused")
        ), self.assertRaisesRegex(PermissionError, "refused") as raised:
            generate("none", project, replace=True)
        [left] = [path for path in self.root.iterdir() if path != project]
        self.assertEqual(
            [f"The new output was left at {left}: it could not be deleted (in use). Delete it yourself."],
            raised.exception.__notes__,
        )
        self.assertEqual(len(project.name), len(left.name))
        self.assertEqual(tree(GOLDEN / "mini-none"), tree(left))
        self.assertEqual(tree(GOLDEN / "mini-all"), tree(project))

    def test_a_read_only_old_zip_is_deleted(self):
        # Windows deletes no read-only file without its flag cleared first.
        archive = self.root / "Bare.zip"
        generate("all", archive, zip_output=True)
        os.chmod(archive, stat.S_IREAD)
        result = generate("none", archive, zip_output=True, replace=True)
        self.assertEqual(["a warning"], result.warnings)
        self.assertEqual(tree(GOLDEN / "mini-none"), unzipped(archive))
        self.assertEqual([archive], list(self.root.iterdir()))

    def test_the_old_project_is_kept_beside_when_it_can_be_neither_replaced_nor_put_back(self):
        project = self.root / "all"
        generate("all", project)

        def refuse_the_destination(source, target):
            if Path(target) == project:
                raise PermissionError("in use")
            os.replace(source, target)

        with seen_by_placement(os, replace=refuse_the_destination), self.assertRaisesRegex(
            RenderError, "could not be replaced, and what was there could not be put back"
        ) as raised:
            generate("none", project, replace=True)
        [kept] = self.root.iterdir()
        self.assertIn(str(kept), str(raised.exception))
        self.assertLessEqual(len(kept.name), len(project.name), "every path in it is longer than it was")
        self.assertEqual(tree(GOLDEN / "mini-all"), tree(kept))

    def test_the_new_project_stays_when_the_old_one_cannot_all_be_deleted(self):
        project = self.root / "all"
        generate("all", project)

        def held_open(path, *arguments, **options):
            raise PermissionError(errno.EACCES, "in use", str(path))

        with seen_by_placement(shutil, rmtree=held_open):
            result = generate("none", project, replace=True)
        self.assertEqual(tree(GOLDEN / "mini-none"), tree(project))
        [left] = [path for path in self.root.iterdir() if path != project]
        self.assertEqual(tree(GOLDEN / "mini-all"), tree(left))
        [warning] = [warning for warning in result.warnings if warning != "a warning"]
        self.assertIn("could not all be deleted (in use).", warning)
        self.assertIn(f"Delete {left} yourself.", warning)

    def test_a_destination_of_any_name_is_replaced_whole(self):
        # A name of one or two letters leaves the old output little room for a name of its own
        # beside it, and no name the pipeline gives its own folders may collide with the caller's.
        for name in ("a", "ab", "replaced"):
            for zip_output in (False, True):
                with self.subTest(name=name, zip=zip_output):
                    folder = self.root / f"{name}-{zip_output}"
                    destination = folder / name
                    generate("all", destination, zip_output=zip_output)
                    result = generate("none", destination, zip_output=zip_output, replace=True)
                    self.assertEqual(["a warning"], result.warnings)
                    generated = unzipped(destination) if zip_output else tree(destination)
                    self.assertEqual(tree(GOLDEN / "mini-none"), generated)
                    self.assertEqual([name], [path.name for path in folder.iterdir()], "left beside it")


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

    def test_a_zip_is_written_wherever_it_fits_and_nowhere_else(self):
        # A name long enough that the file's limit, not its folder's, is the one reached.
        for length, fits in ((placement._WINDOWS_MAX_FILE, True), (placement._WINDOWS_MAX_FILE + 1, False)):
            with self.subTest(length=length):
                destination = at_length(self.root, length, "TrailNotes.zip")
                if fits:
                    generate("none", destination, zip_output=True)
                    self.assertEqual(tree(GOLDEN / "mini-none"), unzipped(destination))
                else:
                    with self.assertRaisesRegex(
                        RenderError,
                        rf"^The zip would be a path of {length} characters: {re.escape(str(destination))}\. .* "
                        r"shorter destination\.$",
                    ):
                        generate("none", destination, zip_output=True)
                self.assertEqual([destination] if fits else [], list(destination.parent.iterdir()))

    def test_a_project_is_placed_at_the_deepest_destination_it_fits_from_staging_on_either_volume(self):
        # The project is finished beside the destination before it is renamed into place, copied
        # there from staging on another volume, so no path on the way may be longer than in place.
        self.addCleanup(delete_tree, self.root)
        for other_volume in (False, True):
            with self.subTest(other_volume=other_volume):
                folder = self.root / str(other_volume)
                folder.mkdir()
                project = at_length(folder, deepest_destination("all"), "all")
                with staged_on_another_volume() if other_volume else contextlib.nullcontext():
                    generate("all", project)
                self.assertEqual(tree(GOLDEN / "mini-all"), tree(project))
                self.assertEqual([project], list(project.parent.iterdir()))

    def test_an_old_project_goes_whole_however_deep_its_files(self):
        # One file as deep as Windows allows in place, and one past that, as Gradle writes them in a
        # built project: Java reaches past the limit by itself.
        project = self.root / "all"
        generate("all", project)
        self.addCleanup(delete_tree, self.root)
        build = project / "build"
        build.mkdir()
        in_place = build / ("b" * (placement._WINDOWS_MAX_DIRECTORY - len(str(build)) - 1))
        in_place.mkdir()
        (in_place / ("f" * (placement._WINDOWS_MAX_FILE - len(str(in_place)) - 1))).write_bytes(b"")
        past = "\\\\?\\" + str(in_place / ("c" * 60))
        os.mkdir(past)
        with open(os.path.join(past, "classes.dex"), "wb"):
            pass

        result = generate("none", project, replace=True)
        self.assertEqual(["a warning"], result.warnings)
        self.assertEqual(tree(GOLDEN / "mini-none"), tree(project))
        self.assertEqual([project], list(self.root.iterdir()))

    def test_a_failed_replace_at_the_deepest_destination_leaves_only_the_old_project(self):
        # The new project, finished beside the destination as deep as it fits, is deleted whole.
        self.addCleanup(delete_tree, self.root)
        project = at_length(self.root, deepest_destination("all"), "all")
        generate("none", project)

        def held_open(source, target):
            if Path(source) == project:
                raise PermissionError(errno.EACCES, "in use", str(source))
            os.replace(source, target)

        with seen_by_placement(os, replace=held_open), self.assertRaisesRegex(
            RenderError, r"could not be moved aside \(in use\); nothing was changed\."
        ):
            generate("all", project, replace=True)
        self.assertEqual([project], list(project.parent.iterdir()))
        self.assertEqual(tree(GOLDEN / "mini-none"), tree(project))


class WindowsLongPathsTest(unittest.TestCase):
    """
    What the pipeline makes of a path's length, shown a Windows host with long paths off whatever
    this host is. The paths use forward slashes, which both hosts' path functions split on.
    """

    def setUp(self):
        for patch in (
            seen_by_placement(os, name="nt"),
            mock.patch.object(placement, "_windows_long_paths_on", return_value=False),
        ):
            patch.start()
            self.addCleanup(patch.stop)

    def test_with_long_paths_on_a_long_missing_path_is_only_missing(self):
        for on, expected in ((True, FileNotFoundError), (False, RenderError)):
            with self.subTest(long_paths=on), mock.patch.object(placement, "_windows_long_paths_on", return_value=on):
                with self.assertRaises(expected), placement.windows_path_limit_explained("app name"):
                    raise FileNotFoundError(errno.ENOENT, "No such file or directory", "C:/" + "x" * 300)

    def test_the_path_named_is_the_one_too_long(self):
        long = "D:/" + "y" * 300
        failures = {
            "the target of a rename": FileNotFoundError(errno.ENOENT, "No such file", "D:/short", None, long),
            "a file a tree copy could not write": shutil.Error([("C:/staged/file", long, "[WinError 3] not found")]),
        }
        for what, failure in failures.items():
            with self.subTest(what):
                with self.assertRaises(RenderError) as raised, placement.windows_path_limit_explained("destination"):
                    raise failure
                self.assertIn(f"a path of {len(long)} characters: {long}.", str(raised.exception))

    def test_a_tree_copy_is_held_to_the_limit_for_a_file_or_a_folder(self):
        # 254 characters: too long for a folder, not for a file in one of 243.
        file = "D:/" + "y" * 240 + "/shared.bin"
        folder = "D:/" + "y" * 250
        failures = {
            "a file held open": (shutil.Error([("C:/staged/shared.bin", file, "[WinError 32] in use")]), shutil.Error),
            "a folder too deep": (shutil.Error([(str(HERE), folder, "[WinError 3] not found")]), RenderError),
        }
        for what, (failure, expected) in failures.items():
            with self.subTest(what), self.assertRaises(expected), placement.windows_path_limit_explained("destination"):
                raise failure

    def test_a_project_too_deep_at_its_destination_is_refused_before_it_is_placed(self):
        # Staged, the project fits. Placed one character deeper than it fits, it would not, and
        # placing it is a rename, which Windows makes whatever the depth of the files inside.
        temp = Path(tempfile.mkdtemp())
        self.addCleanup(delete_tree, temp)
        destination = at_length(temp, deepest_destination("all") + 1, "all")
        with self.assertRaisesRegex(
            RenderError,
            rf"^At {re.escape(str(destination))} the project would hold a path of {placement._WINDOWS_MAX_FILE + 1} "
            r"characters: .*TrailNotesApplication\.kt\. .* shorter destination, app name or package name\.$",
        ):
            generate("all", destination)
        self.assertEqual([], list(destination.parent.iterdir()))

    def test_a_zip_too_long_for_its_destination_is_refused_before_it_is_written(self):
        temp = Path(tempfile.mkdtemp())
        self.addCleanup(delete_tree, temp)
        destination = at_length(temp, placement._WINDOWS_MAX_FILE + 1, "TrailNotes.zip")
        with self.assertRaisesRegex(
            RenderError, rf"^The zip would be a path of {placement._WINDOWS_MAX_FILE + 1} characters: "
        ):
            generate("all", destination, zip_output=True)
        self.assertEqual([], list(destination.parent.iterdir()))


if __name__ == "__main__":
    unittest.main()
