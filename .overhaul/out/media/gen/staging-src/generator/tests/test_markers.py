"""The marker grammar, every error it raises, and every real template file with all features on and off."""

from __future__ import annotations

import unittest
from pathlib import Path

from genkit.core import markers
from genkit.core.markers import HASH, MARKDOWN, NONE, SLASH, XML, TemplateError, render
from genkit.core.pipeline import is_hollow_kotlin, template_region_names
from genkit.targets import REGISTRY

KNOWN = frozenset({"push", "firebase", "crashlytics", "analytics", "room", "workmanager", "a", "b", "c"})


def run(text: str, enabled=(), regions=None, syntax=SLASH) -> str:
    rendered = render(
        text, path="T.kt", syntax=syntax, enabled=frozenset(enabled), known=KNOWN, regions=regions or {}
    )
    lines = markers.collapse(rendered.lines, rendered.seams) if syntax.collapse else rendered.lines
    return "".join(line for line, _ in lines)


class BlockTest(unittest.TestCase):

    def test_an_enabled_block_keeps_its_lines_and_drops_the_markers(self):
        self.assertEqual("a\nb\nc\n", run("a\n// <opt:push>\nb\n// </opt:push>\nc\n", {"push"}))

    def test_a_disabled_block_removes_everything_inside(self):
        self.assertEqual("a\nc\n", run("a\n// <opt:push>\nb\n// </opt:push>\nc\n"))

    def test_a_disabled_outer_block_suppresses_an_enabled_inner_one(self):
        # An import for the inner feature must not survive when the outer one is off.
        source = "// <opt:firebase>\nouter\n// <opt:crashlytics>\ninner\n// </opt:crashlytics>\n// </opt:firebase>\n"
        self.assertEqual("", run(source, {"crashlytics"}))
        self.assertEqual("outer\ninner\n", run(source, {"firebase", "crashlytics"}))

    def test_or_and_and_not_combine(self):
        cases = {
            "a|b": [({"a"}, True), ({"b"}, True), (set(), False)],
            "a&b": [({"a"}, False), ({"a", "b"}, True)],
            "!a": [(set(), True), ({"a"}, False)],
            "a&!b|c": [({"a"}, True), ({"a", "b"}, False), ({"a", "b", "c"}, True)],
        }
        for expression, expectations in cases.items():
            for enabled, kept in expectations:
                with self.subTest(expression=expression, enabled=enabled):
                    source = f"// <opt:{expression}>\nx\n// </opt:{expression}>\n"
                    self.assertEqual("x\n" if kept else "", run(source, enabled))

    def test_the_inline_form_emits_the_rest_of_the_line(self):
        source = "        // <opt:!workmanager>        return start\n"
        self.assertEqual("        return start\n", run(source))
        self.assertEqual("", run(source, {"workmanager"}))

    def test_an_inline_marker_inside_a_disabled_block_stays_suppressed(self):
        self.assertEqual("", run("// <opt:push>\n// <opt:!room>x\n// </opt:push>\n"))

    def test_opt_else_emits_its_lines_without_the_prefix_when_the_block_is_off(self):
        source = (
            "    // <opt:a>\n    live()\n    // </opt:a>\n"
            "    // <opt-else:a>\n    //~ fallback()\n    //~\n    //~ more()\n    // </opt-else:a>\n"
        )
        self.assertEqual("    fallback()\n\n    more()\n", run(source))
        self.assertEqual("    live()\n", run(source, {"a"}))

    def test_opt_else_inside_a_disabled_block_emits_nothing(self):
        source = "// <opt:b>\n// <opt:a>\nx\n// </opt:a>\n// <opt-else:a>\n//~ y\n// </opt-else:a>\n// </opt:b>\n"
        self.assertEqual("", run(source))

    def test_xml_and_hash_syntaxes(self):
        xml = "<r>\n    <!-- <opt:a> -->\n    <x/>\n    <!-- </opt:a> -->\n    <!-- <opt-else:a> -->\n    <!--~ <y/> -->\n    <!-- </opt-else:a> -->\n</r>\n"
        self.assertEqual("<r>\n    <y/>\n</r>\n", run(xml, syntax=XML))
        self.assertEqual("<r>\n    <x/>\n</r>\n", run(xml, {"a"}, syntax=XML))
        self.assertEqual("k=v\n", run("# <opt:a>\nx=y\n# </opt:a>\nk=v\n", syntax=HASH))

    def test_a_line_mentioning_a_feature_in_prose_is_not_a_marker(self):
        source = "// Firebase is optional. See the readme.\n"
        self.assertEqual(source, run(source))

    def test_line_endings_pass_through(self):
        self.assertEqual("a\r\nc\r\n", run("a\r\n// <opt:a>\r\nb\r\n// </opt:a>\r\nc\r\n"))


