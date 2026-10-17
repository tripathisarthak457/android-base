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

/**
 * The features a visitor's choices come to: `keys` closed over what they need, less any feature the
 * visitor cannot tick (`implied_only`, like Firebase) that nothing chosen needs any more. Without
 * that, Firebase would stay in the spec after the last feature that brought it is unticked, while
 * no step shows it or offers a way to remove it.
 */
export function resolveChoices(keys: Iterable<string>, target: Target): Set<string> {
  const byKey = featureMap(target);
  return closeOver([...keys].filter((key) => !byKey.get(key)?.implied_only), target);
}

export type Toggle = {
  next: Set<string>;
  /** Features that came or went besides the one toggled, for the "also added / also removed" hint. */
  alsoAdded: string[];
  alsoRemoved: string[];
};

/**
 * Ticking a feature also ticks what it needs. Unticking one also unticks everything that needs it,
 * directly or through another feature (`implies` is already transitive), since leaving a dependent
 * on would only have the generator add the unticked feature back.
 */
export function toggleFeature(selected: ReadonlySet<string>, key: string, target: Target): Toggle {
  const byKey = featureMap(target);
  const kept = selected.has(key)
    ? [...selected].filter((other) => other !== key && !byKey.get(other)?.implies.includes(key))
    : [...selected, key];
  const next = resolveChoices(kept, target);
  const others = target.features.map((feature) => feature.key).filter((other) => other !== key);
  return {
    next,
    alsoAdded: others.filter((other) => next.has(other) && !selected.has(other)),
    alsoRemoved: others.filter((other) => selected.has(other) && !next.has(other)),
  };
}

/** The ticked features that need `key`, which unticking it would take with it. */
export function dependentsOf(selected: ReadonlySet<string>, key: string, target: Target): string[] {
  return target.features
    .filter((feature) => selected.has(feature.key) && feature.implies.includes(key))
    .map((feature) => feature.key);
}

/** The preset whose features are exactly `resolved`, or null for a set of the visitor's own. */
export function derivePreset(resolved: ReadonlySet<string>, target: Target): string | null {
  const match = target.presets.find((preset) => {
    const features = presetFeatures(target, preset.key);
    return features.size === resolved.size && [...features].every((key) => resolved.has(key));
  });
  return match?.key ?? null;
}

export function presetFeatures(target: Target, key: string): Set<string> {
  const preset = target.presets.find((candidate) => candidate.key === key);
  return resolveChoices(preset?.features ?? [], target);
}

function featureMap(target: Target): Map<string, Feature> {
  return new Map(target.features.map((feature) => [feature.key, feature]));
}
