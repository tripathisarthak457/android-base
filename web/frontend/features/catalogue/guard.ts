import { type Json, isObject, isStringList } from "../../lib/json.ts";
import type { Catalogue } from "./types.ts";

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
      isStringList(feature.requires) &&
      isStringList(feature.implies),
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
