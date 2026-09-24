"""
The generator's own checks.

    py -m unittest discover -s tests -t .
"""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from genkit.build import plan  # noqa: E402
from genkit.catalogue import GROUPS, HEADLINES, catalogue, describe  # noqa: E402
from genkit.render import (  # noqa: E402
    _is_hollow_kotlin,
    apply_feel,
    brand_colours,
    collapse_blank_runs,
    rename,
    strip_markers,
)
from genkit.scaffold import generated_blocks, pascal, title  # noqa: E402
from genkit.spec import (  # noqa: E402
    FEATURES,
    DESIGN_STYLE_NAMES,
    MOTION_STYLE_NAMES,
    RESERVED_MODULE_NAMES,
    KeystoreSpec,
    ProjectSpec,
    SpecError,
    preset_features,
    resolve_features,
)


def keystore(**overrides) -> KeystoreSpec:
    base = dict(
        name="prod", alias="upload", store_password="hunter22", key_password="hunter22",
    )
    base.update(overrides)
    return KeystoreSpec(**base)


def spec(**overrides) -> ProjectSpec:
    base = dict(app_name="Acme Field", package_name="com.acme.field")
    base.update(overrides)
    return ProjectSpec(**base)


class MarkerStrippingTest(unittest.TestCase):

    def test_enabled_block_keeps_its_contents_and_drops_the_markers(self):
        source = "a\n// <opt:push>\nb\n// </opt:push>\nc\n"

        self.assertEqual("a\nb\nc\n", strip_markers(source, {"push"}, {}))

    def test_disabled_block_removes_everything_inside(self):
        source = "a\n// <opt:push>\nb\n// </opt:push>\nc\n"

        self.assertEqual("a\nc\n", strip_markers(source, set(), {}))

    def test_a_disabled_outer_block_suppresses_an_enabled_inner_one(self):
        # "Crashlytics needs Firebase", expressed at the file level: an import for the inner
        # feature must not survive when the outer one is off, or the file will not compile.
        source = (
            "// <opt:firebase>\n"
            "keep-outer\n"
            "// <opt:crashlytics>\n"
            "keep-inner\n"
            "// </opt:crashlytics>\n"
            "// </opt:firebase>\n"
        )

        self.assertEqual("", strip_markers(source, {"crashlytics"}, {}))

    def test_nested_blocks_both_enabled(self):
        source = (
            "// <opt:firebase>\n"
            "outer\n"
            "// <opt:crashlytics>\n"
            "inner\n"
            "// </opt:crashlytics>\n"
            "// </opt:firebase>\n"
        )

        self.assertEqual("outer\ninner\n", strip_markers(source, {"firebase", "crashlytics"}, {}))

    def test_inline_else_emits_its_code_verbatim_when_the_feature_is_off(self):
        source = "// <opt:!workmanager>class App : Application() {\n"

        self.assertEqual("class App : Application() {\n", strip_markers(source, set(), {}))

    def test_inline_else_is_dropped_when_the_feature_is_on(self):
        source = "// <opt:!workmanager>class App : Application() {\n"

        self.assertEqual("", strip_markers(source, {"workmanager"}, {}))

    def test_inline_else_inside_a_disabled_block_stays_suppressed(self):
        source = "// <opt:push>\n// <opt:!workmanager>x\n// </opt:push>\n"

        self.assertEqual("", strip_markers(source, set(), {}))

    def test_an_any_of_block_survives_when_one_of_its_features_is_on(self):
        # The javax.inject.Inject import in the application class: needed by three optional
        # blocks, owned by none of them.
        source = (
            "// <opt:analytics|push|workmanager>\n"
            "import javax.inject.Inject\n"
            "// </opt:analytics|push|workmanager>\n"
        )

        self.assertEqual("import javax.inject.Inject\n", strip_markers(source, {"push"}, {}))

    def test_an_any_of_block_is_dropped_when_all_of_its_features_are_off(self):
        source = (
            "// <opt:analytics|push|workmanager>\n"
            "import javax.inject.Inject\n"
            "// </opt:analytics|push|workmanager>\n"
        )

        self.assertEqual("", strip_markers(source, {"room"}, {}))

    def test_generated_marker_is_replaced_by_its_lines(self):
        source = "before\n// <generated:modules>\nafter\n"
        blocks = {"modules": ['include(":data:a")\n', 'include(":data:b")\n']}

        self.assertEqual(
            'before\ninclude(":data:a")\ninclude(":data:b")\nafter\n',
            strip_markers(source, set(), blocks),
        )

    def test_generated_marker_with_nothing_to_insert_leaves_no_trace(self):
        self.assertEqual("a\nb\n", strip_markers("a\n// <generated:none>\nb\n", set(), {}))

    def test_a_line_mentioning_a_feature_in_prose_is_not_a_marker(self):
        source = "// Firebase is optional. See the readme.\n"

        self.assertEqual(source, strip_markers(source, set(), {}))


