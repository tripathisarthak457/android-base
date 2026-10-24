import { GLYPHS, type GlyphName } from "./glyphs.ts";

// The tabs a generated app's bar shows, in the generator's order (`tabs.py`): Home, the scaffolded
// modules in the order they were typed, then Feed, Search, Profile and Settings. Past five the bar
// shows the first four and More, which lists the rest. With no tab source the generator scaffolds
// a `home` module. Each scaffolded tab gets its own glyph, picked from its name as the generator
// picks it.

export const MAX_TABS = 5;

/** The most tabs a wider window's rail shows before it, too, moves the rest under More. */
export const MAX_RAIL_TABS = 7;

export type Screen = "signin" | "home" | "feed" | "search" | "profile" | "settings" | "module" | "more";

export type PreviewTab = {
  key: string;
  label: string;
  icon: GlyphName;
  /** The glyph inside the selection pill; the outlined one when absent, as `ShellTab` defaults it. */
  selectedIcon?: GlyphName;
  screen: Screen;
  /** The tabs a More tab lists. */
  listed?: readonly PreviewTab[];
};

type SourcedTab = { feature: string; tab: PreviewTab };

/** The template's own tabs, as `AppDestinations.kt` declares them: no filled forms yet. */
export const TEMPLATE_TABS: readonly SourcedTab[] = [
  { feature: "sample", tab: { key: "home", label: "Home", icon: "Home", screen: "home" } },
  { feature: "paging", tab: { key: "feed", label: "Feed", icon: "ListView", screen: "feed" } },
  { feature: "search", tab: { key: "search", label: "Search", icon: "Search", screen: "search" } },
  { feature: "profile", tab: { key: "profile", label: "Profile", icon: "User", screen: "profile" } },
  { feature: "settings", tab: { key: "settings", label: "Settings", icon: "Settings", screen: "settings" } },
];

const LEADING = TEMPLATE_TABS.slice(0, 1);
const TRAILING = TEMPLATE_TABS.slice(1);

/** A module's glyph, from a word in its name, best match first (`_GLYPHS` in `tabs.py`). */
export const NAMED_GLYPHS: readonly (readonly [words: readonly string[], glyphs: readonly string[]])[] = [
  [["order", "shipment", "delivery", "parcel"], ["Package"]],
  [["cart", "shop", "store", "product"], ["Cart"]],
  [["wallet", "payment", "billing", "card", "invoice"], ["CreditCard"]],
  [["inbox", "chat", "message"], ["Message"]],
  [["mail"], ["Mail"]],
  [["calendar", "event", "booking", "schedule"], ["Calendar"]],
  [["notification", "alert"], ["Bell"]],
  [["favourite", "favorite", "saved", "wishlist"], ["Heart", "Bookmark"]],
  [["file", "document"], ["Folder", "File"]],
  [["map", "place", "location", "trip"], ["MapPin"]],
  [["explore", "discover"], ["Globe"]],
  [["photo", "gallery", "image"], ["ImageIcon", "Camera"]],
  [["team", "contact", "people", "member"], ["Users"]],
];

/** For a name that matches nothing, taken in turn so that no two tabs share one (`_FALLBACK`). */
export const FALLBACK_GLYPHS: readonly string[] = ["Grid", "Tag", "Bookmark", "Star", "Folder", "Globe", "Heart", "Calendar"];

const isGlyph = (name: string): name is GlyphName => name in GLYPHS;

/** Each module's glyph and filled form, never one in `taken` nor one another module got (`icons()`). */
function moduleGlyphs(names: readonly string[], taken: Set<string>): Map<string, { icon: GlyphName; selectedIcon: GlyphName }> {
  const outlines = Object.keys(GLYPHS)
    .filter((glyph) => !glyph.endsWith("Filled"))
    .sort();
  const chosen = new Map<string, { icon: GlyphName; selectedIcon: GlyphName }>();
  for (const name of names) {
    const wanted = NAMED_GLYPHS.filter(([words]) => words.some((word) => name.includes(word))).flatMap(([, glyphs]) => glyphs);
    const icon = [...wanted, ...FALLBACK_GLYPHS, ...outlines].filter(isGlyph).find((glyph) => !taken.has(glyph)) ?? "Grid";
    taken.add(icon);
    const filled = `${icon}Filled`;
    chosen.set(name, { icon, selectedIcon: isGlyph(filled) ? filled : icon });
  }
  return chosen;
}

/** `order_history` → `Order history`, the title a scaffolded screen shows. */
export function moduleTitle(name: string): string {
  const words = name.split("_").filter(Boolean).join(" ");
  return words ? words.charAt(0).toUpperCase() + words.slice(1) : name;
}

type TabPlan = {
  /** What a phone's bar shows, More included. */
  tabs: PreviewTab[];
  /** Every tab the app has, in order. */
  all: PreviewTab[];
  /** The tabs listed under More, because the bar holds five. */
  overflow: PreviewTab[];
  /** True when nothing provides a tab and the generator adds a `home` module. */
  scaffoldedHome: boolean;
};

function sourced(sources: readonly SourcedTab[], features: ReadonlySet<string>): PreviewTab[] {
  return sources.filter((source) => features.has(source.feature)).map((source) => source.tab);
}

export function planTabs(features: ReadonlySet<string>, modules: readonly string[]): TabPlan {
  const leading = sourced(LEADING, features);
  const trailing = sourced(TRAILING, features);
  const glyphs = moduleGlyphs(modules, new Set([...leading, ...trailing].map((tab) => tab.icon)));
  const scaffolded = modules.map(
    (name): PreviewTab => ({ key: `module:${name}`, label: moduleTitle(name), ...(glyphs.get(name) ?? { icon: "Grid" }), screen: "module" }),
  );
  const all = [...leading, ...scaffolded, ...trailing];
  if (all.length === 0) {
    const home: PreviewTab = { key: "module:home", label: "Home", ...(moduleGlyphs(["home"], new Set()).get("home") ?? { icon: "Grid" }), screen: "module" };
    return { tabs: [home], all: [home], overflow: [], scaffoldedHome: true };
  }
  if (all.length <= MAX_TABS) return { tabs: all, all, overflow: [], scaffoldedHome: false };
  const overflow = all.slice(MAX_TABS - 1);
  const more: PreviewTab = { key: "more", label: "More", icon: "MoreHorizontal", screen: "more", listed: overflow };
  return { tabs: [...all.slice(0, MAX_TABS - 1), more], all, overflow, scaffoldedHome: false };
}

/** Every screen the preview can show for these answers, in the order the app reaches them. */
export function screensFor(features: ReadonlySet<string>, modules: readonly string[]): PreviewTab[] {
  const signIn: PreviewTab[] = features.has("auth") ? [{ key: "signin", label: "Sign in", icon: "User", screen: "signin" }] : [];
  const plan = planTabs(features, modules);
  return [...signIn, ...plan.tabs, ...plan.overflow];
}
