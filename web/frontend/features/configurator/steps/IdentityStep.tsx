"use client";

import { useMemo } from "react";
import { Field, TextInput } from "../../../components/primitives.tsx";
import { MAX_TABS, planTabs } from "../../preview/tabs.ts";
import { lowerName, moduleNames } from "../state.ts";
import type { Configurator } from "../useConfigurator.ts";

function listed(names: string[]): string {
  return names.length < 2 ? names.join("") : `${names.slice(0, -1).join(", ")} and ${names.at(-1)}`;
}

export function IdentityStep({ model }: { model: Configurator }) {
  const { state, dispatch, errors, target } = model;
  const pascal = state.appName
    .split(/[^A-Za-z0-9]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join("");
  const derived = pascal || "MyApp";
  const modules = moduleNames(state.modules);
  const plan = useMemo(() => planTabs(new Set(state.features), moduleNames(state.modules)), [state.features, state.modules]);
  const displayLength = [...state.displayName].length;
  const placeholderPackage = !errors.has("package_name") && /^com\.example(\.|$)/.test(state.packageName);

  return (
    <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
      <div className="min-w-0 space-y-5">
        <Field label="App name" hint="Letters, digits, spaces, dots and hyphens. It names the project and its classes." error={errors.get("app_name")}>
          {(control) => (
            <TextInput {...control} value={state.appName} onChange={(event) => dispatch({ type: "appName", value: event.target.value })} placeholder="My App" />
          )}
        </Field>

        <Field
          label="Name on the home screen (optional)"
          hint={`Any characters, emoji included: "Café", "मेरा ऐप". Empty uses the app name. ${displayLength} of 30.`}
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
            plan.overflow.length > 0
              ? `Each module gets a data and a feature module and a tab, but the bar holds ${MAX_TABS}: ${listed(plan.overflow)} ${plan.overflow.length === 1 ? "gets" : "get"} no tab. Link to ${plan.overflow.length === 1 ? "it" : "them"} from another screen.`
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
          <fieldset>
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
                      on ? "border-accent bg-accent-dim text-ink-100" : "border-ink-600 bg-ink-900 text-ink-300 hover:text-ink-100"
                    }`}
                  >
                    {language.name}
                  </button>
                );
              })}
            </div>
            <p className="mt-1.5 text-xs text-ink-400">English is always included. Each one you pick translates every screen and joins the language picker.</p>
            {errors.get("languages") && <p className="mt-1 text-xs text-rose">{errors.get("languages")}</p>}
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
            ["Deep-link scheme", `${lowerName(state.appName) || "app"}://`],
            ...modules.map((name) => [`Module ${name}`, `:feature:${name}, :data:${name}`]),
          ].map(([label, value], index) => (
            // Module names can repeat while being typed, so the label alone is not a unique key.
            <div key={`${index}-${label}`} className="flex items-baseline justify-between gap-3">
              <dt className="shrink-0 font-sans text-xs text-ink-400">{label}</dt>
              <dd className="truncate text-ink-200">{value}</dd>
            </div>
          ))}
        </dl>
        <p className="mt-5 border-t border-ink-700 pt-4 text-xs leading-relaxed text-ink-400">
          Tabs: {plan.tabs.map((tab) => tab.label).join(" · ")}
          {plan.scaffoldedHome && ". Nothing you picked provides a tab, so the project gets a home module with an empty screen to build from."}
        </p>
      </div>
    </div>
  );
}
