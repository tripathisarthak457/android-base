import { CATALOGUE, android } from "../lib/catalogue.ts";

/** What arrives whatever you tick. */
function items() {
  const target = android();
  const flavours = CATALOGUE.rules.flavours;
  return [
    {
      title: "A design system with no Material",
      body:
        "Built on Compose foundation and ui alone: an iOS-sized type scale, spring press feedback instead of a grey overlay, " +
        "inset-grouped lists, detent sheets, menus, toasts and a hand-drawn icon set. The build refuses Material component " +
        "imports and the ripple, so neither can drift back in.",
      detail: "verifyCodeRules",
    },
    {
      title: "Smooth by rule, not by hope",
      body:
        "Animated values are read in layout and draw only, screens slide rather than fade, and the patterns that cost frames " +
        "fail the build: Modifier.composed, collectAsState in features, raw tweens, lists without keys.",
      detail: "./gradlew check",
    },
    {
      title: "Tabs that feel like tabs",
      body:
        "Each tab keeps its own back stack, scroll position and ViewModels, and only the visible one is laid out. The bar " +
        "floats over the content at every depth while the content scrolls under it. Re-tapping a tab pops it to its root, " +
        "and a burst of Back presses settles into one animated pop.",
      detail: "KeptTabs",
    },
    {
      title: "Navigation nobody has to coordinate on",
      body:
        "Navigation 3, with each feature registering its own screens. Adding one touches no file outside its module, so two " +
        "people adding screens in the same week do not conflict. Predictive back follows the finger.",
      detail: "Navigation 3",
    },
    {
      title: "Designed for every screen, not just phones",
      body:
        "Layout follows the window size class, never orientation or device. The tab bar becomes a rail on wide windows, a list " +
        "and its detail share one, and forms stop at a readable width. Locking an activity to one orientation fails the build.",
      detail: "window size classes",
    },
    {
      title: "A look you pick, then own",
      body:
        `${target.design_styles.length} design styles change corners, borders, fields, the tab bar, surface tone and type ` +
        `voice together, each checked against WCAG AA in both themes, and ${target.motion_styles.length} motion styles change ` +
        "every spring. Both come from one set of design tokens; the components are plain Compose in your own module.",
      detail: "AppTheme",
    },
    {
      title: "Compact now, modular when you grow",
      body:
        "Every feature and data module keeps its public surface in an api package from day one, and the build checks the " +
        "module graph, so splitting into api and impl modules later moves files without rewriting imports.",
      detail: "ModuleGraph",
    },
    {
      title: "MVI that cannot block Back",
      body:
        "Events are handled without suspending; work runs through launchLatest or launchExclusive, so a slow request never " +
        "holds up navigation and a double tap never submits twice. Errors are typed, and text lives in the ViewModel's " +
        "TextFieldStates.",
      detail: "MviViewModel",
    },
    {
      title: "Instructions for your AI agent",
      body:
        "AGENTS.md, with CLAUDE.md pointing at it, tells an agent how the project is laid out, what the build refuses, how a " +
        "feature is shaped, and the motion and performance rules to keep it smooth.",
      detail: "AGENTS.md",
    },
    {
      title: `${flavours.length} build environments`,
      body:
        `${new Intl.ListFormat("en-GB", { type: "conjunction" }).format(flavours)}, in debug and release. Only the app module ` +
        "carries flavours, so switching environment rebuilds nothing in core, data or feature.",
      detail: "AppFlavor",
    },
    {
      title: "Release artifacts that are already named",
      body:
        "distProdRelease gives per-ABI APKs plus a universal one, stamped with variant, ABI, version and build time, with a " +
        "checksums file beside them.",
      detail: "build/outputs/dist",
    },
  ];
}

export function Included() {
  return (
    <section className="border-y border-ink-800 bg-ink-900/40">
      <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
        <div className="max-w-2xl">
          <h2 className="font-display text-4xl font-bold text-ink-100">This part is not optional</h2>
          <p className="mt-3 text-ink-300">
            Everything below arrives whatever you tick. It is the reason the template exists, and the part that would take a
            fortnight to assemble by hand.
          </p>
        </div>

        <ul className="mt-10 grid gap-4 md:grid-cols-2">
          {items().map((item) => (
            <li key={item.title} className="rounded-xl border border-ink-700 bg-ink-850 p-5">
              <div className="flex items-start justify-between gap-4">
                <h3 className="font-semibold text-ink-100">{item.title}</h3>
                <code className="shrink-0 rounded bg-ink-800 px-2 py-0.5 font-mono text-[11px] text-ink-300">{item.detail}</code>
              </div>
              <p className="mt-2.5 text-sm leading-relaxed text-ink-300">{item.body}</p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
