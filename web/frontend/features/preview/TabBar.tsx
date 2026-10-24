"use client";

import { type MotionValue, animate, motion, motionValue, useMotionValue, useTransform } from "framer-motion";
import { type CSSProperties, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import {
  BAR,
  type BarLook,
  type PillBox,
  type RestingPills,
  type RowMeasure,
  SHELL_SPRINGS,
  barLook,
  isFloating,
  pillBox,
  restingPills,
} from "./bar.ts";
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

/** The pill, and the row it sits in, in preview px. */
type Capsule = PillBox & { width: number; height: number };

/** The pill as a path the browser fills and clips by, with the row around it when `outside`. */
function capsulePath({ left, right, top, bottom, width, height }: Capsule, outside: boolean): string {
  if (right <= left) return outside ? "none" : "inset(50%)";
  const r = Math.min(right - left, bottom - top) / 2;
  const pill = `M${left + r} ${top}H${right - r}A${r} ${r} 0 0 1 ${right - r} ${bottom}H${left + r}A${r} ${r} 0 0 1 ${left + r} ${top}Z`;
  return outside ? `path(evenodd, "M0 0H${width}V${height}H0Z${pill}")` : `path("${pill}")`;
}

/** A capsule's body with its shadow, and its edge: the hairline, and anything drawn just inside it. */
type CapsuleSurface = { fill: CSSProperties; edge: CSSProperties };

const CAPSULE_SHADOW = `0 ${dp(4)}px ${dp(16)}px rgba(0, 0, 0, 0.10)`;

/**
 * A thumbnail draws the Glass capsule (tinted body, rim and sheen, nothing sampled), as the
 * template draws content glass: a backdrop filter in every scaled phone on the page would drop its
 * frames. Only the one live preview blurs.
 */
function glass(colour: (role: string) => string, dark: boolean, live: boolean): CapsuleSurface {
  const edge = {
    border: `0.75px solid color-mix(in srgb, white ${dark ? 18 : 60}%, transparent)`,
    boxShadow: `inset 0 1px 0 rgba(255,255,255,${dark ? 0.12 : 0.5})`,
  };
  const boxShadow = dark ? undefined : CAPSULE_SHADOW;
  return live
    ? {
        fill: {
          background: `color-mix(in srgb, ${colour("surface")} ${dark ? 38 : 55}%, transparent)`,
          backdropFilter: `blur(${dp(10)}px) saturate(1.6)`,
          WebkitBackdropFilter: `blur(${dp(10)}px) saturate(1.6)`,
          boxShadow,
        },
        edge,
      }
    : {
        fill: {
          background: `linear-gradient(180deg, color-mix(in srgb, ${colour("surface")} ${dark ? 70 : 86}%, ${colour("accent")}) 0%, ${colour("surface")} 100%)`,
          boxShadow,
        },
        edge,
      };
}

function capsuleSurface(look: BarLook): CapsuleSurface {
  return {
    fill: { background: look.surface, boxShadow: look.shadow ? CAPSULE_SHADOW : undefined },
    edge: { border: `${look.borderWidth}px solid ${look.border}` },
  };
}

/** The row's laid-out size, unrounded (clientWidth rounds) and before any scale the phone is drawn at. */
function laidOut(element: HTMLElement): { width: number; height: number } {
  const box = getComputedStyle(element);
  return { width: Number.parseFloat(box.width) || element.clientWidth, height: Number.parseFloat(box.height) || element.clientHeight };
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
  // on the row's size at the template's margins; the labels' widths replace that on mount.
  const estimate = (): RowMeasure => ({
    width: dp(360 - 2 * (look.floating ? BAR.margin : BAR.dockedInset)),
    height: dp(BAR.height),
    contents: tabs.map(() => dp(ICON)),
  });
  const measure = useMotionValue<RowMeasure>(estimate());
  const start = useMotionValue(selected);
  const end = useMotionValue(selected);
  const forward = useRef(true);
  const scales = useMemo(() => tabs.map(() => motionValue(1)), [tabs.length]);
  // A label too long for its slot is set smaller before it is cut, as UIKit's tab bar does.
  const [shrink, setShrink] = useState<number[]>([]);
  const shrinkRef = useRef<number[]>([]);

  // The resting pills change only when the row is measured; the pill is worked out once a frame
  // and both clip paths are drawn from it.
  const resting = useTransform(measure, restingPills);
  const capsule = useTransform([start, end, measure, resting] as MotionValue[], ([from, to, m, pills]): Capsule => {
    const rowSize = m as RowMeasure;
    const box = pillBox(rowSize, pills as RestingPills, from as number, to as number, forward.current);
    return { ...box, width: rowSize.width, height: rowSize.height };
  });
  const inside = useTransform(capsule, (shape) => capsulePath(shape, false));
  const outside = useTransform(capsule, (shape) => capsulePath(shape, true));

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
        ...laidOut(element),
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
    const surface = bar === "Glass" ? glass(t.colour, t.dark, live) : capsuleSurface(look);
    // The hairline is drawn over the row rather than as the capsule's border, as Compose's border
    // modifier draws it, so the row keeps the bar's whole height and width and the cells and the
    // pill sit where TabRow puts them.
    return (
      <div className="absolute" style={{ left: dp(BAR.margin), right: dp(BAR.margin), bottom: dp(NAV_INSET + BAR.lift), height: dp(BAR.height) }}>
        <div className="relative h-full overflow-hidden" style={{ borderRadius: 999, ...surface.fill }}>
          {tabRow}
          <div aria-hidden className="pointer-events-none absolute inset-0" style={{ borderRadius: "inherit", ...surface.edge }} />
        </div>
      </div>
    );
  }

  // No built-in style docks its bar, so the surface and its rule are drawn whole rather than in
  // step with how far content runs under the bar, as AppTabBar's scrolledUnder draws them.
  return (
    <div
      className="absolute inset-x-0 bottom-0"
      style={{
        height: dp(BAR.height + NAV_INSET),
        padding: `0 ${dp(BAR.dockedInset)}px ${dp(NAV_INSET)}px`,
        background: look.surface,
        // A shadow, not a border, so the rule takes nothing from the row, as drawBehind does.
        boxShadow: `inset 0 ${look.borderWidth}px 0 ${look.border}`,
      }}
    >
      {tabRow}
    </div>
  );
}
