"""The Android target: its settings regions, tabs, scaffolding, legacy look appliers and project scripts."""

from __future__ import annotations

import io
import json
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

from genkit import brand
from genkit.core import pipeline, translations
from genkit.core.record import RECORD_NAME
from genkit.core.target import RenderError
from genkit.targets import get
from genkit.targets.android import look, scaffold, settings, tabs, variants

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


class TokenCopyTest(unittest.TestCase):

    def test_a_shipped_token_copy_is_re_emitted_with_the_specs_brand(self):
        import shutil
        from unittest import mock

        from genkit.tokens import kotlin

        with tempfile.TemporaryDirectory() as temp:
            template = Path(temp) / "android"
            shutil.copytree(ANDROID.template_dir, template, ignore=shutil.ignore_patterns("build", ".gradle"))
            copy = template / kotlin.RELATIVE_PATH.format(package_path="com/base/app")
            copy.parent.mkdir(parents=True)
            copy.write_text("// emitted for the template's defaults\n", encoding="utf-8")
            with mock.patch.object(type(ANDROID), "template_dir", template):
                project = generate(Path(temp), accent_colour="#16A34A")
            text = (project / kotlin.RELATIVE_PATH.format(package_path="com/acme/field")).read_text(encoding="utf-8")
        self.assertTrue(text.startswith(kotlin.HEADER))
        self.assertIn("package com.acme.field.core.designsystem.theme.tokens", text)
        self.assertIn(f"accent = Color({brand.to_argb(brand.brand_ramps('#16A34A')['Accent'].base)}),", text)


class TabsTest(unittest.TestCase):

    def test_each_module_becomes_a_tab_in_the_order_given(self):
        regions = tabs.regions(spec(feature_modules=["orders", "order_history"]))
        self.assertIn("key = OrdersListKey", regions["shell-tabs"][0])
        # The label is the module's own title string, so translating the module translates its tab.
        self.assertIn("label = OrderHistoryR.string.order_history_title", regions["shell-tabs"][1])

    def test_the_reference_feature_supplies_its_own_start(self):
        self.assertEqual([], tabs.regions(spec(features=["sample"], feature_modules=["orders"]))["start-destination"])

    def test_without_it_the_first_module_starts(self):
        regions = tabs.regions(spec(feature_modules=["orders", "wallet"]))
        self.assertEqual(["        ?: OrdersListKey\n"], regions["start-destination"])
        self.assertEqual(
            [
                "import com.acme.field.feature.orders.OrdersListKey\n",
                "import com.acme.field.feature.orders.R as OrdersR\n",
                "import com.acme.field.feature.wallet.WalletListKey\n",
                "import com.acme.field.feature.wallet.R as WalletR\n",
            ],
            regions["start-destination-import"],
        )

    def test_the_feed_then_search_start_before_any_module(self):
        for features, key in ((["paging", "search"], "FeedKey"), (["search"], "SearchKey")):
            with self.subTest(features=features):
                regions = tabs.regions(spec(features=features, feature_modules=["orders"]))
                self.assertEqual([f"        ?: {key}\n"], regions["start-destination"])

    def test_profile_starts_before_settings(self):
        self.assertEqual(["        ?: ProfileKey\n"], tabs.regions(spec(features=["profile", "settings"]))["start-destination"])

    def test_settings_starts_when_nothing_else_does(self):
        self.assertEqual(["        ?: SettingsKey\n"], tabs.regions(spec(features=["settings"]))["start-destination"])

    def test_with_no_screen_to_start_on_the_build_says_what_to_do(self):
        (line,) = tabs.regions(spec(features=["network"]))["start-destination"]
        self.assertIn('error("Set a start destination', line)

    def test_the_labels_follow_the_bar(self):
        self.assertEqual(
            ["Home", "Feed", "Orders", "Profile", "Settings"],
            tabs.labels(spec(features=["sample", "paging", "profile", "settings"], feature_modules=["orders"])),
        )


class ScaffoldTest(unittest.TestCase):

    def test_each_module_contributes_its_includes_and_an_app_dependency(self):
        regions = scaffold.regions(spec(feature_modules=["orders", "wallet"]))
        self.assertEqual(['include(":data:orders")\n', 'include(":data:wallet")\n'], regions["data-modules"])
        self.assertEqual(['include(":feature:orders")\n', 'include(":feature:wallet")\n'], regions["feature-modules"])
        self.assertEqual(
            ['    implementation(project(":feature:orders"))\n', '    implementation(project(":feature:wallet"))\n'],
            regions["app-feature-dependencies"],
        )


