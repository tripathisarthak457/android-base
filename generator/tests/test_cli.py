"""create_project.py as scripts and agents call it: --json, --out, --force, --set, --dry-run."""

from __future__ import annotations

import io
import json
import subprocess
import sys
import tempfile
import unittest
import zipfile
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
from unittest import mock

import create_project
from genkit.core import pipeline

GENERATOR = Path(__file__).resolve().parent.parent
SPEC = {"schema": 2, "platform": "android", "app_name": "Json Check", "package_name": "com.example.jsoncheck"}


def run(*arguments: str, cwd: Path | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(
        [sys.executable, str(GENERATOR / "create_project.py"), *arguments],
        capture_output=True, text=True, check=False, cwd=cwd or GENERATOR, stdin=subprocess.DEVNULL,
    )


class CliTest(unittest.TestCase):

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.spec = self.root / "spec.json"
        self.spec.write_text(json.dumps(SPEC), encoding="utf-8")

    def test_json_means_only_json_on_stdout(self):
        result = run("--spec", str(self.spec), "--out", str(self.root / "p"), "--no-zip", "--json")
        self.assertEqual(0, result.returncode, result.stderr)
        data = json.loads(result.stdout)
        self.assertTrue(data["ok"])
        self.assertEqual(str(self.root / "p"), data["path"])
        self.assertTrue((self.root / "p/settings.gradle.kts").is_file())

    def test_a_refusal_with_json_is_the_headless_shape(self):
        self.spec.write_text(json.dumps({**SPEC, "package_name": "com.example.null"}), encoding="utf-8")
        result = run("--spec", str(self.spec), "--out", str(self.root / "p"), "--no-zip", "--json")
        self.assertEqual(2, result.returncode)
        self.assertEqual({"ok": False, "kind": "invalid", "field": "package_name"}, {k: v for k, v in json.loads(result.stdout).items() if k != "error"})

    def test_out_is_the_zip_itself(self):
        result = run("--spec", str(self.spec), "--out", str(self.root / "app.zip"), "--json")
        self.assertEqual(0, result.returncode, result.stderr)
        with zipfile.ZipFile(self.root / "app.zip") as archive:
            self.assertIn("JsonCheck/settings.gradle.kts", archive.namelist())

    def test_json_without_out_is_refused_rather_than_asked(self):
        result = run("--spec", str(self.spec), "--json")
        self.assertEqual(2, result.returncode)
        self.assertEqual("out", json.loads(result.stdout)["field"])

    def test_a_zip_path_must_say_zip(self):
        result = run("--spec", str(self.spec), "--out", str(self.root / "app"), "--json")
        self.assertEqual(2, result.returncode)
        self.assertEqual("out", json.loads(result.stdout)["field"])

    def test_force_replaces_exactly_the_destination(self):
        # --out names the project itself, never its parent, so --force can delete only that path
        # and a sibling project is never at risk.
        sibling = self.root / "parent" / "Sibling"
        sibling.mkdir(parents=True)
        (sibling / "important.txt").write_text("keep", encoding="utf-8")
        destination = self.root / "parent" / "Mine"
        destination.mkdir()
        (destination / "old.txt").write_text("replace", encoding="utf-8")

        refused = run("--spec", str(self.spec), "--out", str(destination), "--no-zip")
        self.assertNotEqual(0, refused.returncode)
        self.assertTrue((destination / "old.txt").exists())

        forced = run("--spec", str(self.spec), "--out", str(destination), "--no-zip", "--force")
        self.assertEqual(0, forced.returncode, forced.stderr)
        self.assertFalse((destination / "old.txt").exists())
        self.assertTrue((destination / "settings.gradle.kts").is_file())
        self.assertTrue((sibling / "important.txt").exists())

    def test_force_replaces_a_link_and_not_the_directory_it_points_at(self):
        real = self.root / "real"
        real.mkdir()
        (real / "precious.txt").write_text("keep", encoding="utf-8")
        link = self.root / "link"
        link.symlink_to(real, target_is_directory=True)

        result = run("--spec", str(self.spec), "--out", str(link), "--no-zip", "--force", "--json")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(str(link), json.loads(result.stdout)["path"])
        self.assertFalse(link.is_symlink())
        self.assertTrue((link / "settings.gradle.kts").is_file())
        self.assertEqual(["precious.txt"], [path.name for path in real.iterdir()])
        self.assertEqual("keep", (real / "precious.txt").read_text(encoding="utf-8"))

    def test_force_never_deletes_the_current_directory(self):
        working = self.root / "work"
        working.mkdir()
        (working / "keep.txt").write_text("keep", encoding="utf-8")
        result = run("--spec", str(self.spec), "--out", ".", "--no-zip", "--force", cwd=working)
        self.assertNotEqual(0, result.returncode)
        self.assertTrue((working / "keep.txt").exists())

    def test_set_overrides_after_the_spec_is_read(self):
        result = run(
            "--spec", str(self.spec), "--set", "package_name=com.example.wp01.std", "--set", "android.min_sdk=28",
            "--dry-run", "--json",
        )
        self.assertEqual(0, result.returncode, result.stderr)
        plan = json.loads(result.stdout)
        self.assertEqual({"project_name", "enabled", "disabled", "removed_paths", "modules", "tabs", "notes", "warnings"}, set(plan))
        self.assertTrue(any("com/example/wp01/std" in path for path in plan["removed_paths"]))

    def test_a_preset_replaces_a_specs_features(self):
        result = run("--spec", str(self.spec), "--preset", "lean", "--dry-run", "--json")
        self.assertEqual(["catalog", "leakcanary", "network", "sample"], json.loads(result.stdout)["enabled"])

    def test_list_features_json(self):
        features = json.loads(run("--list-features", "--json").stdout)
        self.assertIn("network", {feature["key"] for feature in features})


class FailedReplaceTest(unittest.TestCase):
    """--force deletes the old output only once the new one is complete."""

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.spec = self.root / "spec.json"
        self.spec.write_text(json.dumps(SPEC), encoding="utf-8")

    def generate(self, destination: Path, *arguments: str) -> tuple[int, dict]:
        with redirect_stdout(io.StringIO()) as out, redirect_stderr(io.StringIO()):
            code = create_project.main(["--spec", str(self.spec), "--out", str(destination), "--force", "--json", *arguments])
        return code, json.loads(out.getvalue())

    def test_a_failed_regenerate_keeps_the_old_project(self):
        destination = self.root / "Mine"
        destination.mkdir()
        (destination / "old.txt").write_text("keep", encoding="utf-8")
        with mock.patch.object(pipeline.record, "write", side_effect=OSError("disk full")):
            code, answer = self.generate(destination, "--no-zip")
        self.assertEqual((1, "internal"), (code, answer["kind"]))
        self.assertTrue((destination / "old.txt").exists())
        self.assertEqual(["Mine", "spec.json"], sorted(path.name for path in self.root.iterdir()))

    def test_a_failed_zip_keeps_the_old_zip(self):
        destination = self.root / "app.zip"
        destination.write_bytes(b"old")

        def half_written(project, path, executable):
            path.write_bytes(b"half")
            raise OSError("disk full")

        with mock.patch.object(pipeline.package, "zip_project", side_effect=half_written):
            code, answer = self.generate(destination)
        self.assertEqual((1, "internal"), (code, answer["kind"]))
        self.assertEqual(b"old", destination.read_bytes())
        self.assertEqual(["app.zip", "spec.json"], sorted(path.name for path in self.root.iterdir()))

    def test_a_forced_regenerate_replaces_a_zip(self):
        destination = self.root / "app.zip"
        destination.write_bytes(b"old")
        code, answer = self.generate(destination)
        self.assertEqual(0, code, answer)
        with zipfile.ZipFile(destination) as archive:
            self.assertIn("JsonCheck/settings.gradle.kts", archive.namelist())


if __name__ == "__main__":
    unittest.main()
