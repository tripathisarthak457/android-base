"""The API's contract: one JSON object on stdout, and exit codes 0, 2 and 1 that say whose fault it was."""

from __future__ import annotations

import io
import json
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock

from genkit.core import headless, pipeline, record
from genkit.targets import get

GENERATOR = Path(__file__).resolve().parent.parent
SPEC = {"schema": 2, "platform": "android", "app_name": "Head Less", "package_name": "com.example.headless"}


def run_script(stdin: bytes, *arguments: str) -> tuple[int, dict]:
    result = subprocess.run(
        [sys.executable, str(GENERATOR / "generate_headless.py"), *arguments],
        input=stdin, capture_output=True, check=False,
    )
    lines = result.stdout.decode("utf-8").splitlines()
    assert len(lines) == 1, f"stdout must be exactly one JSON object, got: {result.stdout!r}"
    return result.returncode, json.loads(lines[0])


class ScriptTest(unittest.TestCase):

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.out = Path(temp.name) / "out.zip"

    def test_a_good_spec_writes_a_zip_and_says_so(self):
        code, result = run_script(json.dumps({**SPEC, "features": ["offlinequeue"]}).encode(), "--target", "android", str(self.out))
        self.assertEqual(0, code)
        self.assertTrue(result["ok"])
        self.assertEqual("android", result["target"])
        self.assertEqual("HeadLess", result["project_name"])
        self.assertEqual(self.out.stat().st_size, result["zip_bytes"])
        self.assertEqual(["database", "network", "offlinequeue"], result["resolved_features"])
        for key in ("elapsed_ms", "notes", "warnings", "ignored_fields", "generator_commit"):
            self.assertIn(key, result)
        with zipfile.ZipFile(self.out) as archive:
            self.assertIn("HeadLess/settings.gradle.kts", archive.namelist())

    def test_a_schema_one_spec_reports_what_it_ignored(self):
        code, result = run_script(
            json.dumps({"app_name": "Old", "package_name": "com.example.old", "colour": "red"}).encode(), str(self.out)
        )
        self.assertEqual(0, code)
        self.assertEqual(["colour"], result["ignored_fields"])
        self.assertTrue(result["notes"])

    def test_refusals_exit_2_with_the_field(self):
        cases = {
            b'{"schema": 2, "platform": "android", "app_name": "x"}': "package_name",
            json.dumps({**SPEC, "feature_modules": "home"}).encode(): "feature_modules",
            b"[]": None,
            b"null": None,
            b"not json": None,
            b"\xff\xfe": None,
        }
        for stdin, field in cases.items():
            with self.subTest(stdin=stdin[:60]):
                code, result = run_script(stdin, "--target", "android", str(self.out))
                self.assertEqual(2, code)
                self.assertEqual({"ok": False, "kind": "invalid", "error": result["error"], "field": field}, result)
                self.assertNotIn("detail", result)

    def test_a_path_on_the_server_is_never_read(self):
        keystore = {"name": "dev", "alias": "a", "store_password": "secret1", "key_password": "secret1", "existing_path": "/etc/passwd"}
        code, result = run_script(json.dumps({**SPEC, "android": {"keystores": [keystore]}}).encode(), str(self.out))
        self.assertEqual(2, code)
        self.assertEqual("android.keystores", result["field"])
        self.assertIn("existing_path", result["error"])

    def test_more_than_32_kb_is_refused_before_it_is_parsed(self):
        code, result = run_script(b" " * (32 * 1024 + 1), str(self.out))
        self.assertEqual(2, code)
        self.assertIn("32 KB", result["error"])

    def test_an_unknown_target_and_bad_arguments_are_refusals(self):
        self.assertEqual(2, run_script(json.dumps(SPEC).encode(), "--target", "symbian", str(self.out))[0])
        self.assertEqual(2, run_script(json.dumps(SPEC).encode())[0])


class InternalFailureTest(unittest.TestCase):

    def test_a_generator_bug_exits_1_with_its_traceback(self):
        stdout = io.StringIO()
        with tempfile.TemporaryDirectory() as temp, mock.patch.object(
            pipeline, "render_template", side_effect=RuntimeError("template exploded")
        ):
            code = headless.run(["--target", "android", f"{temp}/out.zip"], io.BytesIO(json.dumps(SPEC).encode()), stdout)
        result = json.loads(stdout.getvalue())
        self.assertEqual(1, code)
        self.assertEqual("internal", result["kind"])
        self.assertEqual("The generator failed.", result["error"])
        self.assertIn("template exploded", result["detail"])

    def test_nothing_printed_while_generating_reaches_stdout(self):
        stdout = io.StringIO()
        original = pipeline.render_template

        def noisy(*args, **kwargs):
            print("chatter")
            return original(*args, **kwargs)

        with tempfile.TemporaryDirectory() as temp, mock.patch.object(pipeline, "render_template", noisy), \
                mock.patch("sys.stderr", io.StringIO()):
            code = headless.run(["--target", "android", f"{temp}/out.zip"], io.BytesIO(json.dumps(SPEC).encode()), stdout)
        self.assertEqual(0, code)
        self.assertTrue(json.loads(stdout.getvalue())["ok"])


class CommitStampTest(unittest.TestCase):
    """The commit every response and every generated record carries."""

    def test_a_deployment_without_git_reads_the_commit_ci_wrote(self):
        with tempfile.TemporaryDirectory() as temp:
            repository = Path(temp)
            (repository / "generator").mkdir()
            (repository / "generator" / "COMMIT").write_text("8e3461d0a1b2\n", encoding="utf-8")
            self.assertEqual("8e3461d0a1b2", record.generator_commit(repository))

    def test_no_git_and_no_stamp_is_unknown(self):
        with tempfile.TemporaryDirectory() as temp:
            self.assertEqual("unknown", record.generator_commit(Path(temp)))


class StagingTest(unittest.TestCase):

    def test_staging_lives_under_tmpdir(self):
        # The API sets TMPDIR to its work directory, so a killed generator leaks nothing outside it.
        target = get("android")
        seen: list[Path] = []
        original = target.transform

        def record(project, spec):
            seen.append(project)
            return original(project, spec)

        with tempfile.TemporaryDirectory() as work, mock.patch.object(target, "transform", record), \
                mock.patch("tempfile.tempdir", work):
            headless.run(["--target", "android", f"{work}/out.zip"], io.BytesIO(json.dumps(SPEC).encode()), io.StringIO())
            self.assertTrue(seen)
            self.assertTrue(seen[0].is_relative_to(Path(work)), seen[0])


if __name__ == "__main__":
    unittest.main()
