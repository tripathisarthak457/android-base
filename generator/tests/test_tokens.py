"""The token files, their resolution against a spec, the brand colour maths, and the emitters."""

from __future__ import annotations

import copy
import io
import json
import math
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

from genkit import brand, tokens
from genkit.core import pipeline
from genkit.core.markers import TemplateError
from genkit.targets import get
from genkit.tokens import __main__ as cli
from genkit.tokens import kotlin, swift, web

FIXTURES = Path(__file__).resolve().parent / "fixtures"


def spec(**fields):
    raw = {"schema": 2, "platform": "android", "app_name": "Acme", "package_name": "com.acme.x", **fields}
    return pipeline.read_spec(raw, get("android"))[0].spec


class CheckTest(unittest.TestCase):

    def test_every_checked_in_copy_is_current(self):
        with redirect_stdout(io.StringIO()):
            self.assertEqual(0, cli.main(["--check"]))

    def test_a_copy_with_crlf_line_endings_is_stale_until_written_again(self):
        with tempfile.TemporaryDirectory() as temp:
            templates = Path(temp) / "templates"
            checked_in = templates / "android" / "Tokens.kt"
            checked_in.parent.mkdir(parents=True)
            checked_in.write_bytes(b"val a = 1\r\nval b = 2\r\n")
            emitted = {"android": (checked_in, lambda: "val a = 1\nval b = 2\n")}
            with mock.patch.object(cli, "TEMPLATES", templates), mock.patch.object(cli, "emissions", lambda: emitted), \
                    redirect_stdout(io.StringIO()):
                self.assertEqual(1, cli.main(["--check"]))
                self.assertEqual(0, cli.main(["--write"]))
                self.assertEqual(0, cli.main(["--check"]))
            self.assertEqual(b"val a = 1\nval b = 2\n", checked_in.read_bytes())

    def test_the_files_validate(self):
        tokens.validate(tokens.load())


class SchemaTest(unittest.TestCase):
    """A mistake in a token file names the file and the key."""

    def broken(self, mutate) -> str:
        data = copy.deepcopy(tokens.load())
        mutate(data)
        with self.assertRaises(TemplateError) as caught:
            tokens.validate(data)
        return str(caught.exception)

    def test_a_weight_that_is_not_a_weight(self):
        message = self.broken(lambda d: d["typography"]["roles"]["body"]["weight"].update(Bold=450))
        self.assertIn("typography.json", message)
        self.assertIn("roles.body.weight.Bold", message)

    def test_a_voice_missing_from_a_role(self):
        self.assertIn("missing Rounded", self.broken(lambda d: d["typography"]["roles"]["body"]["weight"].pop("Rounded")))

    def test_a_bounce_of_one_never_settles(self):
        self.assertIn("styles.Bouncy.bounce", self.broken(lambda d: d["motion"]["styles"]["Bouncy"].update(bounce=1)))

    def test_a_role_that_points_nowhere(self):
        self.assertIn("'@nothing' is not a role", self.broken(lambda d: d["palette"]["roles"]["light"].update(fill="@nothing")))

    def test_roles_that_point_at_each_other(self):
        def loop(data):
            data["palette"]["roles"]["light"].update(fill="@separator", separator="@fill")
        self.assertIn("loop", self.broken(loop))

    def test_light_and_dark_with_different_roles(self):
        self.assertIn("same roles", self.broken(lambda d: d["palette"]["roles"]["dark"].pop("fill")))

    def test_a_style_on_a_tone_that_does_not_exist(self):
        self.assertIn("Utility.surfaces", self.broken(lambda d: d["styles"]["Utility"].update(surfaces="Neon")))

    def test_an_alpha_above_one(self):
        self.assertIn("at most 1", self.broken(lambda d: d["palette"]["roles"]["light"]["scrim"].update(alpha=1.5)))

    def test_a_status_colour_missing_a_theme_or_a_field(self):
        self.assertIn("status.success: is missing dark", self.broken(lambda d: d["palette"]["status"]["success"].pop("dark")))
        self.assertIn("status.danger.light: is missing border", self.broken(lambda d: d["palette"]["status"]["danger"]["light"].pop("border")))
        self.assertIn("status", self.broken(lambda d: d["palette"].pop("status")))

    def test_a_size_missing_a_platform(self):
        self.assertIn("sizes.minTouchTarget.ios", self.broken(lambda d: d["foundation"]["sizes"]["minTouchTarget"].pop("ios")))


