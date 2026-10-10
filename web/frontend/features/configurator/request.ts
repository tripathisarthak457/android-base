import { clientHeader } from "../../lib/api.ts";
import type { Catalogue, Field, Target } from "../catalogue/types.ts";
import { type ConfigState, findTarget, hasKeystores, lowerName, moduleNames, settingFields } from "./state.ts";

// The generate request: the schema-2 spec built from the configurator's answers, and the session
// headers that tie a generation to the visit it came from. The spec stays free of telemetry,
// because the generator refuses unknown fields.

export type Spec = Record<string, unknown>;

/** In the catalogue's order, so the app's language picker lists them the same way every time. */
function orderedLanguages(state: ConfigState, target: Target): string[] {
  return target.languages.map((language) => language.tag).filter((tag) => state.languages.includes(tag));
}

function applies(field: Field, features: ReadonlySet<string>): boolean {
  return (field.requires ?? []).every((key) => features.has(key));
}

function nonEmpty(map: Record<string, string>): Record<string, string> {
  return Object.fromEntries(Object.entries(map).filter(([, value]) => value.trim() !== ""));
}

function block(state: ConfigState, target: Target, name: string): Spec {
  const features = new Set(state.features);
  const spec: Spec = {};
  for (const field of settingFields(target)) {
    if (!field.key.startsWith(`${name}.`) || !applies(field, features)) continue;
    const key = field.key.slice(name.length + 1);
    const value = state.settings[field.key];
    if (field.type === "keystores") {
      if (state.signing.enabled) spec[key] = keystores(state, field);
    } else if (typeof value === "object") {
      const entries = nonEmpty(value);
      if (Object.keys(entries).length > 0) spec[key] = entries;
    } else if (typeof value === "string") {
      if (value.trim() !== "") spec[key] = value;
    } else if (typeof value === "number") {
      spec[key] = value;
    }
  }
  if (name === "android") clampSdks(spec, target);
  return spec;
}

/**
 * targetSdk follows minSdk up rather than being refused, and compileSdk is never below either:
 * the visitor picks the two that matter and the build stays compilable.
 */
function clampSdks(android: Spec, target: Target): void {
  const min = android.min_sdk;
  if (typeof min !== "number") return;
  const targetSdk = Math.max(typeof android.target_sdk === "number" ? android.target_sdk : min, min);
  android.target_sdk = targetSdk;
  const compileDefault = target.fields.find((field) => field.key === "android.compile_sdk")?.default;
  android.compile_sdk = Math.max(typeof compileDefault === "number" ? compileDefault : targetSdk, targetSdk);
}

/**
 * One generated key per name the build knows. The store and the key share one password: a
 * PKCS12 store has only one, and it is written to `keystore.properties` in the same zip.
 */
function keystores(state: ConfigState, field: Field): Spec[] {
  const alias = lowerName(state.appName) || "app";
  return (field.names ?? []).map((name) => ({
    name,
    alias: `${alias}-${name}`,
    store_password: state.signing.passwords[name] ?? "",
    common_name: `${state.appName.trim()} (${name})`,
    organisation: state.signing.organisation.trim(),
    country: state.signing.country.trim().toUpperCase(),
  }));
}

export function toSpecV2(state: ConfigState, catalogue: Catalogue): Spec {
  const target = findTarget(catalogue, state.target);
  const spec: Spec = {
    schema: 2,
    platform: target.key,
    app_name: state.appName,
    package_name: state.packageName,
    architecture: state.architecture,
    features: [...state.features].sort(),
    feature_modules: moduleNames(state.modules),
    design_style: state.designStyle,
    motion_style: state.motionStyle,
    accent_colour: state.accent,
    font_name: state.fontName,
    mono_font_name: target.defaults.mono_font_name,
    haptics_enabled: state.hapticsEnabled,
    languages: orderedLanguages(state, target),
  };
  if (state.displayName !== "") spec.display_name = state.displayName;
  if (state.secondary !== "") spec.secondary_colour = state.secondary;
  if (state.tertiary !== "") spec.tertiary_colour = state.tertiary;
  for (const name of new Set(settingFields(target).map((field) => field.key.split(".")[0] ?? ""))) {
    spec[name] = block(state, target, name);
  }
  return spec;
}

/** Whether the Build step should offer signing keys for this target and deployment. */
export function offersSigning(target: Target, keytool: boolean): boolean {
  return keytool && hasKeystores(target);
}

/** A random UUID v4, lowercase, as `randomSessionId` in features/telemetry writes it. */
export const SESSION_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

/** Two hours: the API ignores a longer configurator time rather than capping it. */
export const CONFIG_MS_MAX = 7_200_000;

/**
 * The headers that tie a generation to its visit. Each is sent only when the API would store it:
 * a value it would ignore is left out rather than sent malformed.
 */
export function sessionHeaders(session: { id: string | null; configMs: number | null; build: string }): Record<string, string> {
  const headers: Record<string, string> = { "X-Client": clientHeader(session.build) };
  if (session.id !== null && SESSION_ID.test(session.id)) headers["X-Session-Id"] = session.id.toLowerCase();
  if (session.configMs !== null && Number.isFinite(session.configMs)) {
    headers["X-Config-Ms"] = String(Math.min(Math.max(Math.round(session.configMs), 0), CONFIG_MS_MAX));
  }
  return headers;
}
