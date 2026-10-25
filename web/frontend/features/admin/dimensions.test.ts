import assert from "node:assert/strict";
import { test } from "node:test";
import { ADOPTION_DIMENSIONS, WEEKLY_DIMENSIONS } from "./dimensions.ts";

test("the Adoption panel shows every dimension the API counts, in its order", () => {
  // store.AdoptionDimensions in web/api: a key missing here is counted and never shown.
  assert.deepEqual(
    ADOPTION_DIMENSIONS.map(([key]) => key),
    ["presets", "ui_components", "architectures", "design_styles", "motion_styles", "languages"],
  );
});

test("the share of projects without UI components has its own heading, weekly too", () => {
  assert.equal(new Map(ADOPTION_DIMENSIONS).get("ui_components"), "UI components");
  assert.equal(new Map(WEEKLY_DIMENSIONS).get("ui_components"), "UI components");
  assert.deepEqual(WEEKLY_DIMENSIONS[0], ["features", "Features"]);
});
