import assert from "node:assert/strict";
import { test } from "node:test";
import { disabledFeatures } from "../catalogue/options.ts";
import { SETUP_ONLY, androidTarget, loadCatalogue, lockedCatalogue, threePlatformCatalogue } from "../catalogue/testing.ts";
import type { Catalogue } from "../catalogue/types.ts";
import { draftToStore, parseDraft } from "./draft.ts";
import { toSpecV2 } from "./request.ts";
import { derivePreset, presetFeatures } from "./resolve.ts";
import { type Action, type ConfigState, initialState, reducer, stepOf } from "./state.ts";
import { featureCard, reviewSkipped, uiComponentsBox } from "./uiComponents.ts";
import { validateSpec } from "./validation/validate.ts";

const catalogue = loadCatalogue();
const android = androidTarget(catalogue);
const DISABLED = ["catalog", "collections", "datadisplay", "formfields", "pickers", "liquidglass"];

function run(state: ConfigState, ...actions: Action[]): ConfigState {
  return actions.reduce((current, action) => reducer(current, action, catalogue), state);
}

function runOn(source: Catalogue, state: ConfigState, ...actions: Action[]): ConfigState {
  return actions.reduce((current, action) => reducer(current, action, source), state);
}

const noUi: Action = { type: "uiComponents", value: false };

test("UI components are on by default, so the default answers send the request they always did", () => {
  const state = initialState(catalogue);
  assert.equal(state.uiComponents, true);
  assert.deepEqual(state.skipped, []);
  assert.equal("ui_components" in toSpecV2(state, catalogue), false);
});

test("ticking the box takes out every disabled feature the selection had, and says which", () => {
  const everything = run(initialState(catalogue), { type: "preset", key: "everything" });
  const state = run(everything, noUi);
  assert.equal(state.uiComponents, false);
  for (const key of DISABLED) assert.equal(state.features.includes(key), false, key);
  assert.deepEqual(state.features, everything.features.filter((key) => !DISABLED.includes(key)));
  assert.deepEqual(state.skipped, DISABLED);
});

test("a disabled feature cannot be ticked while the box is ticked, and every other one can", () => {
  const state = run(initialState(catalogue), noUi);
  for (const key of DISABLED) assert.equal(run(state, { type: "feature", key }), state, key);
  const before = state.features.includes("search");
  assert.equal(run(state, { type: "feature", key: "search" }).features.includes("search"), !before);
});

test("each preset is its own list less the disabled features, and is still recognised as itself", () => {
  const disabled = new Set(DISABLED);
  for (const preset of android.presets) {
    const state = run(initialState(catalogue), noUi, { type: "preset", key: preset.key });
    const expected = [...presetFeatures(android, preset.key)].filter((key) => !disabled.has(key)).sort();
    assert.deepEqual(state.features, expected, preset.key);
    assert.deepEqual(state.skipped, DISABLED.filter((key) => presetFeatures(android, preset.key).has(key)), preset.key);
    assert.equal(derivePreset(new Set(state.features), android, disabled), preset.key);
    assert.deepEqual([...validateSpec(toSpecV2(state, catalogue), catalogue, "android")], [], preset.key);
  }
});

test("unticking the box keeps the features and lets the disabled ones be ticked again", () => {
  const ticked = run(initialState(catalogue), { type: "preset", key: "everything" }, noUi);
  const unticked = run(ticked, { type: "uiComponents", value: true });
  assert.equal(unticked.uiComponents, true);
  assert.deepEqual(unticked.features, ticked.features);
  assert.deepEqual(unticked.skipped, []);
  assert.ok(run(unticked, { type: "feature", key: "catalog" }).features.includes("catalog"));
});

test("design and motion style go back to the neutral defaults and cannot be picked; colours, font and haptics stay", () => {
  const picked = run(
    initialState(catalogue),
    { type: "designStyle", value: "Playful" },
    { type: "motionStyle", value: "Bouncy" },
    { type: "colour", which: "accent", value: "#1F7A5C" },
    { type: "fontName", value: "Inter" },
  );
  const state = run(picked, noUi);
  assert.equal(state.designStyle, android.defaults.design_style);
  assert.equal(state.motionStyle, android.defaults.motion_style);
  assert.equal(run(state, { type: "designStyle", value: "Playful" }), state);
  assert.equal(run(state, { type: "motionStyle", value: "Bouncy" }), state);
  const spec = toSpecV2(run(state, { type: "haptics", value: false }), catalogue);
  assert.equal(spec.accent_colour, "#1F7A5C");
  assert.equal(spec.font_name, "Inter");
  assert.equal(spec.haptics_enabled, false);
  assert.equal(spec.design_style, android.defaults.design_style);
  assert.equal(spec.motion_style, android.defaults.motion_style);
});

test("the site refuses what the generator refuses about the field", () => {
  const base = { schema: 2, platform: "android", app_name: "Trail Notes", package_name: "com.example.trail" };
  assert.deepEqual([...validateSpec({ ...base, ui_components: false }, catalogue, "android")], []);
  assert.deepEqual([...validateSpec({ ...base, ui_components: true }, catalogue, "android")], []);
  assert.deepEqual([...validateSpec({ ...base, ui_components: "no" }, catalogue, "android").keys()], ["ui_components"]);
  const three = threePlatformCatalogue();
  const mp = { ...base, platform: "multiplatform" };
  assert.deepEqual([...validateSpec({ ...mp, ui_components: false }, three, "multiplatform").keys()], ["ui_components"]);
});

