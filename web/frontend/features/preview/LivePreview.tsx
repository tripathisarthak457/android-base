"use client";

import { type RefObject, useCallback, useDeferredValue, useEffect, useId, useMemo, useRef, useState } from "react";
import { Segmented } from "../../components/primitives.tsx";
import { AppPreview, PHONE_ASPECT, PHONE_WIDTH } from "./AppPreview.tsx";
import { useFamily } from "./fonts.ts";
import { type PreviewTab, planTabs, screensFor } from "./tabs.ts";
import { weightsFor } from "./theme.ts";
import { type PreviewInput, usePreviewTheme } from "./usePreviewTheme.ts";

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

/** What the visitor chose to look at in the live preview, kept across the steps that show it. */
export type PreviewView = { dark: boolean; screen: string | null };

/**
 * The big phone beside the Look and Motion steps. It reads a deferred copy of the answers, so a
 * colour drag or a keystroke stays responsive while the phone catches up a frame later.
 */
export function LivePreview({ input, view, onView }: { input: PreviewInput; view: PreviewView; onView: (view: PreviewView) => void }) {
  const deferred = useDeferredValue(input);
  const theme = usePreviewTheme(deferred, view.dark);
  const tabs = useMemo(() => planTabs(deferred.features, deferred.modules).tabs, [deferred.features, deferred.modules]);
  const screens = useMemo(() => screensFor(deferred.features, deferred.modules), [deferred.features, deferred.modules]);
  const screenName = useId();
  const screen: PreviewTab | undefined =
    screens.find((candidate) => candidate.key === view.screen) ?? screens.find((candidate) => candidate.screen !== "signin") ?? screens[0];
  const choose = useCallback((key: string) => onView({ ...view, screen: key }), [view, onView]);
  const weights = useMemo(() => weightsFor(deferred.tokens, theme.style.voice), [deferred.tokens, theme.style.voice]);
  useFamily(deferred.font, weights);
  const [fitRef, scale] = useFitScale();
  const options = useMemo(() => ({ features: deferred.features, haptics: deferred.haptics }), [deferred.features, deferred.haptics]);

  return (
    // self-start: a grid item stretches to its row by default, and a sticky box as tall as its
    // container has no room to move.
    <div className="min-w-0 lg:sticky lg:top-24 lg:self-start">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div role="radiogroup" aria-label="Screen" className="flex flex-wrap gap-1">
          {screens.map((option) => {
            const on = option.key === screen?.key;
            return (
              <label
                key={option.key}
                className={`shrink-0 cursor-pointer rounded-full px-2.5 py-1 text-xs font-medium transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent ${
                  on ? "bg-ink-100 text-ink-950" : "text-ink-400 hover:bg-ink-800 hover:text-ink-100"
                }`}
              >
                <input type="radio" name={screenName} checked={on} onChange={() => choose(option.key)} className="sr-only" />
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
          value={view.dark ? "dark" : "light"}
          onChange={(next) => onView({ ...view, dark: next === "dark" })}
        />
      </div>
      <div ref={fitRef} className="flex justify-center">
        {screen && (
          <div style={{ width: PHONE_WIDTH * scale, height: PHONE_WIDTH * PHONE_ASPECT * scale }}>
            <div style={{ transform: `scale(${scale})`, transformOrigin: "top left", width: PHONE_WIDTH }}>
              <AppPreview theme={theme} font={deferred.font} tabs={tabs} screen={screen} options={options} live onSelectTab={choose} />
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
