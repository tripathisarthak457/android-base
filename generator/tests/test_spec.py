"""Spec v2: the upgrade from schema 1, types, limits, names, and the cases the website checks too."""

from __future__ import annotations

import json
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.core.spec import (
    KOTLIN_KEYWORDS,
    MAX_FEATURE_MODULES,
    SpecError,
    Vocabulary,
    apply_overrides,
    parse,
    to_json,
    upgrade,
)
from genkit.targets import get

FIXTURES = Path(__file__).resolve().parent / "fixtures"
ANDROID = get("android")


def read(raw, overrides=None):
    return pipeline.read_spec(raw, ANDROID, overrides)[0]


def minimal(**fields):
    return {"schema": 2, "platform": "android", "app_name": "Acme Field", "package_name": "com.acme.field", **fields}


class SchemaOneTest(unittest.TestCase):
    """Every spec an earlier release wrote still generates."""

    def test_the_real_apps_and_the_old_test_specs_upgrade(self):
        for name in ("periflow", "vitfleet", "spec_full", "spec_minimal", "spec_keys"):
            with self.subTest(fixture=name):
                raw = json.loads((FIXTURES / "v1" / f"{name}.json").read_text(encoding="utf-8"))
                parsed = read(raw)
                self.assertEqual("android", parsed.spec.platform)
                self.assertEqual("compact", parsed.spec.architecture)
                self.assertIn("Upgraded a schema-1 spec", parsed.notes[0])
                self.assertEqual(raw.get("min_sdk", 26), parsed.spec.android.min_sdk)
                self.assertEqual(raw.get("api_base_urls", {}), parsed.spec.android.api_base_urls)

    def test_flat_android_fields_move_under_android(self):
        data, notes, ignored, schema = upgrade({"app_name": "A", "package_name": "a.b", "min_sdk": 28}, "android")
        self.assertEqual(1, schema)
        self.assertEqual({"min_sdk": 28}, data["android"])
        self.assertEqual([], ignored)

    def test_unknown_schema_one_fields_are_ignored_and_reported(self):
        parsed = read({"app_name": "Acme", "package_name": "com.acme.x", "existing_path": "/etc"})
        self.assertEqual(("existing_path",), parsed.ignored_fields)

    def test_legacy_feature_keys_map_across_with_a_note(self):
        parsed = read({"app_name": "Acme", "package_name": "com.acme.x", "features": ["crashlytics", "ci", "network"]})
        self.assertIn("firebase", parsed.spec.features)
        self.assertNotIn("ci", parsed.spec.features)
        self.assertIn("'crashlytics' is now part of 'firebase'.", parsed.notes)
        self.assertIn("'ci' is no longer a feature and was left out.", parsed.notes)

    def test_the_record_is_schema_two_and_reads_back_as_the_same_spec(self):
        raw = json.loads((FIXTURES / "v1" / "periflow.json").read_text(encoding="utf-8"))
        first = read(raw).spec
        again = read(to_json(first, keystores=False)).spec
        self.assertEqual(first, again)


class SchemaTwoTest(unittest.TestCase):

    def test_flat_android_fields_are_accepted_as_shorthand(self):
        parsed = read(minimal(min_sdk=29))
        self.assertEqual(29, parsed.spec.android.min_sdk)
        self.assertEqual(("Moved 'min_sdk' under \"android\".",), parsed.notes)

    def test_a_field_given_twice_is_refused(self):
        with self.assertRaises(SpecError) as caught:
            read(minimal(min_sdk=29, android={"min_sdk": 30}))
        self.assertEqual("android.min_sdk", caught.exception.field)

    def test_an_unknown_field_is_an_error_not_a_silent_drop(self):
        with self.assertRaises(SpecError) as caught:
            read(minimal(glass_style="frosted"))
        self.assertEqual("glass_style", caught.exception.field)

    def test_a_spec_for_another_platform_is_refused(self):
        with self.assertRaises(SpecError) as caught:
            read(minimal(platform="ios"))
        self.assertEqual("platform", caught.exception.field)

    def test_null_means_absent(self):
        self.assertEqual("", read(minimal(display_name=None)).spec.display_name)

    def test_the_resolved_set_closes_over_requires(self):
        parsed = read(minimal(features=["googlesignin"]))
        self.assertEqual({"googlesignin", "auth", "network", "forms"}, set(parsed.spec.features))
        self.assertEqual({"googlesignin"}, set(parsed.requested))