class ResolveTest(unittest.TestCase):

    def test_springs_follow_the_ios_mapping(self):
        stiffness, damping = tokens.spring(0.24, 0.0)
        self.assertAlmostEqual((2 * math.pi / 0.24) ** 2, stiffness)
        self.assertEqual(1.0, damping)

    def test_each_motion_style_scales_bounce_and_speed(self):
        motion = tokens.load()["motion"]
        resolved = tokens.resolve()["motion"]["styles"]
        for name, style in motion["styles"].items():
            for key, definition in motion["springs"].items():
                with self.subTest(style=name, spring=key):
                    spring = resolved[name]["springs"][key]
                    response = definition["response"] / (style["speed"] if definition["bySpeed"] else 1)
                    self.assertAlmostEqual(response, spring["response"], places=4)
                    self.assertAlmostEqual(1 - style["bounce"] * definition["bounceScale"], spring["dampingRatio"], places=4)

    def test_full_screen_springs_never_bounce(self):
        for name, style in tokens.resolve()["motion"]["styles"].items():
            with self.subTest(style=name):
                self.assertEqual(1.0, style["springs"]["screen"]["dampingRatio"])

    def test_without_brand_colours_the_template_palette_stands(self):
        palette = tokens.resolve()["palette"]
        template = tokens.load()["palette"]["templateBrand"]["accent"]
        self.assertEqual("#FF" + template["base"][1:], palette["tones"]["Cool"]["light"]["accent"])
        self.assertEqual("#FF" + template["dark"][1:], palette["tones"]["Cool"]["dark"]["accent"])

    def test_a_brand_colour_drives_every_accent_role(self):
        ramp = brand.brand_ramps("#16A34A")["Accent"]
        roles = tokens.resolve(spec(accent_colour="#16A34A"))["palette"]["tones"]["Cool"]
        self.assertEqual("#FF" + brand.to_hex(ramp.base)[1:], roles["light"]["accent"])
        self.assertEqual("#FF" + brand.to_hex(ramp.subtle_dark)[1:], roles["dark"]["accentSubtle"])

    def test_the_template_secondary_and_tertiary_are_the_brand_maths(self):
        derived = brand.brand_ramps(tokens.load()["palette"]["templateAccent"])
        template = tokens.load()["palette"]["templateBrand"]
        for name in ("Secondary", "Tertiary"):
            self.assertEqual(template[name.lower()]["base"], brand.to_hex(derived[name].base))

    def test_the_brand_maths_constants_are_the_palette_tokens(self):
        # brand.py cannot read the tokens, which import it, so its copies are held to them here.
        palette = tokens.load()["palette"]
        self.assertEqual(palette["templateAccent"], brand.TEMPLATE_ACCENT)
        self.assertEqual(palette["ramps"]["ink"]["900"], brand.to_hex(brand.LIGHT_THEME_INK))
        for colour in palette["templateBrand"].values():
            self.assertIn(colour["onLight"], ("#FFFFFF", brand.to_hex(brand.LIGHT_THEME_INK)))
            self.assertIn(colour["onDark"], ("#FFFFFF", brand.to_hex(brand.DARK_THEME_INK)))

    def test_accent_content_reads_on_every_surface(self):
        for hex_value in ("#FFEB3B", "#16A34A", "#2C6BED", "#0E1A2B", "#9E0000", "#FF69B4", "#00FFFF"):
            palette = tokens.resolve(spec(accent_colour=hex_value))["palette"]["tones"]
            for tone, themes in palette.items():
                for theme, roles in themes.items():
                    for background in ("background", "surface"):
                        with self.subTest(accent=hex_value, tone=tone, theme=theme, on=background):
                            ratio = brand.contrast(_rgb(roles["accentContent"]), _rgb(roles[background]))
                            self.assertGreaterEqual(round(ratio, 2), 4.5)

    def test_brand_tint_mixes_in_oklab_and_keeps_alpha(self):
        cool = tokens.resolve()["palette"]["tones"]["Cool"]["light"]
        tinted = tokens.resolve()["palette"]["tones"]["BrandTint"]["light"]
        self.assertNotEqual(cool["background"], tinted["background"])
        self.assertEqual(cool["scrim"], tinted["scrim"])

    def test_defaults_are_the_specs_choices(self):
        defaults = tokens.resolve(spec(design_style="Playful", motion_style="Calm", haptics_enabled=False))["defaults"]
        self.assertEqual(("Playful", "Calm", False), (defaults["designStyle"], defaults["motionStyle"], defaults["hapticsEnabled"]))


