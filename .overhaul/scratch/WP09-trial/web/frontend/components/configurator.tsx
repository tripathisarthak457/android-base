"use client";

import { AnimatePresence, motion } from "framer-motion";
import { useEffect, useMemo, useState } from "react";
import {
  ApiError,
  type Catalogue,
  type GenerateRequest,
  type Keystore,
  type Language,
  generateProject,
  track,
} from "../lib/api";
import {
  type DesignStyleKey,
  type MotionStyleKey,
  deriveSecondary,
  deriveTertiary,
} from "../lib/app-theme";
import type { PreviewConfig } from "./app-preview";
import type { ReportContext } from "./feedback";
import { LookStep, MotionStep, ScaledPreview } from "./look-step";
import { Badge, Button, Card, Field, Spinner, TextInput, Toggle, press } from "./primitives";

type Step = "identity" | "features" | "look" | "motion" | "build" | "review";

const STEPS: { id: Step; label: string; blurb: string }[] = [
  { id: "identity", label: "Project", blurb: "What it is called and what it is called in code" },
  { id: "features", label: "Features", blurb: "What comes in the box" },
  { id: "look", label: "Look", blurb: "Design style, colours, typeface" },
  { id: "motion", label: "Motion", blurb: "How it moves under a finger" },
  { id: "build", label: "Build", blurb: "SDK levels, version, backend URLs" },
  { id: "review", label: "Review", blurb: "Check it, then download" },
];

/** Mirrors `validate_package_name` in the generator, so a bad id is caught before the round trip. */
const RESERVED = new Set([
  "abstract", "as", "assert", "break", "byte", "case", "catch", "char", "class", "const",
  "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
  "for", "goto", "if", "implements", "import", "in", "instanceof", "int", "interface", "is",
  "long", "native", "new", "package", "private", "protected", "public", "return", "short",
  "static", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
  "val", "var", "void", "volatile", "while", "fun", "object", "when",
]);

function packageError(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) return "A package name is required.";
  const segments = trimmed.split(".");
  if (segments.length < 2) {
    return "Needs at least two segments, like com.example.myapp — Play rejects a single-segment id.";
  }
  for (const segment of segments) {
    if (!/^[a-z][a-z0-9_]*$/.test(segment)) {
      return `"${segment}" must be lowercase, start with a letter, and contain only letters, digits and underscores.`;
    }
    if (RESERVED.has(segment)) return `"${segment}" is a reserved word in Java or Kotlin.`;
  }
  return undefined;
}

function appNameError(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) return "An app name is required.";
  if (!/^[A-Za-z][A-Za-z0-9 ._-]*$/.test(trimmed)) {
    return "Start with a letter; letters, digits, spaces, dots, hyphens and underscores only.";
  }
  return undefined;
}

/** The reserved list is not written out here. */
function moduleError(names: string[], reserved: string[]): string | undefined {
  for (const name of names) {
    if (!/^[a-z][a-z0-9_]*$/.test(name)) {
      return `"${name}" must be lower_snake_case.`;
    }
    if (reserved.includes(name)) {
      return `"${name}" is taken — the template already ships a module or directory called that.`;
    }
  }
  if (new Set(names).size !== names.length) return "Module names must be unique.";
  return undefined;
}

/**
 * Mirrors the generator's URL rule. The value is written into a Kotlin string that Gradle compiles,
 * so a quote, a backslash or a `$` is refused rather than escaped.
 */
