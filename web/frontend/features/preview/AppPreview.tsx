"use client";

import { memo } from "react";
import { ThemeContext } from "./parts.tsx";
import { type ScreenOptions, ScreenBody } from "./screens.tsx";
import { NAV_INSET, TabBar, barReach, isFloating } from "./TabBar.tsx";
import type { PreviewTab } from "./tabs.ts";
import { type PreviewTheme, dp } from "./theme.ts";

export const PHONE_WIDTH = 280;
export const PHONE_ASPECT = 2.05;

export type AppPreviewProps = {
  theme: PreviewTheme;
  font: string;
  tabs: PreviewTab[];
  screen: PreviewTab;
  options: ScreenOptions;
  /** The one interactive preview: real glass blur, a scrollable screen, tappable tabs. */
  live?: boolean;
  onSelectTab?: (key: string) => void;
};

/**
 * The generated app, drawn in the browser. As in the template's shell, the screen runs the full
 * height of the window and the bar floats over it: only the scroll content is padded, so nothing
 * opaque sits between the bar and the bottom edge, and a fade softens the content under the bar.
 */
export const AppPreview = memo(function AppPreview({ theme, font, tabs, screen, options, live = false, onSelectTab }: AppPreviewProps) {
  const selected = tabs.findIndex((tab) => tab.key === screen.key);
  const showBar = screen.screen !== "signin" && tabs.length > 0;
  const bar = theme.style.bar;
  const background = theme.colour("background");

  return (
    <ThemeContext.Provider value={theme}>
      <div
        className="relative shrink-0 overflow-hidden rounded-[34px] border-[5px] shadow-[0_24px_60px_-20px_rgba(40,30,15,0.45)]"
        style={{
          width: PHONE_WIDTH,
          height: PHONE_WIDTH * PHONE_ASPECT,
          borderColor: theme.dark ? "#2b2926" : "#1f1d1a",
          background,
          color: theme.colour("contentPrimary"),
          fontFamily: `"${font}", ui-sans-serif, system-ui, sans-serif`,
        }}
      >
        <div
          className={`no-scrollbar absolute inset-0 ${live ? "overflow-y-auto overscroll-contain" : "overflow-hidden"}`}
          style={{ paddingTop: dp(28), paddingBottom: showBar ? dp(barReach(bar) + 12) : dp(NAV_INSET + 12) }}
        >
          <ScreenBody tab={screen} options={options} />
        </div>

        <StatusBar colour={theme.colour("contentSecondary")} background={background} />

        {showBar && isFloating(bar) && (
          <div
            aria-hidden
            className="pointer-events-none absolute inset-x-0 bottom-0"
            style={{
              height: dp(barReach(bar) + 20),
              background: `linear-gradient(to top, ${background} 35%, color-mix(in srgb, ${background} 0%, transparent))`,
            }}
          />
        )}
        {showBar && <TabBar tabs={tabs} selected={Math.max(selected, 0)} onSelect={onSelectTab} live={live} />}
        <span
          aria-hidden
          className="absolute left-1/2 -translate-x-1/2 rounded-full"
          style={{ bottom: dp(6), width: dp(108), height: dp(4), background: theme.colour("contentPrimary"), opacity: 0.85 }}
        />
      </div>
    </ThemeContext.Provider>
  );
});

function StatusBar({ colour, background }: { colour: string; background: string }) {
  return (
    <div
      aria-hidden
      className="absolute inset-x-0 top-0 flex items-center justify-between font-mono"
      style={{ height: dp(28), padding: `0 ${dp(20)}px`, fontSize: dp(12), color: colour, background }}
    >
      <span>9:41</span>
      <span className="inline-block rounded-[2px]" style={{ width: dp(14), height: dp(8), background: colour }} />
    </div>
  );
}

/** A phone drawn at full size and scaled down, so a thumbnail keeps the real proportions. */
export function ScaledPreview({ scale, ...props }: AppPreviewProps & { scale: number }) {
  return (
    <div style={{ width: PHONE_WIDTH * scale, height: PHONE_WIDTH * PHONE_ASPECT * scale }} className="shrink-0" aria-hidden>
      <div style={{ transform: `scale(${scale})`, transformOrigin: "top left", width: PHONE_WIDTH }}>
        <AppPreview {...props} />
      </div>
    </div>
  );
}
