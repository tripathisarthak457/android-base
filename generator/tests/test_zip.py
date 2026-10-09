"""The zip a user downloads: the same bytes for the same project, executable bits, one root folder."""

from __future__ import annotations

import os
import stat
import tempfile
import time
import unittest
import zipfile
from pathlib import Path

from genkit.core.package import zip_project

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


if __name__ == "__main__":
    unittest.main()