function urlError(url: string): string | undefined {
  if (!/^https?:\/\/[A-Za-z0-9._~:/?#[\]@!&'()*+,;=%-]+$/.test(url.trim())) {
    return "Needs to look like https://api.example.com/ — no spaces, quotes, backslashes or $.";
  }
  return undefined;
}

/** Mirrors the generator's font rule: a family name as fonts.google.com spells it. */
function fontError(name: string): string | undefined {
  if (!/^[A-Za-z0-9][A-Za-z0-9 -]{0,59}$/.test(name.trim())) {
    return "The typeface must be a Google Fonts family name: letters, digits, spaces and hyphens.";
  }
  return undefined;
}

/** Characters that would split one field of the certificate's subject into two. */
const DNAME_SPECIALS = /[,=+<>#;\\"]/;

function signingError(
  enabled: boolean,
  organisation: string,
  country: string,
  passwords: Record<string, string>,
  names: string[],
  minimum: number,
): string | undefined {
  if (!enabled) return undefined;
  if (!organisation.trim()) return "The organisation goes in the certificate; it cannot be empty.";
  if (DNAME_SPECIALS.test(organisation)) {
    return "The organisation cannot contain , = + < > # ; \ or a quote.";
  }
  if (!/^[A-Za-z]{2}$/.test(country.trim())) return "The country is a two-letter code, like US.";
  for (const name of names) {
    if ((passwords[name] ?? "").length < minimum) {
      return `The ${name} password needs at least ${minimum} characters — keytool refuses shorter.`;
    }
  }
  return undefined;
}

/** A password strong enough that accepting the default is the right move. */
function randomPassword(): string {
  const alphabet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = crypto.getRandomValues(new Uint32Array(20));
  return [...bytes].map((value) => alphabet[value % alphabet.length]).join("");
}

/** `My Great App` → `MyGreatApp`, matching the generator's own derivation. */
function pascal(appName: string): string {
  return appName
    .split(/[^A-Za-z0-9]+/)
    .filter(Boolean)
    .map((part) => part[0].toUpperCase() + part.slice(1))
    .join("");
}

export function Configurator({
  catalogue,
  onContextChange,
}: {
  catalogue: Catalogue;
  onContextChange?: (context: ReportContext) => void;
}) {
  const [step, setStep] = useState<Step>("identity");
  const [appName, setAppName] = useState("My App");
  const [packageName, setPackageName] = useState("com.example.myapp");
  const [packageTouched, setPackageTouched] = useState(false);
  const [preset, setPreset] = useState(catalogue.defaults.preset);
  const [features, setFeatures] = useState<Set<string>>(
    () => new Set(catalogue.presets.find((p) => p.key === catalogue.defaults.preset)?.features ?? []),
  );
  const [modules, setModules] = useState("");
  const [languages, setLanguages] = useState<string[]>([]);
  const [fontName, setFontName] = useState(catalogue.defaults.fontName);
  const [accent, setAccent] = useState(catalogue.defaults.accentColour);
  // Empty means "derive it from the primary", which is the same contract the generator has:
  // a blank supporting colour is worked out server-side rather than left at the template's.
  const [secondary, setSecondary] = useState("");
  const [tertiary, setTertiary] = useState("");
  const [motionStyle, setMotionStyle] = useState(catalogue.defaults.motionStyle as MotionStyleKey);
  const [designStyle, setDesignStyle] = useState(catalogue.defaults.designStyle as DesignStyleKey);
  const [previewDark, setPreviewDark] = useState(false);
  const [haptics, setHaptics] = useState(catalogue.defaults.hapticsEnabled);
  const [minSdk, setMinSdk] = useState(catalogue.defaults.minSdk);
  const [targetSdk, setTargetSdk] = useState(catalogue.defaults.targetSdk);
  const [versionName, setVersionName] = useState(catalogue.defaults.versionName);
  const [devUrl, setDevUrl] = useState("https://dev.example.com/api/");
  const [prodUrl, setProdUrl] = useState("https://api.example.com/api/");

  // Off unless the visitor turns it on, and the passwords are only invented once they do — a
  // page that quietly holds four secrets nobody asked for is a page with four secrets to leak.
  const [signing, setSigning] = useState(false);
  const [organisation, setOrganisation] = useState("");
  const [country, setCountry] = useState("US");
  const [keyPasswords, setKeyPasswords] = useState<Record<string, string>>({});

  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);
  const [done, setDone] = useState<{
    filename: string;
    elapsedMs: number;
    bytes: number;
    keystoresSkipped: string[];
  } | null>(null);

  const featureByKey = useMemo(
    () => new Map(catalogue.features.map((feature) => [feature.key, feature])),
    [catalogue.features],
  );

  // Suggest a package from the app name until the visitor edits the package themselves, then
  // stop — an id that keeps rewriting itself under the cursor is worse than no suggestion.
  useEffect(() => {
    if (packageTouched) return;
    const slug = appName.toLowerCase().replace(/[^a-z0-9]/g, "");
    setPackageName(slug ? `com.example.${slug}` : "com.example.myapp");
  }, [appName, packageTouched]);

  const moduleNames = modules
    .split(",")
    .map((name) => name.trim().toLowerCase().replace(/[\s-]+/g, "_"))
    .filter(Boolean);

  const errors = {
    appName: appNameError(appName),
    packageName: packageError(packageName),
    modules: moduleError(moduleNames, catalogue.reservedModuleNames),
    font: fontError(fontName),
    devUrl: features.has("network") ? urlError(devUrl) : undefined,
    prodUrl: features.has("network") ? urlError(prodUrl) : undefined,
    signing: signingError(
      signing,
      organisation,
      country,
      keyPasswords,
      catalogue.keystoreNames,
      catalogue.minKeystorePassword,
    ),
  };
  const identityValid = !errors.appName && !errors.packageName;

  /** Ticking a feature also ticks what it needs; unticking one unticks what needed it. */
  function toggleFeature(key: string) {
    setFeatures((current) => {
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
        for (const other of catalogue.features) {
          if (next.has(other.key) && other.requires.includes(key)) next.delete(other.key);
        }
      } else {
        next.add(key);
        for (const implied of featureByKey.get(key)?.implies ?? []) next.add(implied);
      }
      return next;
    });
    setPreset("custom");
  }

  function applyPreset(key: string) {
    const chosen = catalogue.presets.find((p) => p.key === key);
    if (!chosen) return;
    setPreset(key);
    setFeatures(new Set(chosen.features));
  }

  // Published upward so a bug report can attach it without the reporter retyping any of it.
  useEffect(() => {
    onContextChange?.({
      appName,
      packageName,
      features: [...features],
      preset,
      minSdk,
      motionStyle,
      designStyle,
      fontName,
      accentColour: accent,
    });
  }, [
    onContextChange, appName, packageName, features, preset, minSdk, motionStyle, designStyle,
    fontName, accent,
  ]);

  const previewConfig: PreviewConfig = {
    designStyle,
    motionStyle,
    accent,
    secondary,
    tertiary,
    fontName,
    dark: previewDark,
    features,
  };

  const hasNetwork = features.has("network");
  const hasDeeplink = features.has("deeplink");

  /**
   * Turning signing on invents one password per key, so the visitor can accept four strong ones
   * rather than typing four weak ones — and, because prod and playstore must not share with
   * anything, four *different* ones. Turning it off drops them.
   */
  function toggleSigning(next: boolean) {
    setSigning(next);
    setKeyPasswords(
      next
        ? Object.fromEntries(catalogue.keystoreNames.map((name) => [name, randomPassword()]))
        : {},
    );
    if (next && !organisation) setOrganisation(appName.trim());
  }

  function keystoresFor(): Keystore[] {
    const alias = pascal(appName).toLowerCase() || "app";
    return catalogue.keystoreNames.map((name) => ({
      name,
      alias: `${alias}-${name}`,
      // The store and the key share a password on purpose: Gradle needs both, they live in the
      // same file, and two secrets kept in one place are one secret written down twice.
      store_password: keyPasswords[name],
      key_password: keyPasswords[name],
      common_name: `${appName.trim()} (${name})`,
      organisation: organisation.trim(),
      country: country.trim().toUpperCase(),
    }));
  }

  async function generate() {
    setBusy(true);
    setFailure(null);
    track("configured");

    const request: GenerateRequest = {
      app_name: appName.trim(),
      package_name: packageName.trim(),
      min_sdk: minSdk,
      target_sdk: targetSdk,
      compile_sdk: Math.max(catalogue.defaults.compileSdk, targetSdk),
      version_name: versionName,
      version_code: catalogue.defaults.versionCode,
      features: [...features],
      feature_modules: moduleNames,
      font_name: fontName,
      mono_font_name: catalogue.defaults.monoFontName,
      accent_colour: accent,
      secondary_colour: secondary,
      tertiary_colour: tertiary,
      motion_style: motionStyle,
      design_style: designStyle,
      haptics_enabled: haptics,
      // In the catalogue's order, so the picker in the app lists them the same way every time.
      languages: catalogue.languages.map((l) => l.tag).filter((tag) => languages.includes(tag)),
      preset,
      ...(hasNetwork
        ? {
            api_base_urls: {
              dev: devUrl,
              staging: devUrl,
              prod: prodUrl,
              playstore: prodUrl,
            },
          }
        : {}),
      ...(hasDeeplink
        ? {
            deeplink_scheme: pascal(appName).toLowerCase() || "myapp",
            deeplink_host: "example.com",
          }
        : {}),
      ...(signing ? { keystores: keystoresFor() } : {}),
    };

    try {
      const { blob, filename, elapsedMs, keystoresSkipped } = await generateProject(request);

      // Anchor-and-click rather than location.assign, so the filename from Content-Disposition is
      // used and the page is not navigated away from — the summary has to survive the download.
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = filename;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      setTimeout(() => URL.revokeObjectURL(url), 10_000);

      setDone({ filename, elapsedMs, bytes: blob.size, keystoresSkipped });
      track("downloaded");
    } catch (error) {
      setFailure(
        error instanceof ApiError
          ? error.message
          : "Could not reach the generator. It may be restarting; the CLI in the repository does " +
            "the same job and needs nothing but Python.",
      );
    } finally {
      setBusy(false);
    }
  }

  const stepIndex = STEPS.findIndex((s) => s.id === step);

  return (
    <section id="configure" className="mx-auto max-w-6xl scroll-mt-8 px-6 py-20">
      <div className="max-w-2xl">
        <h2 className="font-display text-4xl font-bold text-ink-100">Configure your project</h2>
        <p className="mt-3 text-ink-300">
          Six short steps. Everything has a working default, so you can jump to Review and
          download something sensible right now.
        </p>
      </div>

      <StepBar steps={STEPS} current={step} index={stepIndex} onSelect={setStep} valid={identityValid} />

      <Card className="mt-6 overflow-hidden">
        {/*
          Keyed, and animating in only — no AnimatePresence and no `mode="wait"`.

          `mode="wait"` holds the incoming panel until the outgoing one has finished exiting, so
          an animation loop that stalls for any reason leaves the step bar saying "5 of 5" above
          the contents of step 1. There is nothing to be gained here by animating the old panel
          out: it is being replaced in place, and the incoming slide reads as a step change on its
          own.
        */}
        <div>
          <motion.div
            key={step}
            initial={{ opacity: 0, x: 22 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.24, ease: [0.16, 1, 0.3, 1] }}
            className="p-6 md:p-8"
          >
            {step === "identity" && (
              <IdentityStep
                appName={appName}
                setAppName={setAppName}
                packageName={packageName}
                setPackageName={(value) => {
                  setPackageTouched(true);
                  setPackageName(value);
                }}
                errors={errors}
                modules={modules}
                setModules={setModules}
                offeredLanguages={catalogue.languages}
                languages={languages}
                setLanguages={setLanguages}
              />
            )}
            {step === "features" && (
              <FeaturesStep
                catalogue={catalogue}
                features={features}
                preset={preset}
                onPreset={applyPreset}
                onToggle={toggleFeature}
              />
            )}
            {step === "look" && (
              <LookStep
                catalogue={catalogue}
                config={previewConfig}
                setDesignStyle={setDesignStyle}
                fontName={fontName}
                setFontName={setFontName}
                accent={accent}
                setAccent={setAccent}
                secondary={secondary}
                setSecondary={setSecondary}
                tertiary={tertiary}
                setTertiary={setTertiary}
                onDark={setPreviewDark}
              />
            )}
            {step === "motion" && (
              <MotionStep
                catalogue={catalogue}
                config={previewConfig}
                motionStyle={motionStyle}
                setMotionStyle={setMotionStyle}
                haptics={haptics}
                setHaptics={setHaptics}
                onDark={setPreviewDark}
              />
            )}
            {step === "build" && (
              <BuildStep
                catalogue={catalogue}
                minSdk={minSdk}
                setMinSdk={setMinSdk}
                targetSdk={targetSdk}
                setTargetSdk={setTargetSdk}
                versionName={versionName}
                setVersionName={setVersionName}
                hasNetwork={hasNetwork}
                devUrl={devUrl}
                setDevUrl={setDevUrl}
                prodUrl={prodUrl}
                setProdUrl={setProdUrl}
                signing={signing}
                onSigning={toggleSigning}
                organisation={organisation}
                setOrganisation={setOrganisation}
                country={country}
                setCountry={setCountry}
                keyPasswords={keyPasswords}
                setKeyPasswords={setKeyPasswords}
                signingError={errors.signing}
                devUrlError={errors.devUrl}
                prodUrlError={errors.prodUrl}
              />
            )}
            {step === "review" && (
              <ReviewStep
                catalogue={catalogue}
                appName={appName}
                packageName={packageName}
                features={features}
                modules={moduleNames}
                fontName={fontName}
                accent={accent}
                secondary={secondary || deriveSecondary(accent)}
                tertiary={tertiary || deriveTertiary(accent)}
                motionStyle={motionStyle}
                designStyle={designStyle}
                previewConfig={previewConfig}
                haptics={haptics}
                minSdk={minSdk}
                targetSdk={targetSdk}
                versionName={versionName}
                signing={signing}
                busy={busy}
                failure={failure}
                done={done}
                errors={errors}
                onGenerate={generate}
              />
            )}
          </motion.div>
        </div>

        <div className="flex items-center justify-between border-t border-ink-700 bg-ink-900/60 px-6 py-4">
          <Button
            variant="ghost"
            onClick={() => setStep(STEPS[Math.max(0, stepIndex - 1)].id)}
            disabled={stepIndex === 0}
          >
            Back
          </Button>
          <span className="font-mono text-xs text-ink-500">
            {stepIndex + 1} / {STEPS.length}
          </span>
          {stepIndex < STEPS.length - 1 ? (
            <Button
              onClick={() => setStep(STEPS[stepIndex + 1].id)}
              disabled={step === "identity" && !identityValid}
            >
              Next
            </Button>
          ) : (
            <span className="w-[76px]" />
          )}
        </div>
      </Card>
    </section>
  );
}

