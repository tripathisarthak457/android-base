import type { Catalogue, Field, Target } from "../catalogue/types.ts";
import { presetFeatures, resolveChoices, toggleFeature } from "./resolve.ts";

export const STEPS = ["platform", "identity", "features", "look", "motion", "build", "review"] as const;

export type Step = (typeof STEPS)[number];

export const STEP_LABELS: Record<Step, string> = {
  platform: "Platform",
  identity: "Project",
  features: "Features",
  look: "Look",
  motion: "Motion",
  build: "Build",
  review: "Review",
};

/** A value of one dotted field (`android.min_sdk`, `android.api_base_urls`). */
export type Setting = string | number | Record<string, string>;

export type Signing = {
  enabled: boolean;
  organisation: string;
  country: string;
  /** One per key name. Never saved with the draft. */
  passwords: Record<string, string>;
};

/** The last feature toggle that took others with it, kept so it can be undone. */
export type ToggleHint = {
  key: string;
  on: boolean;
  alsoAdded: string[];
  alsoRemoved: string[];
  previous: string[];
  /** The design style before the toggle: unticking a feature the style needs puts the default style back. */
  previousStyle: string;
};

export type ConfigState = {
  target: string;
  appName: string;
  displayName: string;
  packageName: string;
  /** Once the visitor types a package, the app name stops suggesting one. */
  packageEdited: boolean;
  /** The module names as typed, comma-separated. */
  modules: string;
  languages: string[];
  architecture: string;
  /** Closed over `requires`, sorted. */
  features: string[];
  designStyle: string;
  motionStyle: string;
  hapticsEnabled: boolean;
  accent: string;
  /** Empty means "work it out from the primary", as the generator reads a blank. */
  secondary: string;
  tertiary: string;
  fontName: string;
  settings: Record<string, Setting>;
  signing: Signing;
  hint: ToggleHint | null;
  /** Bumped by every edit, so a result can tell whether it still describes the current answers. */
  revision: number;
};

export type Action =
  | { type: "target"; target: string }
  | { type: "appName"; value: string }
  | { type: "displayName"; value: string }
  | { type: "packageName"; value: string }
  | { type: "modules"; value: string }
  | { type: "language"; tag: string }
  | { type: "architecture"; value: string }
  | { type: "feature"; key: string }
  | { type: "undoToggle" }
  | { type: "preset"; key: string }
  | { type: "designStyle"; value: string }
  | { type: "motionStyle"; value: string }
  | { type: "haptics"; value: boolean }
  | { type: "colour"; which: "accent" | "secondary" | "tertiary"; value: string }
  | { type: "fontName"; value: string }
  | { type: "setting"; key: string; value: Setting }
  | { type: "signing"; value: Partial<Signing> }
  | { type: "restore"; state: ConfigState }
  | { type: "reset" };

export function findTarget(catalogue: Catalogue, key: string): Target {
  const target = catalogue.targets.find((candidate) => candidate.key === key) ?? selectableTargets(catalogue)[0];
  if (!target) throw new Error("The catalogue has no target this site can generate.");
  return target;
}

/** Targets a visitor can pick: not hidden and not announced as coming soon. */
export function selectableTargets(catalogue: Catalogue): Target[] {
  return catalogue.targets.filter((target) => target.status === "available" || target.status === "beta");
}

/** Settings fields: the dotted ones, which live under a platform block in the spec. */
export function settingFields(target: Target): Field[] {
  return target.fields.filter((field) => field.key.includes("."));
}

function defaultSettings(target: Target): Record<string, Setting> {
  const settings: Record<string, Setting> = {};
  for (const field of settingFields(target)) {
    if (typeof field.default === "number" || typeof field.default === "string") settings[field.key] = field.default;
    else if (field.type === "map") settings[field.key] = {};
  }
  return settings;
}

/** `My Great App` → `mygreatapp`, the generator's lower name: the default deep-link scheme and key alias. */
export function lowerName(appName: string): string {
  return appName.replace(/[^A-Za-z0-9]+/g, "").toLowerCase();
}

export function suggestPackage(appName: string): string {
  const slug = lowerName(appName).replace(/^[0-9]+/, "");
  return `com.example.${slug || "myapp"}`;
}

/** `Orders, wallet-history` → `orders`, `wallet_history`: module names as the generator wants them. */
export function moduleNames(text: string): string[] {
  return text
    .split(",")
    .map((name) => name.trim().toLowerCase().replace(/[\s-]+/g, "_"))
    .filter(Boolean);
}

/**
 * `keys` resolved as a visitor's choices, plus what the design style needs, so Glass keeps
 * liquidglass on through a preset, a platform switch or a restored draft.
 */
export function featuresFor(keys: Iterable<string>, target: Target, designStyle: string): string[] {
  const requires = target.design_styles.find((style) => style.key === designStyle)?.requires ?? [];
  return [...resolveChoices([...keys, ...requires], target)].sort();
}

