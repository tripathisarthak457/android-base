import type { ArgbHex, Roles, SpringName, StyleTokens, Tokens, TypeRole } from "../catalogue/types.ts";
import { type BrandName, type Rgb, brandColours, brandRamp, parseHex, readableShade, toHex } from "./brand.ts";

// What a preview draws with: the catalogue's resolved roles for one design style and theme, with
// the brand roles recomputed from the visitor's colours, the style's shapes, the type scale in its
// voice, and the motion style's springs.

/** A 360 dp phone drawn 280 px wide. */
const DP = 280 / 360;

export const dp = (value: number) => value * DP;

export type TextStyle = { fontSize: number; lineHeight: string; fontWeight: number; letterSpacing: string };

export type FramerSpring = { type: "spring"; stiffness: number; damping: number; mass: 1 };

export type PreviewTheme = {
  dark: boolean;
  colour: (role: string) => string;
  status: (name: string) => { base: string; content: string; subtle: string; on: string };
  style: StyleTokens;
  type: (role: string) => TextStyle;
  spring: (name: SpringName) => FramerSpring;
};

export type PreviewChoices = {
  designStyle: string;
  motionStyle: string;
  dark: boolean;
  accent: string;
  secondary: string;
  tertiary: string;
};

/** `#AARRGGBB` → CSS, which writes the alpha last. */
export function css(argb: ArgbHex): string {
  const alpha = argb.slice(1, 3).toLowerCase();
  const rgb = argb.slice(3).toLowerCase();
  return alpha === "ff" ? `#${rgb}` : `#${rgb}${alpha}`;
}

function rgbOf(argb: ArgbHex | undefined): Rgb | null {
  return argb ? parseHex(argb.slice(3)) : null;
}

const FALLBACK_TEXT: TypeRole = { size: 17, lineHeight: 22, tracking: 0, weight: {} };

function brandRoles(roles: Roles, choices: PreviewChoices): Record<string, string> {
  const accent = parseHex(choices.accent);
  if (!accent) return {};
  const colours = brandColours(accent, parseHex(choices.secondary), parseHex(choices.tertiary));
  const background = rgbOf(typeof roles.background === "string" ? roles.background : undefined);
  const surface = rgbOf(typeof roles.surface === "string" ? roles.surface : undefined);
  const out: Record<string, string> = {};
  for (const name of Object.keys(colours) as BrandName[]) {
    const ramp = brandRamp(colours[name]);
    const shade = choices.dark ? ramp.dark : ramp.base;
    const on = name === "accent" ? "onAccent" : name === "secondary" ? "onSecondary" : "onTertiary";
    out[name] = toHex(shade);
    out[`${name}Pressed`] = toHex(choices.dark ? ramp.darkPressed : ramp.pressed);
    out[`${name}Subtle`] = toHex(choices.dark ? ramp.subtleDark : ramp.subtleLight);
    out[on] = toHex(choices.dark ? ramp.onDark : ramp.onLight);
    if (background && surface) out[`${name}Content`] = toHex(readableShade(readableShade(shade, background), surface));
  }
  return out;
}

export function previewTheme(tokens: Tokens, choices: PreviewChoices): PreviewTheme {
  const styleKey = choices.designStyle in tokens.styles ? choices.designStyle : String(tokens.defaults.designStyle);
  const style = tokens.styles[styleKey] ?? Object.values(tokens.styles)[0]!;
  const palette = tokens.palette[styleKey] ?? Object.values(tokens.palette)[0]!;
  const roles = choices.dark ? palette.dark : palette.light;
  const brand = brandRoles(roles, choices);
  const motion = tokens.motion.styles[choices.motionStyle] ?? tokens.motion.styles[String(tokens.defaults.motionStyle)];

  const colour = (role: string): string => {
    const own = brand[role];
    if (own) return own;
    const value = roles[role];
    return typeof value === "string" ? css(value) : "transparent";
  };

  const status = (name: string) => {
    const value = roles[name];
    if (typeof value !== "object") {
      return { base: colour("contentSecondary"), content: colour("contentSecondary"), subtle: colour("fill"), on: colour("surface") };
    }
    return { base: css(value.base), content: css(value.content), subtle: css(value.subtle), on: css(value.on) };
  };

  const type = (name: string): TextStyle => {
    const role = tokens.type.roles[name] ?? FALLBACK_TEXT;
    return {
      fontSize: dp(role.size),
      lineHeight: `${dp(role.lineHeight)}px`,
      fontWeight: role.weight[style.voice] ?? role.weight.Neutral ?? 400,
      letterSpacing: `${dp(role.tracking)}px`,
    };
  };

  const spring = (name: SpringName): FramerSpring => {
    const value = motion?.springs[name] ?? { stiffness: 685, dampingRatio: 1 };
    return { type: "spring", stiffness: value.stiffness, damping: value.dampingRatio * 2 * Math.sqrt(value.stiffness), mass: 1 };
  };

  return { dark: choices.dark, colour, status, style, type, spring };
}

/** A corner radius in preview pixels; "pill" is half the height, which 999 always reaches. */
export function radius(value: number | "pill"): number {
  return value === "pill" ? 999 : dp(value);
}

/** The font weights a preview draws in one voice, so only those are requested from Google Fonts. */
export function weightsFor(tokens: Tokens, voice: string): number[] {
  const weights = new Set<number>();
  for (const role of Object.values(tokens.type.roles)) weights.add(role.weight[voice] ?? role.weight.Neutral ?? 400);
  return [...weights].sort((a, b) => a - b);
}
