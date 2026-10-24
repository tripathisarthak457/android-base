import assert from "node:assert/strict";
import { test } from "node:test";
import { CI_PROJECTS, ciPresets } from "./ci.ts";
import { androidTarget, readText } from "./testing.ts";

test("the projects the site says CI builds are the ones the workflow's matrix builds", () => {
  const workflow = readText(".github", "workflows", "ci.yml");
  const matrix = /^\s+preset:\s*\[([^\]]*)\]/m.exec(workflow);
  assert.ok(matrix?.[1], "ci.yml no longer lists its projects as `preset: [...]`: update CI_PROJECTS and this test with it");
  const built = matrix[1].split(",").map((entry) => entry.trim());
  assert.deepEqual([...CI_PROJECTS].sort(), built.sort());
  for (const extreme of ["all", "none"]) {
    assert.ok(built.includes(extreme), `the hero and the FAQ say CI also builds every feature at once and none at all, but not ${extreme}`);
  }
});

test("CI's presets are named by title, and only the ones it builds", () => {
  const target = androidTarget();
  const built = ciPresets(target, ["none", "lean", "everything", "all"]);
  assert.deepEqual(built, { phrase: "the Lean and Everything presets", plural: true });
});

test("a matrix with every preset in it says so", () => {
  const target = androidTarget();
  const keys = target.presets.map((preset) => preset.key);
  assert.deepEqual(ciPresets(target, ["none", ...keys, "all"]), { phrase: "every preset", plural: false });
});

test("one preset reads in the singular", () => {
  assert.deepEqual(ciPresets(androidTarget(), ["lean"]), { phrase: "the Lean preset", plural: false });
});
