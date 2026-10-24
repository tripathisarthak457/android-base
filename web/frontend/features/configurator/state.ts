import { disabledFeatures, effectiveUiComponents, hiddenFields, uiComponentsOption } from "../catalogue/options.ts";
import { selectableTargets } from "../catalogue/platforms.ts";
import type { Catalogue, Field, Rules, Target } from "../catalogue/types.ts";
import { presetFeatures, resolveChoices, toggleFeature } from "./resolve.ts";
import { isReserved } from "./validation/names.ts";

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

/** The spec fields the identity step edits; the later steps open only once they are valid. */
export const IDENTITY_FIELDS = ["app_name", "display_name", "package_name", "feature_modules", "languages"];

const LOOK_FIELDS = ["design_style", "accent_colour", "secondary_colour", "tertiary_colour", "font_name", "mono_font_name"];

/** Which step edits each spec field, so an error can send the visitor to where it is fixed. */
export function stepOf(field: string): Step {
  if (field === "platform" || field === "schema") return "platform";
  if (IDENTITY_FIELDS.includes(field)) return "identity";
  if (field === "features" || field === "ui_components") return "features";
  if (LOOK_FIELDS.includes(field)) return "look";
  if (field === "motion_style" || field === "haptics_enabled") return "motion";
  return "build";
}

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
  /** Whether the project gets UI components; false is the ticked "No UI components" box. */
  uiComponents: boolean;
  /** Closed over `requires`, sorted. */
  features: string[];
  /**
   * The features the selection or the chosen preset had that a project without UI components
   * leaves out, in the catalogue's order, so Review can say what was skipped and why.
   */
  skipped: string[];
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
  | { type: "uiComponents"; value: boolean }
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

/**
 * The target `key` names when a visitor can pick it, otherwise the first one they can: a draft or a
 * link may name a target that has since been hidden or put back to coming soon.
 */
export function findTarget(catalogue: Catalogue, key: string): Target {
  const selectable = selectableTargets(catalogue);
  const target = selectable.find((candidate) => candidate.key === key) ?? selectable[0];
  if (!target) throw new Error("The catalogue has no target this site can generate.");
  return target;
}

/**
 * The target to answer for once the server has said which it generates: `current`, unless the
 * server turned it off, then the first selectable one it has not.
 */
export function enabledTarget(catalogue: Catalogue, current: string, enabled: Record<string, boolean> | null): string {
  if (enabled === null || enabled[current] !== false) return current;
  return selectableTargets(catalogue).find((target) => enabled[target.key] !== false)?.key ?? current;
}

/** Settings fields: the dotted ones, which live under a platform block in the spec. */
export function settingFields(target: Target): Field[] {
  return target.fields.filter((field) => field.key.includes("."));
}

/**
 * The value a choice field's option stands for, in the type the catalogue gives it: iOS "18.0"
 * stays a string and API 35 stays a number, as the generator checks each.
 */
