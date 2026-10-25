"""Every template's manifest, and every way a manifest can be wrong."""

from __future__ import annotations

import os
import shutil
import tempfile
import unittest
from pathlib import Path

from genkit.core.copy import SKIP_DIRS, is_under
from genkit.core.manifest import FeatureCopy, LegacyKey, TemplateManifest
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

    def test_a_wrapped_description_reaches_the_catalogue_as_one_line(self):
        for target in REGISTRY.values():
            manifest = target.manifest
            descriptions = [feature.description for feature in manifest.features.values()]
            descriptions += [preset.description for preset in manifest.presets.values()]
            for description in descriptions:
                with self.subTest(target=target.key, description=description[:40]):
                    self.assertEqual(" ".join(description.split()), description)

    def test_a_headline_says_something_the_title_does_not(self):
        for target in REGISTRY.values():
            for feature in target.manifest.features.values():
                with self.subTest(feature=feature.key):
                    self.assertNotEqual(feature.title, feature.headline)
                    self.assertTrue(feature.description)


class OrphanedTestTest(unittest.TestCase):
    """
    A feature that removes a package has to take that package's tests with it. The template
    compiles with every feature on, so a test left behind only fails `./gradlew build` in a
    generated project, which nothing here compiles.
    """

    def test_no_test_outlives_the_main_source_it_tests(self):
        for target in REGISTRY.values():
            manifest, template = target.manifest, target.template_dir
            if not (template / "settings.gradle.kts").is_file():
                # This reads Kotlin source sets. test_target_ios runs the same check on the Swift
                # template's test targets (TemplateTest.test_no_test_outlives_the_source_it_tests).
                continue
            tests = []
            for directory, subdirectories, filenames in os.walk(template):
                subdirectories[:] = [name for name in subdirectories if name not in SKIP_DIRS]
                for filename in filenames:
                    parts = Path(directory, filename).relative_to(template).parts
                    if not filename.endswith(".kt") or "src" not in parts:
                        continue
                    source_set = parts.index("src") + 1
                    if parts[source_set] != "main" and parts[source_set + 1] == "kotlin":
                        main = Path(*parts[:source_set], "main", *parts[source_set + 1 :])
                        tests.append((Path(*parts).as_posix(), main))
            self.assertTrue(tests, f"no test sources found under {template}")

            for key in [*manifest.features, *manifest.options]:
                if key in manifest.options:
                    # An option switched off takes the features it disables with it.
                    disabled = manifest.options[key].disables
                    enabled = frozenset(set(manifest.marker_keys) - {key} - set(disabled))
                else:
                    # Switching one feature off also switches off everything that requires it.
                    enabled = frozenset(other for other in manifest.features if key not in manifest.resolve({other}))
                    enabled |= frozenset(manifest.options)
                owned = manifest.owned_paths(enabled)
                for test, main in tests:
                    if is_under(test, owned):
                        continue
                    tested = (main.parent, main.with_name(main.name.removesuffix("Test.kt") + ".kt"))
                    removed = [path.as_posix() for path in tested if (template / path).exists() and is_under(path.as_posix(), owned)]
                    with self.subTest(target=target.key, feature=key, test=test):
                        self.assertEqual([], removed, f"'{key}' removes what this test compiles against")


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

    def test_a_removed_key_that_is_also_a_plain_legacy_key(self):
        self.write("features/bad.toml", self.feature(legacy='["gone"]'))
        self.write("removed.toml", 'gone = "gone"\n')
        self.assertBroken("both removed and a legacy key without a schema")

    def test_a_removed_key_may_still_map_for_an_older_schema(self):
        self.write("features/bad.toml", self.feature(legacy='[{ key = "gone", schema = 1 }]'))
        self.write("removed.toml", 'gone = "gone"\n')
        manifest = TemplateManifest.load(self.root)
        self.assertEqual(["bad"], manifest.upgrade_keys(["gone"], 1)[0])
        self.assertEqual(([], ["gone"], []), manifest.upgrade_keys(["gone"], 2))

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


