import type { StyleTokens } from "../catalogue/types.ts";

// The tab bar as the template draws it (`AppTabBar.kt`, `TabCells.kt`), in dp: what each
// treatment and indicator look like, where the selection pill rests, and the springs it moves on.

export const BAR = {
  height: 64,
  /** A floating capsule's distance from the window's sides, and its lift off the navigation bar. */
  margin: 20,
  lift: 12,
  /** Between a docked bar's outer tabs and the window's edges. */
  dockedInset: 8,
  pillHeight: 44,
  minPill: 52,
  maxPill: 72,
  /** Between a pill and its slot's edge, so neighbouring pills never touch. */
  slotGap: 4,
  /** Inside the pill on each side of the label; it gives way to the minimum in a narrow slot. */
  labelPadding: 10,
  labelPaddingMin: 4,
  /** How far past one slot the pill may stretch while its leading edge runs ahead. */
  maxStretch: 0.5,
  /** How much the pill thins per slot of stretch, so it reads as a gel rather than a bar. */
  squash: 0.06,
  /** How far the selected glyph dips before it springs back. */
  bounceScale: 0.84,
  /** The smallest a label too long for its slot is set, as a fraction of caption2, before it is cut. */
  minLabelShrink: 0.85,
} as const;

/**
 * The template's `BarTreatment`s, plus Glass, the liquid glass capsule WP05 adds with its style
 * (the preview blurs for real only in the one live phone).
 */
export const DRAWN_TREATMENTS: ReadonlySet<string> = new Set(["Floating", "Translucent", "Docked", "Glass"]);

export const DRAWN_INDICATORS: ReadonlySet<string> = new Set(["Pill", "Subtle"]);

type Spring = { stiffness: number; dampingRatio: number };

/**
 * The springs the template's shell moves with whatever the motion style: the push and pop
 * (`ScreenMotion`, a 0.344 s critically damped spring) and the bar's selection and glyph bounce
 * (`TabCells.kt`). The tokens give every motion style its own, but the shell does not read them
 * yet, so the preview shows what the app does.
 */
export const SHELL_SPRINGS: Record<"screen" | "indicatorLead" | "indicatorTrail" | "iconBounceDown" | "iconBounceUp", Spring> = {
  screen: { stiffness: ((2 * Math.PI) / 0.344) ** 2, dampingRatio: 1 },
  indicatorLead: { stiffness: 815.6, dampingRatio: 0.85 },
  indicatorTrail: { stiffness: 385.5, dampingRatio: 1 },
  iconBounceDown: { stiffness: 1500, dampingRatio: 1 },
  iconBounceUp: { stiffness: 500, dampingRatio: 0.45 },
};

export function isFloating(treatment: string): boolean {
  return treatment !== "Docked";
}

export type BarLook = {
  floating: boolean;
  surface: string;
  border: string;
  /** In dp. A docked bar's rule is a hairline whatever the style's border width. */
  borderWidth: number;
  shadow: boolean;
  indicator: string;
  /** Glyphs and labels inside the pill. */
  selectedContent: string;
  /** Glyphs and labels outside it. */
  content: string;
};

/**
 * The template's `barLookFor`. In dark theme a capsule sits on the elevated surface with a faint
 * light outline, since the surface the cards under it share would merge with them, and casts no
 * shadow, which would not show there. `barIndicator` is the accent's alias in every palette.
 */
export function barLook(style: Pick<StyleTokens, "bar" | "indicator" | "borderWidth">, colour: (role: string) => string, dark: boolean): BarLook {
  const floating = isFloating(style.bar);
  const raised = floating && dark;
  const pill = style.indicator === "Pill";
  const surface = colour(raised ? "surfaceElevated" : "surface");
  return {
    floating,
    surface: style.bar === "Translucent" ? `color-mix(in srgb, ${surface} 88%, transparent)` : surface,
    border: raised ? "rgba(255, 255, 255, 0.07)" : colour("separator"),
    borderWidth: floating ? style.borderWidth : 1,
    shadow: floating && !dark,
    indicator: colour(pill ? "accent" : "accentSubtle"),
    selectedContent: colour(pill ? "onAccent" : "accentContent"),
    content: colour("contentSecondary"),
  };
}

/** The pill's width in a slot whose content (its label, or its glyph without one) is `content` wide. */
export function pillWidth(slot: number, content: number): number {
  const fitted = Math.min(Math.max(content + 2 * BAR.labelPadding, BAR.minPill), BAR.maxPill);
  return Math.min(Math.max(fitted, content + 2 * BAR.labelPaddingMin), slot - 2 * BAR.slotGap);
}

/**
 * The span the pill is drawn over, in slots, from its two edges' positions. The edge facing the
 * new tab runs ahead; the trailing one is held to at most `maxStretch` behind it.
 */
export function pillSpan(start: number, end: number, forward: boolean): [from: number, to: number] {
  const from = Math.min(start, end);
  const to = Math.max(start, end);
  if (to - from <= BAR.maxStretch) return [from, to];
  return forward ? [to - BAR.maxStretch, to] : [from, from + BAR.maxStretch];
}

/** The pill's top and bottom while it is `stretch` times its resting length. */
export function pillCross(top: number, bottom: number, stretch: number): [top: number, bottom: number] {
  const thin = ((bottom - top) * BAR.squash * Math.max(stretch - 1, 0)) / 2;
  return [top + thin, bottom - thin];
}
