"""
The shipped translations against the template they translate.

    py -m unittest tests.test_translations
"""

from __future__ import annotations

import re
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from genkit.build import TEMPLATE_DIR, build  # noqa: E402
from genkit.spec import FEATURES, ProjectSpec, SpecError, resolve_features  # noqa: E402
from genkit.translations import _escape, available, load, resource_qualifier  # noqa: E402

PLACEHOLDER = re.compile(r"%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z]")


def template_strings() -> dict[str, dict[str, object]]:
    """Every translatable string in the template, with every feature's markers still in place."""
    found: dict[str, dict[str, object]] = {}
    for path in sorted(TEMPLATE_DIR.rglob("src/main/res/values/strings.xml")):
        if "build" in path.relative_to(TEMPLATE_DIR).parts:
            continue
        module = path.parent.parent.relative_to(TEMPLATE_DIR).as_posix()
        entries = {}
        for element in ET.parse(path).getroot():
            if element.get("translatable") == "false":
                continue
            if element.tag == "string":
                entries[element.get("name")] = element.text or ""
            elif element.tag == "plurals":
                entries[element.get("name")] = {item.get("quantity"): item.text for item in element}
        if entries:
            found[module] = entries
    return found


def placeholders(text: str) -> list[str]:
    return sorted(match.group(0) for match in PLACEHOLDER.finditer(text))


class CompletenessTest(unittest.TestCase):
    """Lint refuses an app that ships a language with a string missing; these fail first."""

    def test_every_template_string_is_translated_in_every_language(self):
        source = template_strings()
        for tag in available():
            strings = load(tag)["strings"]
            for module, entries in source.items():
                missing = sorted(set(entries) - set(strings.get(module, {})))
                self.assertEqual([], missing, f"{tag}: {module} has no translation for these")

    def test_no_translation_outlives_the_string_it_translated(self):
        source = template_strings()
        for tag in available():
            for module, entries in load(tag)["strings"].items():
                stale = sorted(set(entries) - set(source.get(module, {})))
                self.assertEqual([], stale, f"{tag}: {module} translates strings that are gone")

    def test_placeholders_match_the_english(self):
        # A translation that drops %1$s crashes nothing but shows the wrong thing; one that adds a
        # placeholder the code does not pass throws at runtime.
        source = template_strings()
        for tag in available():
            for module, entries in load(tag)["strings"].items():
                for name, text in entries.items():
                    english = source[module][name]
                    if isinstance(text, dict):
                        self.assertIn("other", text, f"{tag}: {name} has no 'other' quantity")
                        wanted = placeholders(english["other"])
                        for quantity, variant in text.items():
                            self.assertEqual(wanted, placeholders(variant), f"{tag}: {name}/{quantity}")
                    else:
                        self.assertEqual(placeholders(english), placeholders(text), f"{tag}: {name}")

    def test_the_scaffold_strings_are_translated(self):
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp) / "App"
            build(
                ProjectSpec(app_name="Check", package_name="com.example.check", feature_modules=("orders",)),
                project,
                zip_output=False,
            )
            strings = project / "feature/orders/src/main/res/values/strings.xml"
            names = {
                element.get("name").replace("orders", "{name}")
                for element in ET.parse(strings).getroot()
                if element.get("translatable") != "false"
            }
        names.discard("{name}_title")
        for tag in available():
            self.assertEqual(names, set(load(tag)["scaffold"]), tag)


class GenerationTest(unittest.TestCase):

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.project = Path(temp.name) / "App"

    def generate(self, **overrides) -> Path:
        everything = resolve_features({feature.key for feature in FEATURES if not feature.implied_only})
        fields = dict(
            app_name="Check",
            package_name="com.example.check",
            features=frozenset(everything),
            feature_modules=("orders",),
        )
        fields.update(overrides)
        build(ProjectSpec(**fields).validated(), self.project, zip_output=False)
        return self.project

    def test_each_module_with_strings_gets_them_in_every_language(self):
        project = self.generate(languages=("es", "pt-BR"))

        for source in project.rglob("src/main/res/values/strings.xml"):
            names = [e.get("name") for e in ET.parse(source).getroot() if e.get("translatable") != "false"]
            for qualifier in ("es", "pt-rBR"):
                translated = source.parent.parent / f"values-{qualifier}" / "strings.xml"
                if not names:
                    continue
                self.assertTrue(translated.is_file(), translated)
                self.assertEqual(names, [e.get("name") for e in ET.parse(translated).getroot()])

    def test_the_language_picker_lists_them(self):
        project = self.generate(languages=("fr", "hi"))

        locales = next(project.rglob("AppLocales.kt")).read_text(encoding="utf-8")

        self.assertIn('val supported: List<String> = listOf("en", "fr", "hi")', locales)

    def test_no_languages_leaves_english_alone(self):
        project = self.generate()

        self.assertEqual([], list(project.rglob("values-es")))
        self.assertIn('listOf("en")', next(project.rglob("AppLocales.kt")).read_text(encoding="utf-8"))

    def test_a_feature_that_is_off_leaves_no_translation_behind(self):
        project = self.generate(features=frozenset({"network"}), feature_modules=(), languages=("de",))

        self.assertFalse((project / "feature/settings").exists())
        self.assertEqual([], [p for p in project.rglob("values-de") if "feature/settings" in p.as_posix()])


class ValidationTest(unittest.TestCase):

    def test_a_language_without_a_translation_is_refused(self):
        with self.assertRaisesRegex(SpecError, "No translation for 'xx'"):
            ProjectSpec(app_name="Check", package_name="com.example.check", languages=("xx",)).validated()

    def test_a_language_listed_twice_is_refused(self):
        with self.assertRaisesRegex(SpecError, "only be listed once"):
            ProjectSpec(app_name="Check", package_name="com.example.check", languages=("es", "es")).validated()


class EscapingTest(unittest.TestCase):

    def test_apostrophes_and_quotes_are_escaped_for_aapt(self):
        self.assertEqual("l\\'app \\\"x\\\"", _escape("l'app \"x\""))

    def test_xml_specials_and_a_leading_at_sign(self):
        self.assertEqual("\\@a &amp; &lt;b&gt;", _escape("@a & <b>"))

    def test_a_region_becomes_an_android_qualifier(self):
        self.assertEqual("pt-rBR", resource_qualifier("pt-BR"))
        self.assertEqual("es", resource_qualifier("es"))


if __name__ == "__main__":
    unittest.main()
