import assert from "node:assert/strict";
import { test } from "node:test";
import { GENERATION_COLUMNS, uiComponentsCell } from "./columns.ts";

test("the Generations table has a UI components column beside the preset", () => {
  assert.equal(GENERATION_COLUMNS.indexOf("UI components"), GENERATION_COLUMNS.indexOf("Preset") + 1);
  assert.equal(new Set(GENERATION_COLUMNS).size, GENERATION_COLUMNS.length);
});

test("a row says with or without UI components, and nothing when its spec was never read", () => {
  assert.equal(uiComponentsCell("without"), "without");
  assert.equal(uiComponentsCell("with"), "with");
  assert.equal(uiComponentsCell(""), "—");
});
