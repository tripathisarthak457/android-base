// The tabs a generated app's bar shows, in the generator's order (`tabs.py`): Home, the scaffolded
// modules in the order they were typed, then Feed, Search, Profile and Settings. Past five the bar
// shows the first four and More, which lists the rest. With no tab source the generator scaffolds
// a `home` module. Each scaffolded tab gets its own icon.

export const MAX_TABS = 5;

/** The most tabs a wider window's rail shows before it, too, moves the rest under More. */
export const MAX_RAIL_TABS = 7;

export type TabIcon = "home" | "list" | "search" | "user" | "settings" | "grid" | "bag" | "star" | "inbox" | "chart" | "bookmark" | "more";

export type Screen = "signin" | "home" | "feed" | "search" | "profile" | "settings" | "module" | "more";

export type PreviewTab = {
  key: string;
  label: string;
  icon: TabIcon;
  screen: Screen;
  /** The tabs a More tab lists. */
  listed?: readonly PreviewTab[];
};

const LEADING: { feature: string; tab: PreviewTab }[] = [{ feature: "sample", tab: { key: "home", label: "Home", icon: "home", screen: "home" } }];

const TRAILING: { feature: string; tab: PreviewTab }[] = [
  { feature: "paging", tab: { key: "feed", label: "Feed", icon: "list", screen: "feed" } },
  { feature: "search", tab: { key: "search", label: "Search", icon: "search", screen: "search" } },
  { feature: "profile", tab: { key: "profile", label: "Profile", icon: "user", screen: "profile" } },
  { feature: "settings", tab: { key: "settings", label: "Settings", icon: "settings", screen: "settings" } },
];

const MODULE_ICONS: TabIcon[] = ["grid", "bag", "star", "inbox", "chart", "bookmark"];

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

function sourced(sources: readonly { feature: string; tab: PreviewTab }[], features: ReadonlySet<string>): PreviewTab[] {
  return sources.filter((source) => features.has(source.feature)).map((source) => source.tab);
}

export function planTabs(features: ReadonlySet<string>, modules: readonly string[]): TabPlan {
  const scaffolded = modules.map(
    (name, index): PreviewTab => ({ key: `module:${name}`, label: moduleTitle(name), icon: MODULE_ICONS[index % MODULE_ICONS.length] ?? "grid", screen: "module" }),
  );
  const all = [...sourced(LEADING, features), ...scaffolded, ...sourced(TRAILING, features)];
  if (all.length === 0) {
    const home: PreviewTab = { key: "module:home", label: "Home", icon: "home", screen: "module" };
    return { tabs: [home], all: [home], overflow: [], scaffoldedHome: true };
  }
  if (all.length <= MAX_TABS) return { tabs: all, all, overflow: [], scaffoldedHome: false };
  const overflow = all.slice(MAX_TABS - 1);
  const more: PreviewTab = { key: "more", label: "More", icon: "more", screen: "more", listed: overflow };
  return { tabs: [...all.slice(0, MAX_TABS - 1), more], all, overflow, scaffoldedHome: false };
}

/** Every screen the preview can show for these answers, in the order the app reaches them. */
export function screensFor(features: ReadonlySet<string>, modules: readonly string[]): PreviewTab[] {
  const signIn: PreviewTab[] = features.has("auth") ? [{ key: "signin", label: "Sign in", icon: "user", screen: "signin" }] : [];
  const plan = planTabs(features, modules);
  return [...signIn, ...plan.tabs, ...plan.overflow];
}