class OptionTest(unittest.TestCase):
    """An option: a spec-level switch the marker engine, `owns` and the variants treat as a key."""

    def setUp(self):
        self.manifest = TemplateManifest.load(MINI)

    def test_an_option_loads_with_its_reasons_in_file_order(self):
        option = self.manifest.options["visuals"]
        self.assertTrue(option.default)
        self.assertEqual("bare", option.variant)
        self.assertEqual(("assets",), option.owns)
        self.assertEqual({"extra": "Extra draws, and this project draws nothing."}, option.disables)
        self.assertEqual(["design_style"], list(option.hides))
        self.assertIsNone(option.locked)
        self.assertEqual(
            "Everything but what is drawn: the assets go, and a variant takes the place of the resources.",
            option.description,
        )

    def test_an_option_that_is_off_describes_the_features_and_presets_it_changes(self):
        off = self.manifest.option_values({"visuals": False})
        on = self.manifest.option_values({})
        ui = self.manifest.features["ui"]
        self.assertEqual(
            FeatureCopy("Show something", "Screens' state and their routes, undrawn."), self.manifest.feature_copy(ui, off)
        )
        self.assertEqual(FeatureCopy(ui.headline, ui.description), self.manifest.feature_copy(ui, on))
        net = self.manifest.features["net"]
        self.assertEqual(FeatureCopy(net.headline, net.description), self.manifest.feature_copy(net, off))
        self.assertEqual("Everything that draws nothing.", self.manifest.preset_description(self.manifest.presets["all"], off))
        self.assertEqual("Everything.", self.manifest.preset_description(self.manifest.presets["all"], on))
        self.assertEqual("Networking only.", self.manifest.preset_description(self.manifest.presets["small"], off))

    def test_options_are_marker_keys_beside_the_features(self):
        self.assertEqual(frozenset({"net", "ui", "extra", "visuals"}), self.manifest.marker_keys)

    def test_an_option_that_is_off_gives_up_its_paths_and_its_features(self):
        values = self.manifest.option_values({"visuals": False})
        self.assertEqual({"visuals": False}, values)
        self.assertEqual({"extra": "Extra draws, and this project draws nothing."}, self.manifest.disabled_features(values))
        self.assertIn("assets", self.manifest.owned_paths(frozenset({"net", "ui", "extra"})))
        self.assertNotIn("assets", self.manifest.owned_paths(frozenset({"net", "visuals"})))

    def test_an_option_that_is_off_describes_feature_modules(self):
        off = self.manifest.option_values({"visuals": False})
        self.assertEqual("Each module brings its state and its routes, undrawn.", self.manifest.module_description(off))
        self.assertIsNone(self.manifest.module_description(self.manifest.option_values({})))

    def test_an_option_that_is_off_hides_its_fields(self):
        off = self.manifest.option_values({"visuals": False})
        self.assertEqual(
            {"design_style": "Nothing is drawn, so there is no style to choose."}, self.manifest.hidden_fields(off)
        )

    def test_an_option_left_out_of_a_spec_takes_its_default(self):
        self.assertEqual({"visuals": True}, self.manifest.option_values({}))
        self.assertEqual({}, self.manifest.disabled_features({"visuals": True}))
        self.assertEqual({}, self.manifest.hidden_fields({"visuals": True}))

    def test_the_android_template_declares_ui_components(self):
        target = REGISTRY["android"]
        option = target.manifest.options["ui_components"]
        self.assertTrue(option.default)
        self.assertEqual("No UI components", option.label)
        self.assertEqual(
            ["catalog", "collections", "datadisplay", "formfields", "pickers", "liquidglass"], list(option.disables)
        )
        self.assertEqual(["design_style", "motion_style"], list(option.hides))
        for owned in option.owns:
            with self.subTest(path=owned):
                self.assertTrue((target.template_dir / owned.format(pkg_path=target.manifest.package_path_token)).exists())

    def test_no_branch_but_the_setup_only_release_locks_an_option(self):
        # The lock is the release branch's own commit; merged back, it would lock every deployment.
        for target in REGISTRY.values():
            for option in target.manifest.options.values():
                with self.subTest(target=target.key, option=option.key):
                    self.assertIsNone(option.locked)

    def test_the_other_targets_declare_no_options_yet(self):
        for key, target in REGISTRY.items():
            if key != "android":
                self.assertEqual({}, target.manifest.options, key)


