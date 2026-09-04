"use client";

import { AnimatePresence, motion } from "framer-motion";
import { useEffect, useMemo, useState } from "react";
import {
  ApiError,
  type Catalogue,
  type GenerateRequest,
  generateProject,
  track,
} from "../lib/api";
import type { ReportContext } from "./feedback";
import { Badge, Button, Card, Field, Spinner, TextInput, Toggle, press } from "./primitives";

type Step = "identity" | "features" | "look" | "build" | "review";

const STEPS: { id: Step; label: string; blurb: string }[] = [
  { id: "identity", label: "Project", blurb: "What it is called and what it is called in code" },
  { id: "features", label: "Features", blurb: "What comes in the box" },
  { id: "look", label: "Look and feel", blurb: "Typeface, colour, how it moves" },
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

function moduleError(names: string[]): string | undefined {
  for (const name of names) {
    if (!/^[a-z][a-z0-9_]*$/.test(name)) {
      return `"${name}" must be lower_snake_case.`;
    }
    if (["app", "core", "data", "feature", "build", "catalog", "benchmark"].includes(name)) {
      return `"${name}" is already a directory in the project.`;
    }
  }
  if (new Set(names).size !== names.length) return "Module names must be unique.";
  return undefined;
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
  const [fontName, setFontName] = useState(catalogue.defaults.fontName);
  const [accent, setAccent] = useState(catalogue.defaults.accentColour);
  const [motionStyle, setMotionStyle] = useState(catalogue.defaults.motionStyle);
  const [haptics, setHaptics] = useState(catalogue.defaults.hapticsEnabled);
  const [minSdk, setMinSdk] = useState(catalogue.defaults.minSdk);
  const [targetSdk, setTargetSdk] = useState(catalogue.defaults.targetSdk);
  const [versionName, setVersionName] = useState(catalogue.defaults.versionName);
  const [devUrl, setDevUrl] = useState("https://dev.example.com/api/");
  const [prodUrl, setProdUrl] = useState("https://api.example.com/api/");

  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);
  const [done, setDone] = useState<{ filename: string; elapsedMs: number; bytes: number } | null>(
    null,
  );

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
    modules: moduleError(moduleNames),
  };
  const identityValid = !errors.appName && !errors.packageName;

  /**
   * Ticking a feature also ticks what it needs; unticking one unticks what needed it.
   *
   * The dependency graph lives in the generator and comes down in `implies`, so the site does not
   * have a second copy of "push needs Firebase" to keep in step.
   */
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
      fontName,
      accentColour: accent,
    });
  }, [
    onContextChange, appName, packageName, features, preset, minSdk, motionStyle, fontName, accent,
  ]);

  const hasNetwork = features.has("network");
  const hasDeeplink = features.has("deeplink");

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
      motion_style: motionStyle,
      haptics_enabled: haptics,
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
    };

    try {
      const { blob, filename, elapsedMs } = await generateProject(request);

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

      setDone({ filename, elapsedMs, bytes: blob.size });
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
        <h2 className="text-3xl font-bold tracking-tight text-ink-100">Configure your project</h2>
        <p className="mt-3 text-ink-300">
          Five short steps. Everything has a working default, so you can jump to Review and
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
                fontName={fontName}
                setFontName={setFontName}
                accent={accent}
                setAccent={setAccent}
                motionStyle={motionStyle}
                setMotionStyle={setMotionStyle}
                haptics={haptics}
                setHaptics={setHaptics}
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
                motionStyle={motionStyle}
                haptics={haptics}
                minSdk={minSdk}
                targetSdk={targetSdk}
                versionName={versionName}
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
                      ? "bg-accent text-ink-950"
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
}: {
  appName: string;
  setAppName: (v: string) => void;
  packageName: string;
  setPackageName: (v: string) => void;
  errors: { appName?: string; packageName?: string; modules?: string };
  modules: string;
  setModules: (v: string) => void;
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
                              ? "border-accent bg-accent text-ink-950"
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

function LookStep({
  catalogue,
  fontName,
  setFontName,
  accent,
  setAccent,
  motionStyle,
  setMotionStyle,
  haptics,
  setHaptics,
}: {
  catalogue: Catalogue;
  fontName: string;
  setFontName: (v: string) => void;
  accent: string;
  setAccent: (v: string) => void;
  motionStyle: string;
  setMotionStyle: (v: string) => void;
  haptics: boolean;
  setHaptics: (v: boolean) => void;
}) {
  return (
    <div className="grid gap-8 md:grid-cols-2">
      <div className="space-y-6">
        <div>
          <Field
            label="Typeface"
            hint="Any family from fonts.google.com, spelled as it is on the family's page. It is written into one constant that all seventeen text styles read from."
          >
            <TextInput value={fontName} onChange={(event) => setFontName(event.target.value)} />
          </Field>
          <div className="mt-2.5 flex flex-wrap gap-1.5">
            {catalogue.fontSuggestions.map((suggestion) => (
              <button
                key={suggestion}
                type="button"
                onClick={() => setFontName(suggestion)}
                className={`rounded-full border px-2.5 py-1 text-xs transition-colors ${
                  fontName === suggestion
                    ? "border-accent bg-accent-dim text-accent-bright"
                    : "border-ink-600 text-ink-400 hover:border-ink-500 hover:text-ink-200"
                }`}
              >
                {suggestion}
              </button>
            ))}
          </div>
        </div>

        <Field
          label="Accent colour"
          hint="The whole ramp is derived from this one hex: the pressed state, the subtle fill, both dark-theme variants, the launcher background, and whether text on it is black or white."
        >
          <div className="flex items-center gap-3">
            <input
              type="color"
              value={accent}
              onChange={(event) => setAccent(event.target.value)}
              className="h-11 w-14 cursor-pointer rounded-lg border border-ink-600 bg-ink-900 p-1"
              aria-label="Accent colour"
            />
            <TextInput
              value={accent}
              onChange={(event) => setAccent(event.target.value)}
              className="font-mono text-sm uppercase"
            />
          </div>
        </Field>

        <div>
          <p className="mb-2 text-sm font-medium text-ink-200">How it moves</p>
          <div className="grid gap-2">
            {catalogue.motionStyles.map((style) => (
              <motion.button
                key={style.key}
                {...press}
                type="button"
                onClick={() => setMotionStyle(style.key)}
                className={`rounded-lg border p-3 text-left transition-colors ${
                  motionStyle === style.key
                    ? "border-accent/45 bg-accent-dim/45"
                    : "border-ink-700 bg-ink-900 hover:border-ink-600"
                }`}
              >
                <span className="text-sm font-semibold text-ink-100">{style.key}</span>
                <p className="mt-0.5 text-xs text-ink-400">{style.description}</p>
              </motion.button>
            ))}
          </div>
        </div>

        <div className="flex items-start gap-3 rounded-lg border border-ink-700 bg-ink-900 p-4">
          <Toggle checked={haptics} onChange={setHaptics} label="Haptics" />
          <div>
            <p className="text-sm font-medium text-ink-100">Haptics on by default</p>
            <p className="mt-0.5 text-xs leading-relaxed text-ink-400">
              A light vibration when a control answers. Routed through the platform, so the
              device&apos;s own setting still applies on top and this cannot make a phone buzz that
              its owner has asked to stay quiet. One boolean in AppTheme silences the app.
            </p>
          </div>
        </div>
      </div>

      <Preview accent={accent} fontName={fontName} />
    </div>
  );
}

/**
 * A phone-shaped preview of the choices above.
 *
 * Rendered in the browser with the same accent maths the generator uses, so what is on screen is
 * what the APK will look like rather than a mock somebody has to remember to update.
 */
function Preview({ accent, fontName }: { accent: string; fontName: string }) {
  const onAccent = readableOn(accent);

  return (
    <div className="flex items-start justify-center">
      <div
        className="w-full max-w-[280px] overflow-hidden rounded-[28px] border-4 border-ink-700 bg-ink-950 shadow-2xl"
        style={{ fontFamily: `"${fontName}", var(--font-sans)` }}
      >
        <div className="flex items-center justify-between px-5 pt-3 font-mono text-[10px] text-ink-400">
          <span>9:41</span>
          <span>▮▮▮</span>
        </div>

        <div className="px-5 pb-6 pt-5">
          <p className="text-[22px] font-bold leading-tight text-ink-100">Welcome back</p>

          <div className="mt-5 space-y-3">
            <div>
              <p className="mb-1 text-[11px] font-medium text-ink-300">Email</p>
              <div className="h-9 rounded-lg border border-ink-600 bg-ink-900 px-3 pt-2 text-[11px] text-ink-500">
                you@example.com
              </div>
            </div>
            <div>
              <p className="mb-1 text-[11px] font-medium text-ink-300">Password</p>
              <div
                className="h-9 rounded-lg border bg-ink-900"
                style={{ borderColor: accent }}
                aria-hidden
              />
            </div>
          </div>

          <motion.div
            key={accent}
            initial={{ scale: 0.96 }}
            animate={{ scale: 1 }}
            transition={{ type: "spring", stiffness: 420, damping: 18 }}
            className="mt-5 flex h-10 items-center justify-center rounded-lg text-[13px] font-bold"
            style={{ background: accent, color: onAccent }}
          >
            Sign in
          </motion.div>

          <div className="mt-4 flex justify-between text-[11px] font-medium text-ink-400">
            <span>Create account</span>
            <span>Forgot password</span>
          </div>

          <div className="mt-6 flex gap-1.5">
            {[0, 1, 2].map((index) => (
              <div
                key={index}
                className="h-1 rounded-full"
                style={{
                  width: index === 0 ? 18 : 6,
                  background: index === 0 ? accent : "var(--color-ink-600)",
                }}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

/** WCAG relative luminance, the same test the generator uses to pick onAccent. */
function readableOn(hex: string): string {
  const clean = hex.replace("#", "");
  if (clean.length !== 6) return "#ffffff";
  const channels = [0, 2, 4].map((offset) => {
    const value = parseInt(clean.slice(offset, offset + 2), 16) / 255;
    return value <= 0.03928 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  });
  const luminance = 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
  return 1.05 / (luminance + 0.05) >= (luminance + 0.05) / 0.05 ? "#ffffff" : "#0b0e14";
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
            <Field label="Dev base URL" hint="Used by the dev and staging flavours.">
              <TextInput
                value={devUrl}
                onChange={(event) => setDevUrl(event.target.value)}
                className="font-mono text-sm"
              />
            </Field>
            <Field label="Production base URL" hint="Used by prod and playstore.">
              <TextInput
                value={prodUrl}
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
    </div>
  );
}

function ReviewStep({
  catalogue,
  appName,
  packageName,
  features,
  modules,
  fontName,
  accent,
  motionStyle,
  haptics,
  minSdk,
  targetSdk,
  versionName,
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
  motionStyle: string;
  haptics: boolean;
  minSdk: number;
  targetSdk: number;
  versionName: string;
  busy: boolean;
  failure: string | null;
  done: { filename: string; elapsedMs: number; bytes: number } | null;
  errors: { appName?: string; packageName?: string; modules?: string };
  onGenerate: () => void;
}) {
  const blocked = Boolean(errors.appName || errors.packageName || errors.modules);
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
            ["Motion", `${motionStyle}, haptics ${haptics ? "on" : "off"}`],
            ["Modules", modules.length ? modules.join(", ") : "none"],
          ].map(([label, value]) => (
            <div key={label} className="rounded-lg border border-ink-700 bg-ink-900 px-4 py-3">
              <dt className="text-xs text-ink-400">{label}</dt>
              <dd className="mt-0.5 truncate text-sm font-medium text-ink-100">{value}</dd>
            </div>
          ))}
          <div className="rounded-lg border border-ink-700 bg-ink-900 px-4 py-3">
            <dt className="text-xs text-ink-400">Accent</dt>
            <dd className="mt-1 flex items-center gap-2">
              <span
                className="h-4 w-4 rounded border border-ink-600"
                style={{ background: accent }}
              />
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
        <div className="rounded-xl border border-ink-700 bg-ink-900 p-5">
          <p className="text-sm font-semibold text-ink-100">Signing keys are not included</p>
          <p className="mt-2 text-xs leading-relaxed text-ink-400">
            The zip ships <code className="text-ink-300">keystore.properties.template</code> and the
            README has the four <code className="text-ink-300">keytool</code> commands. A production
            upload key generated on someone else&apos;s server and sent back over the wire is a key
            whose custody you cannot claim — and losing control of a Play upload key is the one
            Android mistake that cannot be undone. The CLI in the repository does generate them,
            locally.
          </p>
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
              <ol className="mt-4 space-y-1.5 text-xs text-ink-300">
                <li>1. Unzip and open the folder in Android Studio.</li>
                <li>
                  2. <code className="text-ink-200">./gradlew :app:installDevDebug</code>
                </li>
                <li>
                  3. <code className="text-ink-200">./gradlew build</code> to check everything —
                  compile, tests, detekt and lint.
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
            Fix the app or package name in step 1 first.
          </p>
        )}
      </div>
    </div>
  );
}