function StepBar({
  steps,
  current,
  index,
  onSelect,
  valid,
}: {
  steps: typeof STEPS;
  current: Step;
  index: number;
  onSelect: (step: Step) => void;
  valid: boolean;
}) {
  return (
    <div className="mt-8 flex gap-2 overflow-x-auto pb-1">
      {steps.map((step, position) => {
        const active = step.id === current;
        const reachable = position === 0 || valid;
        return (
          <motion.button
            key={step.id}
            {...press}
            type="button"
            disabled={!reachable}
            onClick={() => onSelect(step.id)}
            className={`relative shrink-0 rounded-lg border px-4 py-2.5 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-40 ${
              active
                ? "border-accent/50 bg-accent-dim"
                : position < index
                  ? "border-ink-600 bg-ink-850"
                  : "border-ink-700 bg-ink-900"
            }`}
          >
            <div className="flex items-center gap-2">
              <span
                className={`flex h-5 w-5 items-center justify-center rounded-full font-mono text-[11px] ${
                  position < index
                    ? "bg-mint/20 text-mint"
                    : active
                      ? "bg-accent text-on-accent"
                      : "bg-ink-700 text-ink-400"
                }`}
              >
                {position < index ? "✓" : position + 1}
              </span>
              <span
                className={`text-sm font-medium ${active ? "text-ink-100" : "text-ink-300"}`}
              >
                {step.label}
              </span>
            </div>
          </motion.button>
        );
      })}
    </div>
  );
}

