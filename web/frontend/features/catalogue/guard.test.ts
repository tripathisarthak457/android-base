import assert from "node:assert/strict";
import { test } from "node:test";
import { canMakeKeystores, parseCatalogue } from "./guard.ts";
import { loadCatalogue } from "./testing.ts";

test("the checked-in catalogue passes the guard", () => {
  assert.notEqual(parseCatalogue(loadCatalogue()), null);
});

test("a body that is not catalogue v2 is refused", () => {
  const catalogue = loadCatalogue();
  assert.equal(parseCatalogue(null), null);
  assert.equal(parseCatalogue("<html>"), null);
  assert.equal(parseCatalogue({ ...catalogue, version: 1 }), null);
  assert.equal(parseCatalogue({ ...catalogue, targets: {} }), null);
  assert.equal(parseCatalogue({ ...catalogue, rules: undefined }), null);
  assert.equal(parseCatalogue({ ...catalogue, environment: [] }), null);
});

test("a target whose features lost their requires is refused", () => {
  const catalogue = loadCatalogue();
  const targets = catalogue.targets.map((target) => ({
    ...target,
    features: target.features.map(({ requires: _, ...feature }) => feature),
  }));
  assert.equal(parseCatalogue({ ...catalogue, targets }), null);
});

test("keystores are offered unless the serving machine says it has no keytool", () => {
  const catalogue = loadCatalogue();
  assert.equal(canMakeKeystores(null, "android"), true);
  assert.equal(canMakeKeystores(catalogue, "android"), true);
  assert.equal(canMakeKeystores({ ...catalogue, environment: { android: { keytool: false } } }, "android"), false);
  assert.equal(canMakeKeystores({ ...catalogue, environment: { android: { keytool: true } } }, "android"), true);
});
