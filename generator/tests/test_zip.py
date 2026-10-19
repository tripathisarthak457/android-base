"""
What a user receives: the zip, with the same bytes for the same project, executable bits and one
root folder, or the first commit, with the same executable bits.
"""

from __future__ import annotations

import contextlib
import os
import shutil
import stat
import subprocess
import tempfile
import time
import unittest
import zipfile
from collections.abc import Iterator
from pathlib import Path
from unittest import mock

from genkit.core.package import git_init, zip_project

EXECUTABLE = ("gradlew", "tools/perf/*.sh")


class ZipTest(unittest.TestCase):

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.project = self.root / "Trail"
        for relative, text in {
            "gradlew": "#!/bin/sh\n",
            "tools/perf/perf.sh": "#!/bin/sh\n",
            "tools/perf/gates.toml": "x = 1\n",
            "app/build.gradle.kts": "plugins {}\n",
            "gradle/wrapper/gradle-wrapper.jar": "PK\n",
            "README.md": "# Trail\n",
            # Path order puts a/b.txt first and name order a-c.txt; the archive follows the names.
            "a/b.txt": "b\n",
            "a-c.txt": "c\n",
        }.items():
            path = self.project / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")

    def test_the_same_project_gives_the_same_bytes_whatever_its_timestamps(self):
        first = zip_project(self.project, self.root / "a.zip", EXECUTABLE).read_bytes()
        later = time.time() + 86_400
        for path in self.project.rglob("*"):
            os.utime(path, (later, later))
        second = zip_project(self.project, self.root / "b.zip", EXECUTABLE).read_bytes()
        self.assertEqual(first, second)

    def test_one_root_folder_and_sorted_entries(self):
        with zipfile.ZipFile(zip_project(self.project, self.root / "a.zip", EXECUTABLE)) as archive:
            names = archive.namelist()
        self.assertEqual(sorted(names), names)
        self.assertTrue(all(name.startswith("Trail/") for name in names))

    def test_executable_bits_travel_inside_the_archive(self):
        with zipfile.ZipFile(zip_project(self.project, self.root / "a.zip", EXECUTABLE)) as archive:
            modes = {info.filename: (info.external_attr >> 16) for info in archive.infolist()}
        self.assertEqual(0o755, stat.S_IMODE(modes["Trail/gradlew"]))
        self.assertEqual(0o755, stat.S_IMODE(modes["Trail/tools/perf/perf.sh"]))
        self.assertEqual(0o644, stat.S_IMODE(modes["Trail/tools/perf/gates.toml"]))
        self.assertTrue(stat.S_ISREG(modes["Trail/README.md"]))

    @unittest.skipIf(shutil.which("git") is None, "git is not installed")
    def test_the_commit_marks_the_scripts_it_holds_and_leaves_out_the_ignored_one(self):
        (self.project / ".gitignore").write_bytes(b"local.sh\n")
        (self.project / "tools/perf/local.sh").write_bytes(b"#!/bin/sh\n")
        self.assertEqual([], git_init(self.project, "Initial commit", EXECUTABLE))
        listed = subprocess.run(
            ["git", "ls-tree", "-r", "HEAD"], cwd=self.project, capture_output=True, text=True, check=True
        ).stdout
        modes = {path: mode for mode, _, _, path in (line.split(maxsplit=3) for line in listed.splitlines())}
        self.assertNotIn("tools/perf/local.sh", modes)
        self.assertEqual(
            {"gradlew", "tools/perf/perf.sh"}, {path for path, mode in modes.items() if mode == "100755"}
        )

    @unittest.skipIf(shutil.which("git") is None, "git is not installed")
    def test_the_commit_is_the_same_whatever_the_users_git_configuration(self):
        # Git reads a user's global settings from GIT_CONFIG_GLOBAL (git 2.32 and later) or from
        # their home, where it also finds its default excludes and attributes files.
        hostile = Hostile(self.root / "hostile")
        global_file = self.root / "gitconfig"
        global_file.write_bytes(hostile.configuration(named_files=True).encode())
        home = self.root / "home"
        (home / "git").mkdir(parents=True)
        (home / ".gitconfig").write_bytes(hostile.configuration(named_files=False).encode())
        (home / "git" / "ignore").write_bytes(b"*.jar\n")
        (home / "git" / "attributes").write_bytes(b"* filter=hostile\n")
        for route, variables in {
            "GIT_CONFIG_GLOBAL": {"GIT_CONFIG_GLOBAL": str(global_file)},
            "home": {"HOME": str(home), "XDG_CONFIG_HOME": str(home), "GIT_CONFIG_GLOBAL": None},
        }.items():
            with self.subTest(route=route):
                project = self.root / route / self.project.name
                shutil.copytree(self.project, project)
                with environment(variables):
                    read = subprocess.run(
                        ["git", "config", "--get", "generatortest.marker"], cwd=self.root, capture_output=True,
                        text=True,
                    )
                    if read.stdout.strip() != "hostile":
                        self.skipTest(f"this git does not read its configuration from {route}")
                    self.assertEqual([], git_init(project, "Initial commit", EXECUTABLE))
                self.assert_is_the_generators_commit(project, hostile)

    @unittest.skipIf(shutil.which("git") is None, "git is not installed")
    def test_the_commit_is_the_same_whatever_git_variables_the_caller_has(self):
        # Git exports the first three to every hook, so to a generator run from one, such as a test
        # suite in pre-push; the others configure git by themselves.
        hostile = Hostile(self.root / "hostile")
        other = self.root / "other"
        subprocess.run(["git", "init", "--template=", str(other)], capture_output=True, check=True)
        variables = {
            "GIT_DIR": str(other / ".git"),
            "GIT_WORK_TREE": str(other),
            "GIT_INDEX_FILE": str(self.root / "index"),
            "GIT_AUTHOR_NAME": "Real Person",
            "GIT_AUTHOR_EMAIL": "real@example.com",
            "GIT_COMMITTER_NAME": "Real Person",
            "GIT_COMMITTER_EMAIL": "real@example.com",
            "GIT_TEMPLATE_DIR": str(hostile.template),
            "GIT_CONFIG_COUNT": "1",
            "GIT_CONFIG_KEY_0": "core.hooksPath",
            "GIT_CONFIG_VALUE_0": str(hostile.hooks),
            "GIT_DEFAULT_HASH": "sha256",
        }
        with environment(variables):
            self.assertEqual([], git_init(self.project, "Initial commit", EXECUTABLE))
        self.assert_is_the_generators_commit(self.project, hostile)
        head = subprocess.run(["git", "rev-parse", "--verify", "--quiet", "HEAD"], cwd=other, capture_output=True)
        self.assertNotEqual(0, head.returncode, "the other repository has a commit")
        self.assertFalse((self.root / "index").exists())

    @unittest.skipIf(shutil.which("git") is None, "git is not installed")
    def test_the_commit_holds_a_file_past_windows_path_limit(self):
        # A project holds one where Windows' long paths are on, and Git for Windows skips it
        # unless told otherwise.
        relative = f"{'d' * 130}/{'e' * 130}/deep.txt"
        self.addCleanup(shutil.rmtree, extended(self.project / ("d" * 130)), ignore_errors=True)
        folder = self.project
        for name in relative.split("/")[:-1]:
            folder = folder / name
            os.mkdir(extended(folder))
        with open(extended(folder / "deep.txt"), "wb") as deep:
            deep.write(b"deep\n")
        self.assertEqual([], git_init(self.project, "Initial commit", EXECUTABLE))
        self.assertIn(relative, committed(self.project))

    def assert_is_the_generators_commit(self, project: Path, hostile: Hostile) -> None:
        on_disk = {
            path.relative_to(project).as_posix()
            for path in project.rglob("*")
            if path.is_file() and ".git" not in path.relative_to(project).parts
        }
        modes = committed(project)
        self.assertEqual(on_disk, set(modes))
        self.assertEqual({"gradlew", "tools/perf/perf.sh"}, {path for path, mode in modes.items() if mode == "100755"})
        commit = subprocess.run(
            ["git", "cat-file", "commit", "HEAD"], cwd=project, capture_output=True, text=True, check=True
        ).stdout
        header, message = commit.split("\n\n", 1)
        fields = dict(line.split(" ", 1) for line in header.splitlines())
        # No gpgsig and no encoding header.
        self.assertEqual(["tree", "author", "committer"], list(fields))
        self.assertRegex(fields["author"], "^Project generator <generator@localhost> ")
        self.assertRegex(fields["committer"], "^Project generator <generator@localhost> ")
        self.assertEqual(40, len(fields["tree"]), "not a SHA-1 repository")
        self.assertEqual("Initial commit\n", message)
        self.assertFalse(hostile.trace.exists(), "these hooks ran")


