// `generator/genkit/brand.py` in TypeScript: the three brand ramps a project's palette draws from,
// and the text colour that reads on each shade. The previews must agree with the generator to the
// last digit, so this follows the Python step for step, down to `colorsys` and Python's rounding;
// `brand.test.ts` compares both on the generator's 200 seeded accents.

export type Rgb = [number, number, number];

const WHITE: Rgb = [1, 1, 1];
const LIGHT_THEME_INK: Rgb = [0x0b / 255, 0x0e / 255, 0x14 / 255];
const DARK_THEME_INK: Rgb = [0x06 / 255, 0x10 / 255, 0x1f / 255];
const SUBTLE_INK: Rgb = [0.043, 0.063, 0.106];
const AA = 4.5;
const SECONDARY_SATURATION = 0.45;
const TERTIARY_HUE_SHIFT = 60;
const MAX_DARKENING_STEPS = 40;

/** Python's `%`, whose result takes the divisor's sign. */
function pyMod(a: number, b: number): number {
  const m = a % b;
  return m !== 0 && m < 0 !== b < 0 ? m + b : m;
}

/** Python's `round`, which sends halves to the even neighbour. */
function pyRound(value: number): number {
  const floor = Math.floor(value);
  const diff = value - floor;
  if (diff > 0.5) return floor + 1;
  if (diff < 0.5) return floor;
  return floor % 2 === 0 ? floor : floor + 1;
}

export function parseHex(value: string): Rgb | null {
  const cleaned = value.trim().replace(/^#/, "");
  if (!/^[0-9A-Fa-f]{6}$/.test(cleaned)) return null;
  return [0, 2, 4].map((offset) => parseInt(cleaned.slice(offset, offset + 2), 16) / 255) as Rgb;
}

export function toHex(rgb: Rgb): string {
  return `#${rgb.map((c) => pyRound(Math.max(0, Math.min(1, c)) * 255).toString(16).padStart(2, "0")).join("").toUpperCase()}`;
}

function rgbToHls([r, g, b]: Rgb): [number, number, number] {
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const sum = max + min;
  const range = max - min;
  const l = sum / 2;
  if (min === max) return [0, l, 0];
  const s = l <= 0.5 ? range / sum : range / (2 - max - min);
  const rc = (max - r) / range;
  const gc = (max - g) / range;
  const bc = (max - b) / range;
  const h = r === max ? bc - gc : g === max ? 2 + rc - bc : 4 + gc - rc;
  return [pyMod(h / 6, 1), l, s];
}

function channel(m1: number, m2: number, hue: number): number {
  const h = pyMod(hue, 1);
  if (h < 1 / 6) return m1 + (m2 - m1) * h * 6;
  if (h < 0.5) return m2;
  if (h < 2 / 3) return m1 + (m2 - m1) * (2 / 3 - h) * 6;
  return m1;
}

function hlsToRgb(h: number, l: number, s: number): Rgb {
  if (s === 0) return [l, l, l];
  const m2 = l <= 0.5 ? l * (1 + s) : l + s - l * s;
  const m1 = 2 * l - m2;
  return [channel(m1, m2, h + 1 / 3), channel(m1, m2, h), channel(m1, m2, h - 1 / 3)];
}

/** Moves a colour along its own hue rather than towards black or white. */
function shift(rgb: Rgb, lightness: number, saturation = 1): Rgb {
  const [hue, existing, sat] = rgbToHls(rgb);
  const target = lightness <= 1 ? existing * lightness : 1 - (1 - existing) / lightness;
  return hlsToRgb(hue, Math.max(0, Math.min(1, target)), Math.max(0, Math.min(1, sat * saturation)));
}

function mix(rgb: Rgb, other: Rgb, amount: number): Rgb {
  return [0, 1, 2].map((i) => rgb[i]! + (other[i]! - rgb[i]!) * amount) as Rgb;
}

function rotateHue(rgb: Rgb, degrees: number): Rgb {
  const [hue, lightness, saturation] = rgbToHls(rgb);
  return hlsToRgb(pyMod(hue + degrees / 360, 1), lightness, saturation);
}

function relativeLuminance(rgb: Rgb): number {
  const [r, g, b] = rgb.map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)) as Rgb;
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(a: Rgb, b: Rgb): number {
  const [light, dark] = [relativeLuminance(a), relativeLuminance(b)].sort((x, y) => y - x) as [number, number];
  return (light + 0.05) / (dark + 0.05);
}

function legible(colour: Rgb, ink: Rgb): Rgb {
  let current = colour;
  for (let step = 0; step < MAX_DARKENING_STEPS; step++) {
    if (Math.max(contrast(current, WHITE), contrast(current, ink)) >= AA) return current;
    current = shift(current, 0.97);
  }
  return current;
}

