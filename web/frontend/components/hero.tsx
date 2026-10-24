import { ciPresets } from "../features/catalogue/ci.ts";
import { selectableFeatures } from "../features/catalogue/features.ts";
import { CATALOGUE, PLATFORMS, PRIVACY, REPO, android } from "../lib/catalogue.ts";
import { listed } from "../lib/text.ts";
import { Badge, LinkButton } from "./ui.tsx";

/**
 * The one screen a visitor decides on. It answers three questions before they scroll: what is
 * this, what do I get, and is it going to work.
 */
export function Hero() {
  const target = android();
  const flavours = CATALOGUE.rules.flavours;
  const ci = ciPresets(target);
  const ciBuilt = ci.phrase.charAt(0).toUpperCase() + ci.phrase.slice(1);
  const stats = [
    { value: String(selectableFeatures(target).length), label: "features you can switch off", detail: "Each one removes its modules, not just its code" },
    { value: String(target.design_styles.length), label: "design styles", detail: target.design_styles.map((style) => style.title).join(", ") },
    { value: String(target.motion_styles.length), label: "motion styles", detail: "Spring presets for presses, sheets and dialogs" },
    {
      value: String(flavours.length),
      label: "build environments",
      detail: `${listed(flavours)}, debug and release`,
    },
    { value: "0", label: "Material components", detail: "A foundation-only design system; the build refuses Material component imports" },
  ];

  return (
    <section className="relative overflow-hidden">
      <div className="paper-backdrop pointer-events-none absolute inset-0" aria-hidden />

      <div className="relative mx-auto max-w-6xl px-4 pb-20 pt-20 sm:px-6 md:pt-28">
        <div className="max-w-3xl animate-rise">
          <ul className="flex flex-wrap items-center gap-2" aria-label="Platforms">
            {PLATFORMS.map((platform) => (
              <li key={platform.key}>
                <Badge tone={platform.status === "available" ? "accent" : platform.status === "beta" ? "amber" : "neutral"}>
                  {platform.title}
                  {platform.status === "beta" ? " · beta" : platform.status === "coming_soon" ? " · coming soon" : ""}
                </Badge>
              </li>
            ))}
          </ul>

          <h1 className="font-display mt-5 text-5xl font-extrabold leading-[1.02] text-ink-100 md:text-7xl">
            A new Android project,
            <br />
            <span className="relative inline-block text-accent">
              already wired up.
              <svg className="absolute -bottom-2 left-0 w-full" height="10" viewBox="0 0 300 10" preserveAspectRatio="none" aria-hidden>
                <path d="M2 7c60-5 130-6 296-2" stroke="currentColor" strokeWidth="3" fill="none" strokeLinecap="round" />
              </svg>
            </span>
          </h1>

          <p className="mt-8 max-w-2xl text-lg leading-relaxed text-ink-300">
            Multi-module Jetpack Compose with MVI and Navigation 3, {flavours.length} build environments, signed release output, and
            a design system with no Material components that moves like iOS. Pick your features and a look, and get a zip that
            compiles.
          </p>

          <div className="mt-9 flex flex-wrap items-center gap-3">
            <LinkButton href="#configure" variant="primary">
              Configure your project
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden>
                <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </LinkButton>
            <LinkButton href={REPO}>
              <svg width="17" height="17" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
                <path d="M8 0C3.58 0 0 3.58 0 8a8 8 0 0 0 5.47 7.59c.4.07.55-.17.55-.38l-.01-1.49c-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.4 7.4 0 0 1 4 0c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48l-.01 2.2c0 .21.15.46.55.38A8 8 0 0 0 16 8c0-4.42-3.58-8-8-8Z" />
              </svg>
              Read the source
            </LinkButton>
          </div>

          <p className="mt-4 text-sm text-ink-400">
            No sign-up and no email. The zip is built when you click and streamed straight back. We count anonymous visits and keep
            what each project was generated with;{" "}
            <a href={PRIVACY} className="underline underline-offset-2 hover:text-ink-200">
              here is exactly what
            </a>
            .
          </p>

          <div className="mt-6 flex max-w-2xl items-start gap-3 rounded-lg border border-amber/30 bg-amber/[0.07] p-4">
            <span className="mt-0.5 text-amber" aria-hidden>
              ⚠
            </span>
            <p className="text-sm leading-relaxed text-ink-300">
              <span className="font-medium text-ink-100">This is beta.</span> {ciBuilt}, plus every feature on at
              once and none at all, {ci.plural ? "are" : "is"} generated and built in CI before a change
              ships, but not many people have used it yet. If something breaks, "Report a problem" (the flag at the top of the page)
              sends it straight to whoever can fix it, with what you configured attached.
            </p>
          </div>
        </div>

        <div className="mt-14 grid animate-rise items-start gap-4 [animation-delay:140ms] md:grid-cols-5">
          <Terminal />
          <ul className="grid gap-3 md:col-span-2">
            {stats.map((stat) => (
              <li key={stat.label} className="rounded-xl border border-ink-700 bg-ink-850 px-4 py-3">
                <div className="flex items-baseline gap-2.5">
                  <span className="font-display text-2xl font-extrabold text-accent">{stat.value}</span>
                  <span className="text-sm font-medium text-ink-200">{stat.label}</span>
                </div>
                <p className="mt-0.5 text-xs text-ink-400">{stat.detail}</p>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

/** The commands the README tells you to run, with their output. */
function Terminal() {
  const lines: { text: string; tone?: "prompt" | "dim" | "ok" }[] = [
    { text: "$ ./gradlew :app:assembleDevDebug", tone: "prompt" },
    { text: "BUILD SUCCESSFUL", tone: "ok" },
    { text: "" },
    { text: "$ ./gradlew build", tone: "prompt" },
    { text: "every variant · unit tests · android lint · code rules", tone: "dim" },
    { text: "BUILD SUCCESSFUL", tone: "ok" },
    { text: "" },
    { text: "$ ./gradlew :app:distDevRelease", tone: "prompt" },
    { text: "MyApp-devRelease-arm64-v8a-1.0.0-1-20260904-1048.apk", tone: "dim" },
    { text: "MyApp-devRelease-universal-1.0.0-1-20260904-1048.apk", tone: "dim" },
    { text: "checksums.sha256", tone: "dim" },
  ];

  return (
    // A terminal is dark in both site themes, the way a terminal on the visitor's own machine is.
    <div className="overflow-hidden rounded-xl border border-[#2c2924] bg-[#1c1a17] shadow-[0_20px_50px_-24px_rgba(40,30,15,0.5)] md:col-span-3">
      <div className="flex items-center gap-2 border-b border-[#2c2924] bg-[#23211d] px-4 py-2.5" aria-hidden>
        <span className="h-2.5 w-2.5 rounded-full bg-[#e2614a]" />
        <span className="h-2.5 w-2.5 rounded-full bg-[#e6b34e]" />
        <span className="h-2.5 w-2.5 rounded-full bg-[#62c98f]" />
        <span className="ml-2 font-mono text-xs text-[#a9a090]">MyApp — zsh</span>
      </div>
      <pre className="overflow-x-auto p-4 font-mono text-[12.5px] leading-[1.75]">
        {lines.map((line, index) => (
          <div key={index} className={line.tone === "ok" ? "text-[#62c98f]" : line.tone === "prompt" ? "text-[#f5efe4]" : "text-[#a9a090]"}>
            {line.text || " "}
          </div>
        ))}
      </pre>
    </div>
  );
}
