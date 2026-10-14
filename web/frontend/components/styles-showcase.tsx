"use client";

import { useInView } from "framer-motion";
import { useEffect, useMemo, useRef, useState, useSyncExternalStore } from "react";
import { ScaledPreview } from "../features/preview/AppPreview.tsx";
import { loadFamily } from "../features/preview/fonts.ts";
import { planTabs } from "../features/preview/tabs.ts";
import { weightsFor } from "../features/preview/theme.ts";
import { traits } from "../features/preview/traits.ts";
import { usePreviewTheme } from "../features/preview/usePreviewTheme.ts";
import { CATALOGUE, android } from "../lib/catalogue.ts";
import { PauseButton, Segmented, usePaused } from "./primitives.tsx";

/** A brand colour and typeface that suit each style; a style without one shows the template's. */
const PAIRINGS: Record<string, { accent: string; font: string; blurb: string }> = {
  Utility: { accent: "#2C6BED", font: "DM Sans", blurb: "Tools, finance, admin" },
  Social: { accent: "#E4405F", font: "Plus Jakarta Sans", blurb: "Feeds, chat, communities" },
  Editorial: { accent: "#1F4D3A", font: "Fraunces", blurb: "Reading, news, portfolios" },
  Playful: { accent: "#F2A93B", font: "Nunito", blurb: "Games, kids, habit trackers" },
};

/** From Tailwind's `sm` the phones wrap; below it they scroll sideways. */
const WRAPS = "(min-width: 40rem)";

function subscribeToWraps(onChange: () => void): () => void {
  const query = matchMedia(WRAPS);
  query.addEventListener("change", onChange);
  return () => query.removeEventListener("change", onChange);
}

const FEATURES: ReadonlySet<string> = new Set(["sample", "paging", "search", "profile", "settings", "language", "media"]);
const CYCLE = ["home", "feed", "profile", "settings"];

function Phone({ styleKey, accent, font, dark, step }: { styleKey: string; accent: string; font: string; dark: boolean; step: number }) {
  const tokens = CATALOGUE.tokens;
  const theme = usePreviewTheme(
    { tokens, designStyle: styleKey, motionStyle: "Standard", accent, secondary: "", tertiary: "", font, features: FEATURES, modules: [], haptics: true },
    dark,
  );
  const tabs = useMemo(() => planTabs(FEATURES, []).tabs, []);
  const screen = tabs.find((tab) => tab.key === CYCLE[step % CYCLE.length]) ?? tabs[0];
  const options = useMemo(() => ({ features: FEATURES, haptics: true }), []);
  if (!screen) return null;
  return <ScaledPreview theme={theme} font={font} tabs={tabs} screen={screen} options={options} scale={0.86} />;
}

/** The design styles side by side, each with a brand colour and typeface that suits it. */
export function StylesShowcase() {
  const [dark, setDark] = useState(false);
  const [tick, setTick] = useState(0);
  const ref = useRef<HTMLDivElement>(null);
  const inView = useInView(ref, { margin: "-120px" });
  const [paused, setPaused] = usePaused();
  const wraps = useSyncExternalStore(subscribeToWraps, () => matchMedia(WRAPS).matches, () => false);
  const styles = android().design_styles;
  const defaults = android().defaults;

  // A hidden tab still runs intervals, throttled, and each tick re-renders all four phones.
  useEffect(() => {
    if (!inView || paused) return;
    let timer: ReturnType<typeof setInterval> | undefined;
    const sync = () => {
      clearInterval(timer);
      timer = document.hidden ? undefined : setInterval(() => setTick((value) => value + 1), 1400);
    };
    sync();
    document.addEventListener("visibilitychange", sync);
    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", sync);
    };
  }, [inView, paused]);

  useEffect(() => {
    if (!inView) return;
    for (const style of styles) {
      const font = PAIRINGS[style.key]?.font ?? defaults.font_name;
      void loadFamily(font, weightsFor(CATALOGUE.tokens, CATALOGUE.tokens.styles[style.key]?.voice ?? "Neutral"));
    }
  }, [inView, styles, defaults.font_name]);

  return (
    <section className="relative border-b border-ink-800">
      <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6" ref={ref}>
        <div className="flex flex-wrap items-end justify-between gap-6">
          <div className="max-w-2xl">
            <p className="font-mono text-xs uppercase tracking-[0.18em] text-accent">Design styles</p>
            <h2 className="font-display mt-3 text-4xl font-bold text-ink-100 md:text-5xl">
              {styles.length} looks. One codebase.
            </h2>
            <p className="mt-4 text-ink-300">
              The same components and screens, with one argument changed:{" "}
              <code className="rounded bg-ink-800 px-1.5 py-0.5 font-mono text-[13px] text-ink-200">AppTheme(designStyle = …)</code>. Pick one
              here, then make it yours: every component is plain Compose you own.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <PauseButton paused={paused} onChange={setPaused} label="the style previews" />
            <Segmented
              label="Showcase theme"
              options={[
                { value: "light", label: "Light" },
                { value: "dark", label: "Dark" },
              ]}
              value={dark ? "dark" : "light"}
              onChange={(next) => setDark(next === "dark")}
            />
          </div>
        </div>

        {/* Only a phone-width window scrolls the phones sideways, where a swipe does it; from sm they
            wrap, so a mouse or keyboard reaches every style without a horizontal scroll. Nothing in
            the scroller takes the focus, and Safari, unlike Chrome and Firefox, does not make a
            scroller a tab stop by itself, so the list is one while it scrolls, for the arrow keys. */}
        <ul
          tabIndex={wraps ? undefined : 0}
          aria-label="Design styles"
          className="-mx-4 mt-12 flex gap-6 overflow-x-auto px-4 pb-4 focus-visible:outline-offset-[-2px] sm:mx-0 sm:grid sm:grid-cols-2 sm:gap-y-12 sm:overflow-visible sm:px-0 lg:grid-cols-4"
        >
          {styles.map((style, index) => {
            const pairing = PAIRINGS[style.key] ?? { accent: defaults.accent_colour, font: defaults.font_name, blurb: "" };
            const tokens = CATALOGUE.tokens.styles[style.key];
            return (
              <li key={style.key} className="flex shrink-0 flex-col items-center">
                <Phone styleKey={style.key} accent={pairing.accent} font={pairing.font} dark={dark} step={Math.floor((tick + index) / 2)} />
                <div className="mt-5 w-full max-w-[240px]">
                  <div className="flex items-baseline justify-between gap-3">
                    <h3 className="font-display text-xl font-bold text-ink-100">{style.title}</h3>
                    <span className="text-xs text-ink-400">{pairing.blurb}</span>
                  </div>
                  <p className="mt-1 flex items-center gap-1.5 text-xs text-ink-400">
                    <span aria-hidden className="h-2.5 w-2.5 shrink-0 rounded-sm" style={{ background: pairing.accent }} />
                    {pairing.font} · <span className="font-mono">{pairing.accent}</span>
                  </p>
                  {tokens && (
                    <ul className="mt-2 space-y-1">
                      {traits(tokens).map((trait) => (
                        <li key={trait} className="flex items-center gap-2 text-sm text-ink-300">
                          <span aria-hidden className="h-1.5 w-1.5 shrink-0 rounded-full" style={{ background: pairing.accent }} />
                          {trait}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </li>
            );
          })}
        </ul>

        <p className="mt-8 max-w-3xl text-sm text-ink-400">
          Drawn in your browser from the generator&apos;s own design tokens, with the brand colour and typeface shown under each.
          The catalog app switches between them live on a device.
        </p>
      </div>
    </section>
  );
}
