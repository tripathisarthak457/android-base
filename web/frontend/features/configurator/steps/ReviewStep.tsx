"use client";

import { useMemo } from "react";
import { Button } from "../../../components/primitives.tsx";
import { Spinner } from "../../../components/ui.tsx";
import { selectableFeatures } from "../../catalogue/features.ts";
import type { Catalogue } from "../../catalogue/types.ts";
import { ScaledPreview } from "../../preview/AppPreview.tsx";
import { usePreviewTheme } from "../../preview/LivePreview.tsx";
import { planTabs } from "../../preview/tabs.ts";
import { STEP_LABELS, stepOf } from "../state.ts";
import type { Configurator } from "../useConfigurator.ts";
import { usePreviewInput } from "../usePreviewInput.ts";

export function ReviewStep({ catalogue, model, dark }: { catalogue: Catalogue; model: Configurator; dark: boolean }) {
  const { state, target, errors, busy, outcome, generate, goTo, preset } = model;
  const input = usePreviewInput(catalogue, state);
  const theme = usePreviewTheme(input, dark);
  const tabs = useMemo(() => planTabs(input.features, input.modules).tabs, [input.features, input.modules]);
  const options = useMemo(() => ({ features: input.features, haptics: input.haptics }), [input.features, input.haptics]);
  const chosen = selectableFeatures(target).filter((feature) => state.features.includes(feature.key));
  const android = (key: string) => state.settings[`android.${key}`];
  const sdk = android("min_sdk") !== undefined ? `${android("min_sdk")} / ${Math.max(Number(android("target_sdk")), Number(android("min_sdk")))}` : null;
  const presetTitle = target.presets.find((option) => option.key === preset)?.title ?? "Your own";

  const rows: [string, string][] = [
    ["Platform", target.title],
    ["App", state.displayName ? `${state.appName} (shown as ${state.displayName})` : state.appName],
    ["Package", state.packageName],
    ["Architecture", target.architectures.find((option) => option.key === state.architecture)?.title ?? state.architecture],
    ...(sdk ? ([["Min / target SDK", sdk]] as [string, string][]) : []),
    ["Design style", state.designStyle],
    ["Motion", `${state.motionStyle}, haptics ${state.hapticsEnabled ? "on" : "off"}`],
    ["Typeface", state.fontName],
    ["Modules", input.modules.length ? input.modules.join(", ") : "none"],
    ["Languages", ["English", ...target.languages.filter((language) => state.languages.includes(language.tag)).map((language) => language.name)].join(", ")],
    ["Signing keys", state.signing.enabled ? "generated here" : "debug key only"],
  ];
  const screen = tabs[0];

  return (
    <div className="grid grid-cols-1 gap-8 md:grid-cols-5">
      <div className="min-w-0 space-y-5 md:col-span-3">
        <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {rows.map(([label, value]) => (
            <div key={label} className="rounded-lg border border-ink-700 bg-ink-900 px-4 py-3">
              <dt className="text-xs text-ink-400">{label}</dt>
              <dd className="mt-0.5 truncate text-sm font-medium text-ink-100" title={value}>
                {value}
              </dd>
            </div>
          ))}
          <div className="rounded-lg border border-ink-700 bg-ink-900 px-4 py-3">
            <dt className="text-xs text-ink-400">Brand colours</dt>
            <dd className="mt-1 flex items-center gap-2">
              {["accent", "secondary", "tertiary"].map((role) => (
                <span key={role} className="h-4 w-4 rounded border border-ink-600" style={{ background: theme.colour(role) }} />
              ))}
              <span className="font-mono text-sm text-ink-100">{state.accent.toUpperCase()}</span>
            </dd>
          </div>
        </dl>

        <div>
          <p className="mb-2 text-sm font-medium text-ink-200">
            {chosen.length} features · {presetTitle}
          </p>
          <ul className="flex flex-wrap gap-1.5">
            {chosen.map((feature) => (
              <li key={feature.key} title={feature.description} className="rounded-md border border-ink-600 bg-ink-900 px-2 py-1 text-[12px] text-ink-300">
                {feature.title}
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="min-w-0 md:col-span-2">
        {screen && (
          <div className="mb-4 flex justify-center">
            <ScaledPreview theme={theme} font={state.fontName} tabs={tabs} screen={screen} options={options} scale={0.62} />
          </div>
        )}

        {errors.size > 0 && (
          <div role="alert" className="mb-4 rounded-lg border border-rose/30 bg-rose/10 p-4 text-xs leading-relaxed text-rose">
            <p className="font-semibold">Fix these first:</p>
            <ul className="mt-1.5 space-y-1">
              {[...errors].map(([field, message]) => (
                <li key={field}>
                  {message}{" "}
                  <button type="button" onClick={() => goTo(stepOf(field))} className="font-semibold underline underline-offset-2">
                    {STEP_LABELS[stepOf(field)]}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        <div role="status">
          {outcome?.kind === "done" && (
            <div className="mb-4 rounded-xl border border-mint/30 bg-mint/10 p-5">
              <p className="text-sm font-semibold text-mint">Downloaded</p>
              <p className="mt-1.5 font-mono text-xs text-ink-300">{outcome.filename}</p>
              <p className="mt-1 text-xs text-ink-400">
                {(outcome.bytes / 1024).toFixed(0)} KB, built in {(outcome.elapsedMs / 1000).toFixed(1)} s
              </p>
              {outcome.keystoresSkipped.length > 0 && (
                <p className="mt-3 rounded-lg border border-amber/30 bg-amber/10 p-3 text-xs leading-relaxed text-amber">
                  Not created: {outcome.keystoresSkipped.join(", ")}. Those variants sign with the debug key until you make them.
                </p>
              )}
              <ol className="mt-4 list-decimal space-y-1.5 pl-4 text-xs text-ink-300">
                <li>Unzip and open the folder in Android Studio.</li>
                <li>
                  <code className="text-ink-200">./gradlew :app:installDevDebug</code>
                </li>
                <li>
                  <code className="text-ink-200">./gradlew build</code> checks everything: compile, tests, lint and the code rules.
                </li>
                <li>
                  Using an AI agent? Point it at <code className="text-ink-200">AGENTS.md</code> first.
                </li>
              </ol>
            </div>
          )}
        </div>
        {outcome?.kind === "failed" && (
          <p role="alert" className="mb-4 rounded-lg border border-rose/30 bg-rose/10 p-4 text-xs leading-relaxed text-rose">
            {outcome.message}
            {outcome.field && (
              <>
                {" "}
                <button type="button" onClick={() => goTo(stepOf(outcome.field))} className="font-semibold underline underline-offset-2">
                  {STEP_LABELS[stepOf(outcome.field)]}
                </button>
              </>
            )}
          </p>
        )}

        <Button size="large" onClick={() => void generate()} disabled={busy || errors.size > 0} className="w-full">
          {busy ? (
            <>
              <Spinner /> Generating…
            </>
          ) : outcome?.kind === "done" ? (
            "Generate again"
          ) : (
            "Generate and download"
          )}
        </Button>
        {state.signing.enabled && (
          <p className="mt-3 text-xs leading-relaxed text-amber">The zip holds all your signing keys and their passwords. Keep it out of version control.</p>
        )}
      </div>
    </div>
  );
}
