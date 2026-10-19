"""The Compose Multiplatform target: presets, the iOS project, scaffolded modules and validation."""

from __future__ import annotations

import io
import plistlib
import re
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

from genkit.core import catalogue, pipeline, prompts
from genkit.core.spec import SpecError
from genkit.targets import get
from genkit.targets.multiplatform import modules, settings
from genkit.targets.multiplatform.target import bundle_id

try:
    import yaml
except ImportError:  # PyYAML is optional; the project.yml checks say so when it is missing.
    yaml = None

MULTIPLATFORM = get("multiplatform")
TEMPLATE_IDENTITY = ("com.base.app", "com/base/app", "BaseApp", "baseapp")
MARKERS = re.compile(r"<(?:/?opt|opt-else|/opt-else|/?generated):")
BINARY = (".jar", ".png", ".jpg", ".webp", ".ttf", ".otf", ".ico")


def spec(**fields):
    raw = {
        "schema": 2,
        "platform": "multiplatform",
        "app_name": "Trail Notes",
        "package_name": "com.acme.trail_notes",
        **fields,
    }
    return pipeline.read_spec(raw, MULTIPLATFORM)[0].spec


def generate(directory: Path, name: str = "project", **fields) -> Path:
    project = directory / name
    pipeline.build(spec(**fields), MULTIPLATFORM, project, zip_output=False, commit="test")
    return project


def kotlin_sources(project: Path) -> str:
    return "\n".join(path.read_text(encoding="utf-8") for path in project.rglob("*.kt"))


def included_modules(project: Path) -> set[str]:
    text = (project / "settings.gradle.kts").read_text(encoding="utf-8")
    return set(re.findall(r'^include\("(:[^"]+)"\)', text, re.MULTILINE))


def module_directories(project: Path) -> set[str]:
    found = {":androidApp", ":shared"}
    for tier in ("core", "data", "feature"):
        for build_file in (project / tier).glob("*/build.gradle.kts"):
            found.add(f":{tier}:{build_file.parent.name}")
    return found