class GeneratedTest(unittest.TestCase):

    def test_a_point_is_replaced_by_the_targets_lines(self):
        self.assertEqual(
            'before\ninclude(":data:a")\nafter\n',
            run("before\n// <generated:modules>\nafter\n", regions={"modules": ['include(":data:a")\n']}),
        )

    def test_a_point_on_a_last_line_with_no_newline_keeps_its_lines_apart(self):
        self.assertEqual("x\none\ntwo\n", run("x\n// <generated:n>", regions={"n": ["one", "two"]}))

    def test_a_region_replaces_its_default_lines(self):
        source = "x\n    // <generated:version>\n    const val V = 1\n    // </generated:version>\ny\n"
        self.assertEqual("x\n    const val V = 2\ny\n", run(source, regions={"version": ["    const val V = 2\n"]}))

    def test_none_keeps_a_regions_default_lines(self):
        source = "// <generated:version>\ndefault\n// </generated:version>\n"
        self.assertEqual("default\n", run(source, regions={"version": None}))

    def test_generated_lines_are_marked_so_they_are_never_renamed(self):
        rendered = render(
            "a\n// <generated:x>\nb\n", path="T.kt", syntax=SLASH, enabled=frozenset(), known=KNOWN,
            regions={"x": ["com.base.app\n"]},
        )
        self.assertEqual([("a\n", False), ("com.base.app\n", True), ("b\n", False)], rendered.lines)

    def test_a_name_inside_a_removed_block_needs_no_lines(self):
        self.assertEqual("", run("// <opt:a>\n// <generated:unsupplied>\n// </opt:a>\n"))


class ErrorTest(unittest.TestCase):

    def assertFails(self, source: str, message: str, syntax=SLASH, regions=None):
        with self.assertRaises(TemplateError) as caught:
            run(source, regions=regions, syntax=syntax)
        self.assertIn(message, str(caught.exception))
        self.assertIn("T.kt:", str(caught.exception))

    def test_an_unclosed_block(self):
        self.assertFails("// <opt:a>\nx\n", "is never closed")

    def test_a_close_that_does_not_match(self):
        self.assertFails("// <opt:a>\n// </opt:b>\n", "closes <opt:a>")

    def test_a_close_with_nothing_open(self):
        self.assertFails("// </opt:a>\n", "closes nothing")

    def test_an_unknown_feature(self):
        self.assertFails("// <opt:pushh>\n// </opt:pushh>\n", "unknown feature 'pushh'")

    def test_the_line_number_counts_only_newlines(self):
        with self.assertRaises(TemplateError) as caught:
            run("val a = 1 // page\x0cbreak\nval b = '\u2028'\n// </opt:a>\n")
        self.assertIn("T.kt:3", str(caught.exception))
        self.assertEqual("x\x0cy\n", run("x\x0cy\n"))

    def test_a_marker_sharing_its_line_with_code(self):
        self.assertFails("foo() // <opt:a>\n", "sharing its line")
        self.assertFails("// <opt:a>\n// </opt:a> trailing\n", "sharing its line")
        self.assertFails("// <opt:a> // </opt:a>\n", "sharing its line")

    def test_opt_else_not_directly_after_its_block(self):
        self.assertFails("// <opt:a>\n// </opt:a>\nx\n// <opt-else:a>\n// </opt-else:a>\n", "must directly follow")

    def test_a_plain_line_inside_opt_else(self):
        self.assertFails("// <opt:a>\n// </opt:a>\n// <opt-else:a>\ncode()\n// </opt-else:a>\n", "must start with")

    def test_a_name_the_target_does_not_supply(self):
        self.assertFails("// <generated:missing>\n", "supplies nothing")

    def test_a_region_end_with_no_start(self):
        self.assertFails("// </generated:x>\n", "no start")

    def test_markers_in_json(self):
        with self.assertRaises(TemplateError):
            run('{"a": "<opt:a>"}\n', syntax=NONE)


