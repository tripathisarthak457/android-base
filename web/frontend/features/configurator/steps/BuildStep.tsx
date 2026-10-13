"use client";

import { type InputHTMLAttributes, useId, useState } from "react";
import { ChoiceCard, Field, TextInput, Toggle } from "../../../components/primitives.tsx";
import type { Catalogue, Field as CatalogueField } from "../../catalogue/types.ts";
import { offersSigning } from "../request.ts";
import { choiceValue, lowerName, settingFields } from "../state.ts";
import type { Configurator } from "../useConfigurator.ts";

/** The API level from which glass blurs; below it the template draws frosted glass. */
const BLUR_FROM_API = 31;

const LABELS: Record<string, { label: string; hint?: string }> = {
  "android.min_sdk": { label: "Minimum SDK", hint: "The oldest Android this installs on." },
  "android.target_sdk": { label: "Target SDK", hint: "What the app declares it was tested against. Never below the minimum." },
  "android.version_name": { label: "Version name", hint: "Three numbers, like 1.0.0. Debug builds of dev and staging get a suffix." },
  "android.version_code": { label: "Version code", hint: "Starts at 1; each Play upload needs a higher one." },
  "android.api_base_urls": { label: "API base URLs", hint: "Per build flavour. Empty keeps the template's placeholder for that flavour." },
  "android.web_socket_urls": { label: "WebSocket URLs", hint: "Per build flavour. Empty keeps the template's placeholder." },
  "android.deeplink_scheme": { label: "Deep-link scheme", hint: "Links like scheme://sample/42 open the app." },
  "android.deeplink_host": { label: "App Links host", hint: "The verified https host. Empty keeps example.com, which you will want to change." },
  "ios.bundle_id": {
    label: "iOS bundle id",
    hint: "Letters, digits and hyphens between dots. Empty uses the package name with underscores as hyphens.",
  },
  "ios.deployment_target": { label: "iOS deployment target" },
  "ios.team_id": { label: "Apple team id", hint: "The 10 capital letters and digits Apple shows under Membership." },
};

function labelOf(key: string): { label: string; hint?: string } {
  const known = LABELS[key];
  if (known) return known;
  const words = key.split(".").at(-1)?.replace(/_/g, " ") ?? key;
  return { label: words.charAt(0).toUpperCase() + words.slice(1) };
}

const FLAVOUR_EXAMPLE: Record<string, string> = {
  dev: "https://dev.example.com/api/",
  staging: "https://staging.example.com/api/",
  prod: "https://api.example.com/api/",
  playstore: "https://api.example.com/api/",
};

export function BuildStep({ catalogue, model }: { catalogue: Catalogue; model: Configurator }) {
  const { state, dispatch, target, errors, live } = model;
  const features = new Set(state.features);
  const architecture = useId();
  const fields = settingFields(target).filter(
    (field) => field.type !== "keystores" && (field.requires ?? []).every((key) => features.has(key)) && !(field.choices && field.choices.length < 2),
  );
  const minSdk = typeof state.settings["android.min_sdk"] === "number" ? state.settings["android.min_sdk"] : undefined;
  const glass = features.has("liquidglass") || catalogue.tokens.styles[state.designStyle]?.bar === "Glass";
  const keystores = target.fields.find((field) => field.type === "keystores");

  return (
    <div className="space-y-8">
      <fieldset>
        <legend className="text-sm font-medium text-ink-200">Architecture</legend>
        <div className="mt-3 grid gap-3 sm:grid-cols-2">
          {target.architectures.map((option) => (
            <ChoiceCard
              key={option.key}
              type="radio"
              name={architecture}
              checked={state.architecture === option.key}
              disabled={!option.available}
              onChange={() => dispatch({ type: "architecture", value: option.key })}
              className="p-4"
            >
              <span className="flex items-center justify-between gap-3">
                <span className="text-sm font-semibold text-ink-100">{option.title}</span>
                {!option.available && <span className="text-xs text-ink-400">Not available yet</span>}
              </span>
              <span className="mt-1 block text-xs leading-relaxed text-ink-400">{option.description}</span>
            </ChoiceCard>
          ))}
        </div>
        {errors.get("architecture") && <p className="mt-2 text-xs text-rose">{errors.get("architecture")}</p>}
      </fieldset>

      {glass && (
        <p className="rounded-lg border border-ink-700 bg-ink-900 p-4 text-sm leading-relaxed text-ink-300">
          Glass blurs what is behind it on Android 12 (API {BLUR_FROM_API}) and newer. Below that, and on low-memory or overheating
          phones, it is drawn frosted, with no blur.
          {minSdk !== undefined && minSdk < BLUR_FROM_API && ` With a minimum of API ${minSdk}, some of your users will see the frosted version.`}
        </p>
      )}

      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        {fields.map((field) => (
          <SettingField key={field.key} field={field} model={model} wide={field.type === "map"} />
        ))}
      </div>

      {keystores && <SigningPanel model={model} field={keystores} available={offersSigning(target, live.keytool[target.key] ?? true)} />}
    </div>
  );
}

