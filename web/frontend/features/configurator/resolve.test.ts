import assert from "node:assert/strict";
import { test } from "node:test";
import { androidTarget } from "../catalogue/testing.ts";
import { closeOver, dependentsOf, derivePreset, presetFeatures, toggleFeature } from "./resolve.ts";

const target = androidTarget();
const everything = presetFeatures(target, "everything");

function isClosed(set: ReadonlySet<string>): boolean {
  const closed = closeOver(set, target);
  return closed.size === set.size && [...closed].every((key) => set.has(key));
}

test("unticking any feature leaves nothing on that needs it", () => {
  for (const key of everything) {
    const { next } = toggleFeature(everything, key, target);
    assert.equal(next.has(key), false, key);
    assert.ok(isClosed(next), `unticking ${key} left ${[...closeOver(next, target)].filter((k) => !next.has(k))}`);
  }
});

test("unticking a transitive requirement takes every dependent with it", () => {
  const selected = closeOver(["push"], target);
  const { next, alsoRemoved } = toggleFeature(selected, "analytics", target);
  assert.deepEqual([...next].sort(), ["flags"]);
  assert.deepEqual(alsoRemoved.sort(), ["firebase", "push"]);
});

test("ticking a feature adds what it needs and says which", () => {
  const { next, alsoAdded } = toggleFeature(new Set(), "googlesignin", target);
  assert.deepEqual([...next].sort(), ["auth", "forms", "googlesignin", "network"]);
  assert.deepEqual(alsoAdded.sort(), ["auth", "forms", "network"]);
  for (const feature of target.features) {
    assert.ok(isClosed(toggleFeature(new Set(), feature.key, target).next), feature.key);
  }
});

test("ticking something already needed adds nothing else", () => {
  const { alsoAdded } = toggleFeature(new Set(["network"]), "sample", target);
  assert.deepEqual(alsoAdded, []);
});

test("dependents are what an untick would also remove", () => {
  assert.deepEqual(dependentsOf(closeOver(["auth"], target), "forms", target), ["auth"]);
  assert.deepEqual(dependentsOf(new Set(["forms"]), "forms", target), []);
});

test("every preset is recognised from its features, and an edit makes the set the visitor's own", () => {
  for (const preset of target.presets) {
    const features = presetFeatures(target, preset.key);
    assert.equal(derivePreset(features, target), preset.key);
    for (const key of features) {
      assert.equal(derivePreset(toggleFeature(features, key, target).next, target), null, `${preset.key} without ${key}`);
    }
  }
});

test("unknown keys are dropped when closing a set", () => {
  assert.deepEqual([...closeOver(["telepathy", "sample"], target)].sort(), ["network", "sample"]);
});
