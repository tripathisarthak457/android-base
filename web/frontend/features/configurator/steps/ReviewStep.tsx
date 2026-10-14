"use client";

import { useMemo } from "react";
import { Button } from "../../../components/primitives.tsx";
import { Spinner } from "../../../components/ui.tsx";
import { selectableFeatures } from "../../catalogue/features.ts";
import type { Catalogue } from "../../catalogue/types.ts";
import { ScaledPreview } from "../../preview/AppPreview.tsx";
import { useFamily } from "../../preview/fonts.ts";
import { planTabs } from "../../preview/tabs.ts";
import { weightsFor } from "../../preview/theme.ts";
import { usePreviewTheme } from "../../preview/usePreviewTheme.ts";
import type { Spec } from "../request.ts";
import { STEP_LABELS, stepOf } from "../state.ts";
import type { ConfiguratorModel } from "../useConfigurator.ts";
import { usePreviewInput } from "../usePreviewInput.ts";

export function ReviewStep({ catalogue, model, dark }: { catalogue: Catalogue; model: ConfiguratorModel; dark: boolean }) {
  const { state, target, spec, errors, busy, outcome, generate, goTo, preset } = model;
  const input = usePreviewInput(catalogue, target, state);
  const theme = usePreviewTheme(input, dark);
  const tabs = useMemo(() => planTabs(input.features, input.modules).tabs, [input.features, input.modules]);
  const options = useMemo(() => ({ features: input.features, haptics: input.haptics }), [input.features, input.haptics]);
  // The step bar reaches Review without passing the Look step, which would otherwise load the typeface.
  const weights = useMemo(() => weightsFor(input.tokens, theme.style.voice), [input.tokens, theme.style.voice]);
  useFamily(input.font, weights);
  const chosen = selectableFeatures(target).filter((feature) => state.features.includes(feature.key));
  // The SDKs as they are sent: the request builder raises targetSdk to minSdk.
  const { min_sdk: minSdk, target_sdk: targetSdk } = (spec.android ?? {}) as Spec;
  const sdk = typeof minSdk === "number" && typeof targetSdk === "number" ? `${minSdk} / ${targetSdk}` : null;
  const presetTitle = target.presets.find((option) => option.key === preset)?.title ?? "Your own";

  const rows: [string, string][] = [
    ["Platform", target.title],
    ["App", state.displayName ? `${state.appName} (shown as ${state.displayName})` : state.appName],
    ["Package", state.packageName],
    ["Architecture", target.architectures.find((option) => option.key === state.architecture)?.title ?? state.architecture],
    ...(sdk ? ([["Min / target SDK", sdk]] as [string, string][]) : []),
    ["Design style", target.design_styles.find((style) => style.key === state.designStyle)?.title ?? state.designStyle],
    [
      "Motion",
      `${target.motion_styles.find((style) => style.key === state.motionStyle)?.title ?? state.motionStyle}, haptics ${state.hapticsEnabled ? "on" : "off"}`,
    ],
    ["Typeface", input.font],
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
            <ScaledPreview theme={theme} font={input.font} tabs={tabs} screen={screen} options={options} scale={0.62} />
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
                {(outcome.bytes / 1024).toFixed(0)} KB{outcome.elapsedMs !== null && `, built in ${(outcome.elapsedMs / 1000).toFixed(1)} s`}
              </p>
              {outcome.keystoresSkipped.length > 0 && (
                <p className="mt-3 rounded-lg border border-amber/30 bg-amber/10 p-3 text-xs leading-relaxed text-amber">
                  Not created: {outcome.keystoresSkipped.join(", ")}. Those variants sign with the debug key until you make them.
                </p>
              )}
              <ol className="mt-4 list-decimal space-y-1.5 pl-4 text-xs text-ink-300">
                <NextSteps target={target.key} />
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

        <Button size="large" onClick={() => void generate()} disabled={busy || errors.size > 0} focusableWhenDisabled className="w-full">
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

/** What to do with the zip. Each template names its app module differently; one this list does not know points at its README. */
function NextSteps({ target }: { target: string }) {
  switch (target) {
    case "android":
      return (
        <>
          <li>Unzip and open the folder in Android Studio.</li>
          <li>
            <code className="text-ink-200">./gradlew :app:installDevDebug</code>
          </li>
          <li>
            <code className="text-ink-200">./gradlew build</code> checks everything: compile, tests, lint and the code rules.
          </li>
        </>
      );
    case "multiplatform":
      return (
        <>
          <li>Unzip and open the folder in Android Studio.</li>
          <li>
            <code className="text-ink-200">./gradlew :androidApp:installDevDebug</code>
          </li>
          <li>
            On a Mac with XcodeGen: <code className="text-ink-200">cd iosApp &amp;&amp; xcodegen generate</code>, then open the Xcode
            project it writes and run it.
          </li>
        </>
      );
    default:
      return <li>Unzip it and follow its README.</li>;
  }
}
