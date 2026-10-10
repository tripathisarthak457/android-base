import assert from "node:assert/strict";
import { test } from "node:test";
import { loadCatalogue, threePlatformCatalogue } from "../catalogue/testing.ts";
import { draftToStore, parseDraft, serializeDraft } from "./draft.ts";
import { toSpecV2 } from "./request.ts";
import { presetFeatures } from "./resolve.ts";
import { type Action, type ConfigState, type Step, initialState, moduleNames, reducer, selectableTargets, stepOf, suggestPackage } from "./state.ts";

const catalogue = loadCatalogue();

function run(state: ConfigState, ...actions: Action[]): ConfigState {
  return actions.reduce((current, action) => reducer(current, action, catalogue), state);
}

test("the initial answers are the catalogue's defaults", () => {
  const state = initialState(catalogue);
  const android = catalogue.targets[0];
  assert.equal(state.target, "android");
  assert.deepEqual(state.features, [...presetFeatures(android!, "standard")].sort());
  assert.equal(state.settings["android.min_sdk"], 26);
  assert.deepEqual(state.settings["android.api_base_urls"], {});
});

test("the package follows the app name until it is typed", () => {
  let state = run(initialState(catalogue), { type: "appName", value: "Trail Notes" });
  assert.equal(state.packageName, "com.example.trailnotes");
  state = run(state, { type: "packageName", value: "io.trail.app" }, { type: "appName", value: "Trail" });
  assert.equal(state.packageName, "io.trail.app");
  assert.equal(suggestPackage(""), "com.example.myapp");
});

test("an untick that takes dependents with it can be undone", () => {
  let state = run(initialState(catalogue), { type: "preset", key: "everything" });
  const everything = state.features;
  state = run(state, { type: "feature", key: "analytics" });
  assert.equal(state.hint?.on, false);
  assert.deepEqual([...(state.hint?.alsoRemoved ?? [])].sort(), ["firebase", "push"]);
  state = run(state, { type: "undoToggle" });
  assert.deepEqual(state.features, everything);
  assert.equal(state.hint, null);
});

test("any other edit drops the undo hint", () => {
  const state = run(initialState(catalogue), { type: "feature", key: "network" }, { type: "fontName", value: "Inter" });
  assert.equal(state.hint, null);
});

test("every edit bumps the revision, so a finished download stops describing the answers", () => {
  const state = initialState(catalogue);
  assert.equal(run(state, { type: "haptics", value: false }).revision, state.revision + 1);
});

test("module names are normalised the way they will be generated", () => {
  assert.deepEqual(moduleNames(" Orders, wallet-history ,, Trip  plans"), ["orders", "wallet_history", "trip_plans"]);
});

test("only available and beta targets can be picked", () => {
  assert.deepEqual(selectableTargets(threePlatformCatalogue()).map((target) => target.key), ["android", "multiplatform"]);
});

test("switching platform keeps compatible answers and drops the rest", () => {
  const three = threePlatformCatalogue();
  let state = initialState(three);
  state = reducer(state, { type: "appName", value: "Trail Notes" }, three);
  state = reducer(state, { type: "language", tag: "hi" }, three);
  state = reducer(state, { type: "language", tag: "es" }, three);
  state = reducer(state, { type: "signing", value: { enabled: true } }, three);
  state = reducer(state, { type: "target", target: "multiplatform" }, three);
  assert.equal(state.target, "multiplatform");
  assert.equal(state.appName, "Trail Notes");
  assert.deepEqual(state.languages, ["es"]);
  assert.equal(state.signing.enabled, false);
  assert.deepEqual(state.features, ["catalog", "network", "sample"], "an untouched default preset becomes the new default");
});

test("a customised feature set keeps what the new platform also has", () => {
  const three = threePlatformCatalogue();
  let state = initialState(three);
  state = reducer(state, { type: "preset", key: "lean" }, three);
  state = reducer(state, { type: "feature", key: "database" }, three);
  state = reducer(state, { type: "target", target: "multiplatform" }, three);
  assert.deepEqual(state.features, ["catalog", "database", "network", "room", "sample"]);
});

test("a design style that needs a feature turns it on", () => {
  const withGlass = loadCatalogue();
  const android = withGlass.targets[0]!;
  android.design_styles = [...android.design_styles, { key: "Glass", title: "Glass", description: "", requires: ["coil"] }];
  let state = reducer(initialState(withGlass), { type: "preset", key: "lean" }, withGlass);
  state = reducer(state, { type: "designStyle", value: "Glass" }, withGlass);
  assert.equal(state.designStyle, "Glass");
  assert.ok(state.features.includes("coil"));
  assert.equal(reducer(state, { type: "designStyle", value: "Brutal" }, withGlass), state);
});

test("Firebase is not sent once the feature that brought it is unticked", () => {
  const tickedAndUnticked = run(initialState(catalogue), { type: "preset", key: "lean" }, { type: "feature", key: "push" }, { type: "feature", key: "push" });
  const fromEverything = run(initialState(catalogue), { type: "preset", key: "everything" }, { type: "feature", key: "push" });
  for (const state of [tickedAndUnticked, fromEverything]) {
    assert.deepEqual((toSpecV2(state, catalogue).features as string[]).filter((key) => key === "push" || key === "firebase"), []);
  }
});

test("a draft that kept a hidden feature on its own loses it", () => {
  const state = parseDraft(JSON.stringify({ version: 1, state: { target: "android", features: ["firebase", "sample"] } }), catalogue);
  assert.deepEqual(state?.features, ["network", "sample"]);
});

