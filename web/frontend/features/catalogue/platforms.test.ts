import assert from "node:assert/strict";
import { test } from "node:test";
import { findTarget, initialState } from "../configurator/state.ts";
import { type Announced, offerOnly, platformsOf, selectableTargets } from "./platforms.ts";
import { loadCatalogue, threePlatformCatalogue } from "./testing.ts";
import type { Catalogue } from "./types.ts";

const ANNOUNCED: Announced[] = [
  { key: "multiplatform", title: "Compose Multiplatform", summary: "One Compose UI for Android and iOS." },
  { key: "ios", title: "Swift", summary: "SwiftUI with @Observable stores." },
];

test("a target the site does not offer is coming soon, even when the generator ships it", () => {
  const catalogue = offerOnly(threePlatformCatalogue(), new Set(["android"]));
  assert.deepEqual(
    catalogue.targets.map((target) => [target.key, target.status]),
    [
      ["android", "available"],
      ["multiplatform", "coming_soon"],
      ["ios", "coming_soon"],
    ],
  );
  assert.deepEqual(selectableTargets(catalogue).map((target) => target.key), ["android"]);
  assert.equal(findTarget(catalogue, "multiplatform").key, "android", "a draft or link for it lands on Android");
  assert.equal(initialState(catalogue, "multiplatform").target, "android");
});

test("offering a target does not make one the generator holds back selectable", () => {
  const base = threePlatformCatalogue();
  const hidden: Catalogue = {
    ...base,
    targets: base.targets.map((target) => (target.key === "multiplatform" ? { ...target, status: "hidden" } : target)),
  };
  const catalogue = offerOnly(hidden, new Set(["android", "multiplatform", "ios"]));
  assert.deepEqual(
    catalogue.targets.map((target) => target.status),
    ["available", "hidden", "coming_soon"],
  );
});

test("announced platforms are shown as coming soon until the catalogue has them", () => {
  const loaded = loadCatalogue();
  const androidOnly = platformsOf({ ...loaded, targets: loaded.targets.filter((target) => target.key === "android") }, ANNOUNCED);
  assert.deepEqual(
    androidOnly.map((platform) => [platform.key, platform.status]),
    [
      ["android", "available"],
      ["multiplatform", "coming_soon"],
      ["ios", "coming_soon"],
    ],
  );

  const catalogue = offerOnly(threePlatformCatalogue(), new Set(["android", "multiplatform"]));
  const shown = platformsOf(catalogue, ANNOUNCED);
  assert.deepEqual(shown.map((platform) => platform.key), ["android", "multiplatform", "ios"], "each platform once");
  const multiplatform = shown.find((platform) => platform.key === "multiplatform");
  assert.equal(multiplatform?.status, "beta", "the catalogue's own target wins over the announcement");
  assert.equal(multiplatform?.status_note, "Beta: the iOS app is not yet verified on a Mac");
});