class PresetTest(unittest.TestCase):
    """Each preset generates a project with nothing of the template's own left in it."""

    @classmethod
    def setUpClass(cls):
        cls.directory = tempfile.TemporaryDirectory()
        root = Path(cls.directory.name)
        cls.projects = {
            preset: generate(root, preset, features=sorted(MULTIPLATFORM.manifest.preset_features(preset)))
            for preset in MULTIPLATFORM.manifest.presets
        }

    @classmethod
    def tearDownClass(cls):
        cls.directory.cleanup()

    def test_no_marker_or_template_identity_is_left(self):
        for preset, project in self.projects.items():
            for path in project.rglob("*"):
                if not path.is_file() or path.suffix in BINARY:
                    continue
                text = path.read_text(encoding="utf-8")
                with self.subTest(preset=preset, file=path.relative_to(project).as_posix()):
                    self.assertIsNone(MARKERS.search(text))
                    for token in TEMPLATE_IDENTITY:
                        self.assertNotIn(token, text)
                    self.assertNotIn("BaseApp", path.name)

    def test_every_included_module_exists_and_every_module_is_included(self):
        for preset, project in self.projects.items():
            with self.subTest(preset=preset):
                self.assertEqual(module_directories(project), included_modules(project))

    def test_every_koin_module_the_app_lists_is_declared(self):
        for preset, project in self.projects.items():
            app_modules = next(project.glob("shared/src/commonMain/kotlin/**/di/AppModules.kt"))
            listed = re.search(r"listOf\((.*?)\n\)", app_modules.read_text(encoding="utf-8"), re.DOTALL).group(1)
            names = set(re.findall(r"^\s*(\w+Module),", listed, re.MULTILINE))
            declared = set(re.findall(r"^val (\w+Module) = module", kotlin_sources(project), re.MULTILINE))
            with self.subTest(preset=preset):
                self.assertTrue(names)
                self.assertLessEqual(names, declared)

    def test_every_preset_opens_on_a_tab_of_its_own(self):
        for preset, project in self.projects.items():
            destinations = next(project.glob("shared/src/commonMain/kotlin/**/navigation/AppDestinations.kt"))
            tabs = re.findall(r"key = (\w+),", destinations.read_text(encoding="utf-8"))
            with self.subTest(preset=preset):
                self.assertTrue(tabs)
                self.assertNotEqual("SettingsKey", tabs[0])

    def test_the_lean_preset_starts_on_a_scaffolded_home(self):
        lean = self.projects["lean"]
        self.assertIn(":feature:home", included_modules(lean))
        self.assertNotIn(":feature:sample", included_modules(lean))
        self.assertFalse((lean / "core" / "database").exists())

    def test_info_plist_keeps_120_hz_and_every_ipad_window(self):
        for preset, project in self.projects.items():
            info = plistlib.loads((project / "iosApp" / "TrailNotes" / "Info.plist").read_bytes())
            with self.subTest(preset=preset):
                self.assertIs(True, info["CADisableMinimumFrameDurationOnPhone"])
                self.assertEqual(
                    {
                        "UIInterfaceOrientationPortrait",
                        "UIInterfaceOrientationPortraitUpsideDown",
                        "UIInterfaceOrientationLandscapeLeft",
                        "UIInterfaceOrientationLandscapeRight",
                    },
                    set(info["UISupportedInterfaceOrientations~ipad"]),
                )
                self.assertNotIn("UIRequiresFullScreen", info)
                self.assertIn("UILaunchScreen", info)
                self.assertIs(True, info["UIApplicationSupportsIndirectInputEvents"])
                self.assertIs(False, info["UIApplicationSceneManifest"]["UIApplicationSupportsMultipleScenes"])
                self.assertEqual("Trail Notes", info["CFBundleDisplayName"])

    @unittest.skipIf(yaml is None, "PyYAML is not installed; project.yml is not parsed")
    def test_project_yml_names_the_app_and_builds_the_framework_first(self):
        for preset, project in self.projects.items():
            document = yaml.safe_load((project / "iosApp" / "project.yml").read_text(encoding="utf-8"))
            target = document["targets"]["TrailNotes"]
            settings_base = target["settings"]["base"]
            with self.subTest(preset=preset):
                self.assertEqual("TrailNotes", document["name"])
                self.assertEqual("17.0", document["options"]["deploymentTarget"]["iOS"])
                self.assertEqual("com.acme.trail-notes", settings_base["PRODUCT_BUNDLE_IDENTIFIER"])
                self.assertEqual("NO", settings_base["ENABLE_USER_SCRIPT_SANDBOXING"])
                self.assertEqual("TrailNotes/Info.plist", settings_base["INFOPLIST_FILE"])
                self.assertEqual(["TrailNotes"], target["sources"])
                script = target["preBuildScripts"][0]
                self.assertIs(False, script["basedOnDependencyAnalysis"])
                self.assertIn("./gradlew :shared:embedAndSignAppleFrameworkForXcode", script["script"])
                self.assertIn("TrailNotes", document["schemes"])

    def test_the_swift_entry_point_is_renamed_with_the_app(self):
        project = self.projects["standard"]
        self.assertTrue((project / "iosApp" / "TrailNotes" / "TrailNotesApp.swift").is_file())
        self.assertIn("struct TrailNotesApp: App", (project / "iosApp" / "TrailNotes" / "TrailNotesApp.swift").read_text())

    def test_a_chosen_font_is_left_to_the_developer_with_a_note(self):
        nowhere = Path(self.directory.name) / "nothing-to-rewrite"
        notes = MULTIPLATFORM.finish(nowhere, spec(font_name="Inter"), None)
        self.assertTrue(any("To use Inter, put the font files in" in note for note in notes))
        self.assertFalse(any("font files" in note for note in MULTIPLATFORM.finish(nowhere, spec(), None)))

    def test_the_token_copy_carries_the_projects_choices(self):
        project = Path(self.directory.name)
        styled = generate(project, "styled", design_style="Social", motion_style="Calm")
        tokens = next(styled.glob("core/designsystem/src/commonMain/kotlin/**/theme/tokens/DesignTokens.kt"))
        text = tokens.read_text(encoding="utf-8")
        self.assertIn("val designStyle: AppDesignStyle = AppDesignStyle.Social", text)
        self.assertIn("val motionStyle: AppMotionStyle = AppMotionStyle.Calm", text)


