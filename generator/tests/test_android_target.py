"""The Android target: its settings regions, tabs, scaffolding, the look it writes, and project scripts."""

from __future__ import annotations

import io
import json
import re
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

from genkit import brand
from genkit.core import pipeline, translations
from genkit.core.record import RECORD_NAME
from genkit.core.target import RenderError
from genkit.targets import get
from genkit.targets.android import database, look, settings, variants
from genkit.tokens import kotlin
from tests.consistency import checks

ANDROID = get("android")


def spec(**fields):
    raw = {"schema": 2, "platform": "android", "app_name": "Acme Field", "package_name": "com.acme.field", **fields}
    return pipeline.read_spec(raw, ANDROID)[0].spec


def generate(directory: Path, **fields) -> Path:
    project = directory / "project"
    pipeline.build(spec(**fields), ANDROID, project, zip_output=False, commit="test")
    return project


class SettingsTest(unittest.TestCase):

    def test_app_config_and_version(self):
        regions = settings.regions(spec(android={"min_sdk": 24, "version_name": "2.1.0", "version_code": 7}))
        self.assertIn('    const val APPLICATION_ID = "com.acme.field"\n', regions["app-config"])
        self.assertIn("    const val MIN_SDK = 24\n", regions["app-config"])
        self.assertEqual(["    const val VERSION_CODE = 7\n", '    const val VERSION_NAME = "2.1.0"\n'], regions["app-version"])

    def test_a_flavour_the_spec_leaves_out_keeps_the_template_placeholder(self):
        regions = settings.regions(spec(android={"api_base_urls": {"dev": "https://dev.acme.com/"}}))
        self.assertEqual(['        apiBaseUrl = "https://dev.acme.com/",\n'], regions["api-url-dev"])
        self.assertIsNone(regions["api-url-prod"])
        self.assertIsNone(regions["socket-url-dev"])

    def test_the_label_is_escaped_for_aapt(self):
        regions = settings.regions(spec(display_name="Mom's & Co"))
        self.assertEqual(['    <string name="app_name" translatable="false">Mom\\\'s &amp; Co</string>\n'], regions["app-name"])

    def test_the_launcher_follows_the_brand_only_when_one_is_given(self):
        self.assertIsNone(settings.regions(spec())["launcher-background"])
        accent = brand.to_hex(brand.brand_ramps("#16A34A")["Accent"].base)[1:]
        self.assertEqual(
            [f'    <color name="launcher_background">#FF{accent}</color>\n'],
            settings.regions(spec(accent_colour="#16A34A"))["launcher-background"],
        )

    def test_one_signing_paragraph_survives(self):
        keyless = settings.regions(spec())
        self.assertIsNone(keyless["readme-signing-manual"])
        self.assertEqual([], keyless["readme-signing-generated"])

    def test_a_generated_project_carries_every_setting(self):
        with tempfile.TemporaryDirectory() as temp:
            project = generate(
                Path(temp), display_name="Café Olé", features=["network", "deeplink", "language"], languages=["fr"],
                android={"min_sdk": 28, "deeplink_scheme": "acme", "api_base_urls": {"prod": "https://api.acme.com/"}},
            )
            config = next(project.rglob("AppConfig.kt")).read_text(encoding="utf-8")
            strings = (project / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8")
            readme = (project / "README.md").read_text(encoding="utf-8")
        self.assertIn("const val MIN_SDK = 28", config)
        self.assertIn('apiBaseUrl = "https://api.acme.com/"', config)
        self.assertIn('apiBaseUrl = "https://dev.example.com/api/"', config)
        self.assertNotIn("<generated:", config)
        self.assertIn('translatable="false">Café Olé</string>', strings)
        self.assertIn('name="deeplink_scheme" translatable="false">acme<', strings)
        self.assertIn('name="deeplink_host" translatable="false">example.com<', strings)
        self.assertTrue(readme.startswith("# Café Olé\n"))
        self.assertIn("- language\n", readme)


    def test_a_project_with_formatting_fails_its_check_on_unformatted_code(self):
        with tempfile.TemporaryDirectory() as temp:
            build = (generate(Path(temp), features=["formatting"]) / "build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn("spotless {", build)
        self.assertNotIn("isEnforceCheck", build)


class ScaffoldTest(unittest.TestCase):

    def test_a_scaffolded_module_names_only_what_the_project_declares(self):
        # The modules are written from code rather than copied from the template, so nothing moves
        # them when the template moves a type they name, and the first build to notice is a user's.
        # Issues the project has without them belong to the template, not to them.
        with tempfile.TemporaryDirectory() as temp:
            for features in ([], ["network"]):
                with self.subTest(features=features):
                    root = Path(temp) / "-".join(["none", *features])
                    without = generate(root / "without", features=features)
                    scaffolded = generate(root / "with", features=features, feature_modules=["orders"])
                    added = set(checks.check(scaffolded, "com.acme.field", None))
                    added -= set(checks.check(without, "com.acme.field", None))
                    self.assertEqual([], sorted(added))


class DatabaseTest(unittest.TestCase):

    def test_each_scaffolded_module_gets_a_table(self):
        regions = database.regions(spec(features=["database"], feature_modules=["order_history"]))
        self.assertEqual(
            [
                "import com.acme.field.core.database.order_history.OrderHistoryDao\n",
                "import com.acme.field.core.database.order_history.OrderHistoryEntity\n",
            ],
            regions["database-imports"],
        )
        self.assertEqual(["        OrderHistoryEntity::class,\n"], regions["database-entities"])
        self.assertEqual(["    abstract fun orderHistory(): OrderHistoryDao\n"], regions["database-daos"])

    def test_a_database_with_no_other_table_gets_the_starter_one(self):
        regions = database.regions(spec(features=["database"]))
        self.assertEqual(["        NoteEntity::class,\n"], regions["database-entities"])

    def test_the_reference_feature_table_is_enough(self):
        regions = database.regions(spec(features=["database", "sample"]))
        self.assertEqual([], regions["database-entities"])

    def test_without_the_database_nothing_is_added(self):
        regions = database.regions(spec(feature_modules=["orders"]))
        self.assertEqual({"database-imports": [], "database-entities": [], "database-daos": []}, regions)

    def test_a_module_cannot_take_a_name_the_database_already_uses(self):
        self.assertTrue({"queue", "note", "sample"} <= ANDROID.reserved_module_names)


class LookTest(unittest.TestCase):
    """The look reaches a project through its DesignTokens, never through edits to the theme's code."""

    def test_a_project_without_its_token_copy_is_an_error(self):
        with tempfile.TemporaryDirectory() as temp, self.assertRaises(RenderError):
            look.apply(Path(temp), spec())

    def test_the_spec_s_choices_become_the_project_s_tokens(self):
        with tempfile.TemporaryDirectory() as temp:
            project = generate(
                Path(temp), accent_colour="#16A34A", design_style="Playful", motion_style="Bouncy",
                haptics_enabled=False, font_name="M PLUS 1p", features=["catalog", "googlefonts"],
            )
            tokens = next(project.rglob("DesignTokens.kt")).read_text(encoding="utf-8")
            theme = next(project.rglob("AppTheme.kt")).read_text(encoding="utf-8")
            catalog_colors = (project / "catalog/src/main/res/values/colors.xml").read_text(encoding="utf-8")
            catalog_label = (project / "catalog/src/main/res/values/strings.xml").read_text(encoding="utf-8")
        self.assertTrue(tokens.startswith(kotlin.PROJECT_HEADER))
        self.assertIn("package com.acme.field.core.designsystem.theme.tokens\n", tokens)
        self.assertIn("val designStyle: AppDesignStyle = AppDesignStyle.Playful\n", tokens)
        self.assertIn("val motionStyle: AppMotionStyle = AppMotionStyle.Bouncy\n", tokens)
        self.assertIn("const val hapticsEnabled: Boolean = false\n", tokens)
        self.assertIn('const val fontName: String = "M PLUS 1p"\n', tokens)
        ramp = brand.brand_ramps("#16A34A")["Accent"]
        self.assertIn(f"accent = Color(0xFF{brand.to_hex(ramp.base)[1:]}),", tokens)
        self.assertIn("designStyle: AppDesignStyle = AppTheme.defaults.designStyle", theme)
        self.assertIn(f"#FF{brand.to_hex(ramp.base)[1:]}", catalog_colors)
        self.assertIn(">Acme Field Catalog<", catalog_label)
        self.assertNotIn("<generated:", catalog_label)

    def test_without_downloadable_fonts_the_project_names_the_platform_families(self):
        with tempfile.TemporaryDirectory() as temp:
            project = generate(Path(temp), font_name="M PLUS 1p")
            tokens = next(project.rglob("DesignTokens.kt")).read_text(encoding="utf-8")
            fonts = next(project.rglob("AppFonts.kt")).read_text(encoding="utf-8")
        self.assertIn('const val fontName: String = "sans-serif"\n', tokens)
        self.assertIn("public val AppFontPreload: FontPreload = FontPreload(PlatformFonts)\n", fonts)
        self.assertNotIn("googlefonts", fonts)


class BrandTest(unittest.TestCase):

    def test_a_blank_supporting_colour_is_derived_from_the_primary(self):
        colours = brand.brand_colours("#16A34A")
        self.assertNotEqual(colours["Accent"], colours["Secondary"])
        self.assertNotEqual(colours["Accent"], colours["Tertiary"])
        self.assertNotEqual(colours["Secondary"], colours["Tertiary"])

    def test_a_supplied_supporting_colour_is_used_as_given(self):
        self.assertEqual((1.0, 0.0, 1.0), brand.brand_colours("#16A34A", "#FF00FF")["Secondary"])

    def test_the_derived_secondary_is_the_primary_with_the_chroma_taken_out(self):
        import colorsys

        colours = brand.brand_colours("#16A34A")
        primary_hue, _, primary_saturation = colorsys.rgb_to_hls(*colours["Accent"])
        derived_hue, _, derived_saturation = colorsys.rgb_to_hls(*colours["Secondary"])
        self.assertAlmostEqual(primary_hue, derived_hue, places=5)
        self.assertLess(derived_saturation, primary_saturation)


class ProjectTest(unittest.TestCase):

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)

    def test_the_record_reads_back_as_the_same_spec_and_holds_no_keys(self):
        made = spec(
            features=["network", "settings"], feature_modules=["orders"], languages=["es"],
            android={"keystores": [{"name": "prod", "alias": "upload", "store_password": "hunter22"}]},
        )
        project = self.root / "p"
        pipeline.build(made, ANDROID, project, zip_output=False, commit="abc123")
        text = (project / RECORD_NAME).read_text(encoding="utf-8")
        again = pipeline.read_spec(json.loads(text), ANDROID)[0].spec
        self.assertNotIn("hunter22", text)
        self.assertEqual("abc123", json.loads(text)["generator"]["commit"])
        self.assertEqual(2, json.loads(text)["schema"])
        self.assertEqual((made.features, made.feature_modules, made.languages), (again.features, again.feature_modules, again.languages))
        self.assertEqual((), again.android.keystores)

    def test_a_dry_run_says_what_a_build_would_do(self):
        parsed, warnings = pipeline.read_spec(
            {"app_name": "Acme Field", "package_name": "com.acme.field", "features": ["push"], "feature_modules": ["orders"]},
            ANDROID,
        )
        plan = pipeline.plan(parsed, ANDROID, warnings)
        self.assertEqual(
            {"enabled", "disabled", "options", "removed_paths", "modules", "tabs", "notes", "warnings"}, set(plan)
        )
        self.assertEqual({"ui_components": True}, plan["options"])
        self.assertIn("firebase", plan["enabled"])
        self.assertIn("core/ui/src/main/kotlin/com/acme/field/core/ui/AppNetworkImage.kt", plan["removed_paths"])
        self.assertEqual([":data:orders", ":feature:orders"], plan["modules"])

    def test_a_seam_without_its_vendor_binds_the_no_op(self):
        project = generate(self.root, features=["analytics"])
        modules = {path.name: path.read_text(encoding="utf-8") for path in project.rglob("*Module.kt")}
        self.assertIn("NoOpAnalyticsTracker()", modules["AnalyticsModule.kt"])
        self.assertIn("NoOpCrashReporter()", modules["CrashModule.kt"])
        self.assertNotIn("FlagsModule.kt", modules)
        self.assertFalse(any(project.rglob("firebase")))
        self.assertEqual({}, variants.VARIANTS)

    def test_only_the_batch_file_of_a_generated_project_holds_a_carriage_return(self):
        # Newline translation on Windows, or a template checked out with CRLF, shows up here as a
        # CR byte in a text file, and the project would differ from one generated on Linux.
        everything = json.loads((Path(__file__).resolve().parent / "fixtures/v2/everything.json").read_bytes())
        languages = list(translations.available("android"))
        project = generate(
            self.root, **{**everything, "feature_modules": ["orders"], "languages": languages}, accent_colour="#16A34A"
        )
        binary = ANDROID.manifest.binary_suffixes
        found = sorted(
            path.relative_to(project).as_posix()
            for path in project.rglob("*")
            if path.is_file() and path.suffix.lower() not in binary and b"\r" in path.read_bytes()
        )
        self.assertEqual(["gradlew.bat"], found)

    def test_dev_tooling_reaches_dev_and_debug_builds_only(self):
        project = generate(self.root, features=["devtools", "perfhud"])
        app_build = (project / "app/build.gradle.kts").read_text(encoding="utf-8")
        for module in (":core:devtools", ":core:perfhud"):
            wiring = [line.strip() for line in app_build.splitlines() if f'project("{module}")' in line]
            self.assertEqual(
                [f'debugImplementation(project("{module}"))', f'"devImplementation"(project("{module}"))'], wiring
            )
        # Nothing a shippable release compiles may name them: :app's main sources would pull them in, and
        # so would any other module that every build has.
        naming = [
            path.relative_to(project).as_posix()
            for path in (project / "app/src/main").rglob("*")
            if path.suffix in (".kt", ".xml") and re.search(r"core\.devtools|perfhud", path.read_text(encoding="utf-8"))
        ]
        self.assertEqual([], naming)
        dependents = sorted(
            path.parent.relative_to(project).as_posix()
            for path in project.rglob("build.gradle.kts")
            if re.search(r'project\(":core:(devtools|perfhud)"\)', path.read_text(encoding="utf-8"))
        )
        self.assertEqual(["app", "core/perfhud"], dependents)
        self.assertTrue(
            (project / "build-logic/convention/src/main/kotlin/com/acme/field/buildlogic/guard/DevToolingGuard.kt").is_file()
        )
        # The adb receiver is the debug manifest's alone: run-as, its only way in, needs a debuggable build.
        self.assertFalse((project / "core/perfhud/src/main/AndroidManifest.xml").exists())
        manifest = (project / "core/perfhud/src/debug/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:exported="false"', manifest)
        self.assertNotIn('android:exported="true"', manifest)
        self.assertTrue((project / "docs/PERFORMANCE-OVERLAY.md").is_file())

    def test_the_overlay_shares_the_analytics_jankstats_or_brings_its_own(self):
        feed = "core/perfhud/src/main/kotlin/com/acme/field/core/perfhud/frame/FrameFeed.kt"
        without = (generate(self.root / "without", features=["devtools", "perfhud"]) / feed).read_text(encoding="utf-8")
        shared = (generate(self.root / "with", features=["devtools", "perfhud", "analytics"]) / feed).read_text(
            encoding="utf-8"
        )
        self.assertIn("JankStats.createAndTrack(window, listener)", without)
        self.assertNotIn("FrameJankRecorder", without)
        self.assertIn("jankRecorder.frameListener = listener", shared)
        self.assertNotIn("createAndTrack", shared)

    def test_without_the_inspector_the_root_has_no_tooling_seam(self):
        project = generate(self.root, features=["network"])
        package = "com/acme/field"
        for owned in (
            "core/devtools",
            f"core/ui/src/main/kotlin/{package}/core/ui/tooling",
            f"core/common/src/main/kotlin/{package}/core/common/environment",
            f"build-logic/convention/src/main/kotlin/{package}/buildlogic/guard/DevToolingGuard.kt",
        ):
            self.assertFalse((project / owned).exists(), owned)
        for name in ("MainActivity.kt", "ui/AppRoot.kt"):
            source = (project / f"app/src/main/kotlin/{package}" / name).read_text(encoding="utf-8")
            self.assertNotIn("DevTooling", source, name)
        flavors = (project / "build-logic/convention/src/main/kotlin/AndroidApplicationFlavorsConventionPlugin.kt")
        self.assertNotIn("DevTooling", flavors.read_text(encoding="utf-8"))

    def test_without_the_overlay_nothing_of_it_is_left(self):
        project = generate(self.root, features=["devtools"])
        self.assertFalse((project / "core/perfhud").exists())
        self.assertFalse((project / "docs/PERFORMANCE-OVERLAY.md").exists())
        guard = "build-logic/convention/src/main/kotlin/com/acme/field/buildlogic/guard/DevToolingGuard.kt"
        for name in ("settings.gradle.kts", "app/build.gradle.kts", "docs/ARCHITECTURE.md", guard):
            self.assertNotIn("perfhud", (project / name).read_text(encoding="utf-8"), name)

    def test_a_module_name_that_is_a_template_token_stays_itself(self):
        project = generate(self.root, app_name="Store", feature_modules=["database_app"])
        settings_text = (project / "settings.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('include(":data:database_app")', settings_text)
        self.assertNotIn(":data:datastore", settings_text)


class ProjectScriptsTest(unittest.TestCase):
    """add_feature.py and remove_feature.py against a real generated project."""

    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.base = generate(
            Path(cls.temp.name), features=["sample", "settings", "network"], feature_modules=["orders"], languages=["de", "pt-BR"],
        )

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def copy(self) -> Path:
        import shutil

        target = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, target)
        shutil.copytree(self.base, target / "p")
        return target / "p"

    def destinations(self, project: Path) -> str:
        return next(project.rglob("AppDestinations.kt")).read_text(encoding="utf-8")

    def test_a_new_tab_never_takes_the_start_destination(self):
        from add_feature import main

        project = self.copy()
        with redirect_stdout(io.StringIO()):
            self.assertEqual(0, main(["wallet", "--tab", "--project", str(project)]))
        text = self.destinations(project)
        order = [text.index(key) for key in ("ShellTab(key = SampleListKey", "ShellTab(key = OrdersListKey", "ShellTab(key = WalletListKey", "ShellTab(key = SettingsKey")]
        self.assertEqual(sorted(order), order)
        self.assertIn("import com.acme.field.feature.wallet.api.WalletListKey", text)
        self.assertIn("icon = AppIcons.CreditCard", text)

    def test_a_tab_list_it_cannot_read_is_left_to_the_user(self):
        from add_feature import main

        project = self.copy()
        path = next(project.rglob("AppDestinations.kt"))
        lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
        opening = next(index for index, line in enumerate(lines) if "val tabs: List<ShellTab> = listOf(" in line)
        unclosed = lines[: opening + 1] + [line for line in lines[opening + 1 :] if line.strip() != ")"]
        path.write_text("".join(unclosed), encoding="utf-8")
        with redirect_stdout(io.StringIO()) as out:
            self.assertEqual(0, main(["wallet", "--tab", "--project", str(project)]))
        self.assertIn("add the tabs by hand", out.getvalue())
        self.assertNotIn("WalletListKey, label", self.destinations(project))

    def test_a_new_module_ships_every_language_the_project_does(self):
        from add_feature import main

        project = self.copy()
        # Only the new module's files are written; a translation someone edited elsewhere stays.
        edited = project / "feature/orders/src/main/res/values-de/strings.xml"
        edited.write_text(edited.read_text(encoding="utf-8").replace("</resources>", "    <!-- mine -->\n</resources>"), encoding="utf-8")
        before = edited.read_text(encoding="utf-8")
        self.assertIn("<!-- mine -->", before)
        with redirect_stdout(io.StringIO()):
            main(["wallet", "--project", str(project)])
        res = project / "feature/wallet/src/main/res"
        self.assertTrue((res / "values-de/strings.xml").is_file())
        self.assertTrue((res / "values-pt-rBR/strings.xml").is_file())
        self.assertEqual(before, edited.read_text(encoding="utf-8"))

    def test_remove_undoes_add(self):
        from add_feature import main as add
        from remove_feature import main as remove

        project = self.copy()
        before = {
            name: (project / name).read_text(encoding="utf-8")
            for name in ("settings.gradle.kts", "app/build.gradle.kts")
        }
        before_tabs = self.destinations(project)
        with redirect_stdout(io.StringIO()):
            add(["wallet", "--tab", "--project", str(project)])
            self.assertEqual(0, remove(["wallet", "--yes", "--project", str(project)]))
        for name, text in before.items():
            self.assertEqual(text, (project / name).read_text(encoding="utf-8"), name)
        self.assertEqual(before_tabs, self.destinations(project))
        self.assertFalse((project / "feature/wallet").exists())

    def test_a_tab_goes_even_when_its_label_has_been_edited(self):
        from add_feature import main as add
        from remove_feature import main as remove

        project = self.copy()
        with redirect_stdout(io.StringIO()):
            add(["wallet", "--tab", "--project", str(project)])
        path = next(project.rglob("AppDestinations.kt"))
        text = path.read_text(encoding="utf-8")
        edited = text.replace("label = WalletR.string.wallet_title", 'label = "Purse"')
        self.assertNotEqual(text, edited)
        path.write_text(edited, encoding="utf-8")
        with redirect_stdout(io.StringIO()):
            self.assertEqual(0, remove(["wallet", "--yes", "--project", str(project)]))
        remaining = self.destinations(project)
        self.assertNotIn("Wallet", remaining)
        self.assertNotIn("Purse", remaining)
        self.assertIn("ShellTab(key = OrdersListKey", remaining)

    def test_a_missing_tab_file_still_gets_the_gradle_edits(self):
        # Stopping at a renamed AppDestinations would leave the module half removed: its directory
        # gone, its include still in settings.gradle.kts.
        from add_feature import main as add
        from remove_feature import main as remove

        project = self.copy()
        with redirect_stdout(io.StringIO()):
            add(["wallet", "--project", str(project)])
        next(project.rglob("AppDestinations.kt")).unlink()
        with redirect_stdout(io.StringIO()) as out:
            self.assertEqual(0, remove(["wallet", "--yes", "--project", str(project)]))
        self.assertIn("check the tabs by hand", out.getvalue())
        self.assertNotIn(":wallet", (project / "settings.gradle.kts").read_text(encoding="utf-8"))
        self.assertNotIn(":wallet", (project / "app/build.gradle.kts").read_text(encoding="utf-8"))
        self.assertFalse((project / "feature/wallet").exists())

    def test_a_template_module_cannot_be_removed(self):
        from remove_feature import main

        with redirect_stdout(io.StringIO()):
            self.assertEqual(1, main(["settings", "--yes", "--project", str(self.copy())]))


if __name__ == "__main__":
    unittest.main()
