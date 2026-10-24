// `npm run glyphs`: writes features/preview/glyphs.ts from the template's icon set, after a glyph
// is added or redrawn there. glyphs.test.ts fails until it has run.

import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { GLYPH_DIRECTORY, readTemplateGlyphs } from "../features/preview/glyphSource.ts";

const directory = join(import.meta.dirname, "..", "..", "..", ...GLYPH_DIRECTORY.split("/"));
const files = Object.fromEntries(readdirSync(directory).map((name) => [name, readFileSync(join(directory, name), "utf8")]));
const glyphs = readTemplateGlyphs(files);
/** A value as the site's own source writes one: unquoted keys, spaces after commas. */
const literal = (value: unknown): string => {
  if (Array.isArray(value)) return `[${value.map(literal).join(", ")}]`;
  if (value && typeof value === "object") {
    return `{ ${Object.entries(value).map(([key, field]) => `${key}: ${literal(field)}`).join(", ")} }`;
  }
  return JSON.stringify(value);
};
const lines = Object.entries(glyphs)
  .sort(([a], [b]) => a.localeCompare(b))
  .map(([name, glyph]) => `  ${name}: ${literal(glyph)},`);

writeFileSync(
  join(import.meta.dirname, "..", "features", "preview", "glyphs.ts"),
  [
    "// Written by `npm run glyphs` from the template's icon set (core/designsystem/.../icon/glyph);",
    "// glyphs.test.ts fails when it differs from the template.",
    "",
    'import type { Glyph } from "./glyphSource.ts";',
    "",
    "export const GLYPHS = {",
    ...lines,
    "} satisfies Record<string, Glyph>;",
    "",
    "export type GlyphName = keyof typeof GLYPHS;",
    "",
  ].join("\n"),
);
console.log(`${lines.length} glyphs written`);
