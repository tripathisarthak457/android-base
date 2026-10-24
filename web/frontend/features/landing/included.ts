import { isSetupOnly } from "../catalogue/options.ts";
import type { Catalogue } from "../catalogue/types.ts";
import { listed } from "../../lib/text.ts";

export interface IncludedItem {
  title: string;
  body: string;
  detail: string;
  /** Only a project with UI components has it, so a setup-only deployment says it comes with the Android release. */
  components: boolean;
}

/**
 * What arrives whatever you tick. On a setup-only deployment every project is made without UI
 * components, so an item that stays says only what such a project has: its motion and gestures
 * are the components' and the shell's, which come with the Android release.
 */
export function includedItems(catalogue: Catalogue): IncludedItem[] {
  const setupOnly = isSetupOnly(catalogue);
  const target = catalogue.targets.find((candidate) => candidate.key === "android") ?? catalogue.targets[0];
  if (!target) throw new Error("The catalogue has no targets.");
  const flavours = catalogue.rules.flavours;
  return [
    {
      title: "A design system with no Material",
      body:
        "Built on Compose foundation and ui alone: an iOS-sized type scale, spring press feedback instead of a grey overlay, " +
        "inset-grouped lists, detent sheets, menus, toasts and a hand-drawn icon set. The build refuses Material component " +
        "imports and the ripple, so neither can drift back in.",
      detail: "verifyCodeRules",
      components: true,
    },
    {
      title: "Smooth by rule, not by hope",
      body: setupOnly
        ? "Animated values are read in layout and draw only, and the patterns that cost frames fail the build: " +
          "Modifier.composed, collectAsState in features, raw tweens, lists without keys."
        : "Animated values are read in layout and draw only, screens slide rather than fade, and the patterns that cost frames " +
          "fail the build: Modifier.composed, collectAsState in features, raw tweens, lists without keys.",
      detail: "./gradlew check",
      components: false,
    },
    {
      title: "Tabs that feel like tabs",
      body:
        "Each tab keeps its own back stack, scroll position and ViewModels, and only the visible one is laid out. The bar " +
        "floats over the content at every depth while the content scrolls under it. Re-tapping a tab pops it to its root, " +
        "and a burst of Back presses settles into one animated pop.",
      detail: "KeptTabs",
      components: true,
    },
    {
      title: "Navigation nobody has to coordinate on",
      body:
        "Navigation 3, with each feature registering its own screens. Adding one touches no file outside its module, so two " +
        "people adding screens in the same week do not conflict. " +
        (setupOnly
          ? "A deep link opens its screen on the back stack, and the stack survives the process being killed."
          : "Predictive back follows the finger."),
      detail: "Navigation 3",
      components: false,
    },
    {
      title: "Designed for every screen, not just phones",
      body:
        "Layout follows the window size class, never orientation or device. The tab bar becomes a rail on wide windows, a list " +
        "and its detail share one, and forms stop at a readable width. Locking an activity to one orientation fails the build.",
      detail: "window size classes",
      components: true,
    },
    {
      title: "A look you pick, then own",
      body:
        `${target.design_styles.length} design styles change corners, borders, fields, the tab bar, surface tone and type ` +
        `voice together, each checked against WCAG AA in both themes, and ${target.motion_styles.length} motion styles change ` +
        "every spring. Both come from one set of design tokens; the components are plain Compose in your own module.",
      detail: "AppTheme",
      components: true,
    },
    {
      title: "Compact now, modular when you grow",
      body:
        "Every feature and data module keeps its public surface in an api package from day one, and the build checks the " +
        "module graph, so splitting into api and impl modules later moves files without rewriting imports.",
      detail: "ModuleGraph",
      components: false,
    },
    {
      title: "MVI that cannot block Back",
      body:
        "Events are handled without suspending; work runs through launchLatest or launchExclusive, so a slow request never " +
        "holds up navigation and a double tap never submits twice. Errors are typed, and text lives in the ViewModel's " +
        "TextFieldStates.",
      detail: "MviViewModel",
      components: false,
    },
    {
      title: "Instructions for your AI agent",
      body:
        "AGENTS.md, with CLAUDE.md pointing at it, tells an agent how the project is laid out, what the build refuses, how a " +
        "feature is shaped, and the motion and performance rules to keep it smooth.",
      detail: "AGENTS.md",
      components: false,
    },
    {
      title: `${flavours.length} build environments`,
      body:
        `${listed(flavours)}, in debug and release. Only the app module ` +
        "carries flavours, so switching environment rebuilds nothing in core, data or feature.",
      detail: "AppFlavor",
      components: false,
    },
    {
      title: "Release artifacts that are already named",
      body:
        "distProdRelease gives per-ABI APKs plus a universal one, stamped with variant, ABI, version and build time, with a " +
        "checksums file beside them.",
      detail: "build/outputs/dist",
      components: false,
    },
  ];
}
