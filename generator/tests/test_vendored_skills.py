"""
Google's Android skills, vendored into the templates (STANDARDS-PLAN 4): the whole official set
except three skills left out with a reason, in every project, unmodified, licensed, pointed at from
`.claude/skills/`, listed in CLAUDE.md and AGENTS.md exactly when shipped, never renamed, and
refreshable from inside a generated project with one command.
"""

from __future__ import annotations

import contextlib
import hashlib
import importlib.util
import io
import re
import shutil
import subprocess
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
PROJECT_TOOL = "tools/android_skills.py"


def load_tool():
    spec = importlib.util.spec_from_file_location("update_android_skills", REPOSITORY / "tools/update_android_skills.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


TOOL = load_tool()
PROJECT = TOOL.skills


def front_matter(path: Path) -> dict[str, str]:
    """`name` and `description` as YAML reads them: continuation lines fold into one space."""
    fields = PROJECT.front_matter(path.read_text(encoding="utf-8"), str(path))
    return {key: " ".join(part.strip() for part in line.split(":", 1)[1].splitlines()) for key, line in fields.items()}


def skill_folders(root: Path) -> set[str]:
    return {path.name for path in (root / SKILLS).iterdir() if path.is_dir()} if (root / SKILLS).is_dir() else set()


def skills_table(root: Path, document: str = "AGENTS.md") -> set[str]:
    """Every skill the "Google's Android skills" section of `document` has a row for."""
    text = (root / document).read_text(encoding="utf-8")
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
        self.assertEqual(
            {"styles", "migrate-xml-views-to-jetpack-compose", "leanback-to-compose-tv-migration"}, set(PROJECT.LEFT_OUT)
        )
        self.assertFalse(set(PROJECT.LEFT_OUT) & skill_folders(self.root))

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

    def test_the_readme_names_the_release_it_came_from_and_the_refresh_command(self):
        text = (self.root / SKILLS / "README.md").read_text(encoding="utf-8")
        self.assertRegex(text, r"tag v\d+\.\d+\.\d+\n\(commit [0-9a-f]{40}\)")
        for name in skill_folders(self.root):
            self.assertIn(f"| `{name}` |", text)
        for name in PROJECT.LEFT_OUT:
            self.assertIn(f"- `{name}`: ", text)
        self.assertIn(f"python3 {PROJECT_TOOL} --tag", text)

    def test_no_feature_or_option_owns_a_skill_because_every_project_ships_them_all(self):
        manifest = ANDROID.manifest
        owners = {f"feature {key}": feature.owns for key, feature in manifest.features.items()}
        owners.update({f"option {key}": option.owns for key, option in getattr(manifest, "options", {}).items()})
        for owner, owns in owners.items():
            for path in owns:
                with self.subTest(owner=owner, path=path):
                    self.assertFalse(path.startswith((SKILLS, POINTERS)))

    def test_every_page_a_code_rule_cites_ships_with_every_project(self):
        guard = self.root / "build-logic/convention/src/main/kotlin/com/base/app/buildlogic/guard"
        cited = {page for path in guard.glob("*.kt") for page in re.findall(r'\breference = "([^"]+)"', path.read_text(encoding="utf-8"))}
        self.assertTrue(cited)
        for page in cited:
            with self.subTest(page=page):
                self.assertIn(page.split("/")[0], {skill.name for skill in TOOL.SKILLS})
                self.assertTrue((self.root / SKILLS / page).is_file())

    def test_the_vendored_tree_is_copied_verbatim(self):
        self.assertIn(".agents/skills/**", ANDROID.manifest.verbatim)

    def test_the_project_tool_writes_the_readme_and_pointers_the_template_has(self):
        readme = (self.root / SKILLS / "README.md").read_text(encoding="utf-8")
        tag, commit = re.search(r"tag (v[\d.]+)\n\(commit ([0-9a-f]{40})\)", readme).groups()
        release = PROJECT.Release(tag=tag, commit=commit)
        upstream = dict(re.findall(r"^\| `([a-z0-9-]+)` \| `([^`]+)` \|$", readme, re.MULTILINE))
        self.assertEqual(skill_folders(self.root), set(upstream))
        self.assertEqual(PROJECT.readme(upstream, release), (self.root / SKILLS / "README.md").read_bytes())
        for name in upstream:
            with self.subTest(skill=name):
                fields = PROJECT.front_matter((self.root / SKILLS / name / "SKILL.md").read_text(encoding="utf-8"), name)
                self.assertEqual(PROJECT.pointer(name, fields, release), (self.root / POINTERS / name / "SKILL.md").read_bytes())


@unittest.skipIf(shutil.which("git") is None, "git is not on the PATH")
class ToolTest(unittest.TestCase):
    """Both tools against a small repository shaped like upstream's, built here so no test needs the network."""

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.checkout = Path(self.temp.name) / "upstream"
        self.checkout.mkdir()
        self.git("init", "--quiet")
        self.git("config", "core.autocrlf", "true")
        self.commit_skills({"alpha": "group/alpha", "beta": "beta"}, "v1.0.0")

    def git(self, *command: str, stdin: bytes | None = None) -> str:
        result = subprocess.run(
            ["git", "-C", str(self.checkout), "-c", "user.name=Test", "-c", "user.email=test@example.com", *command],
            input=stdin, capture_output=True, check=True,
        )
        return result.stdout.decode().strip()

    def commit_skills(self, skills: dict[str, str], tag: str) -> None:
        for path in list(self.checkout.iterdir()):
            if path.name != ".git":
                shutil.rmtree(path) if path.is_dir() else path.unlink()
        files = {"LICENSE.txt": b"Apache License\nVersion 2.0\n"}
        for name, folder in skills.items():
            files[f"{folder}/SKILL.md"] = f"---\nname: {name}\ndescription: Does\n  {name} things.\n---\n\nBody\n".encode()
            files[f"{folder}/references/notes.md"] = b"line one\nline two\n"
        for relative, content in files.items():
            path = self.checkout / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
        self.git("add", "--all")
        self.git("commit", "--quiet", "-m", tag)
        self.git("tag", tag)
        # A checkout with core.autocrlf on rewrites the working files with CRLF, as on this machine.
        for relative in files:
            (self.checkout / relative).unlink()
        self.git("checkout", "--", ".")

    def test_files_come_from_git_objects_not_the_converted_checkout(self):
        self.assertIn(b"\r\n", (self.checkout / "beta/references/notes.md").read_bytes())
        files = PROJECT.export(self.checkout, ["beta"])
        self.assertEqual(b"line one\nline two\n", files["beta/references/notes.md"])
        self.assertEqual({"LICENSE.txt", "beta/SKILL.md", "beta/references/notes.md"}, set(files))

    def test_upstream_skills_are_found_by_their_skill_file(self):
        self.assertEqual({"alpha": "group/alpha", "beta": "beta"}, PROJECT.upstream_skills(self.checkout))

    def test_a_link_upstream_is_refused(self):
        blob = self.git("hash-object", "-w", "--stdin", stdin=b"../../outside")
        self.git("update-index", "--add", "--cacheinfo", f"120000,{blob},beta/escape")
        self.git("commit", "--quiet", "-m", "link")
        with self.assertRaisesRegex(PROJECT.SkillsError, "beta/escape is not a regular file"):
            PROJECT.export(self.checkout, ["beta"])

    def refresh(self, project: Path, tag: str, *extra: str) -> tuple[int, str]:
        with contextlib.redirect_stdout(io.StringIO()) as output, contextlib.redirect_stderr(output):
            code = PROJECT.main(["--tag", tag, "--checkout", str(self.checkout), "--project", str(project), *extra])
        return code, output.getvalue()

    def test_the_project_tool_refreshes_what_the_project_ships_and_names_what_is_new(self):
        project = Path(self.temp.name) / "project"
        project.mkdir()
        with contextlib.redirect_stdout(io.StringIO()) as output:
            self.assertEqual(0, PROJECT.main(
                ["--tag", "v1.0.0", "--checkout", str(self.checkout), "--project", str(project), "--add", "beta"]
            ))
        self.assertEqual({"beta"}, skill_folders(project))
        self.assertIn("Not shipped yet (add with --add <name>): alpha", output.getvalue())
        self.assertEqual(b"line one\nline two\n", (project / SKILLS / "beta/references/notes.md").read_bytes())
        self.assertEqual(b"Apache License\nVersion 2.0\n", (project / SKILLS / "beta/LICENSE.txt").read_bytes())
        self.assertIn("`.agents/skills/beta/SKILL.md`", (project / POINTERS / "beta/SKILL.md").read_text(encoding="utf-8"))
        assert_matches_manifest(self, project)

        with contextlib.redirect_stdout(io.StringIO()):
            PROJECT.main(["--tag", "v1.0.0", "--checkout", str(self.checkout), "--project", str(project), "--add", "alpha"])
        self.assertEqual({"alpha", "beta"}, skill_folders(project))
        self.assertTrue((project / SKILLS / "alpha/references/notes.md").is_file())

        self.commit_skills({"alpha": "group/alpha"}, "v1.0.1")
        with contextlib.redirect_stdout(io.StringIO()) as output:
            PROJECT.main(["--tag", "v1.0.1", "--checkout", str(self.checkout), "--project", str(project)])
        self.assertEqual({"alpha"}, skill_folders(project))
        self.assertEqual({"alpha"}, {path.name for path in (project / POINTERS).iterdir()})
        self.assertIn("no longer has them: beta", output.getvalue())

    def test_the_project_tool_leaves_the_projects_own_skills_alone(self):
        project = Path(self.temp.name) / "project"
        project.mkdir()
        self.assertEqual(0, self.refresh(project, "v1.0.0", "--add", "beta")[0])
        own = {
            f"{POINTERS}/my-own-skill/SKILL.md": b"---\nname: my-own-skill\ndescription: Ours.\n---\n",
            f"{SKILLS}/my-team-skill/SKILL.md": b"---\nname: my-team-skill\ndescription: The team's.\n---\n",
            f"{SKILLS}/my-team-skill/references/notes.md": b"kept\n",
        }
        for relative, content in own.items():
            (project / relative).parent.mkdir(parents=True, exist_ok=True)
            (project / relative).write_bytes(content)

        self.assertEqual(0, self.refresh(project, "v1.0.0")[0])
        self.commit_skills({"alpha": "group/alpha"}, "v1.0.1")
        code, output = self.refresh(project, "v1.0.1")

        self.assertEqual(0, code)
        self.assertIn("no longer has them: beta\n", output)
        self.assertNotIn("my-team-skill", output)
        self.assertEqual({"my-team-skill"}, skill_folders(project))
        self.assertEqual({"my-own-skill"}, {path.name for path in (project / POINTERS).iterdir()})
        for relative, content in own.items():
            self.assertEqual(content, (project / relative).read_bytes(), relative)
        self.assertNotIn("my-team-skill", (project / SKILLS / "MANIFEST.sha256").read_text(encoding="utf-8"))

    def test_the_project_tool_refuses_to_write_over_a_skill_it_did_not_vendor(self):
        project = Path(self.temp.name) / "project"
        project.mkdir()
        self.assertEqual(0, self.refresh(project, "v1.0.0", "--add", "beta")[0])
        theirs = project / SKILLS / "alpha" / "SKILL.md"
        theirs.parent.mkdir(parents=True)
        theirs.write_bytes(b"their own alpha\n")
        before = {path: path.read_bytes() for path in project.rglob("*") if path.is_file()}

        code, output = self.refresh(project, "v1.0.0", "--add", "alpha")

        self.assertEqual(1, code)
        self.assertIn("alpha would replace a folder this tool did not vendor", output)
        self.assertEqual(before, {path: path.read_bytes() for path in project.rglob("*") if path.is_file()})

    def test_the_project_tool_refuses_a_checkout_at_another_tag(self):
        with contextlib.redirect_stderr(io.StringIO()) as errors:
            code = PROJECT.main(["--tag", "v9.9.9", "--checkout", str(self.checkout), "--project", self.temp.name])
        self.assertEqual(1, code)
        self.assertIn("not at v9.9.9", errors.getvalue())

    def test_the_repository_tool_stops_on_an_upstream_skill_it_has_no_plan_for(self):
        planned = {skill.name: skill.upstream for skill in TOOL.SKILLS}
        TOOL.check_planned(planned | {"styles": "jetpack-compose/theming/styles"})
        with self.assertRaisesRegex(PROJECT.SkillsError, "in neither SKILLS nor LEFT_OUT: brand-new"):
            TOOL.check_planned(planned | {"brand-new": "new/brand-new"})
        with self.assertRaisesRegex(PROJECT.SkillsError, "adaptive is gone"):
            TOOL.check_planned({name: folder for name, folder in planned.items() if name != "adaptive"})


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

    @unittest.skipUnless((REPOSITORY / "templates/multiplatform" / SKILLS).is_dir(), "no multiplatform skills yet")
    def test_it_has_no_agp_skill_and_shares_the_android_copies_byte_for_byte(self):
        self.assertNotIn("agp-9-upgrade", skill_folders(self.root))
        assert_matches_manifest(self, self.root)
        for name in skill_folders(self.root):
            for path in (self.root / SKILLS / name).rglob("*"):
                if path.is_file():
                    twin = ANDROID.template_dir / SKILLS / name / path.relative_to(self.root / SKILLS / name)
                    self.assertEqual(twin.read_bytes(), path.read_bytes(), path)

    def test_skills_are_vendored_only_where_the_generator_copies_them_verbatim(self):
        self.assertEqual(
            TOOL.copies_skills_verbatim(self.root), (self.root / SKILLS).is_dir(),
            "a template that renames files must not carry upstream's skills",
        )


class GeneratedProjectTest(unittest.TestCase):
    """What a project gets, for the presets at both ends of the catalogue, with sign-in on and off."""

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        manifest = ANDROID.manifest
        cases = {
            "none": [],
            "lean": manifest.preset_features("lean"),
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
            cls.projects[name] = project

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_every_project_ships_the_whole_set(self):
        for name, project in self.projects.items():
            with self.subTest(preset=name):
                self.assertEqual({skill.name for skill in TOOL.SKILLS}, skill_folders(project))
                self.assertEqual(skill_folders(project), {path.name for path in (project / POINTERS).iterdir()})

    def test_claude_md_and_agents_md_list_exactly_the_skills_the_project_has(self):
        for name, project in self.projects.items():
            for document in ("CLAUDE.md", "AGENTS.md"):
                with self.subTest(preset=name, document=document):
                    self.assertEqual(skill_folders(project), skills_table(project, document))

    def test_the_copies_are_byte_identical_to_the_template(self):
        for name, project in self.projects.items():
            with self.subTest(preset=name):
                assert_matches_manifest(self, project)

    def test_the_refresh_command_ships_unrenamed(self):
        for name, project in self.projects.items():
            with self.subTest(preset=name):
                self.assertEqual((ANDROID.template_dir / PROJECT_TOOL).read_bytes(), (project / PROJECT_TOOL).read_bytes())

    def test_upstream_text_that_looks_like_the_identity_is_not_renamed(self):
        hilt = self.projects["lean"] / SKILLS / "testing-setup/references/android/training/dependency-injection/hilt-testing.md"
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
