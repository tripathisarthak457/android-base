"""
The design system's contract in generated projects, with and without UI components: every component
file carries a preview the screenshot tests render, the preview annotation is in every project, and
a theme skeleton holds no token that only the template's components read.
"""

from __future__ import annotations

import re
import shutil
import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get
from genkit.tokens import kotlin

ANDROID = get("android")
PACKAGE = "com.acme.design"
PKG = PACKAGE.replace(".", "/")
DESIGN_SYSTEM = f"core/designsystem/src/main/kotlin/{PKG}/core/designsystem"

#: The design system's plumbing, which draws nothing a preview could show: the theme and its
#: tokens, the window's size classes and the preview annotations themselves.
PLUMBING = ("theme/", "foundation/adaptive/", "preview/")

_COMPOSABLE = re.compile(r"@Composable\s+((?:(?:public|internal|private|inline)\s+)*)fun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)\s*\(")
_MODIFIER_PARAMETER = re.compile(r"\bmodifier\s*:\s*Modifier\b")
_PREVIEW = re.compile(r"@AppComponentPreviews\s+@Composable\s+private\s+fun\s+\w+Preview\s*\(")


def generate(directory: Path, name: str, **fields) -> Path:
    raw = {"schema": 2, "platform": "android", "app_name": "Acme Design", "package_name": PACKAGE, **fields}
    project = directory / name
    pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
    return project


def components(source: str) -> list[str]:
    """
    The components a file declares: every composable that is not private and takes a `modifier`, as
    the component-preview rule counts them.
    """
    found = []
    for match in _COMPOSABLE.finditer(source):
        if "private" in match.group(1):
            continue
        start = match.end() - 1
        depth = 0
        for end in range(start, len(source)):
            depth += {"(": 1, ")": -1}.get(source[end], 0)
            if depth == 0:
                break
        if _MODIFIER_PARAMETER.search(source[start:end]):
            found.append(match.group(2))
    return found


class DesignSystemContractTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.directory = Path(tempfile.mkdtemp(prefix="design-"))
        everything = sorted(ANDROID.manifest.preset_features("everything"))
        lean = sorted(ANDROID.manifest.preset_features("lean"))
        cls.ui = {
            "everything": generate(cls.directory, "ui-everything", features=everything),
            "lean": generate(cls.directory, "ui-lean", features=lean),
        }
        cls.noui = generate(cls.directory, "noui-everything", features=everything, ui_components=False)

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.directory, ignore_errors=True)

    def test_every_component_file_has_a_private_preview_of_its_own(self):
        for shape, project in self.ui.items():
            with self.subTest(shape=shape):
                root = project / DESIGN_SYSTEM
                unpreviewed = {}
                counted = 0
                for path in sorted(root.rglob("*.kt")):
                    relative = path.relative_to(root).as_posix()
                    if relative.startswith(PLUMBING):
                        continue
                    source = path.read_text(encoding="utf-8")
                    declared = components(source)
                    if not declared:
                        continue
                    counted += 1
                    if not _PREVIEW.search(source):
                        unpreviewed[relative] = declared
                self.assertGreater(counted, 0, "no component files found: the scan is looking in the wrong place")
                self.assertEqual({}, unpreviewed)

    def test_the_component_preview_annotation_is_in_every_project(self):
        for name, project in {**self.ui, "noui-everything": self.noui}.items():
            with self.subTest(project=name):
                annotation = project / DESIGN_SYSTEM / "preview/AppComponentPreviews.kt"
                self.assertIn("annotation class AppComponentPreviews", annotation.read_text(encoding="utf-8"))
        variant = ANDROID.manifest.root.parent.parent / "generator/variants/noui" / (
            "core/designsystem/src/main/kotlin/com/base/app/core/designsystem/preview/AppComponentPreviews.kt"
        )
        self.assertFalse(variant.exists(), "the template's copy serves both modes")

    def test_a_theme_skeleton_holds_no_token_only_the_components_read(self):
        emitted = (self.noui / kotlin.RELATIVE_PATH.format(package_path=PKG)).read_text(encoding="utf-8")
        code = "\n".join(line for line in emitted.splitlines() if not line.lstrip().startswith(("//", "/*", "*")))
        sections = set(re.findall(r"^\s*(?:object|class)\s+(\w+)", code, re.M))
        self.assertEqual(set(), sections & {"Sheet", "Borders", "MotionStyle", "Style", "Styles"})
        for name in ("tabBar", "Grabber", "glass", "Glass", "barIndicator", "skeleton"):
            with self.subTest(name=name):
                self.assertNotIn(name, code)
        radii = re.search(r"object Radii \{(.*?)\}", emitted, re.S).group(1)
        self.assertNotIn("sheet", radii)


if __name__ == "__main__":
    unittest.main()