function SettingField({ field, model, wide }: { field: CatalogueField; model: Configurator; wide: boolean }) {
  const { state, dispatch, errors } = model;
  const { label, hint } = labelOf(field.key);
  const value = state.settings[field.key];
  const error = errors.get(field.key);
  const set = (next: string | number | Record<string, string>) => dispatch({ type: "setting", key: field.key, value: next });

  if (field.choices) {
    const minSdk = state.settings["android.min_sdk"];
    const choices =
      field.key === "android.target_sdk" && typeof minSdk === "number"
        ? field.choices.filter((choice) => typeof choice.value !== "number" || choice.value >= minSdk)
        : field.choices;
    const chosen = field.choices.find((choice) => choice.value === value);
    const shown = field.key === "android.target_sdk" && typeof minSdk === "number" && typeof value === "number" ? Math.max(value, minSdk) : value;
    const desugared = field.key === "android.min_sdk" && chosen?.needs_desugaring;
    return (
      <Field label={label} hint={desugared ? "Below API 26, so core-library desugaring is turned on and java.time keeps working." : hint} error={error}>
        {(control) => (
          <select
            {...control}
            value={String(shown ?? "")}
            onChange={(event) => {
              const picked = choiceValue(field, event.target.value);
              if (picked !== undefined) set(picked);
            }}
            className="h-11 w-full rounded-lg border border-ink-600 bg-ink-900 px-3 text-[15px] text-ink-100 hover:border-ink-500 focus:border-accent focus:outline-none"
          >
            {choices.map((choice) => (
              <option key={String(choice.value)} value={String(choice.value)}>
                {choice.label}
              </option>
            ))}
          </select>
        )}
      </Field>
    );
  }

  if (field.type === "map") {
    const map = typeof value === "object" ? value : {};
    return (
      <fieldset className={wide ? "md:col-span-2" : ""}>
        <legend className="mb-1.5 text-sm font-medium text-ink-200">{label}</legend>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          {(field.keys ?? []).map((key) => (
            <label key={key} className="flex items-center gap-2">
              <span className="w-20 shrink-0 font-mono text-xs text-ink-400">{key}</span>
              <TextInput
                value={map[key] ?? ""}
                aria-invalid={error ? true : undefined}
                onChange={(event) => set({ ...map, [key]: event.target.value })}
                placeholder={
                  field.rule === "socket_url" ? (FLAVOUR_EXAMPLE[key] ?? "").replace("https://", "wss://").replace("/api/", "/ws") : FLAVOUR_EXAMPLE[key]
                }
                className="h-10 font-mono text-xs"
                spellCheck={false}
              />
            </label>
          ))}
        </div>
        {hint && <p className="mt-1.5 text-xs text-ink-400">{hint}</p>}
        {error && <p className="mt-1 text-xs text-rose">{error}</p>}
      </fieldset>
    );
  }

  const placeholder =
    field.key === "android.deeplink_scheme"
      ? lowerName(state.appName) || "app"
      : field.key === "android.deeplink_host"
        ? "example.com"
        : field.key === "ios.bundle_id"
          ? state.packageName.replace(/_/g, "-")
          : typeof field.default === "string" || typeof field.default === "number"
            ? String(field.default)
            : "";
  return (
    <Field label={label} hint={hint} error={error}>
      {(control) =>
        field.type === "int" ? (
          <IntegerInput
            {...control}
            min={field.min}
            max={field.max}
            value={typeof value === "number" ? value : Number(field.default ?? 1)}
            onCommit={set}
            className="font-mono text-sm"
          />
        ) : (
          <TextInput
            {...control}
            value={typeof value === "string" ? value : ""}
            onChange={(event) => set(event.target.value)}
            placeholder={placeholder}
            className="font-mono text-sm"
            spellCheck={false}
            autoCapitalize="off"
          />
        )
      }
    </Field>
  );
}

/**
 * A whole-number field that keeps what is typed, so it can be cleared and typed again; only a whole
 * number reaches the answers, and leaving the field with anything else shows the answer again.
 */
