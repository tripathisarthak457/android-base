/**
 * The generated app's look, recomputed in the browser for the previews.
 *
 * Every number here is copied from the template: the neutrals from `Palette.kt`, the ramp maths
 * from `render.py`'s `apply_accent`, the style tokens from `AppDesignStyle.kt` and the springs
 * from `AppMotion.kt`. When one of those changes, this file changes with it — the previews are
 * only worth showing while they are the same numbers the APK uses.
 */

type Rgb = [number, number, number];

function hexToRgb(hex: string): Rgb {
  const clean = hex.trim().replace("#", "");
  if (!/^[0-9a-fA-F]{6}$/.test(clean)) return [0.17, 0.42, 0.93];
  return [0, 2, 4].map((offset) => parseInt(clean.slice(offset, offset + 2), 16) / 255) as Rgb;
}

function rgbToHex([r, g, b]: Rgb): string {
  return (
    "#" +
    [r, g, b]
      .map((channel) =>
        Math.round(Math.max(0, Math.min(1, channel)) * 255)
          .toString(16)
          .padStart(2, "0"),
      )
      .join("")
  );
}

/** Python's colorsys.rgb_to_hls, so the maths agrees with the generator to the last digit. */
function rgbToHls([r, g, b]: Rgb): [number, number, number] {
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const l = (max + min) / 2;
  if (max === min) return [0, l, 0];
  const s = l <= 0.5 ? (max - min) / (max + min) : (max - min) / (2 - max - min);
  const rc = (max - r) / (max - min);
  const gc = (max - g) / (max - min);
  const bc = (max - b) / (max - min);
  let h = r === max ? bc - gc : g === max ? 2 + rc - bc : 4 + gc - rc;
  h = ((h / 6) % 1 + 1) % 1;
  return [h, l, s];
}

function hlsToRgb(h: number, l: number, s: number): Rgb {
  if (s === 0) return [l, l, l];
  const m2 = l <= 0.5 ? l * (1 + s) : l + s - l * s;
  const m1 = 2 * l - m2;
  const v = (hue: number) => {
    const x = ((hue % 1) + 1) % 1;
    if (x < 1 / 6) return m1 + (m2 - m1) * x * 6;
    if (x < 0.5) return m2;
    if (x < 2 / 3) return m1 + (m2 - m1) * (2 / 3 - x) * 6;
    return m1;
  };
  return [v(h + 1 / 3), v(h), v(h - 1 / 3)];
}

function shift(rgb: Rgb, lightness: number, saturation = 1): Rgb {
  const [h, l, s] = rgbToHls(rgb);
  const nextL = lightness <= 1 ? l * lightness : 1 - (1 - l) / lightness;
  return hlsToRgb(h, Math.max(0, Math.min(1, nextL)), Math.max(0, Math.min(1, s * saturation)));
}

function mix(a: Rgb, b: Rgb, amount: number): Rgb {
  return a.map((channel, index) => channel + (b[index] - channel) * amount) as Rgb;
}

function rotateHue(rgb: Rgb, degrees: number): Rgb {
  const [h, l, s] = rgbToHls(rgb);
  return hlsToRgb((h + degrees / 360) % 1, l, s);
}

function luminance([r, g, b]: Rgb): number {
  const lin = (c: number) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
  return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
}

const WHITE: Rgb = [1, 1, 1];
const INK: Rgb = [0.043, 0.063, 0.106];

export function deriveSecondary(primary: string): string {
  return rgbToHex(shift(hexToRgb(primary), 1, 0.45));
}

export function deriveTertiary(primary: string): string {
  return rgbToHex(rotateHue(hexToRgb(primary), 60));
}

export type Ramp = { base: string; pressed: string; subtle: string; on: string };

