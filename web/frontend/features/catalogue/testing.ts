import { readFileSync } from "node:fs";
import { join } from "node:path";
import type { Catalogue, Option, OptionLock, Target } from "./types.ts";

/** The repository root, for the fixtures the generator and the API share with these tests. */
const REPOSITORY = join(import.meta.dirname, "..", "..", "..", "..");

export function readJson<T>(...path: string[]): T {
  return JSON.parse(readFileSync(join(REPOSITORY, ...path), "utf8")) as T;
}

/**
 * The android target's `ui_components` option, word for word as the generator's option file
 * describes it, for a checked-in catalogue written before the generator emitted options.
 */
const UI_COMPONENTS: Option = {
  key: "ui_components",
  title: "UI components",
  checkbox: { label: "No UI components", ticked_value: false },
  headline: "Setup only: the architecture, every build guard and a theme skeleton, without UI components",
  description:
    "Every non-UI part of the project: the modules, MVI, Navigation 3 with deep links and process-death restore, data, DI, " +
    "tests and every build guard. core:designsystem is a theme skeleton (AppTheme with colour, type, spacing, shape and " +
    "motion slots, filled with neutral values and your colours and font) with no components; each screen is a one-line " +
    "placeholder route over its ViewModel, and the app opens on a blank start screen. The developer panel, the performance " +
    "overlay and LeakCanary stay.",
  default: true,
  disables: [
    { feature: "catalog", reason: "The catalog app shows the design system's components, and this project has none." },
    {
      feature: "collections",
      reason: "Carousels, reorderable lists and the image viewer are components; build yours on the theme skeleton.",
    },
    { feature: "datadisplay", reason: "Stat tiles, charts and tables are components; build yours on the theme skeleton." },
    { feature: "formfields", reason: "Code, money and phone fields are components; form state and validation (forms) stay." },
    { feature: "pickers", reason: "Calendars and wheel pickers are components; build yours on the theme skeleton." },
    { feature: "liquidglass", reason: "Glass is drawn by components and the shell, which this project leaves to your design system." },
  ],
  hides: [
    { field: "design_style", reason: "Your design system decides corners, surfaces and type voice; the theme starts from neutral values." },
    { field: "motion_style", reason: "Your components decide their motion; the theme keeps neutral springs and reduce-motion." },
  ],
  locked: null,
};

export function loadCatalogue(): Catalogue {
  const catalogue = readJson<Catalogue>("web", "frontend", "lib", "catalogue.json");
  return {
    ...catalogue,
    targets: catalogue.targets.map((target) =>
      Array.isArray(target.options) ? target : { ...target, options: target.key === "android" ? [UI_COMPONENTS] : [] },
    ),
  };
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