class CollapseTest(unittest.TestCase):

    def test_a_gap_left_by_a_removed_block_collapses_to_one_blank_line(self):
        self.assertEqual("a\n\nc\n", run("a\n\n// <opt:a>\nb\n// </opt:a>\n\nc\n"))

    def test_a_blank_line_before_a_closing_brace_goes(self):
        self.assertEqual("fun x() {\n    a()\n}\n", run("fun x() {\n    a()\n\n    // <opt:a>\n    b()\n    // </opt:a>\n}\n"))

    def test_a_blank_line_after_an_opening_brace_goes(self):
        self.assertEqual("fun x() {\n    a()\n}\n", run("fun x() {\n    // <opt:a>\n    b()\n    // </opt:a>\n\n    a()\n}\n"))

    def test_untouched_code_keeps_its_spacing(self):
        # Collapsing every file would reformat code nobody asked the generator to touch.
        source = "class A {\n\n    val x = 1\n\n\n    val y = 2\n}\n"
        self.assertEqual(source, run(source))

    def test_markdown_is_never_collapsed(self):
        source = "a\n\n<!-- <opt:a> -->\nb\n<!-- </opt:a> -->\n\nc\n"
        self.assertEqual("a\n\n\nc\n", run(source, syntax=MARKDOWN))


class HollowFileTest(unittest.TestCase):
    """A Kotlin file the markers leave with nothing to declare is dropped rather than written."""

    def test_a_file_left_with_only_a_package_and_imports_is_hollow(self):
        # The app module's FeatureBindingsModule with none of settings, auth or onboarding on: an
        # empty Hilt module and unused imports, which detekt fails the build over.
        self.assertTrue(is_hollow_kotlin("\n".join(["package a.b", "", "import c.D", "import c.E", ""])))

    def test_comments_alone_do_not_save_a_file(self):
        self.assertTrue(is_hollow_kotlin("\n".join(["package a.b", "", "/**", " * Why this exists.", " */", ""])))

    def test_one_surviving_declaration_keeps_it(self):
        self.assertFalse(is_hollow_kotlin("\n".join(["package a.b", "", "import c.D", "", "object X", ""])))

    def test_an_empty_file_is_hollow(self):
        self.assertTrue(is_hollow_kotlin(""))


class SyntaxTableTest(unittest.TestCase):

    def test_extensions_and_names(self):
        for name, expected in (
            ("A.kt", SLASH), ("b.gradle.kts", SLASH), ("X.swift", SLASH), ("libs.versions.toml", HASH),
            ("gradle.properties", HASH), ("proguard-rules.pro", HASH), (".gitignore", HASH), ("Podfile", HASH),
            ("strings.xml", XML), ("Info.plist", XML), ("README.md", MARKDOWN), ("a.json", NONE),
        ):
            self.assertIs(expected, markers.syntax_for(Path(name)), name)
        self.assertIsNone(markers.syntax_for(Path("gradlew")))

    def test_the_manifest_can_override(self):
        self.assertIs(HASH, markers.syntax_for(Path("gradlew"), {"gradlew": "#"}))


class RealTemplatesTest(unittest.TestCase):
    """Every file of every template renders with all features on and with all off."""

    def test_every_template_renders_both_ways(self):
        from genkit.core.copy import is_verbatim, template_files

        for target in REGISTRY.values():
            manifest = target.manifest
            known = frozenset(manifest.features)
            names = template_region_names(target.template_dir)
            regions: dict[str, list[str] | None] = dict.fromkeys(names, None)
            for enabled in (known, frozenset()):
                for source in template_files(target.template_dir, list(manifest.template_only), {}):
                    if is_verbatim(source.relative, manifest.verbatim):
                        continue
                    try:
                        text = source.source.read_text(encoding="utf-8")
                    except UnicodeDecodeError:
                        continue
                    if not markers.MARKER_TEXT.search(text):
                        continue
                    syntax = markers.syntax_for(Path(source.relative), manifest.syntax)
                    with self.subTest(target=target.key, file=source.relative, on=bool(enabled)):
                        self.assertIsNotNone(syntax, "markers in a file whose syntax is unknown")
                        render(
                            text, path=source.relative, syntax=syntax, enabled=enabled, known=known,
                            regions=regions,
                        )

    def test_every_required_region_is_in_the_template_and_nothing_else_is(self):
        for target in REGISTRY.values():
            with self.subTest(target=target.key):
                self.assertEqual(set(target.manifest.required_regions), set(template_region_names(target.template_dir)))


if __name__ == "__main__":
    unittest.main()
