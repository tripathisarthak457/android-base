// The tabs a generated app's bar shows: the template's tab sources in a fixed order, then
// scaffolded modules in the order they were typed, at most five. With no tab source the generator
// scaffolds a `home` module. Each scaffolded tab gets its own icon.

export const MAX_TABS = 5;

export type TabIcon = "home" | "list" | "search" | "user" | "settings" | "grid" | "bag" | "star" | "inbox" | "chart" | "bookmark";

export type Screen = "signin" | "home" | "feed" | "search" | "profile" | "settings" | "module";

export type PreviewTab = {
  key: string;
  label: string;
  icon: TabIcon;
  screen: Screen;
};

const SOURCES: { feature: string; tab: PreviewTab }[] = [
  { feature: "sample", tab: { key: "home", label: "Home", icon: "home", screen: "home" } },
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
  tabs: PreviewTab[];
  /** Destinations that exist but are not on the bar, because it holds five. */
  overflow: string[];
  /** True when nothing provides a tab and the generator adds a `home` module. */
  scaffoldedHome: boolean;
};

export function planTabs(features: ReadonlySet<string>, modules: readonly string[]): TabPlan {
  const candidates: PreviewTab[] = SOURCES.filter((source) => features.has(source.feature)).map((source) => source.tab);
  modules.forEach((name, index) => {
    candidates.push({ key: `module:${name}`, label: moduleTitle(name), icon: MODULE_ICONS[index % MODULE_ICONS.length] ?? "grid", screen: "module" });
  });
  if (candidates.length === 0) {
    return { tabs: [{ key: "module:home", label: "Home", icon: "home", screen: "module" }], overflow: [], scaffoldedHome: true };
  }
  return {
    tabs: candidates.slice(0, MAX_TABS),
    overflow: candidates.slice(MAX_TABS).map((tab) => tab.label),
    scaffoldedHome: false,
  };
}

/** Every screen the preview can show for these answers, in the order the app reaches them. */
export function screensFor(features: ReadonlySet<string>, modules: readonly string[]): PreviewTab[] {
  const signIn: PreviewTab[] = features.has("auth") ? [{ key: "signin", label: "Sign in", icon: "user", screen: "signin" }] : [];
  return [...signIn, ...planTabs(features, modules).tabs];
}