function IdentityStep({
  appName,
  setAppName,
  packageName,
  setPackageName,
  errors,
  modules,
  setModules,
  offeredLanguages,
  languages,
  setLanguages,
}: {
  appName: string;
  setAppName: (v: string) => void;
  packageName: string;
  setPackageName: (v: string) => void;
  errors: { appName?: string; packageName?: string; modules?: string };
  modules: string;
  setModules: (v: string) => void;
  offeredLanguages: Language[];
  languages: string[];
  setLanguages: (update: (current: string[]) => string[]) => void;
}) {
  const derived = pascal(appName) || "MyApp";

  return (
    <div className="grid gap-6 md:grid-cols-2">
      <div className="space-y-5">
        <Field
          label="App name"
          hint="What appears under the launcher icon. Spaces are fine."
          error={errors.appName}
        >
          <TextInput
            value={appName}
            invalid={Boolean(errors.appName)}
            onChange={(event) => setAppName(event.target.value)}
            placeholder="My App"
          />
        </Field>

        <Field
          label="Package name"
          hint="The application id. Cannot be changed after publishing to Play."
          error={errors.packageName}
        >
          <TextInput
            value={packageName}
            invalid={Boolean(errors.packageName)}
            onChange={(event) => setPackageName(event.target.value)}
            placeholder="com.example.myapp"
            className="font-mono text-sm"
          />
        </Field>

        <Field
          label="Feature modules (optional)"
          hint="Comma-separated, lower_snake_case. Each becomes a :data: and :feature: pair with a repository, ViewModel, screen and tests."
          error={errors.modules}
        >
          <TextInput
            value={modules}
            invalid={Boolean(errors.modules)}
            onChange={(event) => setModules(event.target.value)}
            placeholder="orders, profile"
            className="font-mono text-sm"
          />
        </Field>

        {offeredLanguages.length > 0 && (
          <Field
            label="Languages"
            hint="English is always included. Each one you pick ships a translation of every screen, and the language picker in Settings lists it."
          >
            <div className="flex flex-wrap gap-2" role="group" aria-label="Languages">
              {offeredLanguages.map((language) => {
                const on = languages.includes(language.tag);
                return (
                  <button
                    key={language.tag}
                    type="button"
                    aria-pressed={on}
                    lang={language.tag}
                    onClick={() =>
                      setLanguages((current) =>
                        on ? current.filter((tag) => tag !== language.tag) : [...current, language.tag],
                      )
                    }
                    className={`rounded-md border px-3 py-1.5 text-sm transition-colors ${
                      on
                        ? "border-accent bg-ink-700 text-ink-100"
                        : "border-ink-600 bg-ink-900 text-ink-400 hover:text-ink-200"
                    }`}
                  >
                    {language.name}
                  </button>
                );
              })}
            </div>
          </Field>
        )}
      </div>

      <div className="rounded-lg border border-ink-700 bg-ink-900 p-5">
        <p className="text-xs font-medium uppercase tracking-wide text-ink-400">
          What that produces
        </p>
        <dl className="mt-4 space-y-3 font-mono text-[13px]">
          {[
            ["Root project", derived],
            ["Application class", `${derived}Application`],
            ["Namespace", packageName || "com.example.myapp"],
            ["dev id", `${packageName}.dev`],
            ["staging id", `${packageName}.staging`],
            ["prod id", packageName],
          ].map(([label, value]) => (
            <div key={label} className="flex items-baseline justify-between gap-3">
              <dt className="shrink-0 font-sans text-xs text-ink-400">{label}</dt>
              <dd className="truncate text-ink-200">{value}</dd>
            </div>
          ))}
        </dl>
        <p className="mt-5 border-t border-ink-700 pt-4 text-xs leading-relaxed text-ink-400">
          dev and staging install alongside production, so all three can be on one phone at once.
          Their version names are stamped <code className="text-ink-300">1.0.0-devDebug</code> and
          so on; prod and playstore stay a bare <code className="text-ink-300">1.0.0</code>,
          because that string ends up on a store listing.
        </p>
      </div>
    </div>
  );
}

