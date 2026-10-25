"use client";

import { useId, useMemo } from "react";
import { Field, TextInput } from "../../../components/primitives.tsx";
import { codePoints, listed } from "../../../lib/text.ts";
import { MAX_RAIL_TABS, MAX_TABS, planTabs } from "../../preview/tabs.ts";
import { moduleCopy } from "../../catalogue/options.ts";
import type { Catalogue } from "../../catalogue/types.ts";
import { deepLinkScheme } from "../request.ts";
import { moduleNames } from "../state.ts";
import { producesNote } from "../uiComponents.ts";
import type { ConfiguratorModel } from "../useConfigurator.ts";

export function IdentityStep({ catalogue, model }: { catalogue: Catalogue; model: ConfiguratorModel }) {
  const { state, dispatch, errors, target } = model;
  const pascal = state.appName
    .split(/[^A-Za-z0-9]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join("");
  const derived = pascal || "MyApp";
  const modules = useMemo(() => moduleNames(state.modules), [state.modules]);
  const plan = useMemo(() => planTabs(new Set(state.features), modules), [state.features, modules]);
  const noUiModules = moduleCopy(target, state.uiComponents);
  const displayMax = catalogue.rules.display_name_max;
  const languagesId = useId();
  const languagesError = errors.get("languages");
  const placeholderPackage = !errors.has("package_name") && /^com\.example(\.|$)/.test(state.packageName);

  return (
    <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
      <div className="min-w-0 space-y-5">
        <Field
          label="App name"
          hint="A letter, then letters, digits, spaces, dots, hyphens or underscores. It names the project and its classes."
          error={errors.get("app_name")}
        >
          {(control) => (
            <TextInput {...control} value={state.appName} onChange={(event) => dispatch({ type: "appName", value: event.target.value })} placeholder="My App" />
          )}
        </Field>

        <Field
          label="Name on the home screen (optional)"
          hint={
            <>
              Any characters, emoji included: &quot;Café&quot;, &quot;<span lang="hi">मेरा ऐप</span>&quot;. Empty uses the app name.{" "}
              {codePoints(state.displayName)} of {displayMax}.
            </>
          }
          error={errors.get("display_name")}
        >
          {(control) => (
            <TextInput
              {...control}
              value={state.displayName}
              onChange={(event) => dispatch({ type: "displayName", value: event.target.value })}
              placeholder={state.appName}
            />
          )}
        </Field>

        <Field
          label="Package name"
          hint={
            placeholderPackage
              ? "Play Console refuses com.example ids. Fine for trying this out; pick your own before you publish."
              : "The application id. It cannot change after the app is on Play."
          }
          error={errors.get("package_name")}
        >
          {(control) => (
            <TextInput
              {...control}
              value={state.packageName}
              onChange={(event) => dispatch({ type: "packageName", value: event.target.value })}
              placeholder="com.example.myapp"
              className="font-mono text-sm"
              spellCheck={false}
              autoCapitalize="off"
            />
          )}
        </Field>

        <Field
          label="Feature modules (optional)"
          hint={
            noUiModules
              ? `Comma-separated. ${noUiModules}`
              : plan.overflow.length > 0
                ? `Each gets a data and a feature module and a tab. A phone's bar holds ${MAX_TABS}, so ${listed(plan.overflow.map((tab) => tab.label))} go under More; ${plan.all.length <= MAX_RAIL_TABS ? "a wider window's rail" : "a large window's sidebar"} shows every tab.`
                : "Comma-separated. Each gets a data and a feature module with a repository, ViewModel, screen and tests, and a tab."
          }
          error={errors.get("feature_modules")}
        >
          {(control) => (
            <TextInput
              {...control}
              value={state.modules}
              onChange={(event) => dispatch({ type: "modules", value: event.target.value })}
              placeholder="orders, wallet"
              className="font-mono text-sm"
              spellCheck={false}
            />
          )}
        </Field>

        {target.languages.length > 0 && (
          <fieldset aria-describedby={`${languagesId}-hint${languagesError ? ` ${languagesId}-error` : ""}`}>
            <legend className="mb-1.5 text-sm font-medium text-ink-200">Languages</legend>
            <div className="flex flex-wrap gap-2">
              {target.languages.map((language) => {
                const on = state.languages.includes(language.tag);
                return (
                  <button
                    key={language.tag}
                    type="button"
                    aria-pressed={on}
                    lang={language.tag}
                    onClick={() => dispatch({ type: "language", tag: language.tag })}
                    className={`rounded-md border px-3 py-1.5 text-sm transition-colors ${
                      on ? "border-accent bg-accent-dim text-ink-100 ring-1 ring-accent" : "border-ink-600 bg-ink-900 text-ink-300 hover:text-ink-100"
                    }`}
                  >
                    {language.name}
                  </button>
                );
              })}
            </div>
            <p id={`${languagesId}-hint`} className="mt-1.5 text-xs text-ink-400">
              English is always included. Each one you pick translates every screen and joins the language picker.
            </p>
            {languagesError && (
              <p id={`${languagesId}-error`} className="mt-1 text-xs text-rose">
                {languagesError}
              </p>
            )}
          </fieldset>
        )}
      </div>

      <div className="rounded-lg border border-ink-700 bg-ink-900 p-5">
        <p className="text-xs font-medium uppercase tracking-wide text-ink-400">What that produces</p>
        <dl className="mt-4 space-y-3 font-mono text-[13px]">
          {[
            ["Root project", derived],
            ["Application class", `${derived}Application`],
            ["Namespace", state.packageName || "com.example.myapp"],
            ["Deep-link scheme", `${deepLinkScheme(model.spec, state.appName)}://`],
            ...modules.map((name) => [`Module ${name}`, `:feature:${name}, :data:${name}`]),
          ].map(([label, value], index) => (
            // Module names can repeat while being typed, so the label alone is not a unique key.
            <div key={`${index}-${label}`} className="flex items-baseline justify-between gap-3">
              <dt className="shrink-0 font-sans text-xs text-ink-400">{label}</dt>
              <dd className="truncate text-ink-200">{value}</dd>
            </div>
          ))}
        </dl>
        <p className="mt-5 border-t border-ink-700 pt-4 text-xs leading-relaxed text-ink-400">{producesNote(state)}</p>
      </div>
    </div>
  );
}
