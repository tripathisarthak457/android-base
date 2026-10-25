"""
Navigation in every project shape: the runtime (`:core:navigation`) apart from the shell that draws it
(`:core:navigation:shell`), features on the runtime alone, a destination registered in one step, and each feature's
navigation in one file.
"""

from __future__ import annotations

import re
import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.core.spec import pascal_case
from genkit.targets import get

ANDROID = get("android")
PACKAGE = "com.acme.nav"
PKG = PACKAGE.replace(".", "/")

#: Every preset in both architectures, with and without UI components, and each kind with a scaffolded module.
SHAPES = {
    **{
        f"{'ui' if components else 'noui'}-{preset}-{architecture}": {
            "features": sorted(ANDROID.manifest.preset_features(preset)),
            "architecture": architecture,
            **({} if components else {"ui_components": False}),
        }
        for components in (True, False)
        for preset in ("lean", "standard", "everything")
        for architecture in ("compact", "modular")
    },
    "ui-everything-modules": {"features": sorted(ANDROID.manifest.preset_features("everything")), "feature_modules": ["orders"]},
    "noui-none-modules": {"features": [], "feature_modules": ["order_history"], "ui_components": False},
}

#: The shell, by package or by module path: what a feature never reaches.
SHELL = re.compile(r"\.core\.navigation\.shell\b|:core:navigation:shell\b|projects\.core\.navigation\.shell\b")
DESIGN_SYSTEM = re.compile(r"\.core\.designsystem\b|:core:designsystem\b|projects\.core\.designsystem\b")
HOST_API = re.compile(r"\bNavigationHostApi\b")
NAV_KEYS = re.compile(r"\bnavKeys\b")
#: A Hilt provider of a serializers module: the second registration step, which no longer exists.
SERIALIZERS_PROVIDER = re.compile(r"\)\s*:\s*SerializersModule\b")
#: The navigation files a feature had before each moved to navigation/<Feature>Navigation.kt.
OLD_NAVIGATION_FILE = re.compile(r"(?:Nav|NavModule)\.kt$")
_COMMENT = re.compile(r"/\*.*?\*/|//[^\n]*", re.DOTALL)
DOCS = ("AGENTS.md", "CLAUDE.md", "docs/ARCHITECTURE.md", "docs/PERFORMANCE.md", "docs/DESIGN-SYSTEM.md")


def generate(directory: Path, name: str, **fields) -> Path:
    raw = {"schema": 2, "platform": "android", "app_name": "Acme Nav", "package_name": PACKAGE, **fields}
    project = directory / name
    pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
    return project


def code(path: Path) -> str:
    """The file without its comments: what the compiler reads."""
    return _COMMENT.sub("", path.read_text(encoding="utf-8"))


def sources(root: Path) -> list[Path]:
    """Kotlin and Gradle files under `root`, outside build output."""
    return sorted(
        path for path in root.rglob("*")
        if path.is_file() and path.suffix in (".kt", ".kts") and "build" not in path.relative_to(root).parts[:-1]
    )


def feature_sources(feature: Path) -> list[Path]:
    """A feature's main sources, in either architecture: `feature/x/src` or `feature/x/{api,impl}/src`."""
    return sorted(path for path in feature.rglob("src/main/kotlin/**/*.kt"))


def lines_matching(paths: list[Path], pattern: re.Pattern[str], project: Path, strip_comments: bool = False) -> list[str]:
    found = []
    for path in paths:
        text = code(path) if strip_comments else path.read_text(encoding="utf-8")
        for number, line in enumerate(text.splitlines(), 1):
            if pattern.search(line):
                found.append(f"{path.relative_to(project).as_posix()}:{number}: {line.strip()[:100]}")
    return found


class NavigationShapeTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.projects = {name: generate(Path(cls.temp.name), name, **fields) for name, fields in SHAPES.items()}

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def features(self, project: Path) -> list[Path]:
        root = project / "feature"
        return sorted(path for path in root.iterdir() if path.is_dir()) if root.is_dir() else []

    def test_no_feature_reaches_the_shell_or_opts_in_to_the_host_api(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                files = [path for feature in self.features(project) for path in sources(feature)]
                self.assertTrue(files or "lean" in name, "no feature sources were read")
                self.assertEqual([], lines_matching(files, SHELL, project))
                self.assertEqual([], lines_matching(files, HOST_API, project))

    def test_the_runtime_never_depends_on_the_design_system_or_the_shell(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                runtime = project / "core/navigation"
                build = runtime / "build.gradle.kts"
                files = [build, *sorted((runtime / "src").rglob("*.kt"))]
                self.assertEqual([], lines_matching([build], DESIGN_SYSTEM, project, strip_comments=True))
                self.assertEqual([], lines_matching(files, DESIGN_SYSTEM, project, strip_comments=True))
                self.assertEqual([], lines_matching(files, SHELL, project, strip_comments=True))

    def test_the_shell_is_a_module_of_its_own_only_with_ui_components(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                shell = project / "core/navigation/shell"
                settings = (project / "settings.gradle.kts").read_text(encoding="utf-8")
                builds = [path for path in sources(project) if path.name == "build.gradle.kts"]
                if name.startswith("noui"):
                    self.assertFalse(shell.exists())
                    self.assertNotIn(":core:navigation:shell", settings)
                    self.assertEqual([], lines_matching(builds, SHELL, project))
                    continue
                self.assertIn('include(":core:navigation:shell")', settings)
                build = (shell / "build.gradle.kts").read_text(encoding="utf-8")
                self.assertIn('api(project(":core:navigation"))', build)
                self.assertIn(f'namespace = "{PACKAGE}.core.navigation.shell"', build)
                packages = {
                    re.search(r"^package ([\w.]+)", path.read_text(encoding="utf-8"), re.MULTILINE).group(1)
                    for path in (shell / "src").rglob("*.kt")
                }
                outside = sorted(p for p in packages if not p.startswith(f"{PACKAGE}.core.navigation.shell"))
                self.assertEqual([], outside)
                for composition_root in ("app", "catalog"):
                    root = project / composition_root / "build.gradle.kts"
                    if root.is_file():
                        self.assertIn('implementation(project(":core:navigation:shell"))', root.read_text(encoding="utf-8"))

    def test_every_feature_s_navigation_is_one_object_at_navigation_feature_navigation(self):
        for name, project in self.projects.items():
            for feature in self.features(project):
                with self.subTest(project=name, feature=feature.name):
                    files = feature_sources(feature)
                    pascal = pascal_case(feature.name)
                    expected = f"{PKG}/feature/{feature.name}/navigation/{pascal}Navigation.kt"
                    registering = [
                        path for path in files if re.search(r"\bnavGraph\s*\{", code(path))
                    ]
                    self.assertEqual([expected], [path.as_posix().split("/src/main/kotlin/")[1] for path in registering])
                    self.assertIn(f"internal object {pascal}Navigation", registering[0].read_text(encoding="utf-8"))
                    self.assertEqual([], [path.name for path in files if OLD_NAVIGATION_FILE.search(path.name)])

    def test_a_destination_is_registered_in_one_step(self):
        for name, project in self.projects.items():
            with self.subTest(project=name):
                files = sources(project)
                self.assertEqual([], lines_matching(files, NAV_KEYS, project, strip_comments=True))
                self.assertEqual([], lines_matching(files, SERIALIZERS_PROVIDER, project, strip_comments=True))
                docs = [project / doc for doc in DOCS if (project / doc).is_file()]
                self.assertTrue(docs)
                self.assertEqual([], lines_matching(docs, NAV_KEYS, project))


if __name__ == "__main__":
    unittest.main()
