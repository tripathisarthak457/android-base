import assert from "node:assert/strict";
import { test } from "node:test";
import { disabledFeatures, effectiveUiComponents, featureCopy, moduleCopy, presetDescription, uiComponentsOption } from "./options.ts";
import { androidTarget, loadCatalogue, lockedCatalogue } from "./testing.ts";
import type { Target } from "./types.ts";

/**
 * What a project without UI components lacks: the harness the option owns, a tab bar and its tabs,
 * the navigation shell, a home screen, core/ui's list component, drawn screens and their rows, the
 * onboarding pages and the permission prompt at their end, and entry gates a link waits behind. A
 * browser's Custom Tab and the inspector's own tabs are no such promise: "a Performance tab in the
 * inspector" stays. The generator's test_android_noui finds which features need the option's words;
 * this list keeps those words, and every text left as it is, from promising these again.
 */
const MISSING = [
  /tools\/perf/,
  /tools\/adaptive/,
  /\b(?<!(?:Custom|Performance) )tabs?\b/i,
  /navigation shell/i,
  /home screen/i,
  /list component/i,
  /\bscreens\b/i,
  /\brows?\b/i,
  /walkthrough/i,
  /working screen/i,
  /already drawn/i,
  /language picker/i,
  /end of onboarding/i,
  /held until[^.]*\b(?:onboarding|sign-in)\b/i,
];

function copyOf(target: Target, uiComponents: boolean): { name: string; text: string }[] {
  const disabled = disabledFeatures(target, uiComponents);
  return [
    { name: "feature modules", text: moduleCopy(target, uiComponents) ?? "" },
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