class RenameTest(unittest.TestCase):

    def test_package_import_and_path_are_all_rewritten(self):
        source = "package com.base.app.core.ui\nimport com.base.app.R\n// com/base/app/core\n"

        result = rename(source, spec())

        self.assertIn("package com.acme.field.core.ui", result)
        self.assertIn("import com.acme.field.R", result)
        self.assertIn("com/acme/field/core", result)
        self.assertNotIn("base", result)

    def test_class_names_resource_names_and_schemes_each_use_their_own_form(self):
        source = 'BaseAppApplication "base_app_default" baseapp.example.com'

        result = rename(source, spec())

        self.assertEqual('AcmeFieldApplication "acme_field_default" acmefield.example.com', result)

    def test_the_package_token_is_consumed_before_the_shorter_ones_could_match(self):
        # `com.base.app` contains no `baseapp`, but ordering is the kind of thing that breaks
        # silently the day someone adds a token — so it is pinned here.
        self.assertEqual("com.acme.field.di", rename("com.base.app.di", spec()))

    def test_a_single_word_app_name_still_produces_a_usable_snake_form(self):
        result = rename('"base_app_default"', spec(app_name="Bare", package_name="io.bare.min"))

        self.assertEqual('"bare_default"', result)


class BlankRunTest(unittest.TestCase):

    def test_a_gap_left_by_a_stripped_block_collapses_to_one_blank_line(self):
        self.assertEqual("a\n\nb\n", collapse_blank_runs("a\n\n\n\n\nb\n"))

    def test_a_blank_line_before_a_closing_brace_is_removed(self):
        # ktlint fails the build on this one, so it is correctness rather than tidiness: a block
        # stripped from the end of a function would otherwise leave a generated project red.
        self.assertEqual(
            "fun x() {\n    a()\n}\n",
            collapse_blank_runs("fun x() {\n    a()\n\n}\n"),
        )

    def test_indentation_before_the_brace_is_preserved(self):
        self.assertEqual(
            "class A {\n    fun b() {\n        c()\n    }\n}\n",
            collapse_blank_runs("class A {\n    fun b() {\n        c()\n\n    }\n}\n"),
        )

    def test_a_blank_line_after_an_opening_brace_is_removed(self):
        # The mirror case, and ktlint's NoEmptyFirstLineInMethodBlock fails on it just as hard.
        self.assertEqual(
            "fun x() {\n    a()\n}\n",
            collapse_blank_runs("fun x() {\n\n    a()\n}\n"),
        )

    def test_a_blank_line_after_an_opening_parenthesis_is_left_alone(self):
        # Legal Kotlin, and sometimes how a long argument list is laid out. Collapsing it would
        # be the generator reformatting code nobody asked it to touch.
        argument_list = "val m = mapOf(\n\n    1 to 2,\n)\n"
        self.assertEqual(argument_list, collapse_blank_runs(argument_list))

    def test_a_single_blank_line_is_left_alone(self):
        self.assertEqual("a\n\nb\n", collapse_blank_runs("a\n\nb\n"))


