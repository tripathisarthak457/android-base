import assert from "node:assert/strict";
import { test } from "node:test";
import { loadCatalogue, readJson } from "../catalogue/testing.ts";
import type { BrandField } from "../catalogue/types.ts";
import { type BrandRamp, type Rgb, brandColours, brandRamp, parseHex, readableShade, toHex } from "./brand.ts";

type Golden = { fields: string[]; cases: { accent: string; ramps: Record<string, string[]> }[] };

const golden = readJson<Golden>("generator", "tests", "fixtures", "brand_golden.json");

const FIELDS: Record<string, keyof BrandRamp> = {
  base: "base",
  pressed: "pressed",
  subtle_light: "subtleLight",
  dark: "dark",
  dark_pressed: "darkPressed",
  subtle_dark: "subtleDark",
  on_light: "onLight",
  on_dark: "onDark",
};

function rgb(hex: string): Rgb {
  const parsed = parseHex(hex.length === 9 ? `#${hex.slice(3)}` : hex);
  if (!parsed) throw new Error(`not a colour: ${hex}`);
  return parsed;
}

test("the ramps match the generator on its 200 seeded accents", () => {
  assert.equal(golden.cases.length, 200);
  const mismatches: string[] = [];
  for (const entry of golden.cases) {
    const colours = brandColours(rgb(entry.accent), null, null);
    for (const [name, expected] of Object.entries(entry.ramps)) {
      const ramp = brandRamp(colours[name.toLowerCase() as keyof typeof colours]);
      const actual = golden.fields.map((field) => toHex(ramp[FIELDS[field] ?? "base"]));
      if (JSON.stringify(actual) !== JSON.stringify(expected)) mismatches.push(`${entry.accent} ${name}`);
    }
  }
  assert.deepEqual(mismatches, []);
});

test("the readable shades match the catalogue's resolved content roles", () => {
  const { tokens } = loadCatalogue();
  const field = (name: "accent" | "secondary" | "tertiary", key: BrandField) => rgb(tokens.brand[name][key]);
  for (const [style, themes] of Object.entries(tokens.palette)) {
    for (const [theme, roles] of Object.entries(themes)) {
      for (const name of ["accent", "secondary", "tertiary"] as const) {
        const shade = field(name, theme === "light" ? "base" : "dark");
        const background = rgb(String(roles.background));
        const surface = rgb(String(roles.surface));
        const content = readableShade(readableShade(shade, background), surface);
        assert.equal(`#FF${toHex(content).slice(1)}`, roles[`${name}Content`], `${style} ${theme} ${name}`);
      }
    }
  }
});

test("hex parsing takes six digits with or without the hash, and nothing else", () => {
  assert.deepEqual(parseHex("#ffffff"), [1, 1, 1]);
  assert.deepEqual(parseHex("000000"), [0, 0, 0]);
  for (const bad of ["#fff", "#12345", "blue", "#1234567", ""]) assert.equal(parseHex(bad), null, bad);
});
