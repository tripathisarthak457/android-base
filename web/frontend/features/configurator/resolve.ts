import type { Feature, Target } from "../catalogue/types.ts";

/** `keys` plus everything they need, as the generator resolves a spec's features. */
export function closeOver(keys: Iterable<string>, target: Target): Set<string> {
  const byKey = featureMap(target);
  const resolved = new Set<string>();
  for (const key of keys) {
    const feature = byKey.get(key);
    if (!feature) continue;
    resolved.add(key);
    for (const implied of feature.implies) resolved.add(implied);
  }
  return resolved;
}

export type Toggle = {
  next: Set<string>;
  /** Features that came or went besides the one toggled, for the "also added / also removed" hint. */
  alsoAdded: string[];
  alsoRemoved: string[];
};

/**
 * Ticking a feature also ticks what it needs. Unticking one also unticks everything that needs it,
 * directly or through another feature: `implies` is already transitive, so one pass finds them all.
 * Leaving a dependent on would only have the generator add the unticked feature back.
 */
export function toggleFeature(selected: ReadonlySet<string>, key: string, target: Target): Toggle {
  const next = new Set(selected);
  if (selected.has(key)) {
    next.delete(key);
    const alsoRemoved: string[] = [];
    for (const feature of target.features) {
      if (next.has(feature.key) && feature.implies.includes(key)) {
        next.delete(feature.key);
        alsoRemoved.push(feature.key);
      }
    }
    return { next, alsoAdded: [], alsoRemoved };
  }
  const alsoAdded: string[] = [];
  next.add(key);
  for (const implied of featureMap(target).get(key)?.implies ?? []) {
    if (!next.has(implied)) {
      next.add(implied);
      alsoAdded.push(implied);
    }
  }
  return { next, alsoAdded, alsoRemoved: [] };
}

/** The ticked features that need `key`, which unticking it would take with it. */
export function dependentsOf(selected: ReadonlySet<string>, key: string, target: Target): string[] {
  return target.features
    .filter((feature) => selected.has(feature.key) && feature.implies.includes(key))
    .map((feature) => feature.key);
}

/** The preset whose features are exactly `resolved`, or null for a set of the visitor's own. */
export function derivePreset(resolved: ReadonlySet<string>, target: Target): string | null {
  const match = target.presets.find(
    (preset) =>
      preset.features.length === resolved.size && preset.features.every((key) => resolved.has(key)),
  );
  return match?.key ?? null;
}

export function presetFeatures(target: Target, key: string): Set<string> {
  const preset = target.presets.find((candidate) => candidate.key === key);
  return closeOver(preset?.features ?? [], target);
}

function featureMap(target: Target): Map<string, Feature> {
  return new Map(target.features.map((feature) => [feature.key, feature]));
}
