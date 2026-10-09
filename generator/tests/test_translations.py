"""The shipped translations against the template they translate, and how they land in a project."""

from __future__ import annotations

import re
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

from genkit.core import pipeline, translations
from genkit.core.spec import SpecError
from genkit.core.translations import module_slug
from genkit.targets import get
from genkit.targets.android.strings import escape, resource_qualifier

ANDROID = get("android")
TEMPLATE = ANDROID.template_dir
PLACEHOLDER = re.compile(r"%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z]")


def template_strings() -> dict[str, dict[str, object]]:
    """Every translatable string in the template, by module slug, every feature's markers in place."""
    found: dict[str, dict[str, object]] = {}
    for path in sorted(TEMPLATE.rglob("src/main/res/values/strings.xml")):
        if "build" in path.relative_to(TEMPLATE).parts:
            continue
        module = path.parent.parent.parent.parent.parent.relative_to(TEMPLATE).as_posix()
        entries = {}
        for element in ET.parse(path).getroot():
            if element.get("translatable") == "false":
                continue
            if element.tag == "string":
                entries[element.get("name")] = element.text or ""
            elif element.tag == "plurals":
                entries[element.get("name")] = {item.get("quantity"): item.text for item in element}
        if entries:
            found[module_slug(module)] = entries
    return found


def placeholders(text: str) -> list[str]:
    return sorted(match.group(0) for match in PLACEHOLDER.finditer(text))


class LayoutTest(unittest.TestCase):
    """One file per module per language, and every one of them in step with the template."""

    def test_every_language_names_itself(self):
        languages = translations.available("android")
        self.assertEqual({"de", "es", "fr", "hi", "pt-BR"}, set(languages))
        self.assertEqual("Deutsch", languages["de"])

    def test_every_template_string_is_translated_in_every_language(self):
        source = template_strings()
        for tag in translations.available("android"):
            modules = translations.load("android", tag).modules
            for slug, entries in source.items():
                with self.subTest(language=tag, module=slug):
                    self.assertEqual([], sorted(set(entries) - set(modules.get(slug, {}))))

    def test_no_translation_outlives_the_string_it_translated(self):
        source = template_strings()
        for tag in translations.available("android"):
            for slug, entries in translations.load("android", tag).modules.items():
                with self.subTest(language=tag, module=slug):
                    self.assertEqual([], sorted(set(entries) - set(source.get(slug, {}))))

    def test_placeholders_match_the_english(self):
        # A translation that drops %1$s shows the wrong thing; one that adds a placeholder the code
        # does not pass throws at runtime.
        source = template_strings()
        for tag in translations.available("android"):
            for slug, entries in translations.load("android", tag).modules.items():
                for name, text in entries.items():
                    english = source[slug][name]
                    if isinstance(text, dict):
                        self.assertIn("other", text, f"{tag}: {name} has no 'other' quantity")
                        for quantity, variant in text.items():
                            self.assertEqual(placeholders(english["other"]), placeholders(variant), f"{tag}: {name}/{quantity}")
                    else:
                        self.assertEqual(placeholders(english), placeholders(text), f"{tag}: {name}")

    def test_the_scaffold_strings_are_translated(self):
        with tempfile.TemporaryDirectory() as temp:
            parsed, _ = pipeline.read_spec(
                {"app_name": "Check", "package_name": "com.example.check", "feature_modules": ["orders"]}, ANDROID
            )
            project = Path(temp) / "p"
            pipeline.build(parsed.spec, ANDROID, project, zip_output=False, commit="test")
            names = {
                element.get("name").replace("orders", "{name}")
                for element in ET.parse(project / "feature/orders/src/main/res/values/strings.xml").getroot()
                if element.get("translatable") != "false"
            }
        names.discard("{name}_title")
        for tag in translations.available("android"):
            self.assertEqual(names, set(translations.load("android", tag).scaffold), tag)


class GenerationTest(unittest.TestCase):

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.project = Path(temp.name) / "App"

    def generate(self, **fields) -> Path:
        raw = {
            "app_name": "Check", "package_name": "com.example.check",
            "features": [feature.key for feature in ANDROID.manifest.selectable], "feature_modules": ["orders"],
            **fields,
        }
        parsed, _ = pipeline.read_spec(raw, ANDROID)
        pipeline.build(parsed.spec, ANDROID, self.project, zip_output=False, commit="test")
        return self.project

    def test_each_module_with_strings_gets_them_in_every_language(self):
        project = self.generate(languages=["es", "pt-BR"])
        for source in project.rglob("src/main/res/values/strings.xml"):
            names = [e.get("name") for e in ET.parse(source).getroot() if e.get("translatable") != "false"]
            if not names:
                continue
            for qualifier in ("es", "pt-rBR"):
                translated = source.parent.parent / f"values-{qualifier}" / "strings.xml"
                self.assertTrue(translated.is_file(), translated)
                self.assertEqual(names, [e.get("name") for e in ET.parse(translated).getroot()])

    def test_the_language_picker_lists_them(self):
        locales = next(self.generate(languages=["fr", "hi"]).rglob("AppLocales.kt")).read_text(encoding="utf-8")
        self.assertIn('val supported: List<String> = listOf("en", "fr", "hi")', locales)

    def test_no_languages_leaves_english_alone(self):
        project = self.generate()
        self.assertEqual([], list(project.rglob("values-es")))
        self.assertIn('listOf("en")', next(project.rglob("AppLocales.kt")).read_text(encoding="utf-8"))

    def test_a_feature_that_is_off_leaves_no_translation_behind(self):
        project = self.generate(features=["network"], feature_modules=[], languages=["de"])
        self.assertFalse((project / "feature/settings").exists())


class ValidationTest(unittest.TestCase):

    def test_a_language_without_a_translation_is_refused(self):
        with self.assertRaisesRegex(SpecError, "No translation for 'xx'"):
            pipeline.read_spec({"app_name": "Check", "package_name": "com.example.check", "languages": ["xx"]}, ANDROID)


class EscapingTest(unittest.TestCase):

    def test_apostrophes_and_quotes_are_escaped_for_aapt(self):
        self.assertEqual("l\\'app \\\"x\\\"", escape("l'app \"x\""))

    def test_xml_specials_and_a_leading_at_sign(self):
        self.assertEqual("\\@a &amp; &lt;b&gt;", escape("@a & <b>"))

    def test_a_region_becomes_an_android_qualifier(self):
        self.assertEqual("pt-rBR", resource_qualifier("pt-BR"))
        self.assertEqual("es", resource_qualifier("es"))


if __name__ == "__main__":
    unittest.main()
