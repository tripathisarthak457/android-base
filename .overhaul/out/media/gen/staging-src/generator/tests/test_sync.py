"""tools/sync_check.py: the shared code of the Android and multiplatform templates stays one copy."""

from __future__ import annotations

import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPOSITORY / "tools"))

import sync_check  # noqa: E402

ANDROID_FILE = """\
package com.base.app.core.mvi

import java.util.concurrent.atomic.AtomicLong

class Counter {
    private val ids = AtomicLong()
    fun next(): Long = ids.incrementAndGet()
}
"""

COMMON_FILE = """\
package com.base.app.core.mvi

import kotlin.concurrent.atomics.AtomicLong

class Counter {
    private val ids = AtomicLong(0)
    fun next(): Long = ids.incrementAndFetch()
}
"""

ALLOWANCE = """
[[pair.difference]]
file = "Counter.kt"
reason = "No java.util.concurrent on Kotlin/Native."
android = ["    private val ids = AtomicLong()", "    fun next(): Long = ids.incrementAndGet()"]
multiplatform = ["    private val ids = AtomicLong(0)", "    fun next(): Long = ids.incrementAndFetch()"]
"""


class RepositoryTest(unittest.TestCase):

    def test_the_templates_are_in_sync(self):
        self.assertEqual([], sync_check.check(sync_check.load(REPOSITORY)))


class CheckTest(unittest.TestCase):

    def setUp(self):
        self.root = Path(self.enterContext(tempfile.TemporaryDirectory()))

    def tree(self, android: dict[str, str], common: dict[str, str], manifest: str = "") -> list[str]:
        for side, files in (("android", android), ("multiplatform", common)):
            directory = self.root / "templates" / side / "mvi"
            directory.mkdir(parents=True, exist_ok=True)
            for name, text in files.items():
                (directory / name).write_text(text, encoding="utf-8")
        head = '[[pair]]\nname = "mvi"\nandroid = "mvi"\nmultiplatform = "mvi"\n'
        (self.root / sync_check.MANIFEST).write_text(head + textwrap.dedent(manifest), encoding="utf-8")
        return sync_check.check(sync_check.load(self.root))

    def test_identical_files_differing_only_in_imports_pass(self):
        android = "package a\n\nimport java.util.UUID\n\nval x = 1\n"
        common = "package a\n\nimport kotlin.uuid.Uuid\n\nval x = 1\n"
        self.assertEqual([], self.tree({"A.kt": android}, {"A.kt": common}))

    def test_a_difference_nothing_allows_fails_and_names_the_line(self):
        problems = self.tree({"Counter.kt": ANDROID_FILE}, {"Counter.kt": COMMON_FILE})

        self.assertIn("mvi/Counter.kt: only the commonMain copy has 'private val ids = AtomicLong(0)'", problems)
        self.assertIn("mvi/Counter.kt: only the Android copy has   'fun next(): Long = ids.incrementAndGet()'", problems)

    def test_an_allowed_difference_passes(self):
        self.assertEqual([], self.tree({"Counter.kt": ANDROID_FILE}, {"Counter.kt": COMMON_FILE}, ALLOWANCE))

    def test_a_new_difference_beside_an_allowed_one_still_fails(self):
        drifted = COMMON_FILE.replace("class Counter {", "class Counter {\n    val extra = 2")

        problems = self.tree({"Counter.kt": ANDROID_FILE}, {"Counter.kt": drifted}, ALLOWANCE)

        self.assertEqual(["mvi/Counter.kt: only the commonMain copy has 'val extra = 2'"], problems)

    def test_an_allowance_that_no_longer_matches_fails(self):
        problems = self.tree({"Counter.kt": ANDROID_FILE}, {"Counter.kt": ANDROID_FILE}, ALLOWANCE)

        self.assertIn(
            "mvi/Counter.kt: allowed commonMain line no longer differs: 'private val ids = AtomicLong(0)'",
            problems,
        )

    def test_a_file_on_one_side_fails_unless_it_is_listed_with_a_reason(self):
        self.assertEqual(
            ["mvi: Only.kt has no commonMain copy"],
            self.tree({"Only.kt": "val a = 1\n"}, {}),
        )
        listed = '\n[[pair.android_only]]\nfile = "Only.kt"\nreason = "Android Context."\n'
        self.assertEqual([], self.tree({"Only.kt": "val a = 1\n"}, {}, listed))

    def test_a_listing_for_a_file_that_is_now_on_both_sides_fails(self):
        listed = '\n[[pair.android_only]]\nfile = "Both.kt"\nreason = "Android Context."\n'

        problems = self.tree({"Both.kt": "val a = 1\n"}, {"Both.kt": "val a = 1\n"}, listed)

        self.assertEqual(["mvi: Both.kt is listed as Android-only but is not"], problems)

    def test_a_difference_without_a_reason_is_refused(self):
        unexplained = '\n[[pair.difference]]\nfile = "Counter.kt"\nandroid = ["x"]\n'
        with self.assertRaises(sync_check.ManifestError):
            self.tree({"Counter.kt": ANDROID_FILE}, {"Counter.kt": ANDROID_FILE}, unexplained)


if __name__ == "__main__":
    unittest.main()
