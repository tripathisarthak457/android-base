import assert from "node:assert/strict";
import { test } from "node:test";
import { loadCatalogue, readJson, threePlatformCatalogue } from "../../catalogue/testing.ts";
import { validateSpec } from "./validate.ts";

type Case = { name: string; valid: boolean; field?: string; spec: Record<string, unknown> };

const catalogue = loadCatalogue();
const { cases } = readJson<{ cases: Case[] }>("generator", "tests", "fixtures", "validation_cases.json");

// A spec without a `schema` is schema 1: an old CLI file or the legacy API alias. The generator
// upgrades it, including renamed feature keys that live in its manifest and not in the catalogue,
// and the site never writes one, so those cases are the generator's alone.
const schemaTwo = cases.filter((entry) => "schema" in entry.spec);

test("the shared fixture still has cases for the site", () => {
  assert.ok(schemaTwo.length >= 90, `only ${schemaTwo.length} schema-2 cases`);
  assert.ok(schemaTwo.some((entry) => entry.valid));
  assert.ok(schemaTwo.some((entry) => !entry.valid));
});

for (const entry of schemaTwo) {
  test(`${entry.valid ? "accepts" : "refuses"}: ${entry.name}`, () => {
    const errors = validateSpec(entry.spec, catalogue, String(entry.spec.platform ?? "android"));
    if (entry.valid) {
      assert.deepEqual([...errors], []);
    } else {
      const first = errors.keys().next().value;
      assert.equal(first, entry.field, `errors: ${JSON.stringify([...errors])}`);
    }
  });
}

test("a spec for a target the catalogue lacks is refused on platform", () => {
  assert.deepEqual([...validateSpec({ schema: 2 }, catalogue, "windows").keys()], ["platform"]);
});

test("several problems are all reported, each once, in the generator's order", () => {
  const errors = validateSpec(
    {
      schema: 2,
      platform: "android",
      app_name: "2048",
      package_name: "com.example.fun",
      accent_colour: "blue",
      android: { min_sdk: 30, target_sdk: 28 },
    },
    catalogue,
    "android",
  );
  assert.deepEqual([...errors.keys()], ["app_name", "package_name", "accent_colour", "android.target_sdk"]);
});

test("a settings block without checks of its own is held to its fields' rules and limits", () => {
  const three = threePlatformCatalogue();
  const refused = (ios: Record<string, string>) => [
    ...validateSpec({ schema: 2, platform: "multiplatform", app_name: "Trail Notes", package_name: "com.example.trail", ios }, three, "multiplatform").keys(),
  ];
  assert.deepEqual(refused({ bundle_id: "com.example.trail-notes", deployment_target: "18.0", team_id: "ABCDE12345" }), []);
  assert.deepEqual(refused({ bundle_id: "com.example.trail_notes" }), ["ios.bundle_id"]);
  assert.deepEqual(refused({ bundle_id: `com.${"a".repeat(160)}` }), ["ios.bundle_id"]);
  assert.deepEqual(refused({ team_id: "abc" }), ["ios.team_id"]);
});