class TypeTest(unittest.TestCase):
    """The shapes a hand-written or API spec can get wrong (audit generator-python 4.8)."""

    def test_wrong_shapes_name_their_field(self):
        cases = {
            "feature_modules": ("home", "feature_modules"),
            "features": ("network", "features"),
            "haptics_enabled": ("false", "haptics_enabled"),
            "app_name": (5, "app_name"),
            "android": ({"version_code": 1.5}, "android.version_code"),
        }
        for key, (value, field) in cases.items():
            with self.subTest(key=key), self.assertRaises(SpecError) as caught:
                read(minimal(**{key: value}))
            self.assertEqual(field, caught.exception.field)

    def test_not_an_object(self):
        for raw in ([], "x", None, 5):
            with self.subTest(raw=raw), self.assertRaises(SpecError):
                read(raw)


class NameTest(unittest.TestCase):

    def test_kotlin_hard_keywords_and_java_literals_are_refused_as_segments(self):
        for segment in ("null", "true", "false", "typealias", "typeof", "fun", "object", "when", "class", "goto"):
            with self.subTest(segment=segment), self.assertRaises(SpecError):
                read(minimal(package_name=f"com.example.{segment}"))

    def test_python_keywords_and_kotlin_soft_keywords_are_ordinary_segments(self):
        # Python's keyword list once refused com.global.bank (audit 4.1). Soft keywords such as
        # `value` and `data` are identifiers wherever a package segment can appear.
        for segment in ("global", "pass", "async", "lambda", "value", "data", "get", "field"):
            with self.subTest(segment=segment):
                read(minimal(package_name=f"com.example.{segment}"))

    def test_kotlin_keywords_are_refused_as_module_names(self):
        for name in sorted(KOTLIN_KEYWORDS):
            with self.subTest(name=name), self.assertRaises(SpecError):
                read(minimal(feature_modules=[name]))

    def test_display_name_takes_any_printable_characters(self):
        for name in ("Café Olé", "Mom's Kitchen", "Tom & Jerry", "Rocket 🚀", 'Say "Hi"', "2048 Game"):
            with self.subTest(name=name):
                self.assertEqual(name, read(minimal(display_name=name)).spec.label)

    def test_every_pattern_must_match_the_whole_value(self):
        # `$` matches before a trailing newline; a newline in a Kotlin string literal fails the
        # build (audit web-api M2).
        for raw in (
            minimal(package_name="com.acme.field\n"),
            minimal(font_name="DM Sans\n"),
            minimal(feature_modules=["orders\n"]),
            minimal(android={"version_name": "1.0.0\n"}),
            minimal(android={"api_base_urls": {"dev": "https://a.com/\n"}}),
            minimal(android={"deeplink_host": "a.com\n"}),
        ):
            with self.subTest(raw=raw), self.assertRaises(SpecError):
                read(raw)

    def test_limits(self):
        cases = (
            minimal(feature_modules=[f"m{i}" for i in range(MAX_FEATURE_MODULES + 1)]),
            minimal(app_name="A" * 51),
            minimal(display_name="D" * 31),
            minimal(package_name=".".join(["seg"] * 9)),
            minimal(features=["network"] * 65),
        )
        for raw in cases:
            with self.subTest(raw=str(raw)[:80]), self.assertRaises(SpecError):
                read(raw)


