import assert from "node:assert/strict";
import { test } from "node:test";
import { CLIENT_PREFIX, clientHeader } from "../../lib/api.ts";
import { disabledFeatures } from "../catalogue/options.ts";
import { androidTarget, loadCatalogue, readJson, threePlatformCatalogue } from "../catalogue/testing.ts";
import { CONFIG_MS_MAX, SESSION_ID, deepLinkScheme, sessionHeaders, toSpecV2 } from "./request.ts";
import { presetFeatures } from "./resolve.ts";
import { type Action, type ConfigState, choiceValue, initialState, reducer } from "./state.ts";
import { validateSpec } from "./validation/validate.ts";

const catalogue = loadCatalogue();

function run(state: ConfigState, ...actions: Action[]): ConfigState {
  return actions.reduce((current, action) => reducer(current, action, catalogue), state);
}

test("the default answers make a spec the generator accepts", () => {
  const spec = toSpecV2(initialState(catalogue), catalogue);
  assert.equal(spec.schema, 2);
  assert.equal(spec.platform, "android");
  assert.deepEqual([...validateSpec(spec, catalogue, "android")], []);
});

test("blank optional answers are left out, so the generator keeps its own defaults", () => {
  const spec = toSpecV2(initialState(catalogue), catalogue);
  assert.equal("display_name" in spec, false);
  assert.equal("secondary_colour" in spec, false);
  const android = spec.android as Record<string, unknown>;
  assert.equal("api_base_urls" in android, false);
  assert.equal("deeplink_host" in android, false);
  assert.equal("keystores" in android, false);
});

test("settings for a feature that is off are not sent", () => {
  const state = run(
    initialState(catalogue),
    { type: "preset", key: "lean" },
    { type: "setting", key: "android.web_socket_urls", value: { dev: "wss://dev.trail.app/ws" } },
    { type: "setting", key: "android.api_base_urls", value: { prod: "https://api.trail.app/", dev: " " } },
  );
  const android = toSpecV2(state, catalogue).android as Record<string, unknown>;
  assert.equal("web_socket_urls" in android, false);
  assert.deepEqual(android.api_base_urls, { prod: "https://api.trail.app/" });
});

test("targetSdk follows minSdk up and compileSdk is never below it", () => {
  const state = run(
    initialState(catalogue),
    { type: "setting", key: "android.target_sdk", value: 30 },
    { type: "setting", key: "android.min_sdk", value: 33 },
  );
  const android = toSpecV2(state, catalogue).android as Record<string, number>;
  assert.equal(android.min_sdk, 33);
  assert.equal(android.target_sdk, 33);
  assert.ok((android.compile_sdk ?? 0) >= 33);
});

test("signing keys are described only when asked for, and pass the generator's rules", () => {
  const passwords = { dev: "aaaaaa1", staging: "bbbbbb2", prod: "cccccc3", playstore: "dddddd4" };
  const state = run(
    initialState(catalogue),
    { type: "appName", value: "Trail Notes" },
    { type: "signing", value: { enabled: true, organisation: "Trail Ltd", country: "in", passwords } },
  );
  const spec = toSpecV2(state, catalogue);
  const keystores = (spec.android as { keystores: Record<string, string>[] }).keystores;
  assert.deepEqual(keystores.map((keystore) => [keystore.name, keystore.alias, keystore.country]), [
    ["dev", "trailnotes-dev", "IN"],
    ["staging", "trailnotes-staging", "IN"],
    ["prod", "trailnotes-prod", "IN"],
    ["playstore", "trailnotes-playstore", "IN"],
  ]);
  assert.deepEqual([...validateSpec(spec, catalogue, "android")], []);
  const weak = run(state, { type: "signing", value: { passwords: { ...passwords, prod: "123" } } });
  assert.deepEqual([...validateSpec(toSpecV2(weak, catalogue), catalogue, "android").keys()], ["android.keystores"]);
});

/** Every field a request had before the "No UI components" box, in the order it was sent. */
const UI_REQUEST_FIELDS = [
  "schema",
  "platform",
  "app_name",
  "package_name",
  "architecture",
  "features",
  "feature_modules",
  "design_style",
  "motion_style",
  "accent_colour",
  "font_name",
  "mono_font_name",
  "haptics_enabled",
  "languages",
  "android",
];

test("a UI request is the one the site sent before the box existed, for every preset", () => {
  const android = androidTarget(catalogue);
  for (const preset of android.presets) {
    const state = run(initialState(catalogue), { type: "preset", key: preset.key });
    const spec = toSpecV2(state, catalogue);
    assert.deepEqual(Object.keys(spec), UI_REQUEST_FIELDS, preset.key);
    assert.deepEqual(spec.features, [...presetFeatures(android, preset.key)].sort(), preset.key);
    const backAgain = run(state, { type: "uiComponents", value: false }, { type: "uiComponents", value: true }, { type: "preset", key: preset.key });
    assert.equal(JSON.stringify(toSpecV2(backAgain, catalogue)), JSON.stringify(spec), preset.key);
  }
});

test("a No UI request carries the field and none of the disabled features, for every preset", () => {
  const android = androidTarget(catalogue);
  const disabled = disabledFeatures(android, false);
  assert.ok(disabled.size > 0);
  for (const preset of android.presets) {
    const state = run(initialState(catalogue), { type: "uiComponents", value: false }, { type: "preset", key: preset.key });
    const spec = toSpecV2(state, catalogue);
    assert.equal(spec.ui_components, false, preset.key);
    assert.deepEqual((spec.features as string[]).filter((key) => disabled.has(key)), [], preset.key);
    assert.deepEqual(spec.features, [...presetFeatures(android, preset.key)].filter((key) => !disabled.has(key)).sort(), preset.key);
    assert.deepEqual([...validateSpec(spec, catalogue, "android")], [], preset.key);
  }
});

