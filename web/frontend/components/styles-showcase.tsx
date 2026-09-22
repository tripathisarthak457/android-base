"use client";

import { motion, useInView, useReducedMotion } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { DESIGN_TRAITS, type DesignStyleKey } from "../lib/app-theme";
import type { Screen } from "./app-preview";
import { ScaledPreview } from "./look-step";
import { Segmented } from "./primitives";

const STYLES: { key: DesignStyleKey; accent: string; font: string; blurb: string }[] = [
  { key: "Utility", accent: "#2C6BED", font: "DM Sans", blurb: "Tools, finance, admin" },
  { key: "Social", accent: "#E4405F", font: "Plus Jakarta Sans", blurb: "Feeds, chat, communities" },
  { key: "Editorial", accent: "#1F4D3A", font: "Fraunces", blurb: "Reading, news, portfolios" },
  { key: "Playful", accent: "#F2A93B", font: "Nunito", blurb: "Games, kids, habit trackers" },
];

const SHOWCASE_FEATURES = new Set(["sample", "paging", "search", "profile", "settings", "language", "media"]);
const CYCLE: Screen[] = ["home", "feed", "profile", "settings"];

/**
 * Four design styles side by side, each with a brand colour and typeface that suits it.
 *
 * The point is that these are one codebase: the same eighty components, the same screens, with
 * one enum changed. Each phone steps through its screens on a timer, offset from its neighbour so
 * the row never changes all at once.
 */
export function StylesShowcase() {
  const [dark, setDark] = useState(false);
  const [tick, setTick] = useState(0);
  const ref = useRef<HTMLDivElement>(null);
  const inView = useInView(ref, { margin: "-120px" });
  const reduce = useReducedMotion();

  useEffect(() => {
    if (!inView || reduce) return;
    const timer = setInterval(() => setTick((value) => value + 1), 1400);
    return () => clearInterval(timer);
  }, [inView, reduce]);

  useEffect(() => {
    const families = STYLES.map((style) => style.font.replace(/ /g, "+")).join("&family=");
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = `https://fonts.googleapis.com/css2?family=${families}:wght@400;600;700&display=swap`;
    document.head.appendChild(link);
    return () => link.remove();
  }, []);

  return (
    <section className="relative border-b border-ink-800">
      <div className="mx-auto max-w-6xl px-6 py-20" ref={ref}>
        <div className="flex flex-wrap items-end justify-between gap-6">
          <div className="max-w-2xl">
            <p className="font-mono text-xs uppercase tracking-[0.18em] text-accent">Design styles</p>
            <h2 className="font-display mt-3 text-4xl font-bold text-ink-100 md:text-5xl">
              Four looks. One codebase.
            </h2>
            <p className="mt-4 text-ink-300">
              The same components and screens, with one enum changed:{" "}
              <code className="rounded bg-ink-800 px-1.5 py-0.5 font-mono text-[13px] text-ink-200">
                AppTheme(designStyle = …)
              </code>
              . Pick one here, then make it yours — every component is plain Compose you own.
            </p>
          </div>
          <Segmented
            options={[
              { value: "light", label: "Light" },
              { value: "dark", label: "Dark" },
            ]}
            value={dark ? "dark" : "light"}
            onChange={(next) => setDark(next === "dark")}
            layoutId="showcase-theme"
          />
        </div>

        <div className="no-scrollbar -mx-6 mt-12 flex gap-6 overflow-x-auto px-6 pb-4 lg:mx-0 lg:grid lg:grid-cols-4 lg:overflow-visible lg:px-0">
          {STYLES.map((style, index) => (
            <motion.div
              key={style.key}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-80px" }}
              transition={{ duration: 0.5, delay: index * 0.07, ease: [0.16, 1, 0.3, 1] }}
              className="flex shrink-0 flex-col items-center"
            >
              <ScaledPreview
                config={{
                  designStyle: style.key,
                  motionStyle: "Standard",
                  accent: style.accent,
                  fontName: style.font,
                  dark,
                  features: SHOWCASE_FEATURES,
                }}
                screen={CYCLE[Math.floor((tick + index) / 2) % CYCLE.length]}
                scale={0.86}
              />
              <div className="mt-5 w-full max-w-[240px]">
                <div className="flex items-baseline justify-between gap-3">
                  <h3 className="font-display text-xl font-bold text-ink-100">{style.key}</h3>
                  <span className="text-xs text-ink-400">{style.blurb}</span>
                </div>
                <ul className="mt-2 space-y-1">
                  {DESIGN_TRAITS[style.key].map((trait) => (
                    <li key={trait} className="flex items-center gap-2 text-sm text-ink-300">
                      <span className="h-1.5 w-1.5 shrink-0 rounded-full" style={{ background: style.accent }} />
                      {trait}
                    </li>
                  ))}
                </ul>
              </div>
            </motion.div>
          ))}
        </div>

        <p className="mt-8 max-w-3xl text-sm text-ink-400">
          Drawn in your browser from the template&apos;s own tokens and copy, with the brand colour
          and typeface shown under each. The catalog app switches between all four live on a device.
        </p>
      </div>
    </section>
  );
}