test("the draft keeps the box and what it skipped, and reads them back against the catalogue", () => {
  const state = run(initialState(catalogue), { type: "preset", key: "everything" }, noUi);
  const text = draftToStore(state, catalogue);
  assert.ok(text);
  const restored = parseDraft(text, catalogue);
  assert.equal(restored?.uiComponents, false);
  assert.deepEqual(restored?.features, state.features);
  assert.deepEqual(restored?.skipped, DISABLED);

  const forged = JSON.stringify({
    version: 1,
    state: { target: "android", uiComponents: false, features: ["catalog", "network"], designStyle: "Playful", skipped: ["network"] },
  });
  const read = parseDraft(forged, catalogue);
  assert.equal(read?.features.includes("catalog"), false, "a disabled feature never comes back from a draft");
  assert.equal(read?.designStyle, android.defaults.design_style);
  assert.deepEqual(read?.skipped, ["catalog"], "only disabled features are named as skipped");
  assert.equal(parseDraft(JSON.stringify({ version: 1, state: { target: "android" } }), catalogue)?.uiComponents, true);
});

test("a platform without the option has UI components whatever the answers carried", () => {
  const three = threePlatformCatalogue();
  const state = runOn(three, initialState(three), noUi, { type: "target", target: "multiplatform" });
  assert.equal(state.uiComponents, true);
  assert.deepEqual(state.skipped, []);
  assert.equal("ui_components" in toSpecV2(state, three), false);
  assert.equal(runOn(three, state, noUi), state, "the box cannot be ticked there");
});

test("an error on the field sends the visitor to the Features step", () => {
  assert.equal(stepOf("ui_components"), "features");
});

test("the box shows the option's own words, and the note stands in for it on another platform", () => {
  const box = uiComponentsBox(catalogue, android, true);
  assert.equal(box.kind, "box");
  if (box.kind !== "box") return;
  assert.equal(box.label, "No UI components");
  assert.equal(box.ticked, false);
  assert.equal(box.lockedNote, null);
  const ticked = uiComponentsBox(catalogue, android, false);
  assert.equal(ticked.kind === "box" && ticked.ticked, true);

  const three = threePlatformCatalogue();
  const multiplatform = three.targets.find((target) => target.key === "multiplatform");
  assert.ok(multiplatform);
  assert.deepEqual(uiComponentsBox(three, multiplatform, true), {
    kind: "note",
    text: "No UI components is Android-only for now; it comes to Compose Multiplatform and Swift after the Android release.",
  });
});

test("a disabled card is greyed with its reason, and every other card is not", () => {
  for (const feature of android.features) {
    const card = featureCard(catalogue, android, false, feature.key);
    if (DISABLED.includes(feature.key)) {
      assert.equal(card.disabled, true, feature.key);
      assert.equal(card.disabled && card.reason, disabledFeatures(android, false).get(feature.key));
      assert.equal(card.disabled && card.release, null, "no release badge while the site is not locked");
    } else {
      assert.equal(card.disabled, false, feature.key);
    }
    assert.equal(featureCard(catalogue, android, true, feature.key).disabled, false, feature.key);
  }
});

test("Review says what was skipped and why: the disabled features the selection had, then the hidden fields", () => {
  const state = run(initialState(catalogue), noUi);
  assert.deepEqual(reviewSkipped(android, state), [
    { title: "Component catalog app", reason: "The catalog app shows the design system's components, and this project has none." },
    {
      title: "Design style",
      reason: "Your design system decides corners, surfaces and type voice; the theme starts from neutral values.",
    },
    { title: "Motion style", reason: "Your components decide their motion; the theme keeps neutral springs and reduce-motion." },
  ]);
  const lean = run(state, { type: "preset", key: "lean" });
  assert.deepEqual(reviewSkipped(android, lean).map((entry) => entry.title), ["Design style", "Motion style"]);
  assert.deepEqual(reviewSkipped(android, initialState(catalogue)), []);
});

test("the release switch: the box is ticked and fixed with the note, and every request asks for no UI components", () => {
  const locked = lockedCatalogue();
  const target = androidTarget(locked);
  const state = initialState(locked);
  assert.equal(state.uiComponents, false);
  assert.equal(state.features.includes("catalog"), false);
  assert.equal(runOn(locked, state, { type: "uiComponents", value: true }), state, "the box cannot be unticked");
  for (const preset of target.presets) {
    const spec = toSpecV2(runOn(locked, state, { type: "preset", key: preset.key }), locked);
    assert.equal(spec.ui_components, false, preset.key);
    assert.deepEqual((spec.features as string[]).filter((key) => DISABLED.includes(key)), [], preset.key);
  }
  const draft = JSON.stringify({ version: 1, state: { target: "android", uiComponents: true, features: ["catalog"] } });
  assert.equal(parseDraft(draft, locked)?.uiComponents, false, "a draft from before the lock does not undo it");
  assert.equal(draftToStore(state, locked), null, "the locked defaults are no answer to remember");

  const box = uiComponentsBox(locked, target, state.uiComponents);
  assert.equal(box.kind === "box" && box.ticked, true);
  assert.equal(box.kind === "box" && box.lockedNote, SETUP_ONLY.note);
  const card = featureCard(locked, target, state.uiComponents, "catalog");
  assert.equal(card.disabled && card.release, "Coming with the Android release");
});
