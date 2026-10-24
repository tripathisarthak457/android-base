import assert from "node:assert/strict";
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { test } from "node:test";
import { repositoryPath } from "../catalogue/testing.ts";
import { GLYPH_DIRECTORY, parseGlyphs, readTemplateGlyphs } from "./glyphSource.ts";
import { GLYPHS } from "./glyphs.ts";

function templateFiles(): Record<string, string> {
  const directory = repositoryPath(...GLYPH_DIRECTORY.split("/"));
  return Object.fromEntries(readdirSync(directory).map((name) => [name, readFileSync(join(directory, name), "utf8")]));
}

test("the preview's glyphs are the template's, path for path: run npm run glyphs after changing them", () => {
  const template = readTemplateGlyphs(templateFiles());
  assert.ok(Object.keys(template).length >= 80);
  assert.deepEqual(GLYPHS, template);
});

test("every glyph AppIcons offers is one the preview can draw", () => {
  const icons = readFileSync(repositoryPath(...GLYPH_DIRECTORY.split("/").slice(0, -1), "AppIcons.kt"), "utf8");
  const offered = [...icons.matchAll(/^\s+val (\w+): ImageVector get\(\) = \w+\.(\w+)$/gm)];
  assert.ok(offered.length >= 80);
  for (const [, name = "", glyph = ""] of offered) {
    assert.equal(name, glyph, `AppIcons.${name} reads another glyph's name`);
  }
  // The generator picks a module's glyph from AppIcons' names, and the preview picks from these.
  assert.deepEqual(Object.keys(GLYPHS).sort(), offered.map(([, name]) => name).sort());
});

test("the DSL's circles, dots, fills and stroke widths become their SVG equivalents", () => {
  const widths = { StrokeWidth: 1.8, BoldStrokeWidth: 2.6, DotWidth: 2.4 };
  const glyphs = parseGlyphs(
    `
    // A comment with moveTo(1f, 1f) in it is not a path.
    val Ring by icon("Ring", autoMirror = true) {
        stroke(BoldStrokeWidth) { circle(12f, 12f, 4f) }
        stroke(DotWidth) { dot(4.5f, 6.5f) }
        fill(evenOdd = true) { moveTo(1f, 2f); lineTo(3f, 4f); curveTo(1f, 2f, 3f, 4f, 5f, 6f); close() }
    }`,
    widths,
  );
  assert.deepEqual(glyphs.Ring, {
    mirrored: true,
    paths: [
      { kind: "stroke", d: "M8 12A4 4 0 1 1 16 12A4 4 0 1 1 8 12Z", width: 2.6 },
      { kind: "stroke", d: "M4.5 6.5L4.51 6.5", width: 2.4 },
      { kind: "fill", d: "M1 2L3 4C1 2 3 4 5 6Z", evenOdd: true },
    ],
  });
  assert.throws(() => parseGlyphs(`val X by icon("X") { stroke { quadTo(1f, 2f, 3f, 4f) } }`, widths), /quadTo/);
});
