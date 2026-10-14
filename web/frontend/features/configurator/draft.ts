import type { Catalogue } from "../catalogue/types.ts";
import { type ConfigState, type Setting, featuresFor, findTarget, initialState, settingFields, suggestPackage, typeface } from "./state.ts";

/**
 * The configurator's answers, kept in `localStorage` so a visitor who comes back finds them again.
 * It holds no signing passwords, no date and no identifier; its presence at load is the only
 * "returning visitor" signal the site sends.
 */
export const DRAFT_KEY = "android-base.draft";

const VERSION = 1;

type Saved = Omit<ConfigState, "signing" | "hint" | "revision">;

export function serializeDraft(state: ConfigState): string {
  const { signing: _signing, hint: _hint, revision: _revision, ...saved } = state;
  return JSON.stringify({ version: VERSION, state: saved satisfies Saved });
}

/**
 * The settings that say something. A blank field or flavour URL, or a value equal to the field's
 * default, is what the generator assumes anyway, so typing one and clearing it again is no answer.
 */
function answered(state: ConfigState, catalogue: Catalogue): Record<string, Setting> {
  const settings: Record<string, Setting> = {};
  for (const field of settingFields(findTarget(catalogue, state.target))) {
    const value = state.settings[field.key];
    if (value === undefined || value === field.default) continue;
    if (typeof value === "object") {
      const entries = Object.entries(value).filter(([, entry]) => entry.trim() !== "");
      if (entries.length > 0) settings[field.key] = Object.fromEntries(entries);
    } else if (typeof value === "number" || value.trim() !== "") {
      settings[field.key] = value;
    }
  }
  return settings;
}

/**
 * What storage should hold for `state`: its draft, or null to remove the key. Answers nobody has
 * changed from the defaults are not kept, so a visit that only looks, or ends with Start over,
 * leaves nothing behind and does not count as returning next time. A package typed back to the one
 * the app name suggests is no answer either, nor is an emptied typeface, which is the default's.
 */
export function draftToStore(state: ConfigState, catalogue: Catalogue): string | null {
  const plain = (answers: ConfigState) =>
    serializeDraft({
      ...answers,
      packageEdited: answers.packageEdited && answers.packageName !== suggestPackage(answers.appName, catalogue.rules),
      fontName: typeface(answers.fontName, findTarget(catalogue, answers.target)),
      settings: answered(answers, catalogue),
    });
  return plain(state) === plain(initialState(catalogue)) ? null : serializeDraft(state);
}

type Json = Record<string, unknown>;

function isObject(value: unknown): value is Json {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function strings(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
}

function isSetting(value: unknown): value is Setting {
  if (typeof value === "string" || typeof value === "number") return true;
  return isObject(value) && Object.values(value).every((item) => typeof item === "string");
}

/**
 * A saved draft read back against today's catalogue, or null when there is none or it cannot be
 * read. A feature, style or language the catalogue no longer has is dropped rather than failing
 * the page, since the draft may be months older than the deploy.
 */
export function parseDraft(text: string | null, catalogue: Catalogue): ConfigState | null {
  if (!text) return null;
  let parsed: unknown;
  try {
    parsed = JSON.parse(text);
  } catch {
    return null;
  }
  if (!isObject(parsed) || parsed.version !== VERSION || !isObject(parsed.state)) return null;
  const saved = parsed.state;
  const target = findTarget(catalogue, typeof saved.target === "string" ? saved.target : "android");
  const state = initialState(catalogue, target.key);

  const savedText = (key: keyof ConfigState) => (typeof saved[key] === "string" ? (saved[key] as string) : (state[key] as string));
  const offered = new Set(target.languages.map((language) => language.tag));
  const settings = { ...state.settings };
  if (isObject(saved.settings)) {
    for (const field of settingFields(target)) {
      const value = saved.settings[field.key];
      if (isSetting(value)) settings[field.key] = value;
    }
  }
  const designStyle = target.design_styles.find((style) => style.key === saved.designStyle);
  const motionStyle = target.motion_styles.find((style) => style.key === saved.motionStyle);
  const architecture = target.architectures.find((option) => option.key === saved.architecture && option.available);

  return {
    ...state,
    appName: savedText("appName"),
    displayName: savedText("displayName"),
    packageName: savedText("packageName"),
    packageEdited: saved.packageEdited === true,
    modules: savedText("modules"),
    languages: strings(saved.languages).filter((tag) => offered.has(tag)),
    architecture: architecture?.key ?? state.architecture,
    features: featuresFor(Array.isArray(saved.features) ? strings(saved.features) : state.features, target, designStyle?.key ?? state.designStyle),
    designStyle: designStyle?.key ?? state.designStyle,
    motionStyle: motionStyle?.key ?? state.motionStyle,
    hapticsEnabled: typeof saved.hapticsEnabled === "boolean" ? saved.hapticsEnabled : state.hapticsEnabled,
    accent: savedText("accent"),
    secondary: savedText("secondary"),
    tertiary: savedText("tertiary"),
    fontName: savedText("fontName"),
    settings,
  };
}
