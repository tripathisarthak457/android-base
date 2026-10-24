import assert from "node:assert/strict";
import { test } from "node:test";
import {
  COMING_WITH_RELEASE,
  androidOnlyNote,
  disabledFeatures,
  effectiveUiComponents,
  hiddenFields,
  isSetupOnly,
  setupOnlyNote,
  uiComponentsOption,
} from "./options.ts";
import { SETUP_ONLY, androidTarget, loadCatalogue, lockedCatalogue, threePlatformCatalogue } from "./testing.ts";

const catalogue = loadCatalogue();
const android = androidTarget(catalogue);

test("the android target offers the option, unticked, with a box that stands for false", () => {
  const option = uiComponentsOption(android);
  assert.ok(option);
  assert.equal(option.default, true);
  assert.equal(option.checkbox.ticked_value, false);
  assert.equal(option.locked, null, "only the release branch locks it");
});

test("every feature the option disables exists, can be ticked, and has a reason", () => {
  const option = uiComponentsOption(android);
  assert.ok(option);
  const byKey = new Map(android.features.map((feature) => [feature.key, feature]));
  assert.ok(option.disables.length > 0);
  for (const { feature, reason } of option.disables) {
    assert.equal(byKey.get(feature)?.implied_only, false, feature);
    assert.ok(reason.trim().length > 0, feature);
  }
  const disabled = new Set(option.disables.map((entry) => entry.feature));
  for (const feature of android.features) {
    if (disabled.has(feature.key)) continue;
    assert.deepEqual(feature.implies.filter((key) => disabled.has(key)), [], `${feature.key} would bring a disabled feature back`);
  }
});

test("disabled features and hidden fields apply only without UI components", () => {
  assert.equal(disabledFeatures(android, true).size, 0);
  assert.equal(hiddenFields(android, true).size, 0);
  const disabled = disabledFeatures(android, false);
  assert.deepEqual([...disabled.keys()], ["catalog", "collections", "datadisplay", "formfields", "pickers", "liquidglass"]);
  assert.deepEqual([...hiddenFields(android, false).keys()], ["design_style", "motion_style"]);
});

test("a target without the option always has UI components, and the lock overrides the visitor", () => {
  const multiplatform = threePlatformCatalogue().targets.find((target) => target.key === "multiplatform");
  assert.ok(multiplatform);
  assert.equal(uiComponentsOption(multiplatform), null);
  assert.equal(effectiveUiComponents(multiplatform, false), true);
  assert.equal(disabledFeatures(multiplatform, false).size, 0);

  assert.equal(effectiveUiComponents(android, false), false);
  assert.equal(effectiveUiComponents(android, true), true);
  const locked = androidTarget(lockedCatalogue());
  assert.equal(effectiveUiComponents(locked, true), false);
  assert.equal(effectiveUiComponents(locked, false), false);
});

test("only a locked catalogue is setup-only, and it says why in the lock's words", () => {
  assert.equal(isSetupOnly(catalogue), false);
  assert.equal(setupOnlyNote(catalogue), null);
  assert.equal(isSetupOnly(lockedCatalogue()), true);
  assert.equal(setupOnlyNote(lockedCatalogue()), SETUP_ONLY.note);
  assert.equal(isSetupOnly(lockedCatalogue({ ...SETUP_ONLY, value: true })), false, "a lock to UI components is not setup-only");
  assert.equal(COMING_WITH_RELEASE, "Coming with the Android release");
});

test("multiplatform and iOS say the option is Android-only, and Android says nothing", () => {
  const three = threePlatformCatalogue();
  const note = "No UI components is Android-only for now; it comes to Compose Multiplatform and Swift after the Android release.";
  assert.equal(androidOnlyNote(three, "multiplatform"), note);
  assert.equal(androidOnlyNote(three, "ios"), note);
  assert.equal(androidOnlyNote(catalogue, "ios"), note, "also for a platform the catalogue only announces");
  assert.equal(androidOnlyNote(three, "android"), null);
});
