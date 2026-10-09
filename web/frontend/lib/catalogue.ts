import type { Catalogue, Target } from "../features/catalogue/types.ts";
import data from "./catalogue.json";

/**
 * The catalogue this build was made with. It is emitted by the generator and checked in, so the
 * page, its counts and its copy deploy together with the generator they describe.
 */
export const CATALOGUE = data as unknown as Catalogue;

export const REPO = "https://github.com/tripathisarthak457/android-base";

/** The privacy page: exactly what is stored, on the server and in the browser. */
export const PRIVACY = `${REPO}/blob/main/docs/telemetry-and-privacy.md`;

export function android(): Target {
  const target = CATALOGUE.targets.find((candidate) => candidate.key === "android") ?? CATALOGUE.targets[0];
  if (!target) throw new Error("The checked-in catalogue has no targets.");
  return target;
}

/** Features a visitor can switch on or off: everything not reached only through another. */
export function selectableFeatures(target: Target = android()) {
  return target.features.filter((feature) => !feature.implied_only);
}

/** Optional features that bring third-party code under its own licence. */
export function licensedFeatures() {
  return CATALOGUE.targets.flatMap((target) => target.features).filter((feature, index, all) => feature.licence && all.findIndex((other) => other.key === feature.key) === index);
}
