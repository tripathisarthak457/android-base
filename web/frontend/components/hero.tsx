"use client";

import { motion } from "framer-motion";
import { Badge, Button } from "./primitives";

const REPO = "https://github.com/tripathisarthak457/android-base";

/**
 * The one screen a visitor decides on.
 *
 * It has to answer three questions before they scroll: what is this, what do I get, and is it
 * going to work. So: a sentence, a terminal showing the thing actually being run, and the counts
 * that make the claim checkable.
 */
export function Hero({ onStart }: { onStart: () => void }) {
  return (
    <section className="relative overflow-hidden">
      <div className="grid-backdrop pointer-events-none absolute inset-0" aria-hidden />

      <div className="relative mx-auto max-w-6xl px-6 pb-20 pt-20 md:pt-28">
        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
          className="max-w-3xl"
        >
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone="amber">Beta</Badge>
            <Badge tone="accent">Open source · MIT</Badge>
          </div>

          <h1 className="mt-5 text-4xl font-bold leading-[1.08] tracking-tight text-ink-100 md:text-6xl">
            A new Android project,
            <br />
            <span className="text-accent">already wired up.</span>
          </h1>

          <p className="mt-6 max-w-2xl text-lg leading-relaxed text-ink-300">
            Multi-module Compose with MVI, Hilt and Ktor. Four build environments, signed release
            output, a design system with no Material dependency, and a catalog app that shows every
            component. Pick what you want, get a zip that compiles.
          </p>

          <div className="mt-9 flex flex-wrap items-center gap-3">
            <Button size="large" onClick={onStart}>
              Configure your project
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden>
                <path
                  d="M3 8h10M9 4l4 4-4 4"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </Button>
            <Button
              size="large"
              variant="secondary"
              onClick={() => window.open(REPO, "_blank", "noopener")}
            >
              <svg width="17" height="17" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
                <path d="M8 0C3.58 0 0 3.58 0 8a8 8 0 0 0 5.47 7.59c.4.07.55-.17.55-.38l-.01-1.49c-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.4 7.4 0 0 1 4 0c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48l-.01 2.2c0 .21.15.46.55.38A8 8 0 0 0 16 8c0-4.42-3.58-8-8-8Z" />
              </svg>
              Read the source
            </Button>
          </div>

          <p className="mt-4 text-sm text-ink-400">
            No sign-up, nothing stored, no email. The zip is built when you click and streamed
            straight back.
          </p>

          {/*
            Stated plainly rather than buried. Everything here is tested — the repository builds
            both extremes of the generator on every push — but "tested" and "used by a lot of
            people" are different things, and only the second one finds the last few bugs.
          */}
          <div className="mt-6 flex max-w-2xl items-start gap-3 rounded-lg border border-amber/25 bg-amber/[0.06] p-4">
            <span className="mt-0.5 text-amber" aria-hidden>
              ⚠
            </span>
            <p className="text-sm leading-relaxed text-ink-300">
              <span className="font-medium text-ink-100">This is beta.</span> Every combination is
              compiled, tested and linted in CI before it ships, but not many people have used it
              yet. If something breaks, the button in the corner sends a report straight to whoever
              can fix it — and it attaches what you configured, so you do not have to describe it.
            </p>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 26 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.14, ease: [0.16, 1, 0.3, 1] }}
          className="mt-14 grid gap-4 md:grid-cols-5"
        >
          <Terminal />
          <Stats />
        </motion.div>
      </div>
    </section>
  );
}

/**
 * The same commands the README tells you to run, with their real output.
 *
 * Not a marketing screenshot: `7m 14s` is what `./gradlew build` actually takes on the template,
 * and quoting a real number is the difference between "this works" and "somebody typed that it
 * works".
 */
function Terminal() {
  const lines: { text: string; tone?: "prompt" | "dim" | "ok" }[] = [
    { text: "$ ./gradlew :app:assembleDevDebug", tone: "prompt" },
    { text: "BUILD SUCCESSFUL in 1m 4s", tone: "ok" },
    { text: "" },
    { text: "$ ./gradlew build", tone: "prompt" },
    { text: "compiles 7 variants · unit tests · detekt · android lint", tone: "dim" },
    { text: "BUILD SUCCESSFUL in 7m 14s", tone: "ok" },
    { text: "" },
    { text: "$ ./gradlew :app:distDevRelease", tone: "prompt" },
    { text: "MyApp-devRelease-arm64-v8a-1.0.0-1-20260904-1048.apk", tone: "dim" },
    { text: "MyApp-devRelease-universal-1.0.0-1-20260904-1048.apk", tone: "dim" },
    { text: "checksums.sha256", tone: "dim" },
  ];

  return (
    <div className="md:col-span-3 overflow-hidden rounded-xl border border-ink-700 bg-ink-900">
      <div className="flex items-center gap-2 border-b border-ink-700 bg-ink-850 px-4 py-2.5">
        <span className="h-2.5 w-2.5 rounded-full bg-ink-600" />
        <span className="h-2.5 w-2.5 rounded-full bg-ink-600" />
        <span className="h-2.5 w-2.5 rounded-full bg-ink-600" />
        <span className="ml-2 font-mono text-xs text-ink-400">MyApp — zsh</span>
      </div>
      <pre className="overflow-x-auto p-4 font-mono text-[12.5px] leading-[1.75]">
        {lines.map((line, index) => (
          <div
            key={index}
            className={
              line.tone === "ok"
                ? "text-mint"
                : line.tone === "prompt"
                  ? "text-ink-100"
                  : "text-ink-400"
            }
          >
            {line.text || " "}
          </div>
        ))}
      </pre>
    </div>
  );
}

const STATS = [
  { value: "24", label: "features you can switch off", detail: "Each removes a module, not just code" },
  { value: "80+", label: "components, zero Material", detail: "An androidx.compose.material import fails the build" },
  { value: "7", label: "build variants", detail: "dev / staging / prod / playstore × debug / release" },
  { value: "0", label: "setup steps after unzip", detail: "Open it and press run" },
];

function Stats() {
  return (
    <div className="md:col-span-2 grid gap-3">
      {STATS.map((stat, index) => (
        <motion.div
          key={stat.label}
          initial={{ opacity: 0, x: 14 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.45, delay: 0.24 + index * 0.07 }}
          className="rounded-xl border border-ink-700 bg-ink-850 px-4 py-3"
        >
          <div className="flex items-baseline gap-2.5">
            <span className="font-mono text-2xl font-bold text-accent">{stat.value}</span>
            <span className="text-sm font-medium text-ink-200">{stat.label}</span>
          </div>
          <p className="mt-0.5 text-xs text-ink-400">{stat.detail}</p>
        </motion.div>
      ))}
    </div>
  );
}
