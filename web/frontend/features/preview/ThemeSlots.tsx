"use client";

import { memo, useDeferredValue, useMemo } from "react";
import { Segmented } from "../../components/primitives.tsx";
import { PHONE_ASPECT, PHONE_WIDTH, StatusBar } from "./AppPreview.tsx";
import { useFamily } from "./fonts.ts";
import type { PreviewView } from "./LivePreview.tsx";
import { type PreviewTheme, dp, weightsFor } from "./theme.ts";
import { type PreviewInput, usePreviewTheme } from "./usePreviewTheme.ts";

// What a project without UI components looks like: its theme's slots, filled with the visitor's
// colours and font and the neutral values, and the blank start screen the app opens on. There is
// no app to draw beyond that, so the preview does not pretend there is.

const SWATCHES: [role: string, label: string][] = [
  ["background", "background"],
  ["surface", "surface"],
  ["contentPrimary", "contentPrimary"],
  ["contentSecondary", "contentSecondary"],
  ["accent", "accent"],
  ["onAccent", "onAccent"],
  ["secondary", "secondary"],
  ["tertiary", "tertiary"],
];

const TYPE_ROLES: [role: string, sample: string][] = [
  ["largeTitle", "Large title"],
  ["title1", "Title 1"],
  ["title2", "Title 2"],
  ["headline", "Headline"],
  ["body", "Body text reads in this"],
  ["callout", "Callout"],
  ["footnote", "Footnote"],
  ["caption1", "Caption"],
];

/** The blank start screen, in the theme's background, at `scale` of the full-size phone. */
export const StartScreenPreview = memo(function StartScreenPreview({ theme, scale }: { theme: PreviewTheme; scale: number }) {
  const background = theme.colour("background");
  return (
    <div style={{ width: PHONE_WIDTH * scale, height: PHONE_WIDTH * PHONE_ASPECT * scale }} className="shrink-0">
      <div
        aria-hidden
        className="relative overflow-hidden rounded-[34px] border-[5px] shadow-[0_24px_60px_-20px_rgba(40,30,15,0.45)]"
        style={{
          width: PHONE_WIDTH,
          height: PHONE_WIDTH * PHONE_ASPECT,
          transform: `scale(${scale})`,
          transformOrigin: "top left",
          borderColor: theme.dark ? "#2b2926" : "#1f1d1a",
          background,
        }}
      >
        <StatusBar colour={theme.colour("contentSecondary")} background={background} />
        <span
          className="absolute left-1/2 -translate-x-1/2 rounded-full"
          style={{ bottom: dp(6), width: dp(108), height: dp(4), background: theme.colour("contentPrimary"), opacity: 0.85 }}
        />
      </div>
    </div>
  );
});

/** Beside the Look and Motion steps in place of the app, while the box is ticked. */
export function ThemeSlotsPreview({ input, view, onView }: { input: PreviewInput; view: PreviewView; onView: (view: PreviewView) => void }) {
  const deferred = useDeferredValue(input);
  const theme = usePreviewTheme(deferred, view.dark);
  const weights = useMemo(() => weightsFor(deferred.tokens, theme.style.voice), [deferred.tokens, theme.style.voice]);
  useFamily(deferred.font, weights);
  const family = `"${deferred.font}", ui-sans-serif, system-ui, sans-serif`;

  return (
    <div className="min-w-0 lg:sticky lg:top-24 lg:self-start">
      <div className="mb-3 flex items-center justify-between gap-2">
        <p className="text-xs font-medium text-ink-300">Your theme&apos;s slots</p>
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
      <div className="flex gap-4">
        <StartScreenPreview theme={theme} scale={0.48} />
        <ul className="min-w-0 flex-1 space-y-1.5">
          {SWATCHES.map(([role, label]) => (
            <li key={role} className="flex items-center gap-2">
              <span aria-hidden className="h-5 w-5 shrink-0 rounded border border-ink-600" style={{ background: theme.colour(role) }} />
              <span className="truncate font-mono text-[11px] text-ink-300">{label}</span>
            </li>
          ))}
        </ul>
      </div>
      <div
        aria-hidden
        className="mt-4 space-y-1 overflow-hidden rounded-xl border border-ink-700 p-4"
        style={{ background: theme.colour("background"), color: theme.colour("contentPrimary"), fontFamily: family }}
      >
        {TYPE_ROLES.map(([role, sample]) => (
          <p key={role} className="truncate" style={theme.type(role)}>
            {sample}
          </p>
        ))}
      </div>
      <p className="mt-3 text-center text-xs leading-relaxed text-ink-400">
        No components to draw: the theme holds your colours and typeface with neutral shapes and motion, and the app opens on a
        blank start screen.
      </p>
    </div>
  );
}
