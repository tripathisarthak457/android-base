import type { Catalogue, Target, TargetStatus } from "./types.ts";

/** A platform as the site names it, whether or not the generator has a target for it yet. */
export type Platform = { key: string; title: string; summary: string; status: TargetStatus; status_note: string };

/** A platform named before the generator has a target for it. */
export type Announced = Pick<Platform, "key" | "title" | "summary">;

/**
 * `catalogue` as this site offers it: a target outside `offered` is coming soon, whatever the
 * generator says of it. A template reaches the generator, and its CLI, before it has been verified
 * well enough for the site to hand it to anyone who asks.
 */
export function offerOnly(catalogue: Catalogue, offered: ReadonlySet<string>): Catalogue {
  return {
    ...catalogue,
    targets: catalogue.targets.map((target) =>
      offered.has(target.key) || target.status === "hidden" ? target : { ...target, status: "coming_soon" },
    ),
  };
}

/** Targets a visitor can pick: not hidden and not announced as coming soon. */
export function selectableTargets(catalogue: Catalogue): Target[] {
  return catalogue.targets.filter((target) => target.status === "available" || target.status === "beta");
}

/**
 * The platforms the site shows, in order: the catalogue's targets that are not hidden, then each
 * announced platform the catalogue has no target for, as coming soon.
 */
export function platformsOf(catalogue: Catalogue, announced: readonly Announced[]): Platform[] {
  const shown: Platform[] = catalogue.targets
    .filter((target) => target.status !== "hidden")
    .map(({ key, title, summary, status, status_note }) => ({ key, title, summary, status, status_note }));
  const known = new Set(catalogue.targets.map((target) => target.key));
  const upcoming = announced
    .filter((platform) => !known.has(platform.key))
    .map((platform): Platform => ({ ...platform, status: "coming_soon", status_note: "" }));
  return [...shown, ...upcoming];
}
