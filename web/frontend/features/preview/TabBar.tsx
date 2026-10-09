"use client";

import { motion } from "framer-motion";
import type { CSSProperties } from "react";
import { Icon } from "./icons.tsx";
import { useTheme } from "./parts.tsx";
import type { PreviewTab } from "./tabs.ts";
import { dp } from "./theme.ts";

/** Gesture navigation's inset, which every bar sits above. */
export const NAV_INSET = 16;
const BAR_HEIGHT = 64;
const LIFT = 12;
const SIDE_MARGIN = 20;

/** Treatments drawn as a capsule over the content; the rest are attached to the bottom edge. */
const ATTACHED = new Set(["Docked", "Minimal", "Chunky"]);

export function isFloating(bar: string): boolean {
  return !ATTACHED.has(bar);
}

/** How far up the screen the bar reaches, which is how much the scroll content is padded. */
export function barReach(bar: string): number {
  return isFloating(bar) ? NAV_INSET + LIFT + BAR_HEIGHT : NAV_INSET + BAR_HEIGHT;
}

/**
 * Glass is real blur only in the one live preview. A thumbnail draws it (tinted body, rim and
 * sheen, nothing sampled), as the template does for content glass: nine stacked backdrop filters
 * inside scaled phones would drop the page's frames.
 */
function capsule(bar: string, colour: (role: string) => string, dark: boolean, live: boolean): CSSProperties {
  const shadow = dark ? "0 8px 24px rgba(0,0,0,0.45)" : "0 8px 24px rgba(11,15,26,0.14)";
  switch (bar) {
    case "Translucent":
      return { background: `color-mix(in srgb, ${colour("surface")} 88%, transparent)`, border: `1px solid ${colour("separator")}`, boxShadow: shadow };
    case "Glass": {
      const rim = `0.75px solid color-mix(in srgb, white ${dark ? 18 : 60}%, transparent)`;
      const sheen = `inset 0 1px 0 rgba(255,255,255,${dark ? 0.12 : 0.5})`;
      return live
        ? {
            background: `color-mix(in srgb, ${colour("surface")} ${dark ? 38 : 55}%, transparent)`,
            backdropFilter: `blur(${dp(10)}px) saturate(1.6)`,
            WebkitBackdropFilter: `blur(${dp(10)}px) saturate(1.6)`,
            border: rim,
            boxShadow: `${sheen}, ${shadow}`,
          }
        : {
            background: `linear-gradient(180deg, color-mix(in srgb, ${colour("surface")} ${dark ? 70 : 86}%, ${colour("accent")}) 0%, ${colour("surface")} 100%)`,
            border: rim,
            boxShadow: `${sheen}, ${shadow}`,
          };
    }
    default:
      return { background: colour("surfaceElevated"), border: `1px solid ${colour("separator")}`, boxShadow: shadow };
  }
}

export function TabBar({
  tabs,
  selected,
  onSelect,
  live,
}: {
  tabs: PreviewTab[];
  selected: number;
  /** Absent in a thumbnail, which may sit inside a control of its own. */
  onSelect?: (key: string) => void;
  live: boolean;
}) {
  const t = useTheme();
  const { bar, borderWidth, uppercaseLabels } = t.style;
  const floating = isFloating(bar);
  const spring = t.spring("selection");
  const count = Math.max(tabs.length, 1);
  const pad = floating ? dp(6) : 0;

  const item = (tab: PreviewTab, index: number) => {
    const on = index === selected;
    const chunky = bar === "Chunky";
    const tint = chunky && on ? t.colour("onAccent") : on ? t.colour(bar === "Minimal" ? "contentPrimary" : "accentContent") : t.colour("contentSecondary");
    const labelTint = on ? t.colour(chunky || bar === "Minimal" ? "contentPrimary" : "accentContent") : t.colour("contentSecondary");
    const Element = onSelect ? "button" : "div";
    return (
      <Element
        key={tab.key}
        {...(onSelect ? { type: "button" as const, onClick: () => onSelect(tab.key), "aria-pressed": on } : {})}
        className="relative z-10 flex min-w-0 flex-1 flex-col items-center justify-center"
        style={{ gap: dp(3), height: "100%", padding: `0 ${dp(4)}px` }}
      >
        <span
          style={{
            display: "inline-flex",
            padding: chunky ? `${dp(3)}px ${dp(14)}px` : 0,
            borderRadius: 999,
            background: chunky && on ? t.colour("accent") : "transparent",
          }}
        >
          <Icon name={tab.icon} size={dp(24)} colour={tint} />
        </span>
        <span
          style={{
            ...t.type("caption2"),
            color: labelTint,
            textTransform: uppercaseLabels ? "uppercase" : undefined,
            letterSpacing: uppercaseLabels ? "0.08em" : t.type("caption2").letterSpacing,
            display: "block",
            maxWidth: "100%",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {tab.label}
        </span>
      </Element>
    );
  };

  if (floating) {
    return (
      <div className="absolute" style={{ left: dp(SIDE_MARGIN), right: dp(SIDE_MARGIN), bottom: dp(NAV_INSET + LIFT), height: dp(BAR_HEIGHT) }}>
        <div className="relative flex h-full" style={{ borderRadius: 999, padding: pad, ...capsule(bar, t.colour, t.dark, live) }}>
          {/* The indicator stays clear of the capsule's edges, so the selected label sits inside it. */}
          <motion.span
            aria-hidden
            className="absolute"
            style={{ top: dp(8), bottom: dp(8), left: pad, width: `calc((100% - ${2 * pad}px) / ${count})`, borderRadius: 999, background: t.colour("accentSubtle") }}
            initial={false}
            animate={{ x: `${selected * 100}%` }}
            transition={spring}
          />
          {tabs.map(item)}
        </div>
      </div>
    );
  }

  return (
    <div
      className="absolute inset-x-0 bottom-0"
      style={{
        height: dp(BAR_HEIGHT + NAV_INSET),
        paddingBottom: dp(NAV_INSET),
        background: t.colour("surface"),
        borderTop: bar === "Chunky" ? `${borderWidth}px solid ${t.colour("contentPrimary")}` : `1px solid ${t.colour("separator")}`,
      }}
    >
      <div className="relative flex h-full">
        {bar === "Minimal" && (
          <motion.span
            aria-hidden
            className="absolute top-0"
            style={{ left: 0, width: `${100 / count}%`, height: 2, display: "flex", justifyContent: "center" }}
            initial={false}
            animate={{ x: `${selected * 100}%` }}
            transition={spring}
          >
            <span style={{ width: dp(20), height: 2, background: t.colour("contentPrimary") }} />
          </motion.span>
        )}
        {tabs.map(item)}
      </div>
    </div>
  );
}
