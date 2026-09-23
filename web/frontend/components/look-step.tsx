"use client";

import { motion } from "framer-motion";
import { useEffect, useMemo, useState } from "react";
import type { Catalogue } from "../lib/api";
import {
  DESIGN_TRAITS,
  type DesignStyleKey,
  MOTION_TOKENS,
  type MotionStyleKey,
  deriveSecondary,
  deriveTertiary,
  palette,
} from "../lib/app-theme";
import { AppPreview, type PreviewConfig, type Screen, screensFor } from "./app-preview";
import { FontPicker } from "./font-picker";
import { MotionPreview } from "./motion-preview";
import { Segmented, TextInput, Toggle, press } from "./primitives";

const SCREEN_LABELS: Record<Screen, string> = {
  signin: "Sign in",
  home: "Home",
  feed: "Feed",
  search: "Search",
  profile: "Profile",
  settings: "Settings",
};

/** A phone drawn at full size and scaled down, so thumbnails keep the real proportions. */
export function ScaledPreview({
  config,
  screen,
  scale,
  onScreen,
}: {
  config: PreviewConfig;
  screen: Screen;
  scale: number;
  onScreen?: (screen: Screen) => void;
}) {
  const width = 280;
  return (
    <div style={{ width: width * scale, height: width * 2.05 * scale }} className="shrink-0">
      <div style={{ transform: `scale(${scale})`, transformOrigin: "top left", width }}>
        <AppPreview config={config} screen={screen} onScreen={onScreen} width={width} />
      </div>
    </div>
  );
}

/**
 * The big phone beside the Look and Motion steps.
 *
 * Tapping a tab in the phone switches screen exactly as the app does, and the row above it
 * reaches the screens that have no tab — sign in, when auth is on.
 */