export function initialState(catalogue: Catalogue, targetKey?: string): ConfigState {
  const target = findTarget(catalogue, targetKey ?? "android");
  const defaults = target.defaults;
  const appName = "My App";
  return {
    target: target.key,
    appName,
    displayName: "",
    packageName: suggestPackage(appName),
    packageEdited: false,
    modules: "",
    languages: [...defaults.languages],
    architecture: defaults.architecture,
    features: [...presetFeatures(target, defaults.preset)].sort(),
    designStyle: defaults.design_style,
    motionStyle: defaults.motion_style,
    hapticsEnabled: defaults.haptics_enabled,
    accent: defaults.accent_colour,
    secondary: "",
    tertiary: "",
    fontName: defaults.font_name,
    settings: defaultSettings(target),
    signing: { enabled: false, organisation: "", country: "US", passwords: {} },
    hint: null,
    revision: 0,
  };
}

/**
 * The answers carried over to another platform: everything the new one also offers. A feature set
 * left exactly at the old platform's default preset becomes the new platform's default preset, since
 * the visitor never chose it.
 */
function switchTarget(state: ConfigState, catalogue: Catalogue, key: string): ConfigState {
  if (key === state.target) return state;
  const from = findTarget(catalogue, state.target);
  const to = findTarget(catalogue, key);
  const fresh = initialState(catalogue, to.key);

  const untouched = sameSet(state.features, [...presetFeatures(from, from.defaults.preset)]);
  const designStyle = to.design_styles.some((style) => style.key === state.designStyle) ? state.designStyle : fresh.designStyle;
  const kept = featuresFor(state.features, to, designStyle);
  const offered = new Set(to.languages.map((language) => language.tag));
  const settings = { ...fresh.settings };
  for (const field of settingFields(to)) {
    const value = state.settings[field.key];
    if (value !== undefined) settings[field.key] = value;
  }
  const architecture = to.architectures.find((option) => option.key === state.architecture && option.available);
  return {
    ...state,
    target: to.key,
    features: untouched || kept.length === 0 ? featuresFor(fresh.features, to, designStyle) : kept,
    languages: state.languages.filter((tag) => offered.has(tag)),
    architecture: architecture?.key ?? fresh.architecture,
    designStyle,
    motionStyle: to.motion_styles.some((style) => style.key === state.motionStyle) ? state.motionStyle : fresh.motionStyle,
    settings,
    signing: hasKeystores(to) ? state.signing : fresh.signing,
    hint: null,
    revision: state.revision + 1,
  };
}

export function hasKeystores(target: Target): boolean {
  return target.fields.some((field) => field.type === "keystores");
}

function sameSet(a: readonly string[], b: readonly string[]): boolean {
  return a.length === b.length && b.every((key) => a.includes(key));
}

export function reducer(state: ConfigState, action: Action, catalogue: Catalogue): ConfigState {
  const target = findTarget(catalogue, state.target);
  const edit = (patch: Partial<ConfigState>): ConfigState => ({
    ...state,
    ...patch,
    hint: action.type === "feature" ? (patch.hint ?? null) : null,
    revision: state.revision + 1,
  });
  switch (action.type) {
    case "target":
      return switchTarget(state, catalogue, action.target);
    case "appName":
      return edit({
        appName: action.value,
        packageName: state.packageEdited ? state.packageName : suggestPackage(action.value),
      });
    case "displayName":
      return edit({ displayName: action.value });
    case "packageName":
      return edit({ packageName: action.value, packageEdited: true });
    case "modules":
      return edit({ modules: action.value });
    case "language": {
      const on = state.languages.includes(action.tag);
      return edit({ languages: on ? state.languages.filter((tag) => tag !== action.tag) : [...state.languages, action.tag] });
    }
    case "architecture":
      return edit({ architecture: action.value });
    case "feature": {
      const toggle = toggleFeature(new Set(state.features), action.key, target);
      // A design style that needs what was unticked goes with it, as a feature that needs it does.
      const requires = target.design_styles.find((style) => style.key === state.designStyle)?.requires ?? [];
      const designStyle = requires.every((key) => toggle.next.has(key)) ? state.designStyle : target.defaults.design_style;
      const quiet = toggle.alsoAdded.length === 0 && toggle.alsoRemoved.length === 0 && designStyle === state.designStyle;
      return edit({
        features: featuresFor(toggle.next, target, designStyle),
        designStyle,
        hint: quiet
          ? null
          : {
              key: action.key,
              on: toggle.next.has(action.key),
              alsoAdded: toggle.alsoAdded,
              alsoRemoved: toggle.alsoRemoved,
              previous: state.features,
              previousStyle: state.designStyle,
            },
      });
    }
    case "undoToggle":
      return state.hint ? edit({ features: state.hint.previous, designStyle: state.hint.previousStyle }) : state;
    case "preset":
      return edit({ features: featuresFor(presetFeatures(target, action.key), target, state.designStyle) });
    case "designStyle": {
      const style = target.design_styles.find((option) => option.key === action.value);
      if (!style) return state;
      return edit({ designStyle: style.key, features: featuresFor(state.features, target, style.key) });
    }
    case "motionStyle":
      return edit({ motionStyle: action.value });
    case "haptics":
      return edit({ hapticsEnabled: action.value });
    case "colour":
      return edit({ [action.which]: action.value });
    case "fontName":
      return edit({ fontName: action.value });
    case "setting":
      return edit({ settings: { ...state.settings, [action.key]: action.value } });
    case "signing":
      return edit({ signing: { ...state.signing, ...action.value } });
    case "restore":
      return { ...action.state, revision: state.revision + 1 };
    case "reset":
      return { ...initialState(catalogue), revision: state.revision + 1 };
  }
}
