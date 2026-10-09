import type { Catalogue } from "./types.ts";

type Json = Record<string, unknown>;

function isObject(value: unknown): value is Json {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === "string");
}

function hasStrings(value: Json, keys: string[]): boolean {
  return keys.every((key) => typeof value[key] === "string");
}

function isTarget(value: unknown): boolean {
  if (!isObject(value) || !hasStrings(value, ["key", "title", "status", "summary"])) return false;
  const lists = ["groups", "features", "presets", "fields", "architectures", "design_styles", "motion_styles", "languages"];
  if (!lists.every((key) => Array.isArray(value[key]))) return false;
  if (!isObject(value.defaults)) return false;
  return (value.features as unknown[]).every(
    (feature) =>
      isObject(feature) &&
      hasStrings(feature, ["key", "title", "group"]) &&
      isStringArray(feature.requires) &&
      isStringArray(feature.implies),
  );
}

/**
 * The catalogue a server answered with, or null when its shape is not one this build can read: an
 * older or newer generator behind the API, a proxy's error page, a truncated body. The site then
 * keeps the copy it was built with.
 */
export function parseCatalogue(value: unknown): Catalogue | null {
  if (!isObject(value) || value.version !== 2) return null;
  if (!Array.isArray(value.targets) || !value.targets.every(isTarget)) return null;
  if (!isObject(value.rules) || !isObject(value.tokens)) return null;
  if (value.environment !== undefined && !isObject(value.environment)) return null;
  return value as Catalogue;
}

/** Whether the deployment that served `live` can run keytool for `target`; unknown means yes. */
export function canMakeKeystores(live: Catalogue | null, target: string): boolean {
  return live?.environment?.[target]?.keytool !== false;
}
