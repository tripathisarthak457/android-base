import assert from "node:assert/strict";
import { test } from "node:test";
import { selectableTargets } from "../catalogue/platforms.ts";
import { loadCatalogue, threePlatformCatalogue } from "../catalogue/testing.ts";
import { draftToStore, isUntouched, parseDraft, serializeDraft } from "./draft.ts";
import { toSpecV2 } from "./request.ts";
import { presetFeatures } from "./resolve.ts";
import {
  type Action,
  type ConfigState,
  type StartOverUndo,
  type Step,
  drawsGlassBar,
  enabledTarget,
  findTarget,
  initialState,
  moduleNames,
  reducer,
  snapshotForUndo,
  stepOf,
  suggestPackage,
  typeface,
} from "./state.ts";
import { validateSpec } from "./validation/validate.ts";

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
  assert.equal(suggestPackage("", catalogue.rules), "com.example.myapp");
});

test("the package an app name suggests is one the generator accepts", () => {
  const keywords = Object.values(catalogue.rules.reserved_words)
    .flat()
    .filter((word) => /^[a-z]/.test(word))
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1));
  for (const appName of [...keywords, "Abcdefghij".repeat(5), "42 Switch"]) {
    const state = run(initialState(catalogue), { type: "appName", value: appName });
    const errors = validateSpec(toSpecV2(state, catalogue), catalogue, "android");
    assert.equal(errors.get("package_name"), undefined, `${appName} suggests ${state.packageName}`);
  }
  assert.equal(suggestPackage("Switch", catalogue.rules), "com.example.switchapp");
});

