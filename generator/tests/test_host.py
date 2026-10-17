"""
The generator behaves the same on every host. A newline translated on Windows is invisible on the
Linux CI, so the rule that prevents it is checked in the source, where every host can see it; and
what only one platform's Python defines is kept away from the others by tests that run on all.
"""

from __future__ import annotations

import ast
import os
import stat
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest import mock

import genkit.core.copy as copy_module
from genkit import targets
from genkit.core.copy import is_executable, is_link, template_files, write_text
from genkit.targets.android.variants import VARIANTS_DIR
from tests.links import junction, symlink_or_skip

GENERATOR = Path(__file__).resolve().parent.parent
#: Everything that writes a project, an emitted copy or a template file.
SOURCES = sorted([
    *GENERATOR.glob("*.py"),
    *(GENERATOR / "genkit").rglob("*.py"),
    *(GENERATOR.parent / "tools").glob("*.py"),
])
#: CRLF on purpose, as the repository's .gitattributes checks them out: cmd.exe can miss a label in
#: an LF batch file.
BATCH_SUFFIXES = frozenset({".bat", ".cmd"})


def text_writes(source: str) -> list[int]:
    """
    Lines that can write text with the host's line endings: `.write_text(...)`, or an `open` for text
    without `newline`, which `codecs.open` cannot take (it translates when given no encoding).
    """
    lines = []
    for node in ast.walk(ast.parse(source)):
        if not isinstance(node, ast.Call):
            continue
        function = node.func
        name = function.attr if isinstance(function, ast.Attribute) else getattr(function, "id", None)
        if name == "write_text" and isinstance(function, ast.Attribute):
            lines.append(node.lineno)
        elif name == "open" and _writes_text(node) and not any(keyword.arg == "newline" for keyword in node.keywords):
            lines.append(node.lineno)
    return lines


def _text_with_cr(path: Path) -> bool:
    data = path.read_bytes()
    # Git's own test for text under text=auto: no NUL byte in the first 8000.
    return b"\r" in data and b"\0" not in data[:8000]


def _writes_text(call: ast.Call) -> bool:
    # `open(file, mode)`, `io.open` and `codecs.open` take the mode second, `Path.open(mode)` first.
    function = call.func
    takes_file = isinstance(function, ast.Name) or (
        isinstance(function.value, ast.Name) and function.value.id in {"io", "codecs"}
    )
    position = 1 if takes_file else 0
    modes = [keyword.value for keyword in call.keywords if keyword.arg == "mode"]
    if len(call.args) > position:
        modes.append(call.args[position])
    return any(
        isinstance(mode, ast.Constant) and isinstance(mode.value, str)
        and "b" not in mode.value and any(flag in mode.value for flag in "wax+")
        for mode in modes
    )


class HostTest(unittest.TestCase):

    def test_text_is_written_only_through_write_text(self):
        found = {
            f"{path.relative_to(GENERATOR.parent).as_posix()}:{line}"
            for path in SOURCES
            for line in text_writes(path.read_text(encoding="utf-8"))
        }
        self.assertEqual(set(), found, "write through genkit.core.copy.write_text")

    def test_the_check_sees_each_form_of_text_write(self):
        source = "\n".join([
            "path.write_text(text)",
            "open(name, 'w')",
            "path.open(mode='a', encoding='utf-8')",
            "open(name, 'wb')",
            "open(name, 'w', newline='')",
            "path.open()",
            "Image.open(source)",
            "io.open(name, 'w')",
            "codecs.open(name, 'w', 'utf-8')",
            "io.open(name, 'rb')",
        ])
        self.assertEqual([1, 2, 3, 8, 9], sorted(text_writes(source)))

    def test_the_text_the_generator_copies_has_lf_line_endings(self):
        # A clone made under Git for Windows' core.autocrlf=true before the root .gitattributes
        # existed keeps CRLF working files until they are checked out again, and the generator
        # copies template bytes into every project as they are.
        roots = sorted({*(target.template_dir for target in targets.REGISTRY.values()), VARIANTS_DIR})
        found = [
            source.source.relative_to(GENERATOR.parent).as_posix()
            for root in roots
            for source in template_files(root, [], {})
            if source.source.suffix.lower() not in BATCH_SUFFIXES and _text_with_cr(source.source)
        ]
        self.assertEqual(
            [], found,
            "check the working tree out again with LF: git checkout-index --force --all (it overwrites uncommitted changes)",
        )

    def test_write_text_keeps_the_line_endings_it_is_given(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "mixed.txt"
            write_text(path, "lf\ncrlf\r\nend é\n")
            self.assertEqual("lf\ncrlf\r\nend é\n".encode("utf-8"), path.read_bytes())

    def test_executable_patterns_match_with_case_on_every_host(self):
        patterns = ("gradlew", "tools/perf/*.sh")
        for relative, expected in (
            ("gradlew", True), ("tools/perf/run.sh", True), ("GRADLEW", False), ("tools/perf/RUN.SH", False),
            ("Tools/perf/run.sh", False), ("gradlew.bat", False),
        ):
            with self.subTest(relative):
                self.assertEqual(expected, is_executable(relative, patterns))


class LinkTest(unittest.TestCase):
    """`is_link`, which the replace guard asks of whatever --out names, on every host."""

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.directory = self.root / "directory"
        self.directory.mkdir()

    def test_a_directory_a_file_and_nothing_are_not_links(self):
        file = self.root / "file.txt"
        write_text(file, "text")
        for path in (self.directory, file, self.root / "missing"):
            with self.subTest(path=path.name):
                self.assertFalse(is_link(path))

    def test_a_directory_is_not_a_link_where_stat_has_no_reparse_tags(self):
        # As on Linux and macOS, where CPython defines no IO_REPARSE_TAG_* constants.
        without_reparse_tags = SimpleNamespace(S_ISLNK=stat.S_ISLNK)
        with mock.patch.object(copy_module, "stat", without_reparse_tags), \
                mock.patch.object(copy_module, "os", SimpleNamespace(name="posix")):
            self.assertFalse(is_link(self.directory))

    def test_a_symbolic_link_is_a_link(self):
        link = self.root / "link"
        symlink_or_skip(self, link, self.directory)
        self.assertTrue(is_link(link))

    @unittest.skipUnless(os.name == "nt", "junctions exist only on Windows")
    def test_a_junction_is_a_link(self):
        link = self.root / "junction"
        junction(link, self.directory)
        self.assertTrue(is_link(link))


if __name__ == "__main__":
    unittest.main()