test("languages go out in the catalogue's order", () => {
  const state = run(initialState(catalogue), { type: "language", tag: "pt-BR" }, { type: "language", tag: "de" });
  assert.deepEqual(toSpecV2(state, catalogue).languages, ["de", "pt-BR"]);
});

test("a multiplatform spec carries both settings blocks", () => {
  const three = threePlatformCatalogue();
  const state = reducer(initialState(three), { type: "target", target: "multiplatform" }, three);
  const spec = toSpecV2(state, three);
  assert.equal(spec.platform, "multiplatform");
  assert.deepEqual(spec.ios, { deployment_target: "17.0" });
  assert.equal(typeof spec.android, "object");
  assert.deepEqual([...validateSpec(spec, three, "multiplatform")], []);
});

test("a picked choice keeps its type, so iOS 18 is sent as the string the generator checks for", () => {
  const three = threePlatformCatalogue();
  const fields = three.targets.find((target) => target.key === "multiplatform")?.fields ?? [];
  const deploymentTarget = fields.find((field) => field.key === "ios.deployment_target");
  const minSdk = fields.find((field) => field.key === "android.min_sdk");
  assert.ok(deploymentTarget && minSdk);
  const ios18 = choiceValue(deploymentTarget, "18.0");
  const api33 = choiceValue(minSdk, "33");
  assert.equal(ios18, "18.0");
  assert.equal(api33, 33);
  assert.equal(choiceValue(deploymentTarget, "19.0"), undefined);

  let state = reducer(initialState(three), { type: "target", target: "multiplatform" }, three);
  state = reducer(state, { type: "setting", key: "ios.deployment_target", value: ios18 }, three);
  state = reducer(state, { type: "setting", key: "android.min_sdk", value: api33 }, three);
  const spec = toSpecV2(state, three);
  assert.deepEqual(spec.ios, { deployment_target: "18.0" });
  assert.equal((spec.android as Record<string, unknown>).min_sdk, 33);
  assert.deepEqual([...validateSpec(spec, three, "multiplatform")], []);
});

type HeaderCase = {
  name: string;
  headers: Record<string, string>;
  expected: { session_id: string | null; config_ms: number | null; client: string };
};

const { cases } = readJson<{ cases: HeaderCase[] }>("web", "api", "testdata", "contract", "generate_headers.json");

test("the API's header contract has cases for every header the site sends", () => {
  for (const header of ["X-Session-Id", "X-Config-Ms", "X-Client"]) {
    assert.ok(cases.some((entry) => header in entry.headers), header);
  }
});

for (const entry of cases) {
  test(`session headers agree with the API: ${entry.name}`, () => {
    const id = entry.headers["X-Session-Id"];
    if (id !== undefined) {
      assert.equal(SESSION_ID.test(id), entry.expected.session_id !== null, "accepts the same session ids");
      assert.equal(sessionHeaders({ id, configMs: null, build: "b" })["X-Session-Id"] ?? null, entry.expected.session_id);
    }
    if (entry.expected.config_ms !== null) {
      const sent = sessionHeaders({ id: null, configMs: entry.expected.config_ms, build: "b" })["X-Config-Ms"];
      assert.equal(sent, String(entry.expected.config_ms));
    }
    const client = entry.headers["X-Client"];
    if (client !== undefined && entry.expected.client === "web") {
      assert.equal(clientHeader(client.slice(CLIENT_PREFIX.length)), client, "the site sends exactly what the API classes as web");
    }
    if (client !== undefined && entry.expected.client !== "web") {
      assert.notEqual(clientHeader(client.slice(CLIENT_PREFIX.length)), client, "the site never sends a header the API would not class as web");
    }
  });
}

test("a configurator time past the cap is capped, never dropped, and never negative", () => {
  assert.equal(sessionHeaders({ id: null, configMs: CONFIG_MS_MAX + 1, build: "b" })["X-Config-Ms"], String(CONFIG_MS_MAX));
  assert.equal(sessionHeaders({ id: null, configMs: -5, build: "b" })["X-Config-Ms"], "0");
  assert.equal(sessionHeaders({ id: null, configMs: 1500.5, build: "b" })["X-Config-Ms"], "1501");
  assert.equal("X-Config-Ms" in sessionHeaders({ id: null, configMs: Number.NaN, build: "b" }), false);
});

test("the client header is printable ASCII within 64 characters whatever the build is called", () => {
  for (const build of ["", "café", "a".repeat(200), "1a2b3c4"]) {
    const header = clientHeader(build);
    assert.match(header, /^android-base-web\/[\x21-\x7e]+$/);
    assert.ok(header.length <= 64);
  }
});

test("the deep-link scheme shown is the one the generator will use", () => {
  const named = run(initialState(catalogue), { type: "appName", value: "Trail Notes" });
  assert.equal(deepLinkScheme(toSpecV2(named, catalogue), named.appName), "trailnotes", "the lower name while none is typed");
  const typed = run(named, { type: "setting", key: "android.deeplink_scheme", value: "trail" });
  const withLinks = typed.features.includes("deeplink") ? typed : run(typed, { type: "feature", key: "deeplink" });
  assert.equal(deepLinkScheme(toSpecV2(withLinks, catalogue), withLinks.appName), "trail");
  const withoutLinks = run(withLinks, { type: "feature", key: "deeplink" });
  assert.equal(deepLinkScheme(toSpecV2(withoutLinks, catalogue), withoutLinks.appName), "trailnotes", "not sent without the feature");
});