class HollowFileTest(unittest.TestCase):

    def test_a_file_left_with_only_a_package_and_imports_is_hollow(self):
        # The app module's FeatureBindingsModule when none of settings, auth or onboarding is
        # on: an empty Hilt module and three unused imports, which detekt fails the build over.
        source = "\n".join(["package a.b", "", "import c.D", "import c.E", ""])

        self.assertTrue(_is_hollow_kotlin(source))

    def test_comments_alone_do_not_save_a_file(self):
        source = "\n".join(["package a.b", "", "/**", " * Why this existed.", " */", ""])

        self.assertTrue(_is_hollow_kotlin(source))

    def test_one_surviving_declaration_keeps_it(self):
        source = "\n".join(["package a.b", "", "import c.D", "", "object X", ""])

        self.assertFalse(_is_hollow_kotlin(source))

    def test_an_empty_file_is_hollow(self):
        self.assertTrue(_is_hollow_kotlin(""))


class FeatureResolutionTest(unittest.TestCase):

    def test_requirements_are_pulled_in_transitively(self):
        # push → firebase → the two seams it binds, and nobody who ticked push has to know that.
        self.assertEqual({"push", "firebase", "analytics", "flags"}, resolve_features({"push"}))

    def test_a_two_step_chain_resolves(self):
        self.assertEqual(
            {"googlesignin", "auth", "network", "forms"},
            resolve_features({"googlesignin"}),
        )

    def test_an_empty_selection_stays_empty(self):
        self.assertEqual(set(), resolve_features(set()))


class ValidationTest(unittest.TestCase):

    def test_a_single_segment_package_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(package_name="myapp").validated()

    def test_an_uppercase_package_segment_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(package_name="com.Acme.field").validated()

    def test_a_reserved_word_as_a_package_segment_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(package_name="com.acme.class").validated()

    def test_a_segment_starting_with_a_digit_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(package_name="com.acme.1field").validated()

    def test_target_sdk_below_min_sdk_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(min_sdk=34, target_sdk=30).validated()

    def test_a_malformed_version_name_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(version_name="1.0").validated()

    def test_a_reserved_module_name_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(feature_modules=("core",)).validated()

    def test_a_module_named_after_one_the_template_ships_is_rejected(self):
        # The scaffold would write a generic repository over the curated one and leave the rest
        # of :feature:auth calling methods that no longer exist.
        for name in ("auth", "settings", "onboarding", "sample"):
            with self.subTest(name=name), self.assertRaises(SpecError):
                spec(feature_modules=(name,)).validated()

    def test_duplicate_module_names_are_rejected(self):
        with self.assertRaises(SpecError):
            spec(feature_modules=("home", "home")).validated()

    def test_validation_returns_a_spec_with_requirements_resolved(self):
        result = spec(features=frozenset({"push"})).validated()

        self.assertIn("firebase", result.features)

    def test_desugaring_is_decided_by_min_sdk(self):
        self.assertTrue(spec(min_sdk=24).needs_desugaring)
        self.assertFalse(spec(min_sdk=26).needs_desugaring)