test("a design style keeps what it needs through a preset and a restored draft, and leaves with it", () => {
  const withGlass = loadCatalogue();
  const android = withGlass.targets[0]!;
  android.design_styles = [...android.design_styles, { key: "Glass", title: "Glass", description: "", requires: ["coil"] }];
  let state = reducer(initialState(withGlass), { type: "designStyle", value: "Glass" }, withGlass);
  state = reducer(state, { type: "preset", key: "lean" }, withGlass);
  assert.ok(state.features.includes("coil"));
  assert.ok(parseDraft(serializeDraft(state), withGlass)?.features.includes("coil"));

  const unticked = reducer(state, { type: "feature", key: "coil" }, withGlass);
  assert.equal(unticked.features.includes("coil"), false);
  assert.equal(unticked.designStyle, android.defaults.design_style);
  assert.equal(unticked.hint?.previousStyle, "Glass");
  const undone = reducer(unticked, { type: "undoToggle" }, withGlass);
  assert.equal(undone.designStyle, "Glass");
  assert.deepEqual(undone.features, state.features);
});

test("only answers that differ from the defaults are stored", () => {
  const fresh = initialState(catalogue);
  assert.equal(draftToStore(fresh, catalogue), null, "a visit that only looks stores nothing");
  const edited = run(fresh, { type: "appName", value: "Trail Notes" });
  assert.equal(draftToStore(edited, catalogue), serializeDraft(edited));
  const startedOver = run(edited, { type: "reset" });
  assert.equal(draftToStore(startedOver, catalogue), null, "Start over leaves the key removed");
  assert.equal(draftToStore(run(edited, { type: "appName", value: "My App" }), catalogue), null);
  const typedAndCleared = run(
    fresh,
    { type: "setting", key: "android.api_base_urls", value: { dev: "h" } },
    { type: "setting", key: "android.api_base_urls", value: { dev: "" } },
  );
  assert.equal("api_base_urls" in (toSpecV2(typedAndCleared, catalogue).android as object), false);
  assert.equal(draftToStore(typedAndCleared, catalogue), null, "a flavour URL typed and cleared is not an answer");
  for (const [key, typed] of [
    ["android.deeplink_scheme", "trail"],
    ["android.deeplink_host", "trail.app"],
    ["android.version_name", "2.0.0"],
  ] as const) {
    const cleared = run(fresh, { type: "setting", key, value: typed }, { type: "setting", key, value: "" });
    assert.equal(draftToStore(cleared, catalogue), null, `${key} typed and cleared is not an answer`);
    assert.notEqual(draftToStore(run(fresh, { type: "setting", key, value: typed }), catalogue), null, `${key} typed is an answer`);
  }
});

test("a draft round-trips without the signing passwords", () => {
  let state = run(
    initialState(catalogue),
    { type: "appName", value: "Café Notes" },
    { type: "displayName", value: "Café" },
    { type: "setting", key: "android.api_base_urls", value: { prod: "https://api.trail.app/" } },
    { type: "signing", value: { enabled: true, passwords: { prod: "secret-one" } } },
  );
  const text = serializeDraft(state);
  assert.equal(text.includes("secret-one"), false);
  assert.equal(text.includes("signing"), false);
  const restored = parseDraft(text, catalogue);
  assert.ok(restored);
  state = { ...state, signing: restored.signing, hint: null, revision: restored.revision };
  assert.deepEqual(restored, state);
});

test("a draft from an older catalogue loses only what no longer exists", () => {
  const text = JSON.stringify({
    version: 1,
    state: {
      target: "android",
      appName: "Old",
      features: ["telepathy", "sample"],
      languages: ["xx", "es"],
      designStyle: "Brutal",
      settings: { "android.min_sdk": 30, "android.removed": 1, "android.version_name": { bad: 1 } },
    },
  });
  const state = parseDraft(text, catalogue);
  assert.ok(state);
  assert.equal(state.appName, "Old");
  assert.deepEqual(state.features, ["network", "sample"]);
  assert.deepEqual(state.languages, ["es"]);
  assert.equal(state.designStyle, "Utility");
  assert.equal(state.settings["android.min_sdk"], 30);
  assert.equal("android.removed" in state.settings, false);
  assert.equal(state.settings["android.version_name"], "1.0.0");
});

test("anything that is not a draft reads as none", () => {
  for (const text of [null, "", "{", "[]", JSON.stringify({ version: 2, state: {} }), JSON.stringify({ version: 1 })]) {
    assert.equal(parseDraft(text, catalogue), null, String(text));
  }
});

test("an error sends the visitor to the step that edits its field", () => {
  const expected: Record<string, Step> = {
    platform: "platform",
    schema: "platform",
    app_name: "identity",
    display_name: "identity",
    package_name: "identity",
    feature_modules: "identity",
    languages: "identity",
    features: "features",
    design_style: "look",
    accent_colour: "look",
    secondary_colour: "look",
    tertiary_colour: "look",
    font_name: "look",
    mono_font_name: "look",
    motion_style: "motion",
    haptics_enabled: "motion",
    architecture: "build",
  };
  for (const [field, step] of Object.entries(expected)) assert.equal(stepOf(field), step, field);
  for (const field of catalogue.targets[0]!.fields) {
    if (field.key.includes(".")) assert.equal(stepOf(field.key), "build", field.key);
    else assert.ok(field.key in expected, `${field.key} is a new top-level field: say which step edits it`);
  }
});
