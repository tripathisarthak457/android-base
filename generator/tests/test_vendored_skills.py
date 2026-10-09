"""
Google's Android skills, vendored into the templates (STANDARDS-PLAN 4): unmodified, licensed,
pointed at from `.claude/skills/`, listed in AGENTS.md exactly when shipped, and never renamed.
"""

from __future__ import annotations

import hashlib
import importlib.util
import re
import sys
import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get

REPOSITORY = Path(__file__).resolve().parents[2]
ANDROID = get("android")
SKILLS = ".agents/skills"
POINTERS = ".claude/skills"


def load_tool():
    spec = importlib.util.spec_from_file_location("update_android_skills", REPOSITORY / "tools/update_android_skills.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


TOOL = load_tool()


def front_matter(path: Path) -> dict[str, str]:
    """`name` and `description` as YAML reads them: continuation lines fold into one space."""
    return {key: " ".join(part.strip() for part in line.split(":", 1)[1].splitlines()) for key, line in TOOL.front_matter(path).items()}


def skill_folders(root: Path) -> set[str]:
    return {path.name for path in (root / SKILLS).iterdir() if path.is_dir()} if (root / SKILLS).is_dir() else set()


def agents_table(root: Path) -> set[str]:
    """Every skill the "Google's Android skills" section of AGENTS.md has a row for."""
    text = (root / "AGENTS.md").read_text(encoding="utf-8")
    section = re.search(r"^## (?:\d+\. )?Google's Android skills\n(.*?)(?=^## |\Z)", text, re.MULTILINE | re.DOTALL)
    if section is None:
        return set()
    names: set[str] = set()
    for line in section.group(1).splitlines():
        if line.startswith("| `"):
            names.update(re.findall(r"`([a-z0-9-]+)`", line.split("|")[1]))
    return names


class TemplateCopyTest(unittest.TestCase):

    root = ANDROID.template_dir

    def test_it_ships_the_planned_set_and_nothing_else(self):
        self.assertEqual({skill.name for skill in TOOL.SKILLS}, skill_folders(self.root))
        for excluded in ("styles", "android-cli", "camerax", "play-policy-insights"):
            self.assertNotIn(excluded, skill_folders(self.root))

    def test_every_skill_has_its_skill_file_and_licence(self):
        for name in skill_folders(self.root):
            with self.subTest(skill=name):
                self.assertTrue((self.root / SKILLS / name / "SKILL.md").is_file())
                self.assertTrue((self.root / SKILLS / name / "LICENSE.txt").is_file())
        self.assertIn("Apache License", (self.root / SKILLS / "LICENSE.txt").read_text(encoding="utf-8"))

    def test_the_copies_match_the_manifest_so_they_are_unmodified(self):
        assert_matches_manifest(self, self.root)

    def test_every_skill_has_a_pointer_with_upstreams_name_and_description(self):
        for name in skill_folders(self.root):
            with self.subTest(skill=name):
                pointer = self.root / POINTERS / name / "SKILL.md"
                self.assertEqual(front_matter(self.root / SKILLS / name / "SKILL.md"), front_matter(pointer))
                self.assertIn(f"`{SKILLS}/{name}/SKILL.md`", pointer.read_text(encoding="utf-8"))
        self.assertEqual(skill_folders(self.root), {path.name for path in (self.root / POINTERS).iterdir()})

    def test_the_readme_names_the_release_it_came_from(self):
        text = (self.root / SKILLS / "README.md").read_text(encoding="utf-8")
        self.assertRegex(text, r"tag v\d+\.\d+\.\d+\n\(commit [0-9a-f]{40}\)")
        for name in skill_folders(self.root):
            self.assertIn(f"| `{name}` |", text)

    def test_identity_skills_belong_to_auth(self):
        owns = set(ANDROID.manifest.features["auth"].owns)
        for skill in TOOL.SKILLS:
            for directory in (SKILLS, POINTERS):
                with self.subTest(skill=skill.name, directory=directory):
                    self.assertEqual(skill.feature == "auth", f"{directory}/{skill.name}" in owns)

    def test_the_vendored_tree_is_copied_verbatim(self):
        self.assertIn(".agents/skills/**", ANDROID.manifest.verbatim)


class RepositoryPointerTest(unittest.TestCase):

    def test_the_repositorys_own_pointers_lead_to_the_android_copy(self):
        android = skill_folders(ANDROID.template_dir)
        for directory in (SKILLS, POINTERS):
            folders = {path.name for path in (REPOSITORY / directory).iterdir() if path.is_dir()}
            self.assertEqual(android, folders, directory)
            for name in folders:
                text = (REPOSITORY / directory / name / "SKILL.md").read_text(encoding="utf-8")
                self.assertIn(f"`templates/android/{SKILLS}/{name}/SKILL.md`", text)


class MultiplatformTest(unittest.TestCase):

    root = REPOSITORY / "templates/multiplatform"

    @unittest.skipUnless((REPOSITORY / "templates/multiplatform" / SKILLS).is_dir(), "no multiplatform template yet")
    def test_it_has_no_agp_skill_and_shares_the_android_copies_byte_for_byte(self):
        self.assertNotIn("agp-9-upgrade", skill_folders(self.root))
        assert_matches_manifest(self, self.root)
        for name in skill_folders(self.root):
            for path in (self.root / SKILLS / name).rglob("*"):
                if path.is_file():
                    twin = ANDROID.template_dir / SKILLS / name / path.relative_to(self.root / SKILLS / name)
                    self.assertEqual(twin.read_bytes(), path.read_bytes(), path)


class GeneratedProjectTest(unittest.TestCase):
    """What a project gets, for the presets that bracket the catalogue, with sign-in on and off."""

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        manifest = ANDROID.manifest
        cases = {
            "lean": manifest.preset_features("lean"),
            "standard": manifest.preset_features("standard"),
            "standard-without-auth": manifest.resolve(manifest.preset_features("standard") - {"auth", "googlesignin"}),
            "everything": manifest.preset_features("everything"),
        }
        cls.projects = {}
        for name, features in cases.items():
            raw = {
                "schema": 2, "platform": "android", "app_name": "Base Application Demo",
                "package_name": "com.example.skills", "features": sorted(features),
            }
            project = Path(cls.temp.name) / name
            pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
            cls.projects[name] = (project, "auth" in features)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_agents_md_lists_exactly_the_skills_the_project_has(self):
        for name, (project, _) in self.projects.items():
            with self.subTest(preset=name):
                self.assertEqual(skill_folders(project), agents_table(project))
                self.assertEqual(skill_folders(project), {path.name for path in (project / POINTERS).iterdir()})

    def test_identity_skills_come_only_with_sign_in(self):
        for name, (project, auth) in self.projects.items():
            with self.subTest(preset=name):
                self.assertEqual(auth, "restore-credentials" in skill_folders(project))
                self.assertEqual(auth, "verified-email" in skill_folders(project))

    def test_the_copies_are_byte_identical_to_the_template(self):
        for name, (project, _) in self.projects.items():
            with self.subTest(preset=name):
                assert_matches_manifest(self, project)

    def test_upstream_text_that_looks_like_the_identity_is_not_renamed(self):
        project, _ = self.projects["lean"]
        hilt = project / SKILLS / "testing-setup/references/android/training/dependency-injection/hilt-testing.md"
        self.assertIn("BaseApplication", hilt.read_text(encoding="utf-8"))


def assert_matches_manifest(case: unittest.TestCase, root: Path) -> None:
    """Every file below the skill folders is listed with its hash, and every listed file is there."""
    listed: dict[str, str] = {}
    for line in (root / SKILLS / "MANIFEST.sha256").read_text(encoding="utf-8").splitlines():
        digest, relative = line.split("  ", 1)
        listed[relative] = digest
    present = skill_folders(root)
    expected = {relative: digest for relative, digest in listed.items() if "/" not in relative or relative.split("/")[0] in present}
    actual: dict[str, str] = {}
    for path in (root / SKILLS).rglob("*"):
        relative = path.relative_to(root / SKILLS).as_posix()
        if path.is_file() and relative not in ("README.md", "MANIFEST.sha256"):
            actual[relative] = hashlib.sha256(path.read_bytes()).hexdigest()
    case.assertEqual(expected, actual)


if __name__ == "__main__":
    unittest.main()
