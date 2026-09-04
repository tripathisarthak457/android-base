"use client";

import { motion } from "framer-motion";

/**
 * What arrives whatever you tick.
 *
 * This section exists because the configurator below it is a list of things you can *remove*, and
 * a visitor who only sees that list forms the impression that an untouched project is empty. The
 * opposite is true, and the parts that are not optional are the ones worth choosing this over a
 * blank Android Studio project for.
 */
const ALWAYS = [
  {
    title: "A design system with no Material",
    body:
      "Built on androidx.compose.foundation and ui alone. Its own ripple, type scale, 80-odd " +
      "components, date and time pickers, bottom sheet, charts and a hand-drawn icon set. An " +
      "androidx.compose.material import fails the build, so it cannot drift back in.",
    detail: "verifyComposeUsage",
  },
  {
    title: "One place for the look",
    body:
      "Colour, type, spacing, shape, elevation, motion and haptics are composition locals read " +
      "through AppTheme. Changing the font is one string, the accent is one hex, and how every " +
      "control answers a finger is one enum.",
    detail: "AppTheme",
  },
  {
    title: "Navigation nobody has to coordinate on",
    body:
      "A feature registers its own screens through Hilt multibinding. Adding one touches no file " +
      "outside its module — there is no central sealed Route to extend and no when in :app to add " +
      "a branch to, so two people adding screens in the same week do not conflict.",
    detail: "Navigation 3",
  },
  {
    title: "Per-tab back stacks",
    body:
      "Switching tabs and coming back returns the user where they were. Re-tapping the active tab " +
      "pops it to its root — the gesture everyone uses and nobody is told about.",
    detail: "AppShell",
  },
  {
    title: "Seven build variants",
    body:
      "dev, staging, prod and playstore across debug and release, minus playstoreDebug which does " +
      "not exist. Only the app modules carry flavours, so switching environment rebuilds nothing " +
      "in core, data or feature.",
    detail: "AppFlavor",
  },
  {
    title: "Release artifacts that are already named",
    body:
      "distProdRelease gives per-ABI APKs plus a universal one, stamped with variant, ABI, " +
      "version and build time, with a checksums file beside them. The bundle is a second task, " +
      "because AGP refuses to build both at once.",
    detail: "build/outputs/dist",
  },
  {
    title: "Layering the build enforces",
    body:
      "feature → feature and data → data fail the build rather than a code review. The rule that " +
      "keeps a module graph acyclic is the one that stops being followed the week everyone is busy.",
    detail: "verifyModuleDependencies",
  },
  {
    title: "MVI that cannot interleave",
    body:
      "Events are queued and drained in order on one coroutine, so two of them cannot land " +
      "between a read and a write of the state. LoadState is sealed, so there is no way to " +
      "represent loading and errored at the same time.",
    detail: "MviViewModel",
  },
];

export function Included() {
  return (
    <section className="border-y border-ink-800 bg-ink-900/40">
      <div className="mx-auto max-w-6xl px-6 py-20">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-bold tracking-tight text-ink-100">
            This part is not optional
          </h2>
          <p className="mt-3 text-ink-300">
            Everything below arrives whatever you tick. It is the reason the template exists, and
            the part that would take a fortnight to assemble by hand.
          </p>
        </div>

        <div className="mt-10 grid gap-4 md:grid-cols-2">
          {ALWAYS.map((item, index) => (
            <motion.div
              key={item.title}
              initial={{ opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-80px" }}
              transition={{ duration: 0.45, delay: (index % 2) * 0.06 }}
              className="rounded-xl border border-ink-700 bg-ink-850 p-5"
            >
              <div className="flex items-start justify-between gap-4">
                <h3 className="font-semibold text-ink-100">{item.title}</h3>
                <code className="shrink-0 rounded bg-ink-800 px-2 py-0.5 font-mono text-[11px] text-ink-400">
                  {item.detail}
                </code>
              </div>
              <p className="mt-2.5 text-sm leading-relaxed text-ink-300">{item.body}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
