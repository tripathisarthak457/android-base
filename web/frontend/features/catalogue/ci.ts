import { listed } from "../../lib/text.ts";
import type { Target } from "./types.ts";

/**
 * What `.github/workflows/ci.yml` generates and builds before a change ships: preset keys, plus
 * `all` (every feature at once) and `none`. The site words its CI claims from this list, and
 * `ci.test.ts` holds it equal to the workflow's matrix, so the copy never names a build CI skips.
 */
export const CI_PROJECTS: readonly string[] = ["none", "lean", "everything", "all"];

export type CiPresets = {
  /** Lower case, to sit inside a sentence: "every preset" or "the Lean and Everything presets". */
  phrase: string;
  plural: boolean;
};

export function ciPresets(target: Target, projects: readonly string[] = CI_PROJECTS): CiPresets {
  const built = target.presets.filter((preset) => projects.includes(preset.key));
  if (built.length === target.presets.length) return { phrase: "every preset", plural: false };
  const plural = built.length > 1;
  return { phrase: `the ${listed(built.map((preset) => preset.title))} preset${plural ? "s" : ""}`, plural };
}
