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


if __name__ == "__main__":
    unittest.main()