export function choiceValue(field: Field, option: string): string | number | undefined {
  return field.choices?.find((choice) => String(choice.value) === option)?.value;
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

/**
 * The package the app name suggests. The visitor never typed it, so it has to pass the generator's
 * checks as it stands: a long name is cut to a segment's limit, and a keyword gets a suffix.
 */
export function suggestPackage(appName: string, rules: Rules): string {
  const slug = lowerName(appName).replace(/^[0-9]+/, "").slice(0, rules.segment_max) || "myapp";
  return `com.example.${isReserved(slug, rules) ? `${slug}app` : slug}`;
}

/** `Orders, wallet-history` → `orders`, `wallet_history`: module names as the generator wants them. */
export function moduleNames(text: string): string[] {
  return text
    .split(",")
    .map((name) => name.trim().toLowerCase().replace(/[\s-]+/g, "_"))
    .filter(Boolean);
}

/**
 * The typeface the project gets, for the previews and Review to show: the generator reads an empty
 * answer as the target's default. Spaces alone are drawn the same way while the validator flags them.
 */
export function typeface(fontName: string, target: Target): string {
  return fontName.trim() || target.defaults.font_name;
}

/**
 * `keys` resolved as a visitor's choices, plus what the design style needs, so a style keeps the
 * features it draws with through a preset, a platform switch or a restored draft. Without UI
 * components the disabled features are taken out before `requires` is resolved, as the generator
 * drops them.
 */
export function featuresFor(keys: Iterable<string>, target: Target, designStyle: string, uiComponents: boolean): string[] {
  const disabled = disabledFeatures(target, uiComponents);
  const requires = target.design_styles.find((style) => style.key === designStyle)?.requires ?? [];
  return [...resolveChoices([...keys, ...requires].filter((key) => !disabled.has(key)), target)].sort();
}

/** The disabled features among `keys`, in the order the option lists them. */
export function skippedOf(keys: Iterable<string>, target: Target, uiComponents: boolean): string[] {
  const asked = new Set(keys);
  return [...disabledFeatures(target, uiComponents).keys()].filter((key) => asked.has(key));
}

/** The design and motion style a project gets: the target's own defaults where the option hides them. */
export function stylesFor(
  target: Target,
  uiComponents: boolean,
  designStyle: string,
  motionStyle: string,
): { designStyle: string; motionStyle: string } {
  const hidden = hiddenFields(target, uiComponents);
  return {
    designStyle: hidden.has("design_style") ? target.defaults.design_style : designStyle,
    motionStyle: hidden.has("motion_style") ? target.defaults.motion_style : motionStyle,
  };
}

/**
 * Whether the project draws a glass bar, so the Build step says where glass blurs: only a design
 * style whose bar is Glass draws one. The liquidglass feature alone is a foundation nothing draws
 * with, so a project with it looks the same as one without.
 */
export function drawsGlassBar(state: ConfigState, catalogue: Catalogue): boolean {
  return catalogue.tokens.styles[state.designStyle]?.bar === "Glass";
}

export function initialState(catalogue: Catalogue, targetKey?: string): ConfigState {
  const target = findTarget(catalogue, targetKey ?? "android");
  const defaults = target.defaults;
  const appName = "My App";
  const uiComponents = effectiveUiComponents(target, uiComponentsOption(target)?.default ?? true);
  const preset = presetFeatures(target, defaults.preset);
  const styles = stylesFor(target, uiComponents, defaults.design_style, defaults.motion_style);
  return {
    target: target.key,
    appName,
    displayName: "",
    packageName: suggestPackage(appName, catalogue.rules),
    packageEdited: false,
    modules: "",
    languages: [...defaults.languages],
    architecture: defaults.architecture,
    uiComponents,
    features: featuresFor(preset, target, styles.designStyle, uiComponents),
    skipped: skippedOf(preset, target, uiComponents),
    designStyle: styles.designStyle,
    motionStyle: styles.motionStyle,
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
  const to = selectableTargets(catalogue).find((target) => target.key === key);
  if (!to || to.key === state.target) return state;
  const from = findTarget(catalogue, state.target);
  const fresh = initialState(catalogue, to.key);

  const untouched = sameSet(state.features, [...presetFeatures(from, from.defaults.preset, disabledFeatures(from, state.uiComponents))]);
  const uiComponents = effectiveUiComponents(to, state.uiComponents);
  const { designStyle, motionStyle } = stylesFor(
    to,
    uiComponents,
    to.design_styles.some((style) => style.key === state.designStyle) ? state.designStyle : fresh.designStyle,
    to.motion_styles.some((style) => style.key === state.motionStyle) ? state.motionStyle : fresh.motionStyle,
  );
  const kept = featuresFor(state.features, to, designStyle, uiComponents);
  const source = untouched || kept.length === 0 ? presetFeatures(to, to.defaults.preset) : state.features;
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
    uiComponents,
    features: featuresFor(source, to, designStyle, uiComponents),
    skipped: skippedOf(source, to, uiComponents),
    languages: state.languages.filter((tag) => offered.has(tag)),
    architecture: architecture?.key ?? fresh.architecture,
    designStyle,
    motionStyle,
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
  const hidden = hiddenFields(target, state.uiComponents);
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
        packageName: state.packageEdited ? state.packageName : suggestPackage(action.value, catalogue.rules),
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
    case "uiComponents": {
      const uiComponents = effectiveUiComponents(target, action.value);
      if (uiComponents === state.uiComponents) return state;
      const styles = stylesFor(target, uiComponents, state.designStyle, state.motionStyle);
      return edit({
        uiComponents,
        ...styles,
        features: featuresFor(state.features, target, styles.designStyle, uiComponents),
        skipped: skippedOf(state.features, target, uiComponents),
      });
    }
    case "feature": {
      if (disabledFeatures(target, state.uiComponents).has(action.key)) return state;
      const toggle = toggleFeature(new Set(state.features), action.key, target);
      // A design style that needs what was unticked goes with it, as a feature that needs it does.
      const requires = target.design_styles.find((style) => style.key === state.designStyle)?.requires ?? [];
      const designStyle = requires.every((key) => toggle.next.has(key)) ? state.designStyle : target.defaults.design_style;
      const quiet = toggle.alsoAdded.length === 0 && toggle.alsoRemoved.length === 0 && designStyle === state.designStyle;
      return edit({
        features: featuresFor(toggle.next, target, designStyle, state.uiComponents),
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
    case "preset": {
      const preset = presetFeatures(target, action.key);
      return edit({
        features: featuresFor(preset, target, state.designStyle, state.uiComponents),
        skipped: skippedOf(preset, target, state.uiComponents),
      });
    }
    case "designStyle": {
      const style = target.design_styles.find((option) => option.key === action.value);
      if (!style || hidden.has("design_style")) return state;
      return edit({ designStyle: style.key, features: featuresFor(state.features, target, style.key, state.uiComponents) });
    }
    case "motionStyle":
      return hidden.has("motion_style") ? state : edit({ motionStyle: action.value });
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

/** The answers Start over replaced, and where the visitor was, for its Undo. */
export type StartOverUndo = { state: ConfigState; step: Step; identityAt: number | null; revision: number };

/**
 * What Start over keeps for its Undo, or null when there is nothing to bring back. `revision` is the
 * one the reset gives the answers, so any edit after it ends the offer. A press while the last reset
 * can still be undone keeps that snapshot: the answers on screen are then the defaults it put there
 * and the draft went with the first press. Answers that are `untouched` are those defaults too, so a
 * snapshot of them would offer an Undo that changes nothing.
 */
export function snapshotForUndo(kept: StartOverUndo | null, current: Omit<StartOverUndo, "revision">, untouched: boolean): StartOverUndo | null {
  const revision = current.state.revision + 1;
  if (kept !== null && kept.revision === current.state.revision) return { ...kept, revision };
  return untouched ? null : { ...current, revision };
}
