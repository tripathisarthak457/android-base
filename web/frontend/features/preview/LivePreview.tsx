"use client";

import { type RefObject, useDeferredValue, useEffect, useId, useMemo, useRef, useState } from "react";
import { Segmented } from "../../components/primitives.tsx";
import type { Tokens } from "../catalogue/types.ts";
import { AppPreview, PHONE_ASPECT, PHONE_WIDTH } from "./AppPreview.tsx";
import { useFamily } from "./fonts.ts";
import { type PreviewTab, planTabs, screensFor } from "./tabs.ts";
import { type PreviewTheme, previewTheme, weightsFor } from "./theme.ts";

/** Everything a preview is drawn from: the visitor's answers that change how the app looks. */
export type PreviewInput = {
  tokens: Tokens;
  designStyle: string;
  motionStyle: string;
  accent: string;
  secondary: string;
  tertiary: string;
  font: string;
  features: ReadonlySet<string>;
  modules: string[];
  haptics: boolean;
};

export function usePreviewTheme(input: PreviewInput, dark: boolean): PreviewTheme {
  const { tokens, designStyle, motionStyle, accent, secondary, tertiary } = input;
  return useMemo(
    () => previewTheme(tokens, { designStyle, motionStyle, dark, accent, secondary, tertiary }),
    [tokens, designStyle, motionStyle, dark, accent, secondary, tertiary],
  );
}

/** Scales the phone down to the width it is given, so it is never clipped on a narrow screen. */
function useFitScale(): [RefObject<HTMLDivElement | null>, number] {
  const ref = useRef<HTMLDivElement>(null);
  const [scale, setScale] = useState(1);
  useEffect(() => {
    const element = ref.current;
    if (!element) return;
    const observer = new ResizeObserver(([entry]) => {
      if (entry) setScale(Math.min(1, entry.contentRect.width / PHONE_WIDTH));
    });
    observer.observe(element);
    return () => observer.disconnect();
  }, []);
  return [ref, scale];
}

/**
 * The big phone beside the Look and Motion steps. It reads a deferred copy of the answers, so a
 * colour drag or a keystroke stays responsive while the phone catches up a frame later.
 */
export function LivePreview({ input, dark, onDark }: { input: PreviewInput; dark: boolean; onDark: (dark: boolean) => void }) {
  const deferred = useDeferredValue(input);
  const theme = usePreviewTheme(deferred, dark);
  const tabs = useMemo(() => planTabs(deferred.features, deferred.modules).tabs, [deferred.features, deferred.modules]);
  const screens = useMemo(() => screensFor(deferred.features, deferred.modules), [deferred.features, deferred.modules]);
  const [chosen, setChosen] = useState<string | null>(null);
  const screenName = useId();
  const screen: PreviewTab | undefined =
    screens.find((candidate) => candidate.key === chosen) ?? screens.find((candidate) => candidate.screen !== "signin") ?? screens[0];
  const weights = useMemo(() => weightsFor(deferred.tokens, theme.style.voice), [deferred.tokens, theme.style.voice]);
  useFamily(deferred.font, weights);
  const [fitRef, scale] = useFitScale();
  const options = useMemo(() => ({ features: deferred.features, haptics: deferred.haptics }), [deferred.features, deferred.haptics]);

  return (
    // self-start: a grid item stretches to its row by default, and a sticky box as tall as its
    // container has no room to move.
    <div className="min-w-0 lg:sticky lg:top-24 lg:self-start">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div role="radiogroup" aria-label="Screen" className="no-scrollbar flex gap-1 overflow-x-auto">
          {screens.map((option) => {
            const on = option.key === screen?.key;
            return (
              <label
                key={option.key}
                className={`shrink-0 cursor-pointer rounded-full px-2.5 py-1 text-xs font-medium transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent ${
                  on ? "bg-ink-100 text-ink-950" : "text-ink-400 hover:bg-ink-800 hover:text-ink-100"
                }`}
              >
                <input type="radio" name={screenName} checked={on} onChange={() => setChosen(option.key)} className="sr-only" />
                {option.label}
              </label>
            );
          })}
        </div>
        <Segmented
          label="Preview theme"
          options={[
            { value: "light", label: "Light" },
            { value: "dark", label: "Dark" },
          ]}
          value={dark ? "dark" : "light"}
          onChange={(next) => onDark(next === "dark")}
        />
      </div>
      <div ref={fitRef} className="flex justify-center">
        {screen && (
          <div style={{ width: PHONE_WIDTH * scale, height: PHONE_WIDTH * PHONE_ASPECT * scale }}>
            <div style={{ transform: `scale(${scale})`, transformOrigin: "top left", width: PHONE_WIDTH }}>
              <AppPreview theme={theme} font={deferred.font} tabs={tabs} screen={screen} options={options} live onSelectTab={setChosen} />
            </div>
          </div>
        )}
      </div>
      <p className="mt-3 text-center text-xs leading-relaxed text-ink-400">
        Drawn in your browser from the generator&apos;s own tokens. Tap the tabs; the screen scrolls under the bar.
      </p>
    </div>
  );
}
