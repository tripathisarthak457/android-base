"""Catalogue v2, which the website and the API read, and the website's checked-in copy of it."""

from __future__ import annotations

import io
import json
import re
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

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

    def test_options_follow_the_contract_the_website_and_the_api_read(self):
        by_target = {target["key"]: target["options"] for target in self.data["targets"]}
        self.assertEqual([], by_target.get("multiplatform", []))
        self.assertEqual([], by_target.get("ios", []))
        [option] = by_target["android"]
        self.assertEqual(
            ["key", "title", "checkbox", "headline", "description", "default", "disables", "hides", "locked"],
            list(option),
        )
        self.assertEqual("ui_components", option["key"])
        self.assertEqual({"label": "No UI components", "ticked_value": False}, option["checkbox"])
        self.assertIs(True, option["default"])
        self.assertIsNone(option["locked"])
        self.assertEqual(
            ["catalog", "collections", "datadisplay", "formfields", "pickers", "liquidglass"],
            [entry["feature"] for entry in option["disables"]],
        )
        self.assertEqual(["design_style", "motion_style"], [entry["field"] for entry in option["hides"]])
        for entry in option["disables"] + option["hides"]:
            self.assertEqual(" ".join(entry["reason"].split()), entry["reason"])
            self.assertTrue(entry["reason"].endswith("."), entry)

    def test_an_option_disables_only_features_the_target_offers(self):
        for target in self.data["targets"]:
            selectable = {feature["key"] for feature in target["features"] if not feature["implied_only"]}
            fields = {field["key"] for field in target["fields"]}
            for option in target["options"]:
                with self.subTest(target=target["key"], option=option["key"]):
                    self.assertTrue({entry["feature"] for entry in option["disables"]} <= selectable)
                    self.assertTrue({entry["field"] for entry in option["hides"]} <= fields)

    def test_a_lock_reaches_the_catalogue_with_its_note(self):
        option = mock.Mock(
            key="ui_components", title="UI components", label="No UI components", headline="h", description="d",
            default=True, disables={}, hides={}, locked=mock.Mock(value=False, note="Coming soon."),
        )
        self.assertEqual({"value": False, "note": "Coming soon."}, catalogue._option(option)["locked"])

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

    def test_a_copy_with_crlf_line_endings_is_stale_until_written_again(self):
        with tempfile.TemporaryDirectory() as temp:
            repository = Path(temp)
            copy = repository / catalogue.WEBSITE_COPY.relative_to(catalogue.record.REPOSITORY)
            copy.parent.mkdir(parents=True)
            copy.write_bytes(catalogue.WEBSITE_COPY.read_bytes().replace(b"\n", b"\r\n"))
            with mock.patch.object(catalogue, "WEBSITE_COPY", copy), \
                    mock.patch.object(catalogue.record, "REPOSITORY", repository), \
                    redirect_stdout(io.StringIO()) as output:
                self.assertEqual(1, catalogue.main(["--check"]))
                self.assertIn("not laid out as --write writes it", output.getvalue())
                self.assertEqual(0, catalogue.main(["--write"]))
                self.assertEqual(0, catalogue.main(["--check"]))
            self.assertNotIn(b"\r", copy.read_bytes())

    def test_the_commit_is_ignored_when_comparing(self):
        copy = json.loads(catalogue.WEBSITE_COPY.read_text(encoding="utf-8"))
        copy["generator_commit"] = "something-else"
        self.assertEqual(
            catalogue._comparable(json.dumps(copy)),
            catalogue._comparable(catalogue.WEBSITE_COPY.read_text(encoding="utf-8")),
        )


if __name__ == "__main__":
    unittest.main()
