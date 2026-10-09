"use client";

import { useId } from "react";
import { ChoiceCard } from "../../../components/primitives.tsx";
import { Badge } from "../../../components/ui.tsx";
import type { Catalogue, Target } from "../../catalogue/types.ts";
import type { Configurator } from "../useConfigurator.ts";

/** What another target leaves out that Android offers, by feature, plus any layout it cannot do yet. */
function notYet(target: Target, android: Target | undefined): string[] {
  if (!android || target.key === android.key) return [];
  const own = new Set(target.features.map((feature) => feature.key));
  const missing = android.features.filter((feature) => !feature.implied_only && !own.has(feature.key)).map((feature) => feature.title);
  const layouts = target.architectures.filter((option) => !option.available).map((option) => `${option.title} architecture`);
  return [...missing, ...layouts];
}

export function PlatformStep({ catalogue, model }: { catalogue: Catalogue; model: Configurator }) {
  const name = useId();
  const android = catalogue.targets.find((target) => target.key === "android");
  const shown = catalogue.targets.filter((target) => target.status !== "hidden");
  return (
    <div>
      <p className="max-w-2xl text-sm text-ink-300">
        Which app to start from. Your answers carry over if you change your mind; anything the other platform does not offer
        is left out.
      </p>
      <div role="radiogroup" aria-label="Platform" className="mt-5 grid gap-3 md:grid-cols-3">
        {shown.map((target) => {
          const soon = target.status === "coming_soon";
          const off = model.live.enabled !== null && model.live.enabled[target.key] === false && !soon;
          const missing = notYet(target, android);
          return (
            <div key={target.key} className="flex flex-col gap-2">
              <ChoiceCard
                type="radio"
                name={name}
                checked={model.state.target === target.key}
                disabled={soon || off}
                onChange={() => model.dispatch({ type: "target", target: target.key })}
                className="flex-1 p-5"
              >
                <span className="flex items-start justify-between gap-3">
                  <span className="font-display text-lg font-bold text-ink-100">{target.title}</span>
                  {soon ? <Badge>Coming soon</Badge> : target.status === "beta" ? <Badge tone="amber">Beta</Badge> : null}
                </span>
                <span className="mt-2 block text-sm leading-relaxed text-ink-300">{target.summary}</span>
                {target.status === "beta" && target.status_note && <span className="mt-3 block text-xs text-amber">{target.status_note}</span>}
                {off && <span className="mt-3 block text-xs text-ink-400">This server does not generate it yet.</span>}
              </ChoiceCard>
              {!soon && missing.length > 0 && (
                <details className="px-1 text-xs text-ink-400">
                  <summary className="cursor-pointer select-none text-ink-300">Not in this version ({missing.length})</summary>
                  <p className="mt-1.5 leading-relaxed">{missing.join(", ")}.</p>
                </details>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
