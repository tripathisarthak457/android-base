"""Launcher icons from `--icon`. The XML rewrite needs no Pillow, so it is tested everywhere."""

from __future__ import annotations

import importlib.util
import shutil
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get
from genkit.targets.android import icons

ANDROID = get("android")
TEMPLATE_RES = ANDROID.template_dir / "app/src/main/res"
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"


class MonochromeTest(unittest.TestCase):

    def test_the_rewritten_adaptive_icons_are_valid_xml(self):
        # A substitution that drops its own groups writes `@drawable/… />` as bare text, which fails
        # resource linking in every project generated with an icon (audit 3.2).
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp)
            target = project / "app/src/main/res/mipmap-anydpi-v26"
            shutil.copytree(TEMPLATE_RES / "mipmap-anydpi-v26", target)
            warnings: list[str] = []
            icons._point_monochrome_at_the_template_mark(project, warnings)

            files = sorted(target.glob("ic_launcher*.xml"))
            self.assertTrue(files)
            for path in files:
                monochrome = ET.parse(path).getroot().find("monochrome")
                self.assertEqual("@drawable/ic_launcher_monochrome", monochrome.get(f"{ANDROID_NS}drawable"))
            self.assertEqual(1, len(warnings))


@unittest.skipUnless(importlib.util.find_spec("PIL"), "Pillow is not installed")
class IconGenerationTest(unittest.TestCase):

    def test_a_project_generated_with_an_icon_has_only_valid_xml(self):
        from PIL import Image

        with tempfile.TemporaryDirectory() as temp:
            source = Path(temp) / "icon.png"
            Image.new("RGBA", (1024, 1024), (20, 120, 200, 255)).save(source)
            parsed, _ = pipeline.read_spec({"app_name": "Iconic", "package_name": "com.example.iconic"}, ANDROID)
            project = Path(temp) / "p"
            pipeline.build(parsed.spec, ANDROID, project, zip_output=False, icon_source=source, commit="test")
            for path in project.rglob("*.xml"):
                with self.subTest(path=path.relative_to(project)):
                    ET.parse(path)
            self.assertTrue((project / "play-store-icon.png").is_file())


if __name__ == "__main__":
    unittest.main()
