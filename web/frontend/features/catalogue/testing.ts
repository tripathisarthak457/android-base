import { readFileSync } from "node:fs";
import { join } from "node:path";
import type { Catalogue, Target } from "./types.ts";

/** The repository root, for the fixtures the generator and the API share with these tests. */
const REPOSITORY = join(import.meta.dirname, "..", "..", "..", "..");

export function readJson<T>(...path: string[]): T {
  return JSON.parse(readFileSync(join(REPOSITORY, ...path), "utf8")) as T;
}

export function loadCatalogue(): Catalogue {
  return readJson<Catalogue>("web", "frontend", "lib", "catalogue.json");
}

export function androidTarget(catalogue: Catalogue = loadCatalogue()): Target {
  const target = catalogue.targets.find((candidate) => candidate.key === "android");
  if (!target) throw new Error("The catalogue has no android target.");
  return target;
}

/**
 * The checked-in catalogue plus the two targets the platform step is built for: a beta
 * multiplatform target with a smaller feature set and no signing keys, and an iOS target that is
 * coming soon. The generator ships only Android so far, so the platform logic is tested on these.
 */
export function threePlatformCatalogue(): Catalogue {
  const catalogue = loadCatalogue();
  const android = androidTarget(catalogue);
  const kept = new Set(["network", "coil", "database", "room", "sample", "settings", "onboarding", "language", "catalog"]);
  const features = android.features.filter((feature) => kept.has(feature.key));
  const multiplatform: Target = {
    ...android,
    key: "multiplatform",
    title: "Compose Multiplatform",
    status: "beta",
    status_note: "Beta: the iOS app is not yet verified on a Mac",
    features,
    presets: [{ key: "standard", title: "Standard", description: "", default: true, features: ["catalog", "network", "sample"] }],
    defaults: { ...android.defaults, preset: "standard" },
    fields: [
      ...android.fields.filter((field) => field.type !== "keystores"),
      { key: "ios.bundle_id", type: "string" },
      { key: "ios.deployment_target", type: "string", default: "17.0" },
    ],
    languages: android.languages.filter((language) => language.tag !== "hi"),
  };
  const ios: Target = { ...multiplatform, key: "ios", title: "Swift", status: "coming_soon", status_note: "" };
  return { ...catalogue, targets: [android, multiplatform, ios] };
}
