"""The wizard asks again on an answer the spec would refuse, so nobody loses every answer at the end."""

from __future__ import annotations

import io
import unittest
from contextlib import redirect_stdout
from unittest import mock

from genkit import targets
from genkit.core import pipeline, prompts
from genkit.targets.android.target import API_LEVELS, MAX_VERSION_CODE

NEWEST = max(API_LEVELS)


class AskAndroidTest(unittest.TestCase):

    def answer(self, answers: list[str]) -> tuple[dict, list[str], str]:
        """Runs the Android questions on `answers`; returns the settings, each prompt shown, and the output."""
        prompts_shown: list[str] = []
        remaining = iter(answers)

        def fake_input(prompt: str) -> str:
            prompts_shown.append(prompt)
            return next(remaining)

        output = io.StringIO()
        with mock.patch("builtins.input", fake_input), redirect_stdout(output):
            settings = prompts.ask_android(frozenset(), "Wizard Check")
        self.assertIsNone(next(remaining, None), "the wizard asked fewer questions than expected")
        return settings, prompts_shown, output.getvalue()

    def asked(self, prompts_shown: list[str], question: str) -> int:
        return sum(1 for prompt in prompts_shown if prompt.startswith(question))

    def test_an_api_level_or_version_code_past_the_validator_is_asked_again(self):
        past = str(NEWEST + 1)
        settings, shown, output = self.answer(["", past, "", past, "", "", str(MAX_VERSION_CODE + 1), "", "n"])
        self.assertEqual(2, self.asked(shown, "targetSdk"))
        self.assertEqual(2, self.asked(shown, "compileSdk"))
        self.assertEqual(2, self.asked(shown, "versionCode"))
        self.assertIn(f"Must be {NEWEST} or less.", output)
        self.assertIn(f"Must be {MAX_VERSION_CODE} or less.", output)
        self.assertEqual(1, settings["version_code"])

    def test_the_highest_answers_the_wizard_takes_pass_the_spec(self):
        settings, _, _ = self.answer(["", str(NEWEST), str(NEWEST), "", str(MAX_VERSION_CODE), "n"])
        raw = {"schema": 2, "platform": "android", "app_name": "Wizard Check", "package_name": "com.example.wizard"}
        parsed, _ = pipeline.read_spec({**raw, "android": settings}, targets.get("android"))
        self.assertEqual((NEWEST, NEWEST, MAX_VERSION_CODE), (
            parsed.spec.android.target_sdk, parsed.spec.android.compile_sdk, parsed.spec.android.version_code
        ))

    def test_the_lowest_answers_the_wizard_takes_pass_the_spec(self):
        lowest = str(min(API_LEVELS))
        settings, _, _ = self.answer([lowest, lowest, "", "", "1", "n"])
        raw = {"schema": 2, "platform": "android", "app_name": "Wizard Check", "package_name": "com.example.wizard"}
        parsed, _ = pipeline.read_spec({**raw, "android": settings}, targets.get("android"))
        self.assertEqual(min(API_LEVELS), parsed.spec.android.min_sdk)


