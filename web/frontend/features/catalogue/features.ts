import { selectableTargets } from "./platforms.ts";
import type { Catalogue, Feature, Target } from "./types.ts";

/** Features a visitor can switch on or off: everything not reached only through another. */
export function selectableFeatures(target: Target): Feature[] {
  return target.features.filter((feature) => !feature.implied_only);
}

/**
 * Optional features that bring third-party code under its own licence, each once across the
 * targets a visitor can pick. A coming-soon target's features are left out, so the licence copy
 * never names code from a project the site does not make.
 */
export function licensedFeatures(catalogue: Catalogue): Feature[] {
  const seen = new Set<string>();
  return selectableTargets(catalogue)
    .flatMap((target) => target.features)
    .filter((feature) => {
      if (!feature.licence || seen.has(feature.key)) return false;
      seen.add(feature.key);
      return true;
    });
}