class BrokenOptionTest(unittest.TestCase):
    """Each mistake an option file can make fails when the manifest loads."""

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name) / "template"
        shutil.copytree(MINI, self.root)
        self.path = self.root / "options" / "visuals.toml"
        self.original = self.path.read_text(encoding="utf-8")

    def edit(self, old: str, new: str) -> None:
        self.assertIn(old, self.original)
        self.path.write_text(self.original.replace(old, new), encoding="utf-8")

    def rekey(self, key: str) -> None:
        self.path.unlink()
        (self.root / "options" / f"{key}.toml").write_text(
            self.original.replace('key = "visuals"', f'key = "{key}"'), encoding="utf-8"
        )

    def assertBroken(self, message: str) -> None:
        with self.assertRaises(TemplateError) as caught:
            TemplateManifest.load(self.root)
        self.assertIn(message, str(caught.exception))

    def test_a_key_that_does_not_match_its_file(self):
        self.edit('key = "visuals"', 'key = "visual"')
        self.assertBroken("does not match the file name")

    def test_a_key_no_marker_can_name(self):
        self.rekey("Visuals")
        self.assertBroken("cannot be named in a marker")

    def test_a_key_that_is_also_a_feature(self):
        self.rekey("net")
        self.assertBroken("is also a feature")

    def test_a_key_that_is_already_a_spec_field(self):
        self.rekey("languages")
        self.assertBroken("is already a spec field")

    def test_a_default_that_is_not_a_boolean(self):
        self.edit("default = true", 'default = "yes"')
        self.assertBroken("default must be true or false")

    def test_an_unknown_key(self):
        self.edit("default = true", "default = true\ncolour = 1")
        self.assertBroken("unknown key(s): colour")

    def test_a_missing_label(self):
        self.edit('label = "No visuals"\n', "")
        self.assertBroken("missing label")

    def test_disabling_a_feature_that_does_not_exist(self):
        self.edit("extra = ", "ghost = ")
        self.assertBroken("[disables] names 'ghost', which is not a selectable feature")

    def test_disabling_a_feature_another_feature_requires(self):
        # `ui` requires `net`: with only `net` disabled, `ui` would bring it straight back.
        self.edit("extra = ", "net = ")
        self.assertBroken("'ui' requires net, so it belongs in [disables] too")

    def test_a_reason_that_is_blank(self):
        self.edit('extra = "Extra draws, and this project draws nothing."', 'extra = " "')
        self.assertBroken("[disables] must give each name a one-line reason")

    def test_hiding_a_field_the_spec_does_not_have(self):
        self.edit("design_style = ", "colour_scheme = ")
        self.assertBroken("[hides] names 'colour_scheme', which is not a spec field")

    def test_an_owned_path_that_is_not_there(self):
        self.edit('owns = ["assets"]', 'owns = ["missing"]')
        self.assertBroken("owns 'missing', which is not in the template")

    def test_describing_a_feature_nobody_can_choose(self):
        self.edit("[feature_copy.ui]", "[feature_copy.ghost]")
        self.assertBroken("[feature_copy.ghost] names a feature nobody can choose")

    def test_describing_a_feature_the_option_leaves_out(self):
        self.edit("[feature_copy.ui]", "[feature_copy.extra]")
        self.assertBroken("[feature_copy.extra] describes a feature [disables] leaves out")

    def test_feature_copy_without_a_description(self):
        self.edit('description = """\nScreens\' state and\ntheir routes, undrawn."""', 'headline = "Undrawn"')
        self.assertBroken("[feature_copy.ui] is missing description")

    def test_feature_copy_with_a_key_of_its_own(self):
        self.edit("[feature_copy.ui]\n", '[feature_copy.ui]\ntitle = "Undrawn"\n')
        self.assertBroken("[feature_copy.ui] has unknown key(s): title")

    def test_feature_copy_that_is_blank(self):
        self.edit("[feature_copy.ui]\n", '[feature_copy.ui]\nheadline = " "\n')
        self.assertBroken("[feature_copy.ui] needs words in its headline and description")

    def test_describing_a_preset_that_does_not_exist(self):
        self.edit('all = "Everything that draws nothing."', 'most = "Most of it."')
        self.assertBroken("[preset_copy] names 'most', which is not a preset")

    def test_a_preset_description_that_is_blank(self):
        self.edit('all = "Everything that draws nothing."', 'all = ""')
        self.assertBroken("[preset_copy] must give each preset a description")

    def test_module_copy_that_is_blank(self):
        self.edit('module_copy = """\nEach module brings its state\nand its routes, undrawn."""', 'module_copy = " "')
        self.assertBroken("module_copy needs words")

    def test_a_variant_that_is_not_a_directory_name(self):
        self.edit('variant = "bare"', 'variant = "../bare"')
        self.assertBroken("is not a directory name")

    def test_a_lock_needs_a_boolean_and_a_note(self):
        for lock, message in (
            ('[locked]\nvalue = "false"\nnote = "Soon."\n', "[locked] value must be true or false"),
            ('[locked]\nvalue = false\nnote = ""\n', "[locked] needs a note saying why"),
            ("[locked]\nvalue = false\n", "[locked] is missing note"),
            ('[locked]\nvalue = false\nnote = "Soon."\nuntil = 2\n', "[locked] has unknown key(s): until"),
        ):
            with self.subTest(lock=lock):
                self.path.write_text(self.original + "\n" + lock, encoding="utf-8")
                self.assertBroken(message)

    def test_a_lock_loads(self):
        self.path.write_text(
            self.original + '\n[locked]\nvalue = false\nnote = """\nOnly plain\nprojects for now."""\n', encoding="utf-8"
        )
        lock = TemplateManifest.load(self.root).options["visuals"].locked
        self.assertIsNotNone(lock)
        self.assertFalse(lock.value)
        self.assertEqual("Only plain projects for now.", lock.note)


if __name__ == "__main__":
    unittest.main()