class ScaffoldTest(unittest.TestCase):
    """Named modules arrive as a data and a feature module, wired everywhere the app lists modules."""

    @classmethod
    def setUpClass(cls):
        cls.directory = tempfile.TemporaryDirectory()
        cls.project = generate(
            Path(cls.directory.name),
            features=["network", "sample", "settings"],
            feature_modules=["order_history", "wallet"],
            languages=["de", "pt-BR"],
        )

    @classmethod
    def tearDownClass(cls):
        cls.directory.cleanup()

    def test_each_module_is_included_and_built_by_the_shared_framework(self):
        shared = (self.project / "shared" / "build.gradle.kts").read_text(encoding="utf-8")
        for name, accessor in (("order_history", "orderHistory"), ("wallet", "wallet")):
            with self.subTest(module=name):
                self.assertLessEqual({f":data:{name}", f":feature:{name}"}, included_modules(self.project))
                self.assertIn(f"implementation(projects.data.{accessor})", shared)
                self.assertIn(f"implementation(projects.feature.{accessor})", shared)

    def test_each_module_is_a_tab_with_its_own_icon_between_home_and_settings(self):
        destinations = next(self.project.glob("shared/src/commonMain/kotlin/**/navigation/AppDestinations.kt"))
        text = destinations.read_text(encoding="utf-8")
        self.assertEqual(
            ["SampleListKey", "OrderHistoryListKey", "WalletListKey", "SettingsKey"],
            re.findall(r"key = (\w+),", text),
        )
        icons = re.findall(r"\bicon = AppIcons\.(\w+),", text)
        self.assertEqual(len(icons), len(set(icons)))
        self.assertEqual(["Home", "Order history", "Wallet", "Settings"], modules.labels(spec(
            features=["network", "sample", "settings"], feature_modules=["order_history", "wallet"],
        )))

    def test_each_module_registers_its_koin_modules(self):
        app_modules = next(self.project.glob("shared/src/commonMain/kotlin/**/di/AppModules.kt")).read_text()
        for module in ("orderHistoryDataModule", "orderHistoryModule", "walletDataModule", "walletModule"):
            with self.subTest(module=module):
                self.assertIn(f"    {module},\n", app_modules)
                self.assertRegex(kotlin_sources(self.project), rf"(?m)^val {module} = module")

    def test_packages_drop_the_underscore_and_keep_the_api_shape(self):
        feature = self.project / "feature/order_history/src/commonMain/kotlin/com/acme/trail_notes/feature/orderhistory"
        data = self.project / "data/order_history/src/commonMain/kotlin/com/acme/trail_notes/data/orderhistory"
        self.assertTrue((feature / "api" / "OrderHistoryKeys.kt").is_file())
        self.assertTrue((feature / "list" / "OrderHistoryListScreen.kt").is_file())
        self.assertTrue((data / "api" / "OrderHistoryRepository.kt").is_file())
        self.assertTrue((data / "NetworkOrderHistoryRepository.kt").is_file())
        test = self.project / "feature/order_history/src/commonTest/kotlin/com/acme/trail_notes/feature/orderhistory"
        self.assertTrue((test / "list" / "OrderHistoryListViewModelTest.kt").is_file())

    def test_translations_reach_every_module_with_strings(self):
        sources = sorted(self.project.glob("**/src/commonMain/composeResources/values/strings.xml"))
        self.assertTrue(sources)
        for source in sources:
            for qualifier in ("de", "pt-rBR"):
                translated = source.parent.parent / f"values-{qualifier}" / "strings.xml"
                with self.subTest(file=translated.relative_to(self.project).as_posix()):
                    names = re.findall(r'<string name="(\w+)">', source.read_text(encoding="utf-8"))
                    self.assertEqual(names, re.findall(r'<string name="(\w+)">', translated.read_text(encoding="utf-8")))

    def test_the_languages_reach_both_platforms_language_lists(self):
        locales = (self.project / "androidApp/src/main/res/xml/locales_config.xml").read_text(encoding="utf-8")
        self.assertEqual(["en", "de", "pt-BR"], re.findall(r'<locale android:name="([^"]+)" />', locales))
        info = plistlib.loads((self.project / "iosApp" / "TrailNotes" / "Info.plist").read_bytes())
        self.assertEqual(["en", "de", "pt-BR"], info["CFBundleLocalizations"])

    def test_without_network_the_repository_keeps_items_in_memory(self):
        offline = generate(Path(self.directory.name), "offline", features=["settings"], feature_modules=["notes"])
        data = offline / "data/notes/src/commonMain/kotlin/com/acme/trail_notes/data/notes"
        self.assertTrue((data / "InMemoryNotesRepository.kt").is_file())
        self.assertFalse((data / "remote").exists())
        self.assertNotIn("core.network", (offline / "data" / "notes" / "build.gradle.kts").read_text())

    def test_only_the_batch_file_holds_a_carriage_return(self):
        # Scaffolded modules, translations and the token copy are written, not copied: newline
        # translation on Windows would show up here as a CR byte the Linux CI never sees.
        found = sorted(
            path.relative_to(self.project).as_posix()
            for path in self.project.rglob("*")
            if path.is_file()
            and path.suffix.lower() not in MULTIPLATFORM.manifest.binary_suffixes
            and b"\r" in path.read_bytes()
        )
        self.assertEqual(["gradlew.bat"], found)