test("an untick that takes dependents with it can be undone", () => {
  let state = run(initialState(catalogue), { type: "preset", key: "everything" });
  const everything = state.features;
  state = run(state, { type: "feature", key: "analytics" });
  assert.equal(state.hint?.on, false);
  assert.deepEqual([...(state.hint?.alsoRemoved ?? [])].sort(), ["crashlytics", "firebaseanalytics"]);
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

test("an empty typeface is the target's default, as the generator reads it", () => {
  const android = findTarget(catalogue, "android");
  const state = run(initialState(catalogue), { type: "fontName", value: "" });
  assert.equal(validateSpec(toSpecV2(state, catalogue), catalogue, "android").has("font_name"), false);
  assert.equal(typeface(state.fontName, android), android.defaults.font_name);
  assert.equal(typeface("   ", android), android.defaults.font_name);
  assert.equal(typeface("Inter ", android), "Inter");
});

test("only available and beta targets can be picked", () => {
  const three = threePlatformCatalogue();
  assert.deepEqual(selectableTargets(three).map((target) => target.key), ["android", "multiplatform"]);
  const onMultiplatform = reducer(initialState(three), { type: "target", target: "multiplatform" }, three);
  assert.equal(reducer(onMultiplatform, { type: "target", target: "ios" }, three), onMultiplatform, "a coming-soon target is not switched to");
  assert.equal(parseDraft(JSON.stringify({ version: 1, state: { target: "ios", appName: "Trail" } }), three)?.target, "android");
});

test("a target the server turned off gives way to one it generates", () => {
  const three = threePlatformCatalogue();
  assert.equal(enabledTarget(three, "multiplatform", null), "multiplatform", "nothing is known before the server answers");
  assert.equal(enabledTarget(three, "multiplatform", { android: true, multiplatform: true }), "multiplatform");
  assert.equal(enabledTarget(three, "multiplatform", { android: true, multiplatform: false }), "android");
  assert.equal(enabledTarget(three, "android", { android: false, multiplatform: true }), "multiplatform");
  assert.equal(enabledTarget(three, "android", { android: false, multiplatform: false }), "android", "with nothing to move to, it stays");
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
  assert.deepEqual(state.features, ["coil", "database", "network"]);
});

test("only a design style with a glass bar says where glass blurs, not the liquidglass feature alone", () => {
  const everything = run(initialState(catalogue), { type: "preset", key: "everything" });
  assert.ok(everything.features.includes("liquidglass"));
  assert.notEqual(catalogue.tokens.styles[everything.designStyle]?.bar, "Glass");
  assert.equal(drawsGlassBar(everything, catalogue), false);
  assert.equal(drawsGlassBar(run(initialState(catalogue), { type: "feature", key: "liquidglass" }), catalogue), false);

  const withGlass = loadCatalogue();
  withGlass.tokens.styles.Glass = { ...withGlass.tokens.styles[everything.designStyle]!, bar: "Glass" };
  const android = withGlass.targets[0]!;
  android.design_styles = [...android.design_styles, { key: "Glass", title: "Glass", description: "", requires: ["liquidglass"] }];
  assert.equal(drawsGlassBar(reducer(everything, { type: "designStyle", value: "Glass" }, withGlass), withGlass), true);
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
  // Several features bring Firebase; from everything, unticking them all is what leaves it out.
  const bringers = findTarget(catalogue, "android").features.filter((feature) => feature.requires.includes("firebase"));
  const fromEverything = run(
    initialState(catalogue),
    { type: "preset", key: "everything" },
    ...bringers.map((feature) => ({ type: "feature" as const, key: feature.key })),
  );
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
  const typedPackage = run(fresh, { type: "packageName", value: "com.trail.notes" });
  assert.notEqual(draftToStore(typedPackage, catalogue), null, "a typed package is an answer");
  const retyped = run(typedPackage, { type: "packageName", value: fresh.packageName });
  assert.equal(draftToStore(retyped, catalogue), null, "a package typed back to the suggestion is not an answer");
  assert.equal(draftToStore(run(fresh, { type: "fontName", value: "" }), catalogue), null, "an emptied typeface is the default");
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

/** What Start over keeps for its Undo, asked for as `useConfigurator` asks. */
function keepForUndo(kept: StartOverUndo | null, state: ConfigState, step: Step = "identity"): StartOverUndo | null {
  return snapshotForUndo(kept, { state, step, identityAt: null }, isUntouched(state, catalogue));
}

test("Start over pressed twice still undoes to the answers the first press cleared", () => {
  const answers = run(initialState(catalogue), { type: "appName", value: "Trail Notes" }, { type: "preset", key: "lean" });
  let kept = keepForUndo(null, answers, "look");
  let state = run(answers, { type: "reset" });
  assert.equal(kept?.revision, state.revision, "Undo is offered after the reset");
  kept = keepForUndo(kept, state, "platform");
  state = run(state, { type: "reset" });
  assert.equal(kept?.revision, state.revision, "and still after the second");
  assert.ok(kept);
  assert.equal(kept.step, "look");
  state = run(state, { type: "restore", state: kept.state });
  assert.equal(state.appName, "Trail Notes");
  assert.deepEqual(state.features, answers.features);
});

test("an edit after Start over ends its Undo, and the next Start over keeps the new answers", () => {
  const first = run(initialState(catalogue), { type: "appName", value: "Trail Notes" });
  let kept = keepForUndo(null, first);
  const edited = run(first, { type: "reset" }, { type: "appName", value: "Ledger" });
  assert.notEqual(kept?.revision, edited.revision, "the edit ends the offer");
  kept = keepForUndo(kept, edited);
  assert.equal(kept?.state.appName, "Ledger");
  assert.equal(kept?.revision, run(edited, { type: "reset" }).revision);
});

test("Start over on answers nobody changed offers no Undo", () => {
  assert.equal(keepForUndo(null, initialState(catalogue), "look"), null);
  const retyped = run(initialState(catalogue), { type: "appName", value: "Ledger" }, { type: "appName", value: "My App" });
  assert.equal(keepForUndo(null, retyped), null, "nor on answers typed back to the defaults");
});

test("Start over after typing only signing details offers Undo, which brings them back", () => {
  const signing = { enabled: true, organisation: "Trail Co", country: "DE", passwords: { dev: "s3cret-pass" } };
  const typed = run(initialState(catalogue), { type: "signing", value: signing });
  assert.equal(draftToStore(typed, catalogue), null, "the draft never holds signing details");
  const kept = keepForUndo(null, typed, "build");
  assert.ok(kept, "so Start over has to keep them itself");
  const restored = run(typed, { type: "reset" }, { type: "restore", state: kept.state });
  assert.deepEqual(restored.signing, signing);
  const cleared = run(typed, { type: "signing", value: { enabled: false, organisation: " ", country: "US", passwords: { dev: "" } } });
  assert.equal(keepForUndo(null, cleared), null, "signing details cleared by hand are nothing to undo");
});
