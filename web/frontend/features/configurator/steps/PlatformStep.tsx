"use client";

import { useId } from "react";
import { ChoiceCard } from "../../../components/primitives.tsx";
import { Badge } from "../../../components/ui.tsx";
import type { Platform } from "../../catalogue/platforms.ts";
import type { Catalogue, Target } from "../../catalogue/types.ts";
import type { ConfiguratorModel } from "../useConfigurator.ts";

/** What another target leaves out that Android offers, by feature, plus any layout it cannot do yet. */
function notYet(target: Target, android: Target | undefined): string[] {
  if (!android || target.key === android.key) return [];
  const own = new Set(target.features.map((feature) => feature.key));
  const missing = android.features.filter((feature) => !feature.implied_only && !own.has(feature.key)).map((feature) => feature.title);
  const layouts = target.architectures.filter((option) => !option.available).map((option) => `${option.title} architecture`);
  return [...missing, ...layouts];
}

export function PlatformStep({ catalogue, platforms, model }: { catalogue: Catalogue; platforms: Platform[]; model: ConfiguratorModel }) {
  const name = useId();
  const android = catalogue.targets.find((target) => target.key === "android");
  // A platform this deployment does not generate is, to a visitor, not out yet, whatever its
  // catalogue status: it reads as coming soon, with nothing to pick.
  const isSoon = (platform: Platform) =>
    platform.status === "coming_soon" || (model.live.enabled !== null && model.live.enabled[platform.key] === false);
  const offered = platforms.filter((platform) => !isSoon(platform));
  const only = offered.length === 1 ? offered[0] : undefined;
  return (
    <div>
      <p className="max-w-2xl text-sm text-ink-300">
        {offered.length === 0
          ? "This site is not making projects right now. The command line tool in the repository makes the same ones on your own machine."
          : only
            ? `${only.title} is the platform this version makes.${platforms.length > 1 ? " The others are not ready yet." : ""}`
            : "Which app to start from. Your answers carry over if you change your mind; anything the other platform does not offer is left out."}
      </p>
      {/* A fieldset rather than role=radiogroup: each card's "Not in this version" disclosure sits
          beside its radio, and a radiogroup may hold only radios. */}
      <fieldset className="mt-5 grid grid-cols-1 gap-3 md:grid-cols-3">
        <legend className="sr-only">Platform</legend>
        {platforms.map((platform) => {
          const soon = isSoon(platform);
          const target = soon ? undefined : catalogue.targets.find((candidate) => candidate.key === platform.key);
          const missing = target ? notYet(target, android) : [];
          return (
            <div key={platform.key} className="flex flex-col gap-2">
              <ChoiceCard
                type="radio"
                name={name}
                checked={model.state.target === platform.key}
                disabled={soon}
                onChange={() => model.dispatch({ type: "target", target: platform.key })}
                className="flex-1 p-5"
              >
                <span className="flex items-start justify-between gap-3">
                  <span className={`font-display text-lg font-bold ${soon ? "text-ink-300" : "text-ink-100"}`}>{platform.title}</span>
                  {soon ? <Badge>Coming soon</Badge> : platform.status === "beta" ? <Badge tone="amber">Beta</Badge> : null}
                </span>
                <span className={`mt-2 block text-sm leading-relaxed ${soon ? "text-ink-400" : "text-ink-300"}`}>{platform.summary}</span>
                {!soon && platform.status === "beta" && platform.status_note && (
                  <span className="mt-3 block text-xs text-amber">{platform.status_note}</span>
                )}
              </ChoiceCard>
              {missing.length > 0 && (
                <details className="px-1 text-xs text-ink-400">
                  <summary className="cursor-pointer select-none text-ink-300">Not in this version ({missing.length})</summary>
                  <p className="mt-1.5 leading-relaxed">{missing.join(", ")}.</p>
                </details>
              )}
            </div>
          );
        })}
      </fieldset>
    </div>
  );
}