class LookTest(unittest.TestCase):
    """The legacy appliers fail loudly when the template line they rewrite is gone."""

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.project = Path(temp.name)

    def write(self, relative: str, text: str) -> Path:
        path = self.project / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        return path

    def test_the_styles_go_into_the_one_theme_call(self):
        root = self.write(
            "app/src/main/kotlin/com/acme/field/ui/AppRoot.kt",
            "import com.acme.field.core.designsystem.theme.AppTheme\n"
            "    AppTheme(mode = themeMode, hapticsEnabled = settings.hapticsEnabled) {\n",
        )
        look.apply_feel(self.project, spec(design_style="Playful", motion_style="Bouncy", haptics_enabled=False))
        text = root.read_text(encoding="utf-8")
        self.assertIn("designStyle = AppDesignStyle.Playful", text)
        self.assertIn("motionStyle = AppMotionStyle.Bouncy", text)
        self.assertIn("hapticsEnabled = false", text)
        self.assertIn("import com.acme.field.core.designsystem.theme.AppDesignStyle", text)

    def test_a_missing_theme_call_is_an_error(self):
        self.write("app/src/main/kotlin/com/acme/field/ui/AppRoot.kt", "import com.acme.field.core.designsystem.theme.AppTheme\n")
        with self.assertRaises(RenderError):
            look.apply_feel(self.project, spec())

    def test_a_font_constant_that_moved_is_an_error(self):
        self.write("core/designsystem/src/main/kotlin/com/acme/field/core/designsystem/theme/AppFonts.kt", 'const val Sans = "DM Sans"\n')
        with self.assertRaises(RenderError):
            look.apply_fonts(self.project, spec())

    def test_backslashes_in_a_font_name_do_not_reach_the_regex_engine(self):
        fonts = self.write(
            "core/designsystem/src/main/kotlin/com/acme/field/core/designsystem/theme/AppFonts.kt",
            'const val Sans = "DM Sans"\nconst val Mono = "JetBrains Mono"\n',
        )
        look.apply_fonts(self.project, spec(font_name="M PLUS 1p"))
        self.assertIn('const val Sans = "M PLUS 1p"', fonts.read_text(encoding="utf-8"))

    def test_every_brand_shade_is_written_into_a_real_project(self):
        with tempfile.TemporaryDirectory() as temp:
            project = generate(Path(temp), accent_colour="#16A34A", features=["catalog"])
            palette = next(project.rglob("Palette.kt")).read_text(encoding="utf-8")
            catalog_colors = (project / "catalog/src/main/res/values/colors.xml").read_text(encoding="utf-8")
            catalog_label = (project / "catalog/src/main/res/values/strings.xml").read_text(encoding="utf-8")
        ramp = brand.brand_ramps("#16A34A")["Accent"]
        self.assertIn(f"internal val Accent = Color({brand.to_argb(ramp.base)})", palette)
        self.assertIn(f"#FF{brand.to_hex(ramp.base)[1:]}", catalog_colors)
        self.assertIn(">Acme Field Catalog<", catalog_label)


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
        self.assertEqual({"enabled", "disabled", "removed_paths", "modules", "tabs", "notes", "warnings"}, set(plan))
        self.assertIn("firebase", plan["enabled"])
        self.assertIn("core/ui/src/main/kotlin/com/acme/field/core/ui/AppNetworkImage.kt", plan["removed_paths"])
        self.assertEqual([":data:orders", ":feature:orders"], plan["modules"])

    def test_a_firebase_off_variant_never_recreates_a_module_that_is_also_off(self):
        project = generate(self.root, features=["analytics"])
        self.assertTrue(any(project.rglob("AnalyticsModule.kt")))
        self.assertFalse(any(project.rglob("FlagsModule.kt")))
        self.assertEqual({"firebase", "googlefonts"}, set(variants.VARIANTS))

    def test_only_the_batch_file_of_a_generated_project_holds_a_carriage_return(self):
        # Newline translation on Windows, or a template checked out with CRLF, shows up here as a
        # CR byte in a text file, and the project would differ from one generated on Linux.
        everything = json.loads((Path(__file__).resolve().parent / "fixtures/v2/everything.json").read_bytes())
        project = generate(
            self.root, **{**everything, "feature_modules": ["orders"], "languages": list(translations.available("android"))},
            accent_colour="#16A34A",
        )
        found = sorted(
            path.relative_to(project).as_posix()
            for path in project.rglob("*")
            if path.is_file() and path.suffix.lower() not in ANDROID.manifest.binary_suffixes and b"\r" in path.read_bytes()
        )
        self.assertEqual(["gradlew.bat"], found)

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
        self.assertIn("import com.acme.field.feature.wallet.WalletListKey", text)
        self.assertLess(text.index("WalletListKey, label"), text.index("// Settings sits last"))

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
