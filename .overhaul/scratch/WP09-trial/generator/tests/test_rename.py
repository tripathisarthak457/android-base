"""The single-pass identity rename, and hostile names through the whole pipeline."""

from __future__ import annotations

import re
import tempfile
import unittest
from pathlib import Path

from genkit.core import pipeline
from genkit.core.rename import IdentityRenamer
from genkit.core.spec import SpecError
from genkit.targets import get

ANDROID = get("android")


def renamer(package: str = "com.acme.field", app: str = "Acme Field") -> IdentityRenamer:
    parsed, _ = pipeline.read_spec({"app_name": app, "package_name": package}, ANDROID)
    return IdentityRenamer(ANDROID.manifest.identity, ANDROID.identity(parsed.spec))


class BoundaryTest(unittest.TestCase):

    def test_package_import_and_path_forms(self):
        result = renamer().rename("package com.base.app.core.ui\nimport com.base.app.R\n// com/base/app/core\n")
        self.assertEqual("package com.acme.field.core.ui\nimport com.acme.field.R\n// com/acme/field/core\n", result)

    def test_class_resource_and_scheme_forms(self):
        result = renamer().rename('BaseAppApplication "base_app_default" baseapp.example.com Theme.BaseApp.Starting')
        self.assertEqual('AcmeFieldApplication "acme_field_default" acmefield.example.com Theme.AcmeField.Starting', result)

    def test_snake_literals_with_underscores_after_them_rename(self):
        for literal in ("base_app_default", "base_app_urgent", "base_app_silent", "base_app_promotions", "base_app_datastore_key"):
            with self.subTest(literal=literal):
                self.assertEqual(literal.replace("base_app", "acme_field"), renamer().rename(literal))

    def test_a_token_inside_a_longer_word_is_left_alone(self):
        for text in ("database_app", "com.base.apple", "mycom.base.app", "xbaseapp", "MyBaseApp", "com/base/apples"):
            with self.subTest(text=text):
                self.assertEqual(text, renamer().rename(text))

    def test_baseline_profile_descriptors_rename(self):
        rules = (
            "Lcom/base/app/core/navigation/X;\n"
            "HSPLcom/base/app/BaseAppApplication;->onCreate()V\n"
            "SPLcom/base/app/X;\n"
            "PLandroidx/compose/Foo;\n"
        )
        self.assertEqual(
            "Lcom/acme/field/core/navigation/X;\n"
            "HSPLcom/acme/field/AcmeFieldApplication;->onCreate()V\n"
            "SPLcom/acme/field/X;\n"
            "PLandroidx/compose/Foo;\n",
            renamer().rename(rules),
        )

    def test_a_word_ending_in_l_is_not_a_descriptor(self):
        self.assertEqual("xLcom/base/app/X", renamer().rename("xLcom/base/app/X"))

    def test_the_replacement_is_never_scanned_again(self):
        # The old chain of str.replace calls turned com.base.apple into com.base.applele.
        result = renamer(package="com.base.apple").rename("package com.base.app.feature")
        self.assertEqual("package com.base.apple.feature", result)
        self.assertEqual("com/example/databaseapp/x", renamer(package="com.example.databaseapp").rename("com/base/app/x"))


class HostileNamesTest(unittest.TestCase):
    """Names that contain a template token, through the whole pipeline (audit generator-python 3.1)."""

    CASES = (
        ("Trail Notes", "com.example.databaseapp", ["orders"]),
        ("Trail Notes", "com.firebaseapp.notes", ["orders"]),
        ("Trail Notes", "com.base.apple", ["orders"]),
        ("Trail Notes", "com.base.app.demo", ["orders"]),
        ("Store", "com.example.store", ["database_app"]),
        ("Xbaseapp", "com.example.x", ["baseapp"]),
    )

    def test_every_package_line_matches_its_directory(self):
        for app, package, modules in self.CASES:
            with self.subTest(package=package, modules=modules), tempfile.TemporaryDirectory() as temp:
                parsed, _ = pipeline.read_spec(
                    {"app_name": app, "package_name": package, "features": ["network", "push", "settings"],
                     "feature_modules": modules, "languages": ["de"]},
                    ANDROID,
                )
                project = Path(temp) / "p"
                pipeline.build(parsed.spec, ANDROID, project, zip_output=False, commit="test")

                for source in project.rglob("src/*/kotlin/**/*.kt"):
                    declared = re.search(r"^package ([\w.]+)", source.read_text(encoding="utf-8"), re.M)
                    root = source.relative_to(project).parts.index("kotlin")
                    directory = ".".join(source.relative_to(project).parts[root + 1:-1])
                    self.assertEqual(directory, declared.group(1) if declared else "", source)

                settings = (project / "settings.gradle.kts").read_text(encoding="utf-8")
                for name in modules:
                    self.assertIn(f'include(":feature:{name}")', settings)
                    self.assertTrue((project / "feature" / name).is_dir())

    def test_app_names_containing_a_token_keep_their_own_spelling(self):
        parsed, _ = pipeline.read_spec({"app_name": "Xbaseapp", "package_name": "com.example.x"}, ANDROID)
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp) / "p"
            pipeline.build(parsed.spec, ANDROID, project, zip_output=False, commit="test")
            settings = (project / "settings.gradle.kts").read_text(encoding="utf-8")
            self.assertIn('rootProject.name = "Xbaseapp"', settings)
            self.assertTrue((project / "app/src/main/kotlin/com/example/x/XbaseappApplication.kt").is_file())

    def test_keywords_are_refused_before_anything_is_written(self):
        for package, modules in (("com.example.null", []), ("com.example.trail", ["in"]), ("java.trail.notes", [])):
            with self.subTest(package=package, modules=modules), self.assertRaises(SpecError):
                pipeline.read_spec({"app_name": "Trail", "package_name": package, "feature_modules": modules}, ANDROID)


if __name__ == "__main__":
    unittest.main()