class BrandGoldenTest(unittest.TestCase):
    """`fixtures/brand_golden.json`, which the website's TypeScript port is checked against too."""

    def test_two_hundred_seeded_accents(self):
        data = json.loads((FIXTURES / "brand_golden.json").read_text(encoding="utf-8"))
        for case in data["cases"]:
            ramps = brand.brand_ramps(case["accent"])
            for name, expected in case["ramps"].items():
                actual = [brand.to_hex(getattr(ramps[name], field)) for field in data["fields"]]
                self.assertEqual(expected, actual, f"{case['accent']} {name}")

    def test_text_on_every_brand_shade_reads_at_aa(self):
        failures = []
        for case in json.loads((FIXTURES / "brand_golden.json").read_text(encoding="utf-8"))["cases"]:
            for name, ramp in brand.brand_ramps(case["accent"]).items():
                for shade, text in ((ramp.base, ramp.on_light), (ramp.dark, ramp.on_dark)):
                    if brand.contrast(shade, text) < brand.AA:
                        failures.append((case["accent"], name))
        self.assertEqual([], failures[:10])


class EmitterTest(unittest.TestCase):
    """The emitted shapes. Values are read from the tokens, so tuning a number never breaks these."""

    @classmethod
    def setUpClass(cls):
        cls.resolved = tokens.resolve(None, "android")
        cls.kotlin = kotlin.emit(cls.resolved, "com.base.app")
        cls.swift = swift.emit(tokens.resolve(None, "ios"))

    def test_kotlin_structure(self):
        for line in (
            kotlin.HEADER,
            "package com.base.app.core.designsystem.theme.tokens",
            "internal object DesignTokens {",
            "    object Spacing {",
            "    object Type {",
            "    class MotionStyle(",
            "    object Palette {",
            "        object Cool {",
            "            val light = Roles(",
            "    object Styles {",
            "    object Defaults {",
            "        val designStyle: AppDesignStyle = AppDesignStyle.Utility",
        ):
            self.assertIn(line + "\n", self.kotlin)

    def test_kotlin_values_come_from_the_tokens(self):
        role = tokens.load()["typography"]["roles"]["largeTitle"]
        weights = ", ".join(str(role["weight"][voice]) for voice in tokens.load()["typography"]["voices"])
        self.assertIn(
            f"val largeTitle = TypeRole({role['size']}.sp, {role['lineHeight']}.sp, "
            f"{kotlin._sp(role['tracking'])}, {weights})",
            self.kotlin,
        )
        press = self.resolved["motion"]["styles"]["Standard"]["springs"]["press"]
        self.assertIn(f"press = Spring({press['stiffness']}f, {press['dampingRatio']}f),", self.kotlin)
        accent = self.resolved["palette"]["tones"]["Cool"]["light"]["accent"]
        self.assertIn(f"accent = Color(0x{accent[1:]}),", self.kotlin)
        self.assertIn("        buttonRadius = null,", self.kotlin)

    def test_kotlin_literals(self):
        self.assertEqual("(-0.8).sp", kotlin._sp(-0.8))
        self.assertEqual("1.5.dp", kotlin._dp(1.5))
        self.assertEqual("48.dp", kotlin._dp(48))

    def test_swift_mirrors_it(self):
        for line in (
            swift.HEADER,
            "enum DesignTokens {",
            "    enum Typography {",
            "    struct SpringToken {",
            "    enum Palette {",
            "        enum Cool {",
            "    enum Defaults {",
        ):
            self.assertIn(line + "\n", self.swift)
        self.assertIn("static let minTouchTarget: CGFloat = 44", self.swift)
        # A hairline is device pixels on both platforms, never points.
        self.assertIn(f"static let hairlinePx: Int = {int(tokens.load()['foundation']['borders']['hairlinePx'])}\n", self.swift)

    def test_the_website_gets_a_palette_per_design_style(self):
        catalogue = web.catalogue_tokens(tokens.resolve(None, "web"))
        self.assertEqual(set(tokens.load()["styles"]), set(catalogue["palette"]))
        for style in catalogue["palette"].values():
            self.assertEqual({"light", "dark"}, set(style))


def _rgb(argb: str) -> tuple[float, float, float]:
    digits = argb.lstrip("#")[-6:]
    return tuple(int(digits[i : i + 2], 16) / 255 for i in (0, 2, 4))  # type: ignore[return-value]


if __name__ == "__main__":
    unittest.main()