function IntegerInput({
  value,
  onCommit,
  ...rest
}: { value: number; onCommit: (value: number) => void } & Omit<InputHTMLAttributes<HTMLInputElement>, "value" | "onChange">) {
  const [text, setText] = useState(String(value));
  const [synced, setSynced] = useState(value);
  if (value !== synced) {
    setSynced(value);
    setText(String(value));
  }
  return (
    <TextInput
      {...rest}
      type="number"
      inputMode="numeric"
      value={text}
      onChange={(event) => {
        setText(event.target.value);
        if (/^\d+$/.test(event.target.value)) onCommit(Number(event.target.value));
      }}
      onBlur={() => setText(String(value))}
    />
  );
}

/** A password strong enough that accepting it is the right move, and different for each key. */
function randomPassword(): string {
  const alphabet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = crypto.getRandomValues(new Uint32Array(20));
  return [...bytes].map((value) => alphabet[value % alphabet.length]).join("");
}

function SigningPanel({ model, field, available }: { model: Configurator; field: CatalogueField; available: boolean }) {
  const { state, dispatch, errors } = model;
  const { signing } = state;
  const names = field.names ?? [];
  const error = errors.get(field.key);

  if (!available) {
    return (
      <div className="rounded-xl border border-ink-700 bg-ink-900 p-5">
        <p className="text-sm font-semibold text-ink-100">Signing keys are not available here</p>
        <p className="mt-2 text-xs leading-relaxed text-ink-400">
          This server has no <code className="text-ink-300">keytool</code>. The zip ships <code className="text-ink-300">keystore.properties.template</code> and
          the README has the commands.
        </p>
      </div>
    );
  }

  // Passwords are invented only when the visitor turns signing on, and dropped when they turn it
  // off: a page that quietly holds four secrets nobody asked for has four secrets to leak.
  const toggle = (next: boolean) =>
    dispatch({
      type: "signing",
      value: {
        enabled: next,
        passwords: next ? Object.fromEntries(names.map((name) => [name, randomPassword()])) : {},
        organisation: next && !signing.organisation ? state.appName.trim() : signing.organisation,
      },
    });

  return (
    <div className="rounded-xl border border-ink-700 bg-ink-900 p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-ink-100">Generate signing keys</p>
          <p className="mt-1 text-xs text-ink-400">{names.join(", ")}. Off by default.</p>
        </div>
        <Toggle checked={signing.enabled} onChange={toggle} label="Generate signing keys" />
      </div>

      <div className="mt-4 rounded-lg border border-amber/30 bg-amber/10 p-3 text-xs leading-relaxed text-amber">
        <p>
          <span className="font-semibold">These keys are made on this server and sent to you</span>, so you are not their only
          holder. Fine for dev and staging; make the Play upload key yourself.
        </p>
        <details className="mt-1.5">
          <summary className="cursor-pointer select-none font-semibold">Why?</summary>
          <p className="mt-1.5">
            Losing or leaking the Play upload key cannot be undone, and Google will not reissue it. The README&apos;s keytool
            commands and the command line tool make keys entirely on your own machine.
          </p>
        </details>
      </div>

      {signing.enabled && (
        <div className="mt-4 space-y-4">
          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2">
              <Field label="Organisation">
                {(control) => (
                  <TextInput
                    {...control}
                    value={signing.organisation}
                    onChange={(event) => dispatch({ type: "signing", value: { organisation: event.target.value } })}
                    placeholder="Acme Ltd"
                  />
                )}
              </Field>
            </div>
            <Field label="Country">
              {(control) => (
                <TextInput
                  {...control}
                  value={signing.country}
                  maxLength={2}
                  onChange={(event) => dispatch({ type: "signing", value: { country: event.target.value.toUpperCase() } })}
                  className="font-mono"
                />
              )}
            </Field>
          </div>
          <div className="space-y-2">
            {names.map((name) => (
              <label key={name} className="flex items-center gap-3">
                <span className="w-20 shrink-0 font-mono text-xs text-ink-400">{name}</span>
                {/* Not a password input: the value is written to keystore.properties in plain text in the same zip,
                    and it has to be read and saved somewhere before the tab closes. */}
                <TextInput
                  value={signing.passwords[name] ?? ""}
                  onChange={(event) => dispatch({ type: "signing", value: { passwords: { ...signing.passwords, [name]: event.target.value } } })}
                  className="h-9 font-mono text-xs"
                  spellCheck={false}
                  autoComplete="off"
                />
              </label>
            ))}
          </div>
          {error ? (
            <p className="text-xs text-rose">{error}</p>
          ) : (
            <p className="text-xs leading-relaxed text-ink-400">
              Copy these into your password manager now. They are in <code className="text-ink-300">keystore.properties</code> in the zip,
              which git ignores, and this page never saves them.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