class Hostile:
    """
    Git settings a user may keep for their own repositories, each of which would cost, change or
    leave a trace on the generator's commit: hooks that record that they ran and refuse (a hooks
    folder, a template's, a file system monitor), an excludes file and a template's info/exclude
    that leave out the wrapper's jar, a filter that fails on every file, signing through a gpg that
    is not there, and an identity, commit encoding and object format of their own.
    """

    HOOKS = (
        "pre-commit", "prepare-commit-msg", "commit-msg", "post-commit", "post-index-change", "reference-transaction"
    )

    def __init__(self, root: Path):
        self.root = root
        self.trace = root / "trace"
        self.hooks = root / "hooks"
        self.template = root / "template"
        self.monitor = root / "monitor.sh"
        for folder in (self.hooks, self.template / "hooks"):
            folder.mkdir(parents=True)
            for name in self.HOOKS:
                self._write_hook(folder / name)
        self._write_hook(self.monitor)
        (self.template / "info").mkdir()
        (self.template / "info" / "exclude").write_bytes(b"*.jar\n")
        (root / "ignore").write_bytes(b"*.jar\n")
        (root / "attributes").write_bytes(b"* filter=hostile\n")

    def configuration(self, named_files: bool) -> str:
        """A global configuration file; with `named_files`, it names its excludes and attributes files."""
        named = (
            f"\texcludesFile = {(self.root / 'ignore').as_posix()}\n"
            f"\tattributesFile = {(self.root / 'attributes').as_posix()}\n"
            if named_files
            else ""
        )
        return (
            "[generatortest]\n\tmarker = hostile\n"
            f"[core]\n\tfsmonitor = {self.monitor.as_posix()}\n\thooksPath = {self.hooks.as_posix()}\n{named}"
            f"[init]\n\ttemplateDir = {self.template.as_posix()}\n\tdefaultObjectFormat = sha256\n"
            f"[commit]\n\tgpgSign = true\n[gpg]\n\tprogram = {(self.root / 'no-gpg').as_posix()}\n"
            '[filter "hostile"]\n\tclean = false\n\trequired = true\n'
            "[i18n]\n\tcommitEncoding = ISO-8859-1\n"
            "[user]\n\tname = Real Person\n\temail = real@example.com\n"
        )

    def _write_hook(self, path: Path) -> None:
        path.write_bytes(f"#!/bin/sh\necho \"$0\" >> '{self.trace.as_posix()}'\nexit 1\n".encode())
        path.chmod(0o755)


def committed(project: Path) -> dict[str, str]:
    """Each path in the project's HEAD commit, with its mode."""
    listed = subprocess.run(
        ["git", "ls-tree", "-r", "-z", "HEAD"], cwd=project, capture_output=True, encoding="utf-8", check=True
    ).stdout
    entries = (entry.split("\t", 1) for entry in listed.split("\0") if entry)
    return {path: meta.split(" ", 1)[0] for meta, path in entries}


@contextlib.contextmanager
def environment(variables: dict[str, str | None]) -> Iterator[None]:
    """This process's environment with `variables` set, or removed where None."""
    with mock.patch.dict(os.environ):
        for name, value in variables.items():
            if value is None:
                os.environ.pop(name, None)
            else:
                os.environ[name] = value
        yield


def extended(path: Path) -> str:
    """`path` in the form Windows does not hold to MAX_PATH, and as it is on other hosts."""
    return "\\\\?\\" + str(path) if os.name == "nt" else str(path)


if __name__ == "__main__":
    unittest.main()
