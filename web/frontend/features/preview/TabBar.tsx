"use client";

import { type MotionValue, animate, motion, motionValue, useMotionValue, useTransform } from "framer-motion";
import { type CSSProperties, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { BAR, type BarLook, SHELL_SPRINGS, barLook, isFloating, pillCross, pillSpan, pillWidth } from "./bar.ts";
import { Icon } from "./icons.tsx";
import { useTheme } from "./parts.tsx";
import type { PreviewTab } from "./tabs.ts";
import { dp, framerSpring } from "./theme.ts";

export { isFloating };

/** Gesture navigation's inset, which every bar sits above. */
export const NAV_INSET = 16;
const ICON = 24;

/** How far up the screen the bar reaches, which is how much the scroll content is padded. */
export function barReach(bar: string): number {
  return isFloating(bar) ? NAV_INSET + BAR.lift + BAR.height : NAV_INSET + BAR.height;
}

const LEAD = { ...framerSpring(SHELL_SPRINGS.indicatorLead), restDelta: 0.001 };
const TRAIL = { ...framerSpring(SHELL_SPRINGS.indicatorTrail), restDelta: 0.001 };
const DIP = { ...framerSpring(SHELL_SPRINGS.iconBounceDown), restDelta: 0.001 };
const RISE = { ...framerSpring(SHELL_SPRINGS.iconBounceUp), restDelta: 0.001 };

/** The row's width and each tab's content width (its label, or its glyph without one), in preview px. */
type Measure = { width: number; contents: number[] };

/** Where the pill rests in each slot, along the row, in preview px. */
function restingPills({ width, contents }: Measure): { starts: number[]; ends: number[] } {
  const unit = dp(1);
  const slot = width / Math.max(contents.length, 1);
  const starts: number[] = [];
  const ends: number[] = [];
  contents.forEach((content, index) => {
    const pill = pillWidth(slot / unit, content / unit) * unit;
    starts.push(index * slot + (slot - pill) / 2);
    ends.push(index * slot + (slot + pill) / 2);
  });
  return { starts, ends };
}

function at(values: number[], position: number): number {
  if (values.length === 0) return 0;
  const p = Math.min(Math.max(position, 0), values.length - 1);
  const low = Math.floor(p);
  const high = Math.ceil(p);
  return (values[low] ?? 0) + ((values[high] ?? 0) - (values[low] ?? 0)) * (p - low);
}

type Capsule = { left: number; right: number; top: number; bottom: number; width: number; height: number };

function capsuleAt(measure: Measure, start: number, end: number, forward: boolean): Capsule {
  const height = dp(BAR.height);
  const pills = restingPills(measure);
  const [from, to] = pillSpan(start, end, forward);
  const left = at(pills.starts, from);
  const right = at(pills.ends, to);
  const rest = at(pills.ends, (from + to) / 2) - at(pills.starts, (from + to) / 2);
  const inset = (height - dp(BAR.pillHeight)) / 2;
  const [top, bottom] = pillCross(inset, height - inset, rest > 0 ? (right - left) / rest : 1);
  return { left, right, top, bottom, width: measure.width, height };
}

/** The pill as a path the browser fills and clips by, with the row around it when `outside`. */
function capsulePath({ left, right, top, bottom, width, height }: Capsule, outside: boolean): string {
  if (right <= left) return outside ? "none" : "inset(50%)";
  const r = Math.min(right - left, bottom - top) / 2;
  const pill = `M${left + r} ${top}H${right - r}A${r} ${r} 0 0 1 ${right - r} ${bottom}H${left + r}A${r} ${r} 0 0 1 ${left + r} ${top}Z`;
  return outside ? `path(evenodd, "M0 0H${width}V${height}H0Z${pill}")` : `path("${pill}")`;
}

/**
 * A thumbnail draws the Glass capsule (tinted body, rim and sheen, nothing sampled), as the
 * template draws content glass: a backdrop filter in every scaled phone on the page would drop its
 * frames. Only the one live preview blurs.
 */
function glass(colour: (role: string) => string, dark: boolean, live: boolean): CSSProperties {
  const shadow = dark ? "none" : `0 ${dp(4)}px ${dp(16)}px rgba(0, 0, 0, 0.10)`;
  const rim = `0.75px solid color-mix(in srgb, white ${dark ? 18 : 60}%, transparent)`;
  const sheen = `inset 0 1px 0 rgba(255,255,255,${dark ? 0.12 : 0.5})`;
  return live
    ? {
        background: `color-mix(in srgb, ${colour("surface")} ${dark ? 38 : 55}%, transparent)`,
        backdropFilter: `blur(${dp(10)}px) saturate(1.6)`,
        WebkitBackdropFilter: `blur(${dp(10)}px) saturate(1.6)`,
        border: rim,
        boxShadow: shadow === "none" ? sheen : `${sheen}, ${shadow}`,
      }
    : {
        background: `linear-gradient(180deg, color-mix(in srgb, ${colour("surface")} ${dark ? 70 : 86}%, ${colour("accent")}) 0%, ${colour("surface")} 100%)`,
        border: rim,
        boxShadow: shadow === "none" ? sheen : `${sheen}, ${shadow}`,
      };
}

function capsuleStyle(look: BarLook): CSSProperties {
  return {
    background: look.surface,
    border: `${look.borderWidth}px solid ${look.border}`,
    boxShadow: look.shadow ? `0 ${dp(4)}px ${dp(16)}px rgba(0, 0, 0, 0.10)` : undefined,
  };
}

/**
 * The template's tab bar: a floating capsule, or a bar docked to the bottom edge. The tabs are
 * drawn twice, as `TabCells.kt` draws them: in the resting colour outside the selection pill and
 * in the selected colour inside it, so a tab the pill is half over is half selected. The pill's
 * leading edge runs ahead of its trailing one, so it stretches toward the new tab and settles, and
 * a tapped tab's glyph dips and springs back. Only clip paths and scales change while it moves.
 */
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
  const { bar } = t.style;
  const look = barLook(t.style, t.colour, t.dark);
  const row = useRef<HTMLDivElement>(null);
  const labels = useRef<(HTMLSpanElement | null)[]>([]);

  // Until the row is measured (and in the prerendered page) the pills are sized to their glyphs
  // on the row's width at the template's margins; the labels' widths replace that on mount.
  const estimate = (): Measure => ({
    width: dp(360 - 2 * (look.floating ? BAR.margin : BAR.dockedInset)),
    contents: tabs.map(() => dp(ICON)),
  });
  const measure = useMotionValue<Measure>(estimate());
  const start = useMotionValue(selected);
  const end = useMotionValue(selected);
  const forward = useRef(true);
  const scales = useMemo(() => tabs.map(() => motionValue(1)), [tabs.length]);
  // A label too long for its slot is set smaller before it is cut, as UIKit's tab bar does.
  const [shrink, setShrink] = useState<number[]>([]);
  const shrinkRef = useRef<number[]>([]);

  const inside = useTransform([start, end, measure] as MotionValue[], ([from, to, m]) =>
    capsulePath(capsuleAt(m as Measure, from as number, to as number, forward.current), false),
  );
  const outside = useTransform([start, end, measure] as MotionValue[], ([from, to, m]) =>
    capsulePath(capsuleAt(m as Measure, from as number, to as number, forward.current), true),
  );

  useLayoutEffect(() => {
    const element = row.current;
    if (!element) return;
    const read = () => {
      const next = tabs.map((_, index) => {
        const label = labels.current[index];
        const cell = label?.parentElement;
        if (!label || !cell) return 1;
        const natural = label.scrollWidth / (shrinkRef.current[index] ?? 1);
        const room = cell.clientWidth - 2 * dp(BAR.slotGap + BAR.labelPaddingMin);
        return natural > room ? Math.max(BAR.minLabelShrink, room / natural) : 1;
      });
      if (next.some((value, index) => Math.abs(value - (shrinkRef.current[index] ?? 1)) > 0.01)) {
        shrinkRef.current = next;
        setShrink(next);
      }
      measure.set({
        width: element.clientWidth,
        contents: tabs.map((_, index) => Math.max(labels.current[index]?.offsetWidth ?? 0, dp(ICON))),
      });
    };
    read();
    // The typeface arrives after the first paint and changes every label's width.
    const observer = new ResizeObserver(read);
    observer.observe(element);
    for (const label of labels.current) if (label) observer.observe(label);
    return () => observer.disconnect();
  }, [measure, tabs]);

  useEffect(() => {
    forward.current = selected >= start.get();
    const lead = animate(forward.current ? end : start, selected, LEAD);
    const trail = animate(forward.current ? start : end, selected, TRAIL);
    return () => {
      lead.stop();
      trail.stop();
    };
  }, [selected, start, end]);

  const tap = (index: number, key: string) => {
    const scale = scales[index];
    if (scale && index !== selected && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      void animate(scale, BAR.bounceScale, DIP).then(() => animate(scale, 1, RISE));
    }
    onSelect?.(key);
  };

  const cells = (layer: "base" | "selected") => (
    <div className="pointer-events-none absolute inset-0 flex">
      {tabs.map((tab, index) => {
        const tint = layer === "base" ? look.content : look.selectedContent;
        return (
          <div
            key={tab.key}
            className="flex min-w-0 flex-1 flex-col items-center justify-center"
            style={{ gap: dp(2), padding: `0 ${dp(BAR.slotGap + BAR.labelPaddingMin)}px` }}
          >
            <motion.span className="inline-flex" style={{ scale: scales[index] }}>
              <Icon name={layer === "selected" ? (tab.selectedIcon ?? tab.icon) : tab.icon} size={dp(ICON)} colour={tint} />
            </motion.span>
            <span
              ref={
                layer === "base"
                  ? (element) => {
                      labels.current[index] = element;
                    }
                  : undefined
              }
              style={{
                ...t.type("caption2"),
                fontSize: t.type("caption2").fontSize * (shrink[index] ?? 1),
                color: tint,
                textTransform: t.style.uppercaseLabels ? "uppercase" : undefined,
                display: "inline-block",
                maxWidth: "100%",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
            >
              {tab.label}
            </span>
          </div>
        );
      })}
    </div>
  );

  const tabRow = (
    <div ref={row} className="relative h-full">
      <motion.div className="absolute inset-0" style={{ background: look.indicator, clipPath: inside }} />
      <motion.div className="absolute inset-0" style={{ clipPath: outside }}>
        {cells("base")}
      </motion.div>
      <motion.div className="absolute inset-0" style={{ clipPath: inside }}>
        {cells("selected")}
      </motion.div>
      {onSelect && (
        <div className="absolute inset-0 flex">
          {tabs.map((tab, index) => (
            <button key={tab.key} type="button" tabIndex={-1} className="min-w-0 flex-1" onClick={() => tap(index, tab.key)} />
          ))}
        </div>
      )}
    </div>
  );

  if (look.floating) {
    return (
      <div className="absolute" style={{ left: dp(BAR.margin), right: dp(BAR.margin), bottom: dp(NAV_INSET + BAR.lift), height: dp(BAR.height) }}>
        <div className="h-full overflow-hidden" style={{ borderRadius: 999, ...(bar === "Glass" ? glass(t.colour, t.dark, live) : capsuleStyle(look)) }}>
          {tabRow}
        </div>
      </div>
    );
  }

  return (
    <div
      className="absolute inset-x-0 bottom-0"
      style={{
        height: dp(BAR.height + NAV_INSET),
        padding: `0 ${dp(BAR.dockedInset)}px ${dp(NAV_INSET)}px`,
        background: look.surface,
        borderTop: `${look.borderWidth}px solid ${look.border}`,
      }}
    >
      {tabRow}
    </div>
  );
}
