"""
The generator's own checks.

    py -m unittest discover -s tests -t .

Covers the two things that break silently: the marker grammar, and the rename. A bug in either
produces a project that *looks* right and fails to compile — or worse, compiles with the wrong
package name buried three directories down.

Stdlib `unittest` rather than pytest, so this runs on a machine with nothing installed, which is
the same reason the generator itself is dependency-free.
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from genkit.render import (  # noqa: E402
    _is_hollow_kotlin,
    collapse_blank_runs,
    rename,
    strip_markers,
)
from genkit.scaffold import generated_blocks, pascal, title  # noqa: E402
from genkit.spec import (  # noqa: E402
    MOTION_STYLE_NAMES,
    ProjectSpec,
    SpecError,
    preset_features,
    resolve_features,
)


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
        # push → firebase, and nothing else has to be known by the person who ticked push.
        self.assertEqual({"push", "firebase"}, resolve_features({"push"}))

    def test_a_two_step_chain_resolves(self):
        self.assertEqual(
            {"crashlytics", "firebase", "analytics"},
            resolve_features({"crashlytics"}),
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

    def test_the_accent_must_be_a_six_digit_hex(self):
        for bad in ("blue", "#12345", "#GGGGGG", "2C6BED88"):
            with self.assertRaises(SpecError, msg=bad):
                spec(accent_colour=bad).validated()

    def test_an_accent_is_accepted_with_or_without_the_hash(self):
        for good in ("#2C6BED", "2c6bed"):
            self.assertEqual(good, spec(accent_colour=good).validated().accent_colour)

    def test_no_accent_means_keep_the_template_palette(self):
        self.assertEqual("", spec().validated().accent_colour)


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
        blocks = generated_blocks(spec(feature_modules=("orders", "profile")))

        self.assertEqual(
            ['include(":data:orders")\n', 'include(":data:profile")\n'],
            blocks["data-modules"],
        )
        self.assertEqual(
            ['    implementation(project(":feature:orders"))\n',
             '    implementation(project(":feature:profile"))\n'],
            blocks["app-feature-dependencies"],
        )

    def test_the_reference_feature_supplies_its_own_start_destination(self):
        # The template carries SampleListKey behind an <opt:sample> block, so emitting one here
        # too would produce two.
        blocks = generated_blocks(spec(features=frozenset({"sample"}), feature_modules=("orders",)))

        self.assertEqual([], blocks["start-destination"])

    def test_without_the_reference_feature_the_first_module_becomes_the_start_destination(self):
        blocks = generated_blocks(spec(feature_modules=("orders", "profile")))

        self.assertEqual(["        ?: OrdersListKey\n"], blocks["start-destination"])
        self.assertEqual(
            [
                "import com.acme.field.feature.orders.OrdersListKey\n",
                "import com.acme.field.feature.profile.ProfileListKey\n",
            ],
            blocks["start-destination-import"],
        )

    def test_every_named_module_becomes_a_tab_in_the_order_given(self):
        blocks = generated_blocks(spec(feature_modules=("orders", "order_history")))

        self.assertEqual(2, len(blocks["shell-tabs"]))
        self.assertIn("key = OrdersListKey", blocks["shell-tabs"][0])
        self.assertIn('label = "Order history"', blocks["shell-tabs"][1])

    def test_settings_supplies_the_start_destination_when_nothing_else_does(self):
        blocks = generated_blocks(spec(features=frozenset({"settings"})))

        self.assertEqual(["        ?: SettingsKey\n"], blocks["start-destination"])

    def test_with_neither_the_start_destination_says_what_to_do(self):
        blocks = generated_blocks(spec())

        self.assertIn("error(", blocks["start-destination"][0])


if __name__ == "__main__":
    unittest.main()
