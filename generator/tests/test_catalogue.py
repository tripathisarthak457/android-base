"""Catalogue v2, which the website and the API read, and the website's checked-in copy of it."""

from __future__ import annotations

import io
import json
import re
import unittest
from contextlib import redirect_stdout

from genkit.core import catalogue
from genkit.core.spec import MAX_FEATURE_MODULES, MAX_SPEC_BYTES
from genkit.targets import REGISTRY


class ShapeTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.data = catalogue.catalogue()

    def test_top_level(self):
        self.assertEqual({"version", "generator_commit", "targets", "rules", "tokens"}, set(self.data))
        self.assertEqual(2, self.data["version"])

    def test_every_registered_target_is_described(self):
        self.assertEqual(list(REGISTRY), [target["key"] for target in self.data["targets"]])
        for target in self.data["targets"]:
            with self.subTest(target=target["key"]):
                for key in (
                    "title", "status", "status_note", "summary", "groups", "features", "presets", "defaults",
                    "fields", "architectures", "design_styles", "motion_styles", "languages",
                ):
                    self.assertIn(key, target)
                self.assertIn(target["status"], ("available", "beta", "coming_soon", "hidden"))
                if target["status"] == "beta":
                    self.assertTrue(target["status_note"])

    def test_features_name_groups_and_features_that_exist(self):
        for target in self.data["targets"]:
            groups = {group["key"] for group in target["groups"]}
            keys = {feature["key"] for feature in target["features"]}
            for feature in target["features"]:
                with self.subTest(feature=feature["key"]):
                    self.assertIn(feature["group"], groups)
                    self.assertTrue(set(feature["requires"]) <= keys)
                    self.assertTrue(set(feature["implies"]) <= keys)

    def test_presets_are_resolved_and_one_is_the_default(self):
        for target in self.data["targets"]:
            features = {feature["key"]: feature for feature in target["features"]}
            self.assertEqual(1, sum(preset["default"] for preset in target["presets"]))
            for preset in target["presets"]:
                for key in preset["features"]:
                    self.assertTrue(set(features[key]["requires"]) <= set(preset["features"]), (preset["key"], key))

    def test_rules_carry_the_limits_the_generator_enforces(self):
        rules = self.data["rules"]
        self.assertEqual(MAX_FEATURE_MODULES, rules["feature_modules_max"])
        self.assertEqual(MAX_SPEC_BYTES, rules["spec_bytes_max"])
        self.assertIn("null", rules["reserved_words"]["java"])
        self.assertIn("fun", rules["reserved_words"]["kotlin"])
        self.assertIn("settings", rules["template_module_names"]["android"])
        for key in ("app_name", "segment", "module_name", "hex", "version_name", "http_url", "socket_url", "font", "host"):
            with self.subTest(rule=key):
                self.assertTrue(rules[key].startswith("^") and rules[key].endswith("$"))
                re.compile(rules[key])

    def test_two_targets_cannot_give_one_rule_different_values(self):
        own = {"reserved_module_names": [], "rules": {"min_compile_sdk": 37}}
        self.assertEqual(37, catalogue._rules({"a": own, "b": own})["min_compile_sdk"])
        with self.assertRaises(ValueError):
            catalogue._rules({"a": own, "b": {"reserved_module_names": [], "rules": {"min_compile_sdk": 36}}})
        with self.assertRaises(ValueError):
            catalogue._rules({"a": {"reserved_module_names": [], "rules": {"hex": "^x$"}}})

    def test_styles_come_from_the_tokens(self):
        for target in self.data["targets"]:
            styles = [style["key"] for style in target["design_styles"]]
            self.assertEqual(sorted(styles), sorted(self.data["tokens"]["palette"]))

    def test_the_environment_is_only_in_the_live_catalogue(self):
        self.assertNotIn("environment", self.data)
        live = catalogue.catalogue(environment=True)
        self.assertIsInstance(live["environment"]["android"]["keytool"], bool)


class WebsiteCopyTest(unittest.TestCase):

    def test_the_checked_in_copy_is_current(self):
        output = io.StringIO()
        with redirect_stdout(output):
            code = catalogue.main(["--check"])
        self.assertEqual(0, code, output.getvalue()[-2000:])

    def test_the_commit_is_ignored_when_comparing(self):
        copy = json.loads(catalogue.WEBSITE_COPY.read_text(encoding="utf-8"))
        copy["generator_commit"] = "something-else"
        self.assertEqual(
            catalogue._comparable(json.dumps(copy)),
            catalogue._comparable(catalogue.WEBSITE_COPY.read_text(encoding="utf-8")),
        )


if __name__ == "__main__":
    unittest.main()
