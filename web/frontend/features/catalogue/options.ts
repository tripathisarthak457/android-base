import type { Catalogue, Option, Target } from "./types.ts";

/** The spec field, option key and marker that leave a project's UI components out. */
export const UI_COMPONENTS = "ui_components";

/** The target's UI components option, or null for a target whose projects always have them. */
export function uiComponentsOption(target: Target): Option | null {
  return target.options.find((option) => option.key === UI_COMPONENTS) ?? null;
}

/**
 * Whether a project for `target` gets UI components when the visitor asks for `wanted`: always for
 * a target without the option, and the locked value on a deployment that generates only one.
 */
export function effectiveUiComponents(target: Target, wanted: boolean): boolean {
  const option = uiComponentsOption(target);
  if (!option) return true;
  return option.locked ? option.locked.value : wanted;
}

/** The features a project cannot have, each with the catalogue's reason. Empty while it has UI components. */
export function disabledFeatures(target: Target, uiComponents: boolean): Map<string, string> {
  const option = uiComponentsOption(target);
  if (!option || uiComponents) return new Map();
  return new Map(option.disables.map((entry) => [entry.feature, entry.reason]));
}

/** The spec fields a project without UI components leaves at their defaults, each with the reason. */
export function hiddenFields(target: Target, uiComponents: boolean): Map<string, string> {
  const option = uiComponentsOption(target);
  if (!option || uiComponents) return new Map();
  return new Map(option.hides.map((entry) => [entry.field, entry.reason]));
}

/**
 * Why this deployment generates only projects without UI components, in the lock's own words, or
 * null when it generates both.
 */
export function setupOnlyNote(catalogue: Catalogue): string | null {
  for (const target of catalogue.targets) {
    const locked = uiComponentsOption(target)?.locked;
    if (locked?.value === false) return locked.note;
  }
  return null;
}

export function isSetupOnly(catalogue: Catalogue): boolean {
  return setupOnlyNote(catalogue) !== null;
}

/** What the locked site says beside everything it cannot make yet. */
export const COMING_WITH_RELEASE = "Coming with the Android release";

/**
 * The note for a platform whose projects cannot leave UI components out yet, while another can, or
 * null. The site names it where that platform is picked, in place of the box.
 */
export function androidOnlyNote(catalogue: Catalogue, platform: string): string | null {
  const own = catalogue.targets.find((target) => target.key === platform);
  if (own && uiComponentsOption(own)) return null;
  const android = catalogue.targets.find((target) => target.key === "android");
  const option = android ? uiComponentsOption(android) : null;
  if (!option) return null;
  return `${option.checkbox.label} is Android-only for now; it comes to Compose Multiplatform and Swift after the Android release.`;
}