export function LivePreview({ config, onDark }: { config: PreviewConfig; onDark: (dark: boolean) => void }) {
  const screens = useMemo(() => screensFor(config.features), [config.features]);
  const [screen, setScreen] = useState<Screen>(screens[0] ?? "settings");
  const activeScreen = screens.includes(screen) ? screen : screens[0] ?? "settings";

  useEffect(() => {
    if (activeScreen !== screen) setScreen(activeScreen);
  }, [activeScreen, screen]);

  return (
    <div className="lg:sticky lg:top-24">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div className="no-scrollbar flex gap-1 overflow-x-auto">
          {screens.map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => setScreen(option)}
              className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-medium transition-colors ${
                option === activeScreen ? "bg-ink-100 text-ink-950" : "text-ink-400 hover:bg-ink-800 hover:text-ink-100"
              }`}
            >
              {SCREEN_LABELS[option]}
            </button>
          ))}
        </div>
        <Segmented
          options={[
            { value: "light", label: "Light" },
            { value: "dark", label: "Dark" },
          ]}
          value={config.dark ? "dark" : "light"}
          onChange={(next) => onDark(next === "dark")}
          layoutId="preview-theme"
        />
      </div>
      {screens.length > 0 ? (
        <div className="flex justify-center">
          <AppPreview config={config} screen={activeScreen} onScreen={setScreen} />
        </div>
      ) : (
        <p className="rounded-lg border border-ink-700 bg-ink-900 p-4 text-sm text-ink-400">
          With no screens switched on, the app opens on a single empty stack. Turn on the reference
          feature, settings or a tab in Features to see it here.
        </p>
      )}
      <p className="mt-3 text-center text-xs leading-relaxed text-ink-400">
        Drawn in your browser from the template&apos;s own tokens and copy. Only screens you have
        switched on appear.
      </p>
    </div>
  );
}

export function LookStep({
  catalogue,
  config,
  setDesignStyle,
  fontName,
  setFontName,
  accent,
  setAccent,
  secondary,
  setSecondary,
  tertiary,
  setTertiary,
  onDark,
}: {
  catalogue: Catalogue;
  config: PreviewConfig;
  setDesignStyle: (v: DesignStyleKey) => void;
  fontName: string;
  setFontName: (v: string) => void;
  accent: string;
  setAccent: (v: string) => void;
  secondary: string;
  setSecondary: (v: string) => void;
  tertiary: string;
  setTertiary: (v: string) => void;
  onDark: (dark: boolean) => void;
}) {
  return (
    <div className="grid gap-10 lg:grid-cols-[1fr_300px]">
      <div className="space-y-8">
        <div>
          <p className="text-sm font-medium text-ink-200">Design style</p>
          <p className="mt-1 text-xs leading-relaxed text-ink-400">
            Corners, borders, fields and the tab bar, chosen together. One argument to{" "}
            <code className="text-ink-300">AppTheme</code>, so it is one line to change later — and
            the components are yours to reshape from there.
          </p>
          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            {catalogue.designStyles.map((style) => {
              const key = style.key as DesignStyleKey;
              const selected = config.designStyle === key;
              return (
                <motion.button
                  key={key}
                  {...press}
                  type="button"
                  onClick={() => setDesignStyle(key)}
                  className={`flex gap-3 overflow-hidden rounded-xl border p-3 text-left transition-colors ${
                    selected ? "border-accent bg-accent-dim/60" : "border-ink-700 bg-ink-900 hover:border-ink-600"
                  }`}
                >
                  <ScaledPreview config={{ ...config, designStyle: key }} screen={thumbnailScreen(config.features)} scale={0.4} />
                  <div className="min-w-0 py-1">
                    <p className="font-display text-base font-bold text-ink-100">{key}</p>
                    <p className="mt-1 text-xs leading-relaxed text-ink-400">{style.description}</p>
                    <ul className="mt-2 space-y-0.5">
                      {DESIGN_TRAITS[key].map((trait) => (
                        <li key={trait} className="flex items-center gap-1.5 text-[11px] text-ink-300">
                          <span className="h-1 w-1 shrink-0 rounded-full bg-accent" />
                          {trait}
                        </li>
                      ))}
                    </ul>
                  </div>
                </motion.button>
              );
            })}
          </div>
        </div>

        <div>
          <p className="text-sm font-medium text-ink-200">Brand colours</p>
          <p className="mt-1 text-xs leading-relaxed text-ink-400">
            Each becomes a full ramp — pressed, subtle, both dark-theme variants and whether text on
            it is black or white. The primary is the one the components draw with; leave the other
            two blank and they are worked out from it.
          </p>
          <div className="mt-3 space-y-2.5">
            <BrandColourRow label="Primary" value={accent} resolved={accent} onChange={setAccent} />
            <BrandColourRow
              label="Secondary"
              value={secondary}
              resolved={secondary || deriveSecondary(accent)}
              onChange={setSecondary}
              onDerive={() => setSecondary("")}
            />
            <BrandColourRow
              label="Tertiary"
              value={tertiary}
              resolved={tertiary || deriveTertiary(accent)}
              onChange={setTertiary}
              onDerive={() => setTertiary("")}
            />
          </div>
        </div>

        <FontPicker value={fontName} onChange={setFontName} suggestions={catalogue.fontSuggestions} />
      </div>

      <LivePreview config={config} onDark={onDark} />
    </div>
  );
}

/** The screen that shows most of a style at once: buttons, fields and the tab bar together. */
function thumbnailScreen(features: Set<string>): Screen {
  if (features.has("profile")) return "profile";
  if (features.has("auth")) return "signin";
  if (features.has("sample")) return "home";
  return "settings";
}

export function MotionStep({
  catalogue,
  config,
  motionStyle,
  setMotionStyle,
  haptics,
  setHaptics,
  onDark,
}: {
  catalogue: Catalogue;
  config: PreviewConfig;
  motionStyle: string;
  setMotionStyle: (v: MotionStyleKey) => void;
  haptics: boolean;
  setHaptics: (v: boolean) => void;
  onDark: (dark: boolean) => void;
}) {
  const accent = palette({ accent: config.accent }, false).accent;
  return (
    <div className="grid gap-10 lg:grid-cols-[1fr_300px]">
      <div className="space-y-6">
        <div>
          <p className="text-sm font-medium text-ink-200">How it moves</p>
          <p className="mt-1 text-xs leading-relaxed text-ink-400">
            Each loop is the style&apos;s own springs and durations from{" "}
            <code className="text-ink-300">AppMotion.kt</code>: a press and its release, a screen
            pushed and popped, a tab switch. Press the button to feel it.
          </p>
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          {catalogue.motionStyles.map((style) => {
            const key = style.key as MotionStyleKey;
            const selected = motionStyle === key;
            return (
              <div
                key={key}
                role="button"
                tabIndex={0}
                onClick={() => setMotionStyle(key)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") setMotionStyle(key);
                }}
                className={`flex cursor-pointer gap-4 rounded-xl border p-4 transition-colors ${
                  selected ? "border-accent bg-accent-dim/60" : "border-ink-700 bg-ink-900 hover:border-ink-600"
                }`}
              >
                <MotionPreview styleKey={key} accent={accent.base} onAccent={accent.on} fontName={config.fontName} />
                <div className="min-w-0">
                  <p className="font-display text-base font-bold text-ink-100">{key}</p>
                  <p className="mt-1 text-xs leading-relaxed text-ink-400">{style.description}</p>
                  <MotionNumbers styleKey={key} />
                  {selected && <p className="mt-2 text-[11px] font-semibold text-accent">Selected</p>}
                </div>
              </div>
            );
          })}
        </div>

        <div className="flex items-start gap-3 rounded-xl border border-ink-700 bg-ink-900 p-4">
          <Toggle checked={haptics} onChange={setHaptics} label="Haptics" />
          <div>
            <p className="text-sm font-medium text-ink-100">Haptics on by default</p>
            <p className="mt-0.5 text-xs leading-relaxed text-ink-400">
              A light vibration when a control answers. The device&apos;s own setting still applies
              on top, so this cannot make a phone buzz that its owner has asked to stay quiet.
            </p>
          </div>
        </div>
      </div>

      <LivePreview config={config} onDark={onDark} />
    </div>
  );
}

/** The numbers behind the loop, as they appear in AppMotion.kt. */
function MotionNumbers({ styleKey }: { styleKey: MotionStyleKey }) {
  const t = MOTION_TOKENS[styleKey];
  const rows: [string, string][] = [
    ["press", `${t.pressScale} → ${t.pressOvershoot}`],
    ["press spring", `ζ ${t.pressDamping} · k ${t.pressStiffness}`],
    ["screen spring", `ζ ${t.navigationDamping} · k ${t.navigationStiffness}`],
    ["durations", `${t.quick} / ${t.medium} / ${t.slow} ms`],
  ];
  return (
    <dl className="mt-3 space-y-0.5 font-mono text-[10.5px]">
      {rows.map(([label, value]) => (
        <div key={label} className="flex gap-2">
          <dt className="w-[82px] shrink-0 text-ink-500">{label}</dt>
          <dd className="text-ink-300">{value}</dd>
        </div>
      ))}
    </dl>
  );
}

/**
 * One brand colour: a swatch, the hex, and — for the two supporting colours — a way back to
 * having it worked out from the primary. `value` is what gets sent (empty means derive);
 * `resolved` is what gets shown.
 */
function BrandColourRow({
  label,
  value,
  resolved,
  onChange,
  onDerive,
}: {
  label: string;
  value: string;
  resolved: string;
  onChange: (v: string) => void;
  onDerive?: () => void;
}) {
  return (
    <div className="flex items-center gap-2.5">
      <span className="w-[70px] shrink-0 text-sm text-ink-300">{label}</span>
      <input
        type="color"
        value={resolved}
        onChange={(event) => onChange(event.target.value)}
        className="h-10 w-12 shrink-0 cursor-pointer rounded-lg border border-ink-600 bg-ink-900 p-1"
        aria-label={`${label} colour`}
      />
      <TextInput
        value={resolved}
        onChange={(event) => onChange(event.target.value)}
        className="font-mono text-sm uppercase"
      />
      {onDerive ? (
        <button
          type="button"
          onClick={onDerive}
          disabled={!value}
          className="shrink-0 rounded-md border border-ink-600 px-2 py-1.5 text-xs text-ink-400 transition-colors hover:border-ink-500 hover:text-ink-200 disabled:cursor-default disabled:border-ink-700 disabled:text-ink-500"
          title="Work this colour out from the primary again"
        >
          {value ? "Derive" : "Derived"}
        </button>
      ) : null}
    </div>
  );
}