function ramp(hex: string, dark: boolean): Ramp {
  const base = hexToRgb(hex);
  if (dark) {
    return {
      base: rgbToHex(shift(mix(base, WHITE, 0.22), 1, 0.92)),
      pressed: rgbToHex(shift(mix(base, WHITE, 0.4), 1, 0.85)),
      subtle: rgbToHex(mix(INK, base, 0.14)),
      on: "#06101f",
    };
  }
  const lum = luminance(base);
  return {
    base: rgbToHex(base),
    pressed: rgbToHex(shift(base, 0.78)),
    subtle: rgbToHex(mix(base, WHITE, 0.9)),
    on: 1.05 / (lum + 0.05) >= (lum + 0.05) / 0.05 ? "#ffffff" : "#0b0e14",
  };
}

export type Palette = {
  dark: boolean;
  background: string;
  surface: string;
  surfaceVariant: string;
  contentPrimary: string;
  contentSecondary: string;
  contentTertiary: string;
  border: string;
  borderStrong: string;
  divider: string;
  danger: string;
  skeleton: string;
  accent: Ramp;
  secondary: Ramp;
  tertiary: Ramp;
  avatars: string[];
};

export function palette(
  colours: { accent: string; secondary?: string; tertiary?: string },
  dark: boolean,
): Palette {
  const secondary = colours.secondary || deriveSecondary(colours.accent);
  const tertiary = colours.tertiary || deriveTertiary(colours.accent);
  const neutrals = dark
    ? {
        background: "#0b0e14",
        surface: "#141922",
        surfaceVariant: "#1d232e",
        contentPrimary: "#f2f4f8",
        contentSecondary: "#b4bcca",
        contentTertiary: "#7f8899",
        border: "#262d3a",
        borderStrong: "#38414f",
        divider: "#1e2531",
        danger: "#f0666d",
        skeleton: "#1d232e",
        avatars: ["#16233b", "#10271f", "#2a1f0e", "#221438", "#2b1416"],
      }
    : {
        background: "#f7f8fa",
        surface: "#ffffff",
        surfaceVariant: "#eff1f5",
        contentPrimary: "#0b0f1a",
        contentSecondary: "#3d4552",
        contentTertiary: "#6b7280",
        border: "#e3e6ec",
        borderStrong: "#cbd1db",
        divider: "#e9ecf1",
        danger: "#c42b32",
        skeleton: "#e9ecf1",
        avatars: ["#e8effd", "#e4f5ee", "#fdf1e0", "#f7eafd", "#fce9e9"],
      };
  return {
    dark,
    ...neutrals,
    accent: ramp(colours.accent, dark),
    secondary: ramp(secondary, dark),
    tertiary: ramp(tertiary, dark),
  };
}

// ── Design styles · AppDesignStyle.kt ───────────────────────────────────────────

export type DesignStyleKey = "Utility" | "Social" | "Editorial" | "Playful";

export type DesignTokens = {
  radius: { xs: number; sm: number; md: number; lg: number; xl: number };
  button: number;
  chip: number;
  card: "outlined" | "raised" | "offset";
  field: "outlined" | "filled" | "underlined";
  bar: "docked" | "floating" | "minimal" | "chunky";
  border: number;
  borderStrong: number;
  uppercase: boolean;
  offsetShadow: number;
};

const PILL = 999;

export const DESIGN_TOKENS: Record<DesignStyleKey, DesignTokens> = {
  Utility: {
    radius: { xs: 6, sm: 10, md: 14, lg: 20, xl: 28 },
    button: 10,
    chip: PILL,
    card: "outlined",
    field: "outlined",
    bar: "docked",
    border: 1,
    borderStrong: 1.5,
    uppercase: false,
    offsetShadow: 0,
  },
  Social: {
    radius: { xs: 10, sm: 14, md: 20, lg: 26, xl: 32 },
    button: PILL,
    chip: PILL,
    card: "raised",
    field: "filled",
    bar: "floating",
    border: 1,
    borderStrong: 1.5,
    uppercase: false,
    offsetShadow: 0,
  },
  Editorial: {
    radius: { xs: 2, sm: 3, md: 4, lg: 6, xl: 8 },
    button: 2,
    chip: 2,
    card: "outlined",
    field: "underlined",
    bar: "minimal",
    border: 1,
    borderStrong: 1.5,
    uppercase: true,
    offsetShadow: 0,
  },
  Playful: {
    radius: { xs: 8, sm: 12, md: 16, lg: 22, xl: 28 },
    button: 14,
    chip: 12,
    card: "offset",
    field: "outlined",
    bar: "chunky",
    border: 2,
    borderStrong: 2.5,
    uppercase: false,
    offsetShadow: 4,
  },
};

