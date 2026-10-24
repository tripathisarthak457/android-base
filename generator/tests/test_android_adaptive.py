"""
What keeps a generated project adaptive (ADAPTIVE-PLAN 11.8): for the smallest, the default and the
largest preset, the build still carries the manifest lines, the code rules, the lint errors, the
screenshot tests and the adaptive audit that fail it when a screen stops adapting.
"""

from __future__ import annotations

import re
import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.targets import get

ANDROID = get("android")
PRESETS = ("lean", "standard", "everything")
CONFIG_CHANGES = {"orientation", "screenSize", "smallestScreenSize", "screenLayout", "keyboard", "keyboardHidden", "navigation"}
ADAPTIVE_RULES = (
    "adaptive-orientation", "adaptive-screen-metrics", "adaptive-device-type", "adaptive-fixed-size",
    "adaptive-box-constraints", "adaptive-mediaquery", "adaptive-material-scope", "adaptive-preview",
)
LINT_ERRORS = (
    "LockedOrientationActivity", "SourceLockedOrientationActivity", "NonResizeableActivity",
    "ConfigurationScreenWidthHeight", "LocalContextConfigurationRead", "LocalContextResourcesRead",
    "UnusedBoxWithConstraintsScope", "ResourceName",
)


class PresetTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.projects = {}
        for preset in PRESETS:
            raw = {
                "schema": 2, "platform": "android", "app_name": "Adaptive Check", "package_name": "com.example.adaptive",
                "features": sorted(ANDROID.manifest.preset_features(preset)),
            }
            project = Path(cls.temp.name) / preset
            pipeline.build(pipeline.read_spec(raw, ANDROID)[0].spec, ANDROID, project, zip_output=False, commit="test")
            cls.projects[preset] = project

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def each(self):
        for preset, project in self.projects.items():
            with self.subTest(preset=preset):
                yield project

    def test_the_launcher_activity_survives_every_configuration_change_and_allows_multiple_windows(self):
        for project in self.each():
            manifest = (project / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
            declared = set().union(*(set(found.split("|")) for found in re.findall(r'android:configChanges="([^"]+)"', manifest)))
            self.assertTrue(CONFIG_CHANGES <= declared)
            self.assertIn('android:name="android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI"', manifest)
            self.assertNotIn("screenOrientation", manifest)
            self.assertNotIn('resizeableActivity="false"', manifest)

    def test_the_adaptive_code_rules_and_the_material_exemption_ship(self):
        for project in self.each():
            rules = "".join(path.read_text(encoding="utf-8") for path in project.glob("build-logic/**/guard/*.kt"))
            for rule in ADAPTIVE_RULES:
                self.assertIn(f'"{rule}"', rules)
            self.assertIn("(?!3\\.adaptive\\.)", rules)

    def test_lint_fails_on_the_adaptive_ids(self):
        for project in self.each():
            android = "".join(path.read_text(encoding="utf-8") for path in project.glob("build-logic/**/android/*.kt"))
            for lint_id in LINT_ERRORS:
                self.assertIn(f'"{lint_id}"', android)

    def test_the_design_system_and_every_feature_render_their_previews_as_screenshot_tests(self):
        for project in self.each():
            self.assertIn('id("com.example.adaptive.android.screenshots")', (project / "core/designsystem/build.gradle.kts").read_text())
            feature = next(project.glob("build-logic/**/AndroidFeatureConventionPlugin.kt")).read_text()
            self.assertIn('pluginManager.apply("com.example.adaptive.android.screenshots")', feature)
            for build in project.glob("feature/*/build.gradle.kts"):
                self.assertIn('id("com.example.adaptive.android.feature")', build.read_text())

    def test_the_adaptive_audit_renders_every_screen_preview_from_the_app(self):
        for project in self.each():
            audit = project / "app/src/test/kotlin/com/example/adaptive/adaptive/AdaptiveAuditTest.kt"
            text = audit.read_text(encoding="utf-8")
            self.assertIn('APP_PACKAGE = "com.example.adaptive"', text)
            self.assertIn("includeIfAnnotatedWithAnyOf(AppScreenPreviews::class.java)", text)
            build = (project / "app/build.gradle.kts").read_text(encoding="utf-8")
            self.assertIn("testImplementation(libs.composable.preview.scanner)", build)
            # Something to audit in every preset: the reference feature or a scaffolded module.
            screens = [path for path in project.glob("feature/**/*Screen.kt") if "@AppScreenPreviews" in path.read_text(encoding="utf-8")]
            self.assertTrue(screens)

    def test_the_ci_verifies_screenshots_once_there_are_goldens(self):
        for project in self.each():
            workflow = (project / ".github/workflows/android.yml").read_text(encoding="utf-8")
            self.assertIn("verifyRoborazziDebug", workflow)
            self.assertIn("src/test/screenshots", workflow)


if __name__ == "__main__":
    unittest.main()