class LookAndFeelTest(unittest.TestCase):

    def test_an_unknown_motion_style_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(motion_style="Wobbly").validated()

    def test_every_named_style_validates(self):
        for name in MOTION_STYLE_NAMES:
            self.assertEqual(name, spec(motion_style=name).validated().motion_style)

    def test_an_unknown_design_style_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(design_style="Brutal").validated()

    def test_every_design_style_validates(self):
        for name in DESIGN_STYLE_NAMES:
            self.assertEqual(name, spec(design_style=name).validated().design_style)

    def test_the_styles_are_written_into_the_one_theme_call(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / "app/src/main/kotlin/com/acme/field/ui/AppRoot.kt"
            root.parent.mkdir(parents=True)
            root.write_text(
                "import com.acme.field.core.designsystem.theme.AppTheme\n"
                "    AppTheme(mode = themeMode, hapticsEnabled = settings.hapticsEnabled) {\n",
                encoding="utf-8",
            )
            apply_feel(Path(directory), spec(design_style="Playful", motion_style="Bouncy"))
            text = root.read_text(encoding="utf-8")

        self.assertIn("designStyle = AppDesignStyle.Playful", text)
        self.assertIn("motionStyle = AppMotionStyle.Bouncy", text)
        self.assertIn("import com.acme.field.core.designsystem.theme.AppDesignStyle", text)

    def test_the_accent_must_be_a_six_digit_hex(self):
        for bad in ("blue", "#12345", "#GGGGGG", "2C6BED88"):
            with self.assertRaises(SpecError, msg=bad):
                spec(accent_colour=bad).validated()

    def test_an_accent_is_accepted_with_or_without_the_hash(self):
        for good in ("#2C6BED", "2c6bed"):
            self.assertEqual(good, spec(accent_colour=good).validated().accent_colour)

    def test_no_accent_means_keep_the_template_palette(self):
        self.assertEqual("", spec().validated().accent_colour)

    def test_a_supporting_colour_must_also_be_a_six_digit_hex(self):
        for field in ("secondary_colour", "tertiary_colour"):
            with self.assertRaises(SpecError, msg=field):
                spec(**{field: "#12345"}).validated()

    def test_a_blank_supporting_colour_is_derived_from_the_primary(self):
        colours = brand_colours(spec(accent_colour="#16A34A").validated())

        # Material's rule: the chroma taken out for the secondary, a sixth of a turn for the
        # tertiary. Both must land somewhere other than on the primary, or the palette has one
        # colour in it wearing three names.
        self.assertNotEqual(colours["Accent"], colours["Secondary"])
        self.assertNotEqual(colours["Accent"], colours["Tertiary"])
        self.assertNotEqual(colours["Secondary"], colours["Tertiary"])

    def test_a_supplied_supporting_colour_is_used_as_given(self):
        colours = brand_colours(
            spec(accent_colour="#16A34A", secondary_colour="#FF00FF").validated()
        )

        self.assertEqual((1.0, 0.0, 1.0), colours["Secondary"])

    def test_the_derived_secondary_is_the_primary_with_the_chroma_taken_out(self):
        import colorsys

        colours = brand_colours(spec(accent_colour="#16A34A").validated())
        primary_hue, _, primary_saturation = colorsys.rgb_to_hls(*colours["Accent"])
        derived_hue, _, derived_saturation = colorsys.rgb_to_hls(*colours["Secondary"])

        self.assertAlmostEqual(primary_hue, derived_hue, places=5)
        self.assertLess(derived_saturation, primary_saturation)



class PresetTest(unittest.TestCase):

    def test_a_preset_resolves_its_own_dependencies(self):
        # auth needs network and forms; nobody should have to know that.
        standard = preset_features("standard")
        self.assertIn("network", standard)
        self.assertIn("forms", standard)

    def test_an_unknown_preset_names_the_ones_that_exist(self):
        with self.assertRaises(SpecError) as caught:
            preset_features("massive")
        self.assertIn("standard", str(caught.exception))


class InjectionTest(unittest.TestCase):
    """
    Free text from a spec is written into Kotlin that Gradle compiles and runs, and into XML. A spec
    file can come from anyone, so nothing in it may be able to become code.
    """

    def test_a_url_cannot_close_the_string_or_open_a_template(self):
        for url in (
            'https://x.com/" + System.exit(0) + "',
            "https://x.com/${System.getenv()}",
            "https://x.com/\\path",
            "https://x.com/a b",
            "ftp://x.com/",
            "javascript:alert(1)",
        ):
            with self.assertRaises(SpecError, msg=url):
                spec(api_base_urls={"dev": url}).validated()

    def test_ordinary_urls_are_accepted(self):
        for url in ("https://api.example.com/v1/", "http://10.0.2.2:8080/api/?key=a&b=c"):
            spec(api_base_urls={"prod": url}).validated()
        spec(web_socket_urls={"dev": "wss://api.example.com/ws"}).validated()

    def test_a_socket_url_must_be_a_socket_url(self):
        with self.assertRaises(SpecError):
            spec(web_socket_urls={"dev": "https://api.example.com/ws"}).validated()

    def test_an_unknown_flavour_is_refused(self):
        with self.assertRaises(SpecError):
            spec(api_base_urls={"qa": "https://api.example.com/"}).validated()

    def test_font_names_are_family_names(self):
        for bad in ('Inter" ; val x = "', "Inter\\", "", "a" * 61, "<b>"):
            with self.assertRaises(SpecError, msg=bad):
                spec(font_name=bad).validated()
        for good in ("Plus Jakarta Sans", "M PLUS 1p", "IBM Plex Mono"):
            spec(font_name=good, mono_font_name=good).validated()

    def test_deep_link_parts_cannot_carry_markup(self):
        for scheme in ("my<app", "My", "1app", "a b"):
            with self.assertRaises(SpecError, msg=scheme):
                spec(deeplink_scheme=scheme).validated()
        for host in ("example.com</string>", "localhost", "-x.com", "a..com"):
            with self.assertRaises(SpecError, msg=host):
                spec(deeplink_host=host).validated()
        spec(deeplink_scheme="my-app+v2", deeplink_host="links.example.co.uk").validated()

    def test_backslashes_do_not_reach_the_regex_engine(self):
        from genkit.render import apply_fonts

        with tempfile.TemporaryDirectory() as directory:
            fonts = Path(directory) / "core/designsystem/src/main/kotlin/com/acme/field/core/designsystem/theme/AppFonts.kt"
            fonts.parent.mkdir(parents=True)
            fonts.write_text('const val Sans = "DM Sans"\nconst val Mono = "JetBrains Mono"\n', encoding="utf-8")
            apply_fonts(Path(directory), spec(font_name="M PLUS 1p"))
            self.assertIn('const val Sans = "M PLUS 1p"', fonts.read_text(encoding="utf-8"))


class KeystorePropertiesTest(unittest.TestCase):

    def test_passwords_survive_java_properties(self):
        from genkit.render import _properties_value

        self.assertEqual("a\\\\b", _properties_value("a\\b"))
        self.assertEqual("\\ \\ pw", _properties_value("  pw"))
        self.assertEqual("plain=value:ok", _properties_value("plain=value:ok"))


class LegacySpecTest(unittest.TestCase):
    """A spec saved by an earlier release still generates."""

    def test_removed_features_drop_out_and_merged_ones_map_across(self):
        resolved = spec(features=frozenset({"crashlytics", "staticanalysis", "ci", "network"})).validated()

        self.assertIn("firebase", resolved.features)
        self.assertIn("network", resolved.features)
        for gone in ("crashlytics", "staticanalysis", "ci"):
            self.assertNotIn(gone, resolved.features)

    def test_a_genuinely_unknown_feature_is_still_refused(self):
        with self.assertRaises(SpecError):
            spec(features=frozenset({"telepathy"})).validated()


class ReservedNameTest(unittest.TestCase):

    def test_the_modules_new_features_ship_are_reserved(self):
        for name in ("feed", "search", "profile"):
            self.assertIn(name, RESERVED_MODULE_NAMES)
            with self.assertRaises(SpecError):
                spec(feature_modules=(name,)).validated()


class DerivedNameTest(unittest.TestCase):

    def test_spaces_and_punctuation_become_a_pascal_name(self):
        self.assertEqual("MyGreatApp", spec(app_name="My Great App").pascal_name)
        self.assertEqual("MyGreatApp", spec(app_name="my-great_app").pascal_name)

    def test_the_snake_form_splits_on_case_boundaries(self):
        self.assertEqual("my_great_app", spec(app_name="My Great App").snake_name)

    def test_the_lower_form_has_no_separators(self):
        self.assertEqual("mygreatapp", spec(app_name="My Great App").lower_name)

    def test_the_package_path_uses_directory_separators(self):
        self.assertEqual("com/acme/field", spec().package_path)


class ScaffoldTest(unittest.TestCase):

    def test_module_names_become_class_and_title_forms(self):
        self.assertEqual("OrderHistory", pascal("order_history"))
        self.assertEqual("Order history", title("order_history"))

    def test_each_module_contributes_an_include_and_an_app_dependency(self):
        blocks = generated_blocks(spec(feature_modules=("orders", "wallet")))

        self.assertEqual(
            ['include(":data:orders")\n', 'include(":data:wallet")\n'],
            blocks["data-modules"],
        )
        self.assertEqual(
            ['    implementation(project(":feature:orders"))\n',
             '    implementation(project(":feature:wallet"))\n'],
            blocks["app-feature-dependencies"],
        )

    def test_the_reference_feature_supplies_its_own_start_destination(self):
        # The template carries SampleListKey behind an <opt:sample> block, so emitting one here
        # too would produce two.
        blocks = generated_blocks(spec(features=frozenset({"sample"}), feature_modules=("orders",)))

        self.assertEqual([], blocks["start-destination"])

    def test_without_the_reference_feature_the_first_module_becomes_the_start_destination(self):
        blocks = generated_blocks(spec(feature_modules=("orders", "wallet")))

        self.assertEqual(["        ?: OrdersListKey\n"], blocks["start-destination"])
        self.assertEqual(
            [
                "import com.acme.field.feature.orders.OrdersListKey\n",
                "import com.acme.field.feature.orders.R as OrdersR\n",
                "import com.acme.field.feature.wallet.WalletListKey\n",
                "import com.acme.field.feature.wallet.R as WalletR\n",
            ],
            blocks["start-destination-import"],
        )

    def test_every_named_module_becomes_a_tab_in_the_order_given(self):
        blocks = generated_blocks(spec(feature_modules=("orders", "order_history")))

        self.assertEqual(2, len(blocks["shell-tabs"]))
        self.assertIn("key = OrdersListKey", blocks["shell-tabs"][0])
        # The label is the feature's own title string, so translating the feature translates its tab.
        self.assertIn("label = OrderHistoryR.string.order_history_title", blocks["shell-tabs"][1])

    def test_settings_supplies_the_start_destination_when_nothing_else_does(self):
        blocks = generated_blocks(spec(features=frozenset({"settings"})))

        self.assertEqual(["        ?: SettingsKey\n"], blocks["start-destination"])

    def test_with_neither_the_start_destination_says_what_to_do(self):
        blocks = generated_blocks(spec())

        self.assertIn("error(", blocks["start-destination"][0])



class KeystoreValidationTest(unittest.TestCase):

    def test_a_well_formed_key_validates(self):
        self.assertEqual(1, len(spec(keystores=(keystore(),)).validated().keystores))

    def test_an_unknown_key_name_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(keystores=(keystore(name="release"),)).validated()

    def test_a_password_keytool_would_refuse_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(keystores=(keystore(key_password="short"),)).validated()

    def test_a_distinguished_name_that_would_split_into_two_fields_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(keystores=(keystore(common_name="Acme, O=Somebody Else"),)).validated()

    def test_an_alias_that_would_read_as_a_keytool_flag_is_rejected(self):
        with self.assertRaises(SpecError):
            spec(keystores=(keystore(alias="-storetype"),)).validated()

    def test_the_same_key_cannot_be_described_twice(self):
        with self.assertRaises(SpecError):
            spec(keystores=(keystore(), keystore())).validated()


class CatalogueTest(unittest.TestCase):
    """The tables the website renders from, checked against the features that actually exist."""

    def test_every_feature_has_a_group_and_a_headline(self):
        for feature in FEATURES:
            if feature.implied_only:
                continue
            with self.subTest(feature=feature.key):
                self.assertIn(feature.key, GROUPS)
                self.assertIn(feature.key, HEADLINES)

    def test_a_feature_missing_from_the_tables_raises_rather_than_defaulting(self):
        with self.assertRaises(KeyError):
            describe("a-feature-nobody-has-written")

    def test_every_group_a_feature_names_is_one_the_site_will_render(self):
        rendered = {group["name"] for group in catalogue()["groups"]}

        for entry in catalogue()["features"]:
            with self.subTest(feature=entry["key"]):
                self.assertIn(entry["group"], rendered)

    def test_a_headline_says_something_the_title_does_not(self):
        # A headline that repeats the title tells a visitor nothing, and repeating it is exactly
        # what the missing entries used to produce.
        for feature in FEATURES:
            if feature.implied_only:
                continue
            with self.subTest(feature=feature.key):
                self.assertNotEqual(feature.title, HEADLINES[feature.key])


class VariantOverlayTest(unittest.TestCase):
    """A variant for a feature that is off must not recreate a module that is also off."""

    def test_a_variant_file_under_a_removed_module_is_skipped(self):
        from genkit.render import overlay_variants
        from genkit.build import VARIANTS_DIR

        with tempfile.TemporaryDirectory() as directory:
            project = Path(directory)
            # Firebase off, and flags off too: the firebase-off variant carries a FlagsModule
            # that must not appear in a project with no :core:flags.
            overlay_variants(VARIANTS_DIR, project, spec(features=frozenset({"analytics"})).validated())

            self.assertTrue(any(project.rglob("AnalyticsModule.kt")))
            self.assertFalse(any(project.rglob("FlagsModule.kt")))


class PlanTest(unittest.TestCase):
    """What --dry-run promises, which has to be what a real run would then do."""

    def test_an_implied_feature_shows_as_enabled(self):
        resolved = spec(features=frozenset({"push"})).validated()

        self.assertIn("firebase", plan(resolved)["enabled"])

    def test_a_disabled_feature_contributes_the_paths_it_owns(self):
        resolved = spec(features=frozenset()).validated()

        self.assertIn("core/analytics", plan(resolved)["removed"])

    def test_a_package_placeholder_in_a_path_is_resolved_against_the_spec(self):
        resolved = spec(features=frozenset()).validated()

        self.assertIn(
            "core/ui/src/main/kotlin/com/acme/field/core/ui/AppNetworkImage.kt",
            plan(resolved)["removed"],
        )

    def test_an_enabled_feature_keeps_its_paths(self):
        resolved = spec(features=frozenset({"analytics"})).validated()

        self.assertNotIn("core/analytics", plan(resolved)["removed"])

    def test_each_named_module_is_planned_as_a_data_and_feature_pair(self):
        resolved = spec(feature_modules=("orders",)).validated()

        self.assertEqual([":data:orders", ":feature:orders"], plan(resolved)["modules"])


class SpecRoundTripTest(unittest.TestCase):
    """--save-spec then --spec has to produce the same project."""

    def test_every_answer_survives_being_written_and_read_back(self):
        from create_project import load_spec, save_spec

        original = spec(
            features=frozenset({"network", "room"}),
            feature_modules=("orders",),
            keystores=(keystore(),),
            accent_colour="#112233",
            motion_style="Calm",
            design_style="Editorial",
            haptics_enabled=False,
            api_base_urls={"dev": "https://dev.example.com/"},
        ).validated()

        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "spec.json"
            save_spec(original, path)
            # Parsed as JSON as well, so a field that serialised to something unreadable fails
            # here rather than when somebody opens the file to hand-edit one answer.
            json.loads(path.read_text(encoding="utf-8"))

            self.assertEqual(original, load_spec(path))


class RemoveFeatureTest(unittest.TestCase):
    """The inverse of add_feature, which is the half that has to leave nothing behind."""

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.project = Path(temp.name)

    def _write(self, relative, text):
        path = self.project / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        return path

    def test_the_gradle_includes_go_and_the_others_stay(self):
        from remove_feature import unregister_modules

        path = self._write(
            "settings.gradle.kts",
            'include(":data:orders")\ninclude(":data:sample")\n'
            'include(":feature:orders")\ninclude(":feature:sample")\n',
        )

        unregister_modules(self.project, ("orders",))

        self.assertEqual(
            'include(":data:sample")\ninclude(":feature:sample")\n',
            path.read_text(encoding="utf-8"),
        )

    def test_the_app_dependency_goes(self):
        from remove_feature import unregister_dependencies

        path = self._write(
            "app/build.gradle.kts",
            "dependencies {\n"
            '    implementation(project(":feature:orders"))\n'
            '    implementation(project(":feature:sample"))\n'
            "}\n",
        )

        unregister_dependencies(self.project, ("orders",))

        remaining = path.read_text(encoding="utf-8")
        self.assertNotIn("orders", remaining)
        self.assertIn("sample", remaining)

    def test_the_tab_goes_even_when_its_label_has_been_edited(self):
        from remove_feature import unregister_tabs

        path = self._write(
            "app/src/main/kotlin/com/acme/field/ui/AppDestinations.kt",
            "import com.acme.field.feature.orders.OrdersListKey\n"
            "import com.acme.field.feature.sample.SampleListKey\n"
            "    val tabs: List<ShellTab> = listOf(\n"
            '        ShellTab(key = OrdersListKey, label = "Purchase orders", icon = AppIcons.Cart),\n'
            '        ShellTab(key = SampleListKey, label = "Sample", icon = AppIcons.Grid),\n'
            "    )\n",
        )

        unregister_tabs(self.project, "com.acme.field", ("orders",))

        remaining = path.read_text(encoding="utf-8")
        self.assertNotIn("Orders", remaining)
        self.assertIn("SampleListKey", remaining)

    def test_a_module_the_template_ships_is_refused(self):
        from remove_feature import normalise

        with self.assertRaises(SpecError):
            normalise("settings")

    def test_a_file_that_is_not_there_is_not_an_error(self):
        # A project whose AppDestinations has been renamed still has to get its Gradle edits,
        # rather than failing halfway with two of the three done.
        from remove_feature import unregister_tabs

        unregister_tabs(self.project, "com.acme.field", ("orders",))


if __name__ == "__main__":
    unittest.main()


class RecordTest(unittest.TestCase):
    """The generator-spec.json a project keeps of how it was made."""

    def test_the_record_loads_back_as_the_same_spec_and_holds_no_keys(self):
        from create_project import load_spec
        from genkit.build import build

        made = spec(
            features=frozenset({"network", "settings"}),
            feature_modules=("orders",),
            languages=("es",),
            keystores=(keystore(),),
        ).validated()
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp) / "App"
            build(made, project, zip_output=False)
            recorded = project / "generator-spec.json"

            text = recorded.read_text(encoding="utf-8")
            again = load_spec(recorded)

        self.assertNotIn("hunter22", text)
        self.assertIn('"commit"', text)
        self.assertEqual(made.features, again.features)
        self.assertEqual(made.feature_modules, again.feature_modules)
        self.assertEqual(made.languages, again.languages)
        self.assertEqual((), again.keystores)


class CommitStampTest(unittest.TestCase):

    def test_a_deployment_without_git_reads_the_commit_ci_wrote(self):
        from genkit.record import _commit

        with tempfile.TemporaryDirectory() as temp:
            repository = Path(temp)
            (repository / "generator").mkdir()
            (repository / "generator" / "COMMIT").write_text("8e3461d0a1b2\n", encoding="utf-8")

            self.assertEqual("8e3461d0a1b2", _commit(repository))

    def test_no_git_and_no_stamp_is_unknown(self):
        from genkit.record import _commit

        with tempfile.TemporaryDirectory() as temp:
            self.assertEqual("unknown", _commit(Path(temp)))


class CompileSdkTest(unittest.TestCase):

    def test_a_compile_sdk_below_what_the_libraries_need_is_refused(self):
        with self.assertRaisesRegex(SpecError, "compileSdk must be at least"):
            spec(target_sdk=35, compile_sdk=36).validated()
