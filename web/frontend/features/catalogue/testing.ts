import { readFileSync } from "node:fs";
import { join } from "node:path";
import type { Catalogue, OptionLock, Target } from "./types.ts";

/** The repository root, for the fixtures the generator and the API share with these tests. */
const REPOSITORY = join(import.meta.dirname, "..", "..", "..", "..");

export function readJson<T>(...path: string[]): T {
  return JSON.parse(readFileSync(join(REPOSITORY, ...path), "utf8")) as T;
}

export function loadCatalogue(): Catalogue {
  return readJson<Catalogue>("web", "frontend", "lib", "catalogue.json");
}

/** The note the setup-only release locks the option with. */
export const SETUP_ONLY: OptionLock = {
  value: false,
  note:
    "UI components come with the Android release. Until then this site generates setup-only projects: the architecture, " +
    "the theme skeleton and every build guard, without components.",
};

/**
 * The checked-in catalogue as a deployment that serves only one value of `ui_components` has it.
 * Branches other than the release's carry no lock, so the locked behaviour is tested on this.
 */
export function lockedCatalogue(lock: OptionLock = SETUP_ONLY): Catalogue {
  const catalogue = loadCatalogue();
  return {
    ...catalogue,
    targets: catalogue.targets.map((target) => ({
      ...target,
      options: target.options.map((option) => (option.key === "ui_components" ? { ...option, locked: lock } : option)),
    })),
  };
}

export function androidTarget(catalogue: Catalogue = loadCatalogue()): Target {
  const target = catalogue.targets.find((candidate) => candidate.key === "android");
  if (!target) throw new Error("The catalogue has no android target.");
  return target;
}

/**
 * The checked-in catalogue plus the two targets the platform step is built for: a beta
 * multiplatform target with a smaller feature set and no signing keys, and an iOS target that is
 * coming soon. Their `ios` fields and rules are the ones the iOS generator declares. The platform
 * logic is tested on these fixed targets rather than on whichever the generator ships.
 */
export function threePlatformCatalogue(): Catalogue {
  const catalogue = loadCatalogue();
  const android = androidTarget(catalogue);
  const kept = new Set(["network", "coil", "database", "sample", "settings", "onboarding", "language", "catalog"]);
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
      { key: "ios.bundle_id", type: "string", rule: "ios_bundle_id", max: 155 },
      {
        key: "ios.deployment_target",
        type: "string",
        default: "17.0",
        choices: ["17", "18", "26"].map((major) => ({ value: `${major}.0`, label: `iOS ${major}` })),
      },
      { key: "ios.team_id", type: "string", rule: "ios_team_id" },
    ],
    languages: android.languages.filter((language) => language.tag !== "hi"),
    options: [],
  };
  const ios: Target = { ...multiplatform, key: "ios", title: "Swift", status: "coming_soon", status_note: "" };
  const rules = Object.assign({}, catalogue.rules, { ios_bundle_id: "^[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$", ios_team_id: "^[A-Z0-9]{10}$" });
  return { ...catalogue, rules, targets: [android, multiplatform, ios] };
}
