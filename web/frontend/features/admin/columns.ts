import type { Generation } from "./api.ts";

/** The Generations table's headings, in the order of a row's cells. */
export const GENERATION_COLUMNS = ["When", "Outcome", "App", "Target", "Preset", "UI components", "Features", "Style", "SDK", "Time", "Where"] as const;

/** A row's UI components as the spec asked for them; a request refused before its spec was read has none. */
export function uiComponentsCell(value: Generation["ui_components"]): string {
  return value || "—";
}
