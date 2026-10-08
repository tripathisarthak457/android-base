"""create_project.py as scripts and agents call it: --json, --out, --force, --set, --dry-run."""

from __future__ import annotations

import json
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

GENERATOR = Path(__file__).resolve().parent.parent
SPEC = {"schema": 2, "platform": "android", "app_name": "Json Check", "package_name": "com.example.jsoncheck"}


def run(*arguments: str, cwd: Path | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(
        [sys.executable, str(GENERATOR / "create_project.py"), *arguments],
        capture_output=True, text=True, check=False, cwd=cwd or GENERATOR,
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

    def test_a_zip_path_must_say_zip(self):
        result = run("--spec", str(self.spec), "--out", str(self.root / "app"), "--json")
        self.assertEqual(2, result.returncode)
        self.assertEqual("out", json.loads(result.stdout)["field"])

    def test_force_replaces_exactly_the_destination(self):
        # The old --out meant the parent with a zip and the project without, and --force once
        # deleted a sibling project (audit 4.6).
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


if __name__ == "__main__":
    unittest.main()
