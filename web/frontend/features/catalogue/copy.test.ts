import assert from "node:assert/strict";
import { test } from "node:test";
import { type Action, type ConfigState, initialState, reducer } from "../configurator/state.ts";
import { hapticsHint, languagesHint, producesNote } from "../configurator/uiComponents.ts";
import { disabledFeatures, effectiveUiComponents, featureCopy, fieldCopy, moduleCopy, presetDescription, uiComponentsOption } from "./options.ts";
import { androidTarget, loadCatalogue, lockedCatalogue } from "./testing.ts";
import type { Catalogue, Target } from "./types.ts";

/**
 * What a project without UI components lacks: the harness the option owns, a tab bar and its tabs,
 * the navigation shell, a home screen or a scaffolded home module, core/ui's list component, drawn
 * screens and their rows, the onboarding pages and the permission prompt at their end, entry gates
 * a link waits behind, and the controls that play a haptic tick. A browser's Custom Tab and the
 * inspector's own tabs are no such promise: "a Performance tab in the inspector" stays. The
 * generator's test_android_noui finds which features need the option's words; this list keeps those
 * words, and every text left as it is, from promising these again.
 */
const MISSING = [
  /tools\/perf/,
  /tools\/adaptive/,
  /\b(?<!(?:Custom|Performance) )tabs?\b/i,
  /navigation shell/i,
  /home screen/i,
  /home module/i,
  /list component/i,
  /\bscreens\b/i,
  /\brows?\b/i,
  /walkthrough/i,
  /working screen/i,
  /already drawn/i,
  /language picker/i,
  /end of onboarding/i,
  /held until[^.]*\b(?:onboarding|sign-in)\b/i,
  /selection changes|switch flips|pull reaches/i,
];

function copyOf(target: Target, uiComponents: boolean): { name: string; text: string }[] {
  const disabled = disabledFeatures(target, uiComponents);
  return [
    { name: "feature modules", text: moduleCopy(target, uiComponents) ?? "" },
    { name: "haptics", text: hapticsHint(target, uiComponents) },
    { name: "languages", text: languagesHint(target, uiComponents) },
    ...target.presets.map((preset) => ({ name: `preset ${preset.key}`, text: presetDescription(target, uiComponents, preset) })),
    ...target.features
      .filter((feature) => !feature.implied_only && !disabled.has(feature.key))
      .map((feature) => {
        const copy = featureCopy(target, uiComponents, feature);
        return { name: `feature ${feature.key}`, text: `${copy.headline} ${copy.description}` };
      }),
  ];
}

function assertPromisesNothingMissing(target: Target, uiComponents: boolean): void {
  for (const { name, text } of copyOf(target, uiComponents)) {
    for (const missing of MISSING) {
      assert.doesNotMatch(text, missing, name);
    }
  }
}

test("with the box ticked, no preset or feature promises what a project without UI components lacks", () => {
  assertPromisesNothingMissing(androidTarget(), false);
});

test("a setup-only site, whatever the visitor asked for, promises none of it either", () => {
  const target = androidTarget(lockedCatalogue());
  assertPromisesNothingMissing(target, effectiveUiComponents(target, true));
});

test("with the box unticked every preset and feature reads as the catalogue writes it", () => {
  const target = androidTarget(loadCatalogue());
  for (const preset of target.presets) {
    assert.equal(presetDescription(target, true, preset), preset.description, preset.key);
  }
  for (const feature of target.features) {
    assert.deepEqual(featureCopy(target, true, feature), { headline: feature.headline, description: feature.description }, feature.key);
  }
  const performance = target.features.find((feature) => feature.key === "performance");
  assert.ok(performance);
  assert.match(performance.description, /tools\/perf/, "the harness is still promised where a project has it");
});

test("with the box ticked a feature module reads as such a project gets it; unticked the site keeps its own words", () => {
  const target = androidTarget();
  const option = uiComponentsOption(target);
  assert.ok(option?.module_copy);
  assert.equal(moduleCopy(target, false), option.module_copy);
  assert.match(option.module_copy, /placeholder routes/);
  assert.equal(moduleCopy(target, true), null);
  const locked = androidTarget(lockedCatalogue());
  assert.equal(moduleCopy(locked, effectiveUiComponents(locked, true)), option.module_copy);
});

test("the option describes only features it keeps, and presets the target has", () => {
  const target = androidTarget();
  const option = uiComponentsOption(target);
  assert.ok(option);
  const disabled = disabledFeatures(target, false);
  const keys = new Set(target.features.filter((feature) => !feature.implied_only).map((feature) => feature.key));
  for (const { feature } of option.feature_copy) {
    assert.ok(keys.has(feature) && !disabled.has(feature), feature);
  }
  const presets = new Set(target.presets.map((preset) => preset.key));
  for (const { preset } of option.preset_copy) {
    assert.ok(presets.has(preset), preset);
  }
});

test("with the box ticked the haptics toggle says what the value sets; unticked the site keeps its own words", () => {
  const target = androidTarget();
  const option = uiComponentsOption(target);
  assert.ok(option);
  const own = option.field_copy.find((entry) => entry.field === "haptics_enabled")?.description;
  assert.ok(own, "the option says what haptics_enabled does without components");
  assert.match(own, /AppTheme\.hapticsEnabled/);
  assert.equal(fieldCopy(target, false, "haptics_enabled"), own);
  assert.equal(hapticsHint(target, false), own);
  assert.equal(fieldCopy(target, true, "haptics_enabled"), null);
  assert.match(hapticsHint(target, true), /a switch flips/);
  const locked = androidTarget(lockedCatalogue());
  assert.equal(hapticsHint(locked, effectiveUiComponents(locked, true)), own);
});

function run(source: Catalogue, state: ConfigState, ...actions: Action[]): ConfigState {
  return actions.reduce((current, action) => reducer(current, action, source), state);
}

test("on a setup-only site the Project step's summary names no tab and no home module, whatever was picked", () => {
  const locked = lockedCatalogue();
  const fresh = initialState(locked, "android");
  assert.equal(fresh.uiComponents, false);
  const states: Record<string, ConfigState> = {
    "the default answers": fresh,
    "the lean preset": run(locked, fresh, { type: "preset", key: "lean" }),
    "every feature and two modules": run(locked, fresh, { type: "preset", key: "everything" }, { type: "modules", value: "orders, wallet" }),
    "six modules": run(locked, fresh, { type: "modules", value: "a, b, c, d, e, f" }),
  };
  for (const [name, state] of Object.entries(states)) {
    const note = producesNote(state);
    assert.match(note, /blank start screen/, name);
    for (const missing of MISSING) assert.doesNotMatch(note, missing, name);
  }
});

test("with UI components the summary still lists the tabs, and the home module scaffolded when nothing gives one", () => {
  const catalogue = loadCatalogue();
  const standard = initialState(catalogue, "android");
  assert.equal(standard.uiComponents, true);
  assert.equal(producesNote(standard), "Tabs: Home · Settings");
  assert.equal(
    producesNote(run(catalogue, standard, { type: "preset", key: "lean" })),
    "Tabs: Home. Nothing you picked provides a tab, so the project gets a home module with an empty screen to build from.",
  );
});
