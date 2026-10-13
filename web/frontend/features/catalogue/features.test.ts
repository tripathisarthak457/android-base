import assert from "node:assert/strict";
import { test } from "node:test";
import { licensedFeatures, selectableFeatures } from "./features.ts";
import { androidTarget, threePlatformCatalogue } from "./testing.ts";
import type { Catalogue } from "./types.ts";

test("a feature reached only through another is not selectable", () => {
  const android = androidTarget();
  const keys = selectableFeatures(android).map((feature) => feature.key);
  assert.equal(keys.includes("firebase"), false);
  assert.ok(keys.includes("push"));
});

test("a licensed feature offered on two platforms is listed once", () => {
  const licence = { name: "Apache License 2.0", spdx: "Apache-2.0", url: "https://www.apache.org/licenses/LICENSE-2.0" };
  const base = threePlatformCatalogue();
  const catalogue: Catalogue = {
    ...base,
    targets: base.targets.map((target) => ({
      ...target,
      features: target.features.map((feature) => (feature.key === "coil" ? { ...feature, licence } : feature)),
    })),
  };
  assert.deepEqual(licensedFeatures(catalogue).map((feature) => feature.key), ["coil"]);
});