/** What each style changes, in the words the picker shows under its name. */
export const DESIGN_TRAITS: Record<DesignStyleKey, string[]> = {
  Utility: ["Hairline outlined cards", "10dp buttons", "Outlined fields", "Docked tab bar"],
  Social: ["Pill buttons", "Soft raised cards", "Filled fields", "Floating tab bar, sliding pill"],
  Editorial: ["Near-square corners", "Uppercase labels", "Underlined fields", "Minimal tab bar"],
  Playful: ["2dp ink outlines", "Solid offset shadows", "Buttons sink when pressed", "Chunky tab bar"],
};

// ── Motion styles · AppMotion.kt ────────────────────────────────────────────────

export type MotionStyleKey = "Standard" | "Bouncy" | "Calm" | "Snappy";

export type MotionTokens = {
  instant: number;
  quick: number;
  medium: number;
  slow: number;
  pressScale: number;
  pressOvershoot: number;
  pressDamping: number;
  pressStiffness: number;
  sheetDamping: number;
  navigationDamping: number;
  navigationStiffness: number;
};

export const MOTION_TOKENS: Record<MotionStyleKey, MotionTokens> = {
  Standard: {
    instant: 90, quick: 150, medium: 250, slow: 400,
    pressScale: 0.96, pressOvershoot: 1.02, pressDamping: 0.6, pressStiffness: 1400,
    sheetDamping: 0.86, navigationDamping: 1, navigationStiffness: 400,
  },
  Bouncy: {
    instant: 90, quick: 160, medium: 280, slow: 440,
    pressScale: 0.94, pressOvershoot: 1.045, pressDamping: 0.42, pressStiffness: 900,
    sheetDamping: 0.68, navigationDamping: 0.86, navigationStiffness: 420,
  },
  Calm: {
    instant: 110, quick: 190, medium: 300, slow: 460,
    pressScale: 0.985, pressOvershoot: 1, pressDamping: 1, pressStiffness: 1200,
    sheetDamping: 1, navigationDamping: 1, navigationStiffness: 300,
  },
  Snappy: {
    instant: 60, quick: 100, medium: 160, slow: 240,
    pressScale: 0.96, pressOvershoot: 1.02, pressDamping: 0.75, pressStiffness: 2000,
    sheetDamping: 0.95, navigationDamping: 1, navigationStiffness: 700,
  },
};

/**
 * A Compose spring as framer-motion parameters.
 *
 * Compose takes a damping *ratio*; framer takes the damping coefficient. With unit mass the
 * coefficient is ratio × 2√stiffness, which makes the two the same differential equation.
 */
export function composeSpring(dampingRatio: number, stiffness: number) {
  return {
    type: "spring" as const,
    stiffness,
    damping: dampingRatio * 2 * Math.sqrt(stiffness),
    mass: 1,
  };
}

/** The `enter` easing: decelerate hard so the element arrives settled. */
export const EASE_ENTER = [0.05, 0.7, 0.1, 1] as const;
/** The `exit` easing: accelerate away. */
export const EASE_EXIT = [0.3, 0, 0.8, 0.15] as const;
export const EASE_STANDARD = [0.2, 0, 0, 1] as const;