class ValidationTest(unittest.TestCase):

    def test_the_bundle_id_defaults_to_the_package_without_underscores(self):
        self.assertEqual("com.acme.trail-notes", bundle_id(spec()))
        self.assertEqual("com.acme.trails", bundle_id(spec(ios={"bundle_id": "com.acme.trails"})))

    def test_a_bundle_id_with_an_underscore_is_refused(self):
        with self.assertRaises(SpecError) as caught:
            spec(ios={"bundle_id": "com.acme.trail_notes"})
        self.assertEqual("ios.bundle_id", caught.exception.field)

    def test_the_deployment_target_starts_at_ios_15(self):
        self.assertEqual("15.0", spec(ios={"deployment_target": "15.0"}).ios.deployment_target)
        for refused in ("14.0", "17", "seventeen", "19.0"):
            with self.subTest(deployment_target=refused), self.assertRaises(SpecError) as caught:
                spec(ios={"deployment_target": refused})
            self.assertEqual("ios.deployment_target", caught.exception.field)

    def test_only_the_compact_architecture_is_offered(self):
        with self.assertRaises(SpecError) as caught:
            spec(architecture="modular")
        self.assertEqual("architecture", caught.exception.field)

    def test_android_settings_the_template_has_no_use_for_are_refused(self):
        for field, value in (
            ("web_socket_urls", {"dev": "wss://dev.acme.com/"}),
            ("deeplink_scheme", "trail"),
            ("api_base_urls", {"staging": "https://staging.acme.com/"}),
        ):
            with self.subTest(field=field), self.assertRaises(SpecError) as caught:
                spec(android={field: value})
            self.assertEqual(f"android.{field}", caught.exception.field)

    def test_a_display_name_xcode_would_expand_is_refused(self):
        with self.assertRaises(SpecError) as caught:
            spec(display_name="Trail $(HOME)")
        self.assertEqual("display_name", caught.exception.field)

    def test_two_module_names_that_share_a_package_are_refused(self):
        with self.assertRaises(SpecError) as caught:
            spec(feature_modules=["order_history", "orderhistory"])
        self.assertEqual("feature_modules", caught.exception.field)

    def test_the_xcconfig_url_survives_its_comment_syntax(self):
        regions = settings.ios_regions(spec(android={"api_base_urls": {"dev": "https://dev.acme.com/v1/"}}), "x")
        self.assertEqual(["API_BASE_URL = https:/$()/dev.acme.com/v1/\n"], regions["ios-api-url-dev"])
        self.assertIsNone(regions["ios-api-url-prod"])


class WizardTest(unittest.TestCase):

    def answers(self, given: dict[str, str]):
        """Answers each question by its prompt's start, and takes the default for any other."""
        asked: list[str] = []

        def fake_input(prompt: str) -> str:
            asked.append(prompt)
            self.assertLess(len(asked), 100, "the wizard keeps asking")
            return next((answer for start, answer in given.items() if prompt.strip().startswith(start)), "")

        return asked, fake_input

    def test_the_default_answers_make_a_spec_this_target_accepts(self):
        asked, fake_input = self.answers({"Package name": "com.acme.trail_notes"})
        with mock.patch("builtins.input", fake_input), redirect_stdout(io.StringIO()):
            raw = prompts.run_wizard(MULTIPLATFORM, preset="standard")

        parsed = pipeline.read_spec(raw, MULTIPLATFORM)[0].spec
        self.assertEqual("com.acme.trail-notes", parsed.ios.bundle_id)
        self.assertEqual({"dev", "prod"}, set(parsed.android.api_base_urls))
        self.assertFalse(any(prompt.strip().startswith(("Set up signing", "  staging")) for prompt in asked))

    def test_an_ios_answer_the_target_refuses_is_asked_again(self):
        answers = iter(["com.acme.trail_notes", "com.acme.trail", "14.0", "16.0", "TEAM", "ABCDE12345"])
        with mock.patch("builtins.input", lambda prompt: next(answers)), redirect_stdout(io.StringIO()):
            ios = prompts.ask_ios("com.acme.trail")

        self.assertEqual({"bundle_id": "com.acme.trail", "deployment_target": "16.0", "team_id": "ABCDE12345"}, ios)


class CatalogueTest(unittest.TestCase):

    def test_the_target_is_listed_as_beta_with_its_reason(self):
        entry = next(target for target in catalogue.catalogue()["targets"] if target["key"] == "multiplatform")
        self.assertEqual("beta", entry["status"])
        self.assertEqual("Beta: the iOS app is not yet verified on a Mac", entry["status_note"])
        self.assertEqual(["lean", "standard", "everything"], [preset["key"] for preset in entry["presets"]])


if __name__ == "__main__":
    unittest.main()