class AskFeaturesTest(unittest.TestCase):

    def choose_individually(self, answers: dict[str, str], no_ui: str = "") -> set[str]:
        """
        Answers the one-by-one feature questions: `answers` by feature key, Enter for the rest, and
        `no_ui` to the "No UI components?" question asked before them.
        """
        target = targets.get("android")
        manifest = target.manifest
        menu = str(len(manifest.presets) + 1)
        left_out = set(manifest.options["ui_components"].disables) if no_ui == "y" else set()
        asked = [feature.key for feature in manifest.selectable if feature.key not in left_out]
        replies = iter([no_ui, menu] + [answers.get(key, "") for key in asked])
        with mock.patch("builtins.input", lambda _prompt: next(replies)), redirect_stdout(io.StringIO()):
            selected, values = prompts.ask_features(target, select_all=False, preset=None, options={})
        self.assertIsNone(next(replies, None), "the wizard asked fewer questions than expected")
        self.assertEqual({"ui_components": no_ui != "y"}, values)
        return selected

    def test_a_feature_on_by_default_does_not_bring_back_a_requirement_left_out(self):
        selected = self.choose_individually({"devtools": "n"})
        self.assertNotIn("devtools", selected)
        self.assertNotIn("perfhud", selected)

    def test_a_yes_still_includes_a_feature_whose_requirement_was_left_out(self):
        selected = self.choose_individually({"devtools": "n", "perfhud": "y"})
        self.assertIn("perfhud", selected)

    def test_without_ui_components_the_features_that_need_them_are_not_offered(self):
        selected = self.choose_individually({"devtools": "y"}, no_ui="y")
        self.assertIn("devtools", selected)
        self.assertFalse(selected & set(targets.get("android").manifest.options["ui_components"].disables))

    def test_without_ui_components_presets_and_features_read_as_such_a_project_has_them(self):
        target = targets.get("android")
        manifest = target.manifest
        option = manifest.options["ui_components"]
        asked = [feature for feature in manifest.selectable if feature.key not in option.disables]
        replies = iter(["y", str(len(manifest.presets) + 1)] + [""] * len(asked))
        with mock.patch("builtins.input", lambda _prompt: next(replies)), redirect_stdout(io.StringIO()) as out:
            prompts.ask_features(target, select_all=False, preset=None, options={})
        shown = " ".join(out.getvalue().split())
        self.assertIn(option.preset_copy["lean"], shown)
        self.assertNotIn(manifest.presets["lean"].description, shown)
        self.assertIn(option.feature_copy["performance"].description, shown)
        self.assertNotIn(manifest.features["performance"].description, shown)
        self.assertIn(option.preset_copy["standard"], shown)
        self.assertNotIn(manifest.presets["standard"].description, shown)
        # A feature the option does not describe reads as it always has.
        self.assertIn(manifest.features["network"].description, shown)

    def test_a_preset_without_ui_components_loses_only_what_they_disable(self):
        target = targets.get("android")
        replies = iter(["y"])
        with mock.patch("builtins.input", lambda _prompt: next(replies)), redirect_stdout(io.StringIO()):
            selected, values = prompts.ask_features(target, select_all=False, preset="standard", options={})
        self.assertEqual(set(target.manifest.preset_features("standard")) - {"catalog"}, selected)
        self.assertEqual({"ui_components": False}, values)

    def test_the_command_line_answers_the_option_and_it_is_not_asked(self):
        target = targets.get("android")
        with mock.patch("builtins.input", side_effect=AssertionError("asked")), redirect_stdout(io.StringIO()):
            selected, values = prompts.ask_features(target, select_all=True, preset=None, options={"ui_components": False})
        self.assertEqual({"ui_components": False}, values)
        self.assertNotIn("catalog", selected)


class AskFeatureModulesTest(unittest.TestCase):

    def shown(self, values: dict[str, bool]) -> str:
        replies = iter(["orders"])
        with mock.patch("builtins.input", lambda _prompt: next(replies)), redirect_stdout(io.StringIO()) as out:
            self.assertEqual(["orders"], prompts.ask_feature_modules(targets.get("android"), values))
        return " ".join(out.getvalue().split())

    def test_without_ui_components_a_module_reads_as_such_a_project_gets_it(self):
        shown = self.shown({"ui_components": False})
        self.assertIn(targets.get("android").manifest.options["ui_components"].module_copy, shown)
        self.assertNotIn("working screen", shown)

    def test_with_ui_components_a_module_still_brings_a_working_screen(self):
        shown = self.shown({"ui_components": True})
        self.assertIn("Each produces a working screen, its navigation wiring and a test.", shown)
        self.assertNotIn(targets.get("android").manifest.options["ui_components"].module_copy, shown)


class AskLookTest(unittest.TestCase):

    def test_a_hidden_field_is_not_asked_and_keeps_its_default(self):
        # Primary, secondary and tertiary colours, then haptics: no motion or design style question.
        replies = iter(["", "", "", ""])
        with mock.patch("builtins.input", lambda _prompt: next(replies)), redirect_stdout(io.StringIO()):
            look = prompts.ask_look(frozenset(), frozenset({"design_style", "motion_style"}))
        self.assertIsNone(next(replies, None))
        self.assertEqual(("Utility", "Standard"), (look["design_style"], look["motion_style"]))


if __name__ == "__main__":
    unittest.main()