function FeaturesStep({
  catalogue,
  features,
  preset,
  onPreset,
  onToggle,
}: {
  catalogue: Catalogue;
  features: Set<string>;
  preset: string;
  onPreset: (key: string) => void;
  onToggle: (key: string) => void;
}) {
  return (
    <div>
      <div className="flex flex-wrap items-center gap-3">
        <span className="text-sm text-ink-300">Start from</span>
        {catalogue.presets.map((option) => (
          <motion.button
            key={option.key}
            {...press}
            type="button"
            onClick={() => onPreset(option.key)}
            title={option.description}
            className={`rounded-full border px-4 py-1.5 text-sm font-medium transition-colors ${
              preset === option.key
                ? "border-accent bg-accent-dim text-accent-bright"
                : "border-ink-600 bg-ink-900 text-ink-300 hover:border-ink-500"
            }`}
          >
            {option.title}
            <span className="ml-1.5 font-mono text-xs opacity-60">{option.features.length}</span>
          </motion.button>
        ))}
        {preset === "custom" && <Badge tone="accent">Custom · {features.size}</Badge>}
      </div>

      <p className="mt-3 text-sm text-ink-400">
        {catalogue.presets.find((p) => p.key === preset)?.description ??
          "Your own selection. Ticking something also ticks whatever it needs."}
      </p>

      <div className="mt-7 space-y-8">
        {catalogue.groups.map((group) => {
          const inGroup = catalogue.features.filter((feature) => feature.group === group.name);
          if (inGroup.length === 0) return null;
          return (
            <div key={group.name}>
              <div className="mb-3 flex items-baseline gap-3">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-ink-200">
                  {group.name}
                </h3>
                <span className="text-xs text-ink-500">{group.caption}</span>
              </div>
              <div className="grid gap-2.5 md:grid-cols-2">
                {inGroup.map((feature) => {
                  const on = features.has(feature.key);
                  const requiredBy = catalogue.features.filter(
                    (other) => features.has(other.key) && other.requires.includes(feature.key),
                  );
                  return (
                    <motion.button
                      key={feature.key}
                      {...press}
                      type="button"
                      onClick={() => onToggle(feature.key)}
                      className={`rounded-lg border p-4 text-left transition-colors ${
                        on
                          ? "border-accent/45 bg-accent-dim/45"
                          : "border-ink-700 bg-ink-900 hover:border-ink-600"
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        <span
                          className={`mt-0.5 flex h-4.5 w-4.5 shrink-0 items-center justify-center rounded border text-[10px] ${
                            on
                              ? "border-accent bg-accent text-on-accent"
                              : "border-ink-500 text-transparent"
                          }`}
                          style={{ width: 18, height: 18 }}
                        >
                          ✓
                        </span>
                        <div className="min-w-0">
                          <p className="text-sm font-semibold text-ink-100">{feature.headline}</p>
                          <p className="mt-1 text-xs leading-relaxed text-ink-400">
                            {feature.description}
                          </p>
                          {feature.requires.length > 0 && (
                            <p className="mt-2 font-mono text-[11px] text-ink-500">
                              needs {feature.requires.join(", ")}
                            </p>
                          )}
                          {on && requiredBy.length > 0 && (
                            <p className="mt-1 font-mono text-[11px] text-amber">
                              {requiredBy.map((other) => other.key).join(", ")} depends on this
                            </p>
                          )}
                        </div>
                      </div>
                    </motion.button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function BuildStep({
  catalogue,
  minSdk,
  setMinSdk,
  targetSdk,
  setTargetSdk,
  versionName,
  setVersionName,
  hasNetwork,
  devUrl,
  setDevUrl,
  prodUrl,
  setProdUrl,
  signing,
  onSigning,
  organisation,
  setOrganisation,
  country,
  setCountry,
  keyPasswords,
  setKeyPasswords,
  signingError,
  devUrlError,
  prodUrlError,
}: {
  catalogue: Catalogue;
  minSdk: number;
  setMinSdk: (v: number) => void;
  targetSdk: number;
  setTargetSdk: (v: number) => void;
  versionName: string;
  setVersionName: (v: string) => void;
  hasNetwork: boolean;
  devUrl: string;
  setDevUrl: (v: string) => void;
  prodUrl: string;
  setProdUrl: (v: string) => void;
  signing: boolean;
  onSigning: (next: boolean) => void;
  organisation: string;
  setOrganisation: (v: string) => void;
  country: string;
  setCountry: (v: string) => void;
  keyPasswords: Record<string, string>;
  setKeyPasswords: (next: Record<string, string>) => void;
  signingError?: string;
  devUrlError?: string;
  prodUrlError?: string;
}) {
  const chosen = catalogue.apiLevels.find((level) => level.level === minSdk);

  return (
    <div className="grid gap-8 md:grid-cols-2">
      <div className="space-y-5">
        <Field
          label="Minimum SDK"
          hint={
            chosen?.needsDesugaring
              ? "Below API 26, so core-library desugaring is turned on automatically and java.time keeps working."
              : "The oldest Android this will install on."
          }
        >
          <select
            value={minSdk}
            onChange={(event) => setMinSdk(Number(event.target.value))}
            className="h-11 w-full rounded-lg border border-ink-600 bg-ink-900 px-3 text-[15px] text-ink-100 hover:border-ink-500 focus:border-accent focus:outline-none"
          >
            {catalogue.apiLevels.map((level) => (
              <option key={level.level} value={level.level}>
                {level.label}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Target SDK" hint="What the app declares it was tested against.">
          <select
            value={targetSdk}
            onChange={(event) => setTargetSdk(Number(event.target.value))}
            className="h-11 w-full rounded-lg border border-ink-600 bg-ink-900 px-3 text-[15px] text-ink-100 hover:border-ink-500 focus:border-accent focus:outline-none"
          >
            {catalogue.apiLevels
              .filter((level) => level.level >= minSdk)
              .map((level) => (
                <option key={level.level} value={level.level}>
                  {level.label}
                </option>
              ))}
          </select>
        </Field>

        <Field label="Version name" hint="versionCode starts at 1.">
          <TextInput
            value={versionName}
            onChange={(event) => setVersionName(event.target.value)}
            className="font-mono text-sm"
          />
        </Field>
      </div>

      <div className="space-y-5">
        {hasNetwork ? (
          <>
            <Field label="Dev base URL" hint="Used by the dev and staging flavours." error={devUrlError}>
              <TextInput
                value={devUrl}
                invalid={Boolean(devUrlError)}
                onChange={(event) => setDevUrl(event.target.value)}
                className="font-mono text-sm"
              />
            </Field>
            <Field label="Production base URL" hint="Used by prod and playstore." error={prodUrlError}>
              <TextInput
                value={prodUrl}
                invalid={Boolean(prodUrlError)}
                onChange={(event) => setProdUrl(event.target.value)}
                className="font-mono text-sm"
              />
            </Field>
            <p className="rounded-lg border border-ink-700 bg-ink-900 p-4 text-xs leading-relaxed text-ink-400">
              These land in <code className="text-ink-300">AppConfig.kt</code>, which is the only
              place the build reads them from. Switching environment is a variant switch and
              nothing else — no code change, and no rebuild of any library module.
            </p>
          </>
        ) : (
          <p className="rounded-lg border border-ink-700 bg-ink-900 p-4 text-sm text-ink-400">
            Networking is switched off, so there are no URLs to set. Turn on{" "}
            <span className="text-ink-200">Talk to a REST API</span> in Features if you want them.
          </p>
        )}
      </div>

      <div className="md:col-span-2">
        <SigningPanel
          catalogue={catalogue}
          signing={signing}
          onSigning={onSigning}
          organisation={organisation}
          setOrganisation={setOrganisation}
          country={country}
          setCountry={setCountry}
          keyPasswords={keyPasswords}
          setKeyPasswords={setKeyPasswords}
          error={signingError}
        />
      </div>
    </div>
  );
}

/** Signing keys, off by default. The warning is not a formality. */
function SigningPanel({
  catalogue,
  signing,
  onSigning,
  organisation,
  setOrganisation,
  country,
  setCountry,
  keyPasswords,
  setKeyPasswords,
  error,
}: {
  catalogue: Catalogue;
  signing: boolean;
  onSigning: (next: boolean) => void;
  organisation: string;
  setOrganisation: (v: string) => void;
  country: string;
  setCountry: (v: string) => void;
  keyPasswords: Record<string, string>;
  setKeyPasswords: (next: Record<string, string>) => void;
  error?: string;
}) {
  if (!catalogue.keystoresAvailable) {
    return (
      <div className="rounded-xl border border-ink-700 bg-ink-900 p-5">
        <p className="text-sm font-semibold text-ink-100">Signing keys are not available here</p>
        <p className="mt-2 text-xs leading-relaxed text-ink-400">
          This server has no JDK, so it cannot run <code className="text-ink-300">keytool</code>.
          The zip ships <code className="text-ink-300">keystore.properties.template</code> and the
          README has the four commands.
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-ink-700 bg-ink-900 p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-ink-100">Generate signing keys</p>
          <p className="mt-1 text-xs text-ink-400">
            All four: dev, staging, prod and playstore. Off by default.
          </p>
        </div>
        <Toggle checked={signing} onChange={onSigning} label="Generate signing keys" />
      </div>

      <p className="mt-4 rounded-lg border border-amber/30 bg-amber/10 p-3 text-xs leading-relaxed text-amber">
        <span className="font-semibold">Read this before turning it on.</span> These keys are
        created on this server and sent to you over the wire, so you cannot claim sole custody of
        them. That is fine for dev and staging. For <span className="font-semibold">prod</span> and{" "}
        <span className="font-semibold">playstore</span> it is a real trade: the Play upload key is
        the one credential whose loss — or whose leak — cannot be undone, and Google will not
        reissue it for you. If this app is going to the Play Store, generate those two yourself
        with the <code>keytool</code> commands in the README, or with the CLI in the repository,
        which runs entirely on your machine.
      </p>

      {signing && (
        <div className="mt-4 space-y-4">
          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2">
              <Field label="Organisation">
                <TextInput
                  value={organisation}
                  onChange={(event) => setOrganisation(event.target.value)}
                  placeholder="Acme Ltd"
                />
              </Field>
            </div>
            <Field label="Country">
              <TextInput
                value={country}
                maxLength={2}
                onChange={(event) => setCountry(event.target.value.toUpperCase())}
                className="font-mono"
              />
            </Field>
          </div>

          <div className="space-y-2">
            {catalogue.keystoreNames.map((name) => (
              <div key={name} className="flex items-center gap-3">
                <span className="w-20 shrink-0 font-mono text-xs text-ink-400">{name}</span>
                {/*
                  Deliberately not a password input. The value is written into
                  keystore.properties in plain text inside the same zip moments later, so masking
                  it would imply a secrecy the storage does not provide — and it has to be read
                  and saved somewhere before the tab closes.
                */}
                <TextInput
                  value={keyPasswords[name] ?? ""}
                  onChange={(event) =>
                    setKeyPasswords({ ...keyPasswords, [name]: event.target.value })
                  }
                  className="h-9 font-mono text-xs"
                  spellCheck={false}
                  autoComplete="off"
                />
              </div>
            ))}
          </div>

          {error ? (
            <p className="text-xs text-rose">{error}</p>
          ) : (
            <p className="text-xs leading-relaxed text-ink-400">
              Copy these into your password manager before you close the tab. They are in{" "}
              <code className="text-ink-300">keystore.properties</code> in the zip, which is
              git-ignored — so nothing else will remember them for you.
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function reviewScreen(features: Set<string>) {
  if (features.has("sample")) return "home" as const;
  if (features.has("paging")) return "feed" as const;
  if (features.has("auth")) return "signin" as const;
  return "settings" as const;
}

function ReviewStep({
  catalogue,
  appName,
  packageName,
  features,
  modules,
  fontName,
  accent,
  secondary,
  tertiary,
  motionStyle,
  designStyle,
  previewConfig,
  haptics,
  minSdk,
  targetSdk,
  versionName,
  signing,
  busy,
  failure,
  done,
  errors,
  onGenerate,
}: {
  catalogue: Catalogue;
  appName: string;
  packageName: string;
  features: Set<string>;
  modules: string[];
  fontName: string;
  accent: string;
  secondary: string;
  tertiary: string;
  motionStyle: string;
  designStyle: string;
  previewConfig: PreviewConfig;
  haptics: boolean;
  minSdk: number;
  targetSdk: number;
  versionName: string;
  signing: boolean;
  busy: boolean;
  failure: string | null;
  done: {
    filename: string;
    elapsedMs: number;
    bytes: number;
    keystoresSkipped: string[];
  } | null;
  errors: {
    appName?: string;
    packageName?: string;
    modules?: string;
    font?: string;
    devUrl?: string;
    prodUrl?: string;
    signing?: string;
  };
  onGenerate: () => void;
}) {
  const problem =
    errors.appName || errors.packageName || errors.modules
      ? "Fix the app name, package or modules in the Project step first."
      : errors.font
        ? errors.font
        : errors.devUrl || errors.prodUrl
          ? "Fix the backend URLs in the Build step first."
          : errors.signing;
  const blocked = Boolean(problem);
  const chosen = catalogue.features.filter((feature) => features.has(feature.key));

  return (
    <div className="grid gap-8 md:grid-cols-5">
      <div className="md:col-span-3 space-y-5">
        <dl className="grid gap-3 sm:grid-cols-2">
          {[
            ["App", appName],
            ["Package", packageName],
            ["Min / target SDK", `${minSdk} / ${targetSdk}`],
            ["Version", versionName],
            ["Typeface", fontName],
            ["Design style", designStyle],
            ["Motion", `${motionStyle}, haptics ${haptics ? "on" : "off"}`],
            ["Modules", modules.length ? modules.join(", ") : "none"],
            ["Signing keys", signing ? catalogue.keystoreNames.join(", ") : "debug key only"],
          ].map(([label, value]) => (
            <div key={label} className="rounded-lg border border-ink-700 bg-ink-900 px-4 py-3">
              <dt className="text-xs text-ink-400">{label}</dt>
              <dd className="mt-0.5 truncate text-sm font-medium text-ink-100">{value}</dd>
            </div>
          ))}
          <div className="rounded-lg border border-ink-700 bg-ink-900 px-4 py-3">
            <dt className="text-xs text-ink-400">Brand colours</dt>
            <dd className="mt-1 flex items-center gap-2">
              {[accent, secondary, tertiary].map((colour, index) => (
                <span
                  key={index}
                  title={colour.toUpperCase()}
                  className="h-4 w-4 rounded border border-ink-600"
                  style={{ background: colour }}
                />
              ))}
              <span className="font-mono text-sm text-ink-100">{accent.toUpperCase()}</span>
            </dd>
          </div>
        </dl>

        <div>
          <p className="mb-2 text-sm font-medium text-ink-200">
            {chosen.length} features
          </p>
          <div className="flex flex-wrap gap-1.5">
            {chosen.map((feature) => (
              <span
                key={feature.key}
                title={feature.description}
                className="rounded-md border border-ink-600 bg-ink-900 px-2 py-1 font-mono text-[11px] text-ink-300"
              >
                {feature.key}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div className="md:col-span-2">
        <div className="mb-4 flex justify-center gap-3">
          <ScaledPreview config={previewConfig} screen={reviewScreen(features)} scale={0.62} />
        </div>
        <div className="rounded-xl border border-ink-700 bg-ink-900 p-5">
          {signing ? (
            <>
              <p className="text-sm font-semibold text-amber">
                Four signing keys are in this zip
              </p>
              <p className="mt-2 text-xs leading-relaxed text-ink-400">
                Including <code className="text-ink-300">prod</code> and{" "}
                <code className="text-ink-300">playstore</code>, with their passwords in{" "}
                <code className="text-ink-300">keystore.properties</code>. They were made on this
                server, so their custody is not solely yours — save them somewhere durable, keep
                them out of version control, and replace the Play upload key with one you generate
                yourself before you publish anything you cannot re-sign.
              </p>
            </>
          ) : (
            <>
              <p className="text-sm font-semibold text-ink-100">Signing keys are not included</p>
              <p className="mt-2 text-xs leading-relaxed text-ink-400">
                The zip ships <code className="text-ink-300">keystore.properties.template</code>{" "}
                and the README has the four <code className="text-ink-300">keytool</code> commands.
                Turn on <span className="text-ink-200">Generate signing keys</span> in the Build step if
                you would rather they were made for you — the trade is spelled out there.
              </p>
            </>
          )}
        </div>

        <AnimatePresence>
          {done ? (
            <motion.div
              key="done"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              className="mt-4 rounded-xl border border-mint/30 bg-mint/10 p-5"
            >
              <p className="text-sm font-semibold text-mint">Downloaded</p>
              <p className="mt-1.5 font-mono text-xs text-ink-300">{done.filename}</p>
              <p className="mt-1 text-xs text-ink-400">
                {(done.bytes / 1024).toFixed(0)} KB, built in {(done.elapsedMs / 1000).toFixed(1)}s
              </p>
              {done.keystoresSkipped.length > 0 && (
                <p className="mt-3 rounded-lg border border-amber/30 bg-amber/10 p-3 text-xs leading-relaxed text-amber">
                  These keys could not be created: {done.keystoresSkipped.join(", ")}. The project
                  still builds — those variants fall back to the debug key and the build says so
                  on each run — but you will need to make them yourself before a release.
                </p>
              )}
              <ol className="mt-4 space-y-1.5 text-xs text-ink-300">
                <li>1. Unzip and open the folder in Android Studio.</li>
                <li>
                  2. <code className="text-ink-200">./gradlew :app:installDevDebug</code>
                </li>
                <li>
                  3. <code className="text-ink-200">./gradlew build</code> to check everything —
                  compile, tests and lint.
                </li>
                <li>
                  4. Using an AI agent? Point it at <code className="text-ink-200">AGENTS.md</code>{" "}
                  first.
                </li>
              </ol>
            </motion.div>
          ) : null}
        </AnimatePresence>

        {failure && (
          <motion.p
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-4 rounded-lg border border-rose/30 bg-rose/10 p-4 text-xs leading-relaxed text-rose"
          >
            {failure}
          </motion.p>
        )}

        <Button
          size="large"
          onClick={onGenerate}
          disabled={busy || blocked}
          className="mt-4 w-full"
        >
          {busy ? (
            <>
              <Spinner /> Generating…
            </>
          ) : done ? (
            "Generate again"
          ) : (
            "Generate and download"
          )}
        </Button>

        {blocked && (
          <p className="mt-2 text-center text-xs text-rose">
            {problem}
          </p>
        )}
      </div>
    </div>
  );
}