class DerivedNameTest(unittest.TestCase):

    def test_spaces_and_punctuation_become_a_pascal_name(self):
        self.assertEqual("MyGreatApp", read(minimal(app_name="My Great App")).spec.pascal_name)
        self.assertEqual("MyGreatApp", read(minimal(app_name="my-great_app")).spec.pascal_name)

    def test_the_snake_form_splits_on_case_boundaries(self):
        # The Keystore alias is the snake form, so these are pinned exactly, capitals included: an
        # app regenerated under a different alias can no longer decrypt what it stored.
        for app_name, snake in (
            ("My Great App", "my_great_app"),
            ("NASA Tracker", "n_a_s_a_tracker"),
            ("iPhone Notes", "i_phone_notes"),
            ("Acme 2 Go", "acme2_go"),
        ):
            with self.subTest(app_name=app_name):
                self.assertEqual(snake, read(minimal(app_name=app_name)).spec.snake_name)

    def test_the_lower_form_has_no_separators(self):
        self.assertEqual("mygreatapp", read(minimal(app_name="My Great App")).spec.lower_name)

    def test_the_package_path_uses_directory_separators(self):
        self.assertEqual("com/acme/field", read(minimal()).spec.package_path)


class OverrideTest(unittest.TestCase):

    def test_dotted_and_flat_keys(self):
        data = {"android": {"min_sdk": 26}}
        apply_overrides(data, [("android.target_sdk", 36), ("min_sdk", 28), ("package_name", "com.example.x")])
        self.assertEqual({"android": {"min_sdk": 28, "target_sdk": 36}, "package_name": "com.example.x"}, data)

    def test_overrides_apply_before_validation(self):
        parsed = read(minimal(package_name="com.example.null"), [("package_name", "com.example.wp01.std")])
        self.assertEqual("com.example.wp01.std", parsed.spec.package_name)

    def test_an_override_into_a_value_that_is_not_an_object(self):
        with self.assertRaises(SpecError):
            apply_overrides({"app_name": "x"}, [("app_name.first", 1)])


class DesignStyleRequirementTest(unittest.TestCase):

    def test_a_style_that_needs_a_feature_is_refused_without_it(self):
        vocabulary = Vocabulary(
            design_styles={"Utility": (), "Glass": ("network",)},
            motion_styles=("Standard",),
            reserved_module_names=frozenset(),
        )
        with self.assertRaises(SpecError) as caught:
            parse(minimal(design_style="Glass"), ANDROID.manifest, vocabulary)
        self.assertEqual("design_style", caught.exception.field)
        parse(minimal(design_style="Glass", features=["network"]), ANDROID.manifest, vocabulary)


class ValidationCasesTest(unittest.TestCase):
    """`fixtures/validation_cases.json`, which the website's validators run too."""

    def test_every_case(self):
        data = json.loads((FIXTURES / "validation_cases.json").read_text(encoding="utf-8"))
        for case in data["cases"]:
            with self.subTest(case=case["name"]):
                if case["valid"]:
                    read(case["spec"])
                else:
                    with self.assertRaises(SpecError) as caught:
                        read(case["spec"])
                    self.assertEqual(case["field"], caught.exception.field)


class FixtureTest(unittest.TestCase):

    def test_the_v2_fixtures_read_and_modular_is_refused_for_now(self):
        for path in sorted((FIXTURES / "v2").glob("*.json")):
            raw = json.loads(path.read_text(encoding="utf-8"))
            with self.subTest(fixture=path.name):
                if raw.get("architecture") == "modular":
                    with self.assertRaises(SpecError) as caught:
                        read(raw)
                    self.assertEqual("modular is not available yet; generate compact for now.", str(caught.exception))
                else:
                    self.assertEqual(f"com.example.{path.stem.replace('-', '')}", read(raw).spec.package_name)


if __name__ == "__main__":
    unittest.main()