function darkVariant(base: Rgb): Rgb {
  return shift(mix(base, WHITE, 0.22), 1, 0.92);
}

function whiteReadsBetter(colour: Rgb, ink: Rgb): boolean {
  return contrast(colour, WHITE) >= contrast(colour, ink);
}

export type BrandRamp = {
  base: Rgb;
  pressed: Rgb;
  subtleLight: Rgb;
  dark: Rgb;
  darkPressed: Rgb;
  subtleDark: Rgb;
  onLight: Rgb;
  onDark: Rgb;
};

export type BrandName = "accent" | "secondary" | "tertiary";

/** The three brand colours, with a blank supporting colour worked out from the primary. */
export function brandColours(accent: Rgb, secondary: Rgb | null, tertiary: Rgb | null): Record<BrandName, Rgb> {
  return {
    accent,
    secondary: secondary ?? shift(accent, 1, SECONDARY_SATURATION),
    tertiary: tertiary ?? rotateHue(accent, TERTIARY_HUE_SHIFT),
  };
}

export function brandRamp(colour: Rgb): BrandRamp {
  const base = legible(colour, LIGHT_THEME_INK);
  const dark = legible(darkVariant(base), DARK_THEME_INK);
  return {
    base,
    pressed: shift(base, 0.78),
    subtleLight: mix(base, WHITE, 0.9),
    dark,
    darkPressed: shift(mix(base, WHITE, 0.4), 1, 0.85),
    subtleDark: mix(SUBTLE_INK, base, 0.14),
    onLight: whiteReadsBetter(base, LIGHT_THEME_INK) ? WHITE : LIGHT_THEME_INK,
    onDark: whiteReadsBetter(dark, DARK_THEME_INK) ? WHITE : DARK_THEME_INK,
  };
}

/** The roles a content shade has to read on, as the generator's tokens name them. */
export const SURFACE_ROLES = ["background", "backgroundGrouped", "surface", "surfaceGrouped", "surfaceElevated"] as const;

const SHADE_STEPS = 30;
const SHADE_STEP = 0.05;

/**
 * `colour`, or the nearest shade of it that reaches [minimum] on every one of [backgrounds]: darker
 * in a light palette, lighter in a dark one, each step an Oklab mix rounded to 8 bits. The
 * generator's `_legible_on`, step for step, so the preview's accent text is the app's.
 */
export function legibleOn(colour: Rgb, backgrounds: Rgb[], light: boolean, minimum = AA): Rgb {
  const toward: Rgb = light ? [0, 0, 0] : WHITE;
  let shade = colour;
  for (let step = 0; step < SHADE_STEPS; step++) {
    if (backgrounds.every((background) => contrast(shade, background) >= minimum)) return shade;
    shade = eightBit(lerpOklab(shade, toward, SHADE_STEP));
  }
  return shade;
}

function lerpOklab(start: Rgb, stop: Rgb, fraction: number): Rgb {
  const a = toOklab(start);
  const b = toOklab(stop);
  return fromOklab([0, 1, 2].map((i) => a[i]! + (b[i]! - a[i]!) * fraction) as Rgb);
}

function toOklab(rgb: Rgb): Rgb {
  const [r, g, b] = rgb.map(linear) as Rgb;
  const [l, m, s] = [
    0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b,
    0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b,
    0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b,
  ].map((v) => Math.sign(v) * Math.abs(v) ** (1 / 3)) as Rgb;
  return [
    0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
    1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
    0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
  ];
}

function fromOklab([lightness, a, b]: Rgb): Rgb {
  const l = (lightness + 0.3963377774 * a + 0.2158037573 * b) ** 3;
  const m = (lightness - 0.1055613458 * a - 0.0638541728 * b) ** 3;
  const s = (lightness - 0.0894841775 * a - 1.291485548 * b) ** 3;
  return [
    gamma(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
    gamma(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
    gamma(-0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s),
  ];
}

function linear(c: number): number {
  return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
}

function gamma(c: number): number {
  return c <= 0.0031308 ? 12.92 * c : 1.055 * Math.sign(c) * Math.abs(c) ** (1 / 2.4) - 0.055;
}

/** As the hex it is emitted as. Halves round to even, as Python's `round` does. */
function eightBit(rgb: Rgb): Rgb {
  return rgb.map((c) => roundHalfEven(Math.max(0, Math.min(1, c)) * 255) / 255) as Rgb;
}

function roundHalfEven(value: number): number {
  const floor = Math.floor(value);
  const fraction = value - floor;
  if (fraction !== 0.5) return Math.round(value);
  return floor % 2 === 0 ? floor : floor + 1;
}
