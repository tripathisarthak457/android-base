"""Every template's manifest, and every way a manifest can be wrong."""

from __future__ import annotations

import shutil
import tempfile
import unittest
from pathlib import Path

from genkit.core.manifest import LegacyKey, TemplateManifest
from genkit.core.markers import TemplateError
from genkit.targets import REGISTRY

MINI = Path(__file__).resolve().parent / "fixtures" / "mini-template"


class RealManifestTest(unittest.TestCase):

    def test_every_target_loads(self):
        self.assertIn("android", REGISTRY)
        for target in REGISTRY.values():
            with self.subTest(target=target.key):
                manifest = TemplateManifest.load(target.template_dir)
                self.assertEqual(target.key, manifest.key)

    def test_every_owned_path_exists(self):
        for target in REGISTRY.values():
            manifest = target.manifest
            for feature in manifest.features.values():
                for owned in feature.owns:
                    with self.subTest(feature=feature.key, path=owned):
                        self.assertTrue((target.template_dir / owned.format(pkg_path=manifest.package_path_token)).exists())

    def test_every_preset_resolves_and_everything_is_every_selectable_feature(self):
        for target in REGISTRY.values():
            manifest = target.manifest
            for key in manifest.presets:
                with self.subTest(target=target.key, preset=key):
                    resolved = manifest.preset_features(key)
                    self.assertEqual(resolved, manifest.resolve(resolved))
            if "everything" in manifest.presets:
                selectable = {feature.key for feature in manifest.selectable}
                self.assertTrue(selectable <= manifest.preset_features("everything"))

    def test_android_hides_only_the_firebase_base(self):
        manifest = REGISTRY["android"].manifest
        self.assertEqual(["firebase"], [key for key, feature in manifest.features.items() if feature.implied_only])

    def test_an_implied_feature_is_reachable_through_requires(self):
        for target in REGISTRY.values():
            manifest = target.manifest
            required = {required for feature in manifest.features.values() for required in feature.requires}
            for feature in manifest.features.values():
                if feature.implied_only:
                    self.assertIn(feature.key, required, feature.key)

    def test_removed_keys_are_not_current(self):
        for target in REGISTRY.values():
            self.assertFalse(set(target.manifest.removed) & set(target.manifest.features))

    def test_a_headline_says_something_the_title_does_not(self):
        for target in REGISTRY.values():
            for feature in target.manifest.features.values():
                with self.subTest(feature=feature.key):
                    self.assertNotEqual(feature.title, feature.headline)
                    self.assertTrue(feature.description)


class UpgradeKeysTest(unittest.TestCase):

    def setUp(self):
        self.manifest = TemplateManifest.load(MINI)

    def test_plain_legacy_keys_apply_to_any_schema(self):
        self.assertEqual((["net"], ["'network-v0' is now 'net'."], []), self.manifest.upgrade_keys(("network-v0",), 2))

    def test_versioned_legacy_keys_apply_only_to_their_schema(self):
        self.assertEqual(["ui"], self.manifest.upgrade_keys(("screens",), 1)[0])
        self.assertEqual(["screens"], self.manifest.upgrade_keys(("screens",), 2)[2])

    def test_removed_and_unknown(self):
        keys, notes, unknown = self.manifest.upgrade_keys(("old", "nope", "net"), 1)
        self.assertEqual(["net"], keys)
        self.assertEqual(["'old' is no longer a feature and was left out."], notes)
        self.assertEqual(["nope"], unknown)

    def test_requires_close_transitively(self):
        self.assertEqual(frozenset({"ui", "net"}), self.manifest.resolve({"ui"}))


class BrokenManifestTest(unittest.TestCase):
    """Each mistake a feature file can make fails when the manifest loads."""

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name) / "template"
        shutil.copytree(MINI, self.root)

    def write(self, relative: str, text: str) -> None:
        (self.root / relative).write_text(text, encoding="utf-8")

    def assertBroken(self, message: str) -> None:
        with self.assertRaises(TemplateError) as caught:
            TemplateManifest.load(self.root)
        self.assertIn(message, str(caught.exception))

    def feature(self, **overrides) -> str:
        fields = {
            "key": '"bad"', "title": '"Bad"', "headline": '"Breaks"', "description": '"x"',
            "group": '"Core"', "default": "false",
        }
        fields.update(overrides)
        return "".join(f"{key} = {value}\n" for key, value in fields.items() if value is not None)

    def test_a_key_that_does_not_match_its_file(self):
        self.write("features/bad.toml", self.feature(key='"other"'))
        self.assertBroken("does not match the file name")

    def test_an_unknown_group(self):
        self.write("features/bad.toml", self.feature(group='"Nowhere"'))
        self.assertBroken("is not in template.toml [groups]")

    def test_an_owned_path_that_is_not_there(self):
        self.write("features/bad.toml", self.feature(owns='["missing/dir"]'))
        self.assertBroken("which is not in the template")

    def test_an_unknown_requirement(self):
        self.write("features/bad.toml", self.feature(requires='["ghost"]'))
        self.assertBroken("requires unknown feature 'ghost'")

    def test_a_cycle(self):
        self.write("features/bad.toml", self.feature(requires='["worse"]'))
        self.write("features/worse.toml", self.feature(key='"worse"', requires='["bad"]'))
        self.assertBroken("cycle")

    def test_an_unknown_key(self):
        self.write("features/bad.toml", self.feature(colour='"blue"'))
        self.assertBroken("unknown key(s): colour")

    def test_a_missing_key(self):
        self.write("features/bad.toml", self.feature(headline=None))
        self.assertBroken("missing headline")

    def test_a_removed_key_that_is_current(self):
        self.write("removed.toml", 'net = "gone"\n')
        self.assertBroken("both removed and a current feature")

    def test_a_plain_legacy_key_that_is_current(self):
        self.write("features/bad.toml", self.feature(legacy='["net"]'))
        self.assertBroken("is a current feature")

    def test_a_beta_target_without_a_note(self):
        text = (self.root / "template.toml").read_text(encoding="utf-8").replace('status_note = "A test fixture."\n', "")
        self.write("template.toml", text)
        self.assertBroken("status_note")

    def test_an_unknown_preset_default(self):
        self.write("presets.toml", 'default = "huge"\n\n[all]\ntitle = "All"\ndescription = "x"\nall = true\n')
        self.assertBroken("is not a preset")

    def test_legacy_entries_parse_both_forms(self):
        manifest = TemplateManifest.load(self.root)
        self.assertEqual((LegacyKey("network-v0"),), manifest.features["net"].legacy)
        self.assertEqual((LegacyKey("screens", 1),), manifest.features["ui"].legacy)


if __name__ == "__main__":
    unittest.main()
