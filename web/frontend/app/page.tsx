"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Configurator } from "../components/configurator";
import { Creator } from "../components/creator";
import { BetaBadge, FeedbackButton, type ReportContext } from "../components/feedback";
import { Hero } from "../components/hero";
import { Included } from "../components/included";
import { Button, Card, Spinner } from "../components/primitives";
import { StylesShowcase } from "../components/styles-showcase";
import { type Catalogue, fetchCatalogue, track } from "../lib/api";

const REPO = "https://github.com/tripathisarthak457/android-base";

export default function Home() {
  const [catalogue, setCatalogue] = useState<Catalogue | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const [reportContext, setReportContext] = useState<ReportContext>({});
  const configureRef = useRef<HTMLDivElement>(null);

  // Stable, so publishing it from the configurator's effect does not loop.
  const onContextChange = useCallback((next: ReportContext) => setReportContext(next), []);

  useEffect(() => {
    const controller = new AbortController();

    fetchCatalogue(controller.signal)
      .then(setCatalogue)
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") return;
        setFailure(
          "Could not reach the generator API. Everything on this page is also a command line " +
            "tool — the repository has it, and it needs nothing but Python.",
        );
      });

    track("landed");
    return () => controller.abort();
  }, []);

  function scrollToConfigure() {
    configureRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  return (
    <main className="min-h-screen">
      <Nav />
      <Hero onStart={scrollToConfigure} featureCount={catalogue?.features.length} />
      <StylesShowcase />
      <Included />

      <div ref={configureRef}>
        {catalogue ? (
          <Configurator catalogue={catalogue} onContextChange={onContextChange} />
        ) : (
          <section id="configure" className="mx-auto max-w-6xl px-6 py-20">
            <Card className="p-10 text-center">
              {failure ? (
                <>
                  <p className="text-ink-200">{failure}</p>
                  <Button
                    variant="secondary"
                    className="mt-5"
                    onClick={() => window.open(REPO, "_blank", "noopener")}
                  >
                    Get the CLI instead
                  </Button>
                </>
              ) : (
                <p className="flex items-center justify-center gap-2.5 text-ink-400">
                  <Spinner /> Loading the options from the generator…
                </p>
              )}
            </Card>
          </section>
        )}
      </div>

      <Faq />
      <Creator />
      <Footer />
      <FeedbackButton context={reportContext} />
    </main>
  );
}

function Nav() {
  return (
    <nav className="sticky top-0 z-50 border-b border-ink-800 bg-ink-950/85 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-3.5">
        <a href="#" className="flex items-center gap-2.5">
          <span className="flex h-7 w-7 items-center justify-center rounded-md bg-ink-100 font-display text-sm font-bold text-ink-950">
            a
          </span>
          <span className="whitespace-nowrap font-display font-bold text-ink-100">Android base</span>
          <BetaBadge />
        </a>
        <div className="flex items-center gap-1">
          <ThemeToggle />
          <a
            href="#configure"
            className="hidden rounded-lg px-3 py-2 text-sm text-ink-300 transition-colors hover:bg-ink-800 hover:text-ink-100 sm:inline-block"
          >
            Configure
          </a>
          <a
            href={REPO}
            target="_blank"
            rel="noopener noreferrer"
            className="rounded-lg px-3 py-2 text-sm text-ink-300 transition-colors hover:bg-ink-800 hover:text-ink-100"
          >
            GitHub
          </a>
        </div>
      </div>
    </nav>
  );
}

type ThemeChoice = "system" | "light" | "dark";

/**
 * Light, dark, or whatever the OS says. The choice is a per-visitor convenience, so it lives in
 * localStorage; the bootstrap script in the layout applies it before the first paint.
 */
function ThemeToggle() {
  const [choice, setChoice] = useState<ThemeChoice>("system");

  useEffect(() => {
    try {
      const saved = localStorage.getItem("theme");
      if (saved === "light" || saved === "dark") setChoice(saved);
    } catch {
      // Storage blocked: stay on the OS setting.
    }
  }, []);

  function apply(next: ThemeChoice) {
    setChoice(next);
    const root = document.documentElement;
    if (next === "system") delete root.dataset.theme;
    else root.dataset.theme = next;
    try {
      if (next === "system") localStorage.removeItem("theme");
      else localStorage.setItem("theme", next);
    } catch {
      // Not remembered, which is fine.
    }
  }

  const order: ThemeChoice[] = ["system", "light", "dark"];
  const next = order[(order.indexOf(choice) + 1) % order.length];
  const label = { system: "Theme: follows your system", light: "Theme: light", dark: "Theme: dark" }[choice];

  return (
    <button
      type="button"
      onClick={() => apply(next)}
      aria-label={`${label}. Switch to ${next}.`}
      title={label}
      className="flex h-9 w-9 items-center justify-center rounded-lg text-ink-300 transition-colors hover:bg-ink-800 hover:text-ink-100"
    >
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
        {choice === "dark" ? (
          <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
        ) : choice === "light" ? (
          <>
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
          </>
        ) : (
          <>
            <circle cx="12" cy="12" r="9" />
            <path d="M12 3v18" />
            <path d="M12 3a9 9 0 0 1 0 18z" fill="currentColor" />
          </>
        )}
      </svg>
    </button>
  );
}

const QUESTIONS = [
  {
    q: "Is the project it gives me actually going to build?",
    a:
      "Yes, and that is checked rather than asserted. The template in the repository is a real " +
      "Gradle project, and on every change CI generates four projects — every feature on, every " +
      "feature off, and the lean and everything presets — and runs ./gradlew build on each: all " +
      "seven variants, the unit tests and Android lint. If one of those broke, the site would not " +
      "have been deployed.",
  },
  {
    q: "Why no Material?",
    a:
      "Material is a brand as much as a toolkit, and a design system built on it inherits both. " +
      "Owning the components means the theme is the only thing that decides how anything looks: " +
      "one hex changes every accent, one string changes every text style, one enum changes how " +
      "every control answers a finger. An androidx.compose.material import fails the build, so it " +
      "cannot creep back in.",
  },
  {
    q: "What if I want a feature I turned off?",
    a:
      "Turning one off deletes its module rather than commenting it out, so adding it back means " +
      "generating again or copying the module from the repository. Feature modules are different " +
      "— add_feature.py scaffolds a new :data: and :feature: pair into a project that already " +
      "exists and makes the three edits people forget, and remove_feature.py takes one back out " +
      "again, including those same three edits.",
  },
  {
    q: "Can it make my signing keys?",
    a:
      "Yes, if you ask — it is off by default. Keys made here are created on a server you do not " +
      "control and sent back over the wire, which is fine for dev and staging and a real trade for " +
      "a Play upload key, the one credential in Android whose loss cannot be undone. The Build step " +
      "spells that out before you turn it on. The CLI makes all four on your own machine, and the " +
      "README in every project has the keytool commands.",
  },
  {
    q: "Do I have to use the components as they are?",
    a:
      "No — they are a starting point. Pick the design style closest to your app, then change the " +
      "tokens and the components themselves until it looks like yours. They are ordinary Compose " +
      "in your own module, not a library you have to wrap.",
  },
  {
    q: "Does it work with AI coding agents?",
    a:
      "Every project ships an AGENTS.md (and a CLAUDE.md that points at it) describing the layout, " +
      "the rules the build enforces, how a feature is shaped, and how to write code and comments " +
      "the way the rest of the project does. The repository has its own, explaining how an agent " +
      "can generate a project from a JSON spec without the wizard.",
  },
  {
    q: "Which JDK do I need?",
    a:
      "Whatever Android Studio already has. Gradle runs on its bundled JetBrains Runtime or any JDK " +
      "17 or newer, and nothing downloads a second one. The code is compiled for Java 17.",
  },
  {
    q: "Do you keep anything I typed?",
    a:
      "The app name, package, and which features you picked, so the popular ones can be made " +
      "defaults and the unpopular ones deleted. No IP address is stored — visitor counts use a " +
      "salted hash, and rotating the salt forgets who visited without losing the numbers. No " +
      "cookies, no third-party analytics, and the generated project is never written anywhere but " +
      "a temporary directory that is deleted as the download finishes.",
  },
  {
    q: "Can I run this myself?",
    a:
      "All of it. The generator is a Python script with no dependencies, the API is one Go binary, " +
      "and this site is a Next.js app. Everything is MIT.",
  },
];

function Faq() {
  return (
    <section className="border-t border-ink-800 bg-ink-900/40">
      <div className="mx-auto max-w-4xl px-6 py-20">
        <h2 className="font-display text-4xl font-bold text-ink-100">Questions</h2>
        <div className="mt-8 divide-y divide-ink-800">
          {QUESTIONS.map((item) => (
            <details key={item.q} className="group py-5">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-4">
                <span className="font-medium text-ink-100">{item.q}</span>
                <span className="shrink-0 text-ink-500 transition-transform group-open:rotate-45">
                  +
                </span>
              </summary>
              <p className="mt-3 text-sm leading-relaxed text-ink-300">{item.a}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}

function Footer() {
  return (
    <footer className="border-t border-ink-800">
      <div className="mx-auto flex max-w-6xl flex-col gap-4 px-6 py-10 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-ink-400">
          MIT licensed. Built because setting this up by hand every month stopped being funny.
        </p>
        <div className="flex gap-5 text-sm">
          <a
            href="https://github.com/tripathisarthak457"
            target="_blank"
            rel="noopener noreferrer"
            className="text-ink-300 transition-colors hover:text-ink-100"
          >
            @tripathisarthak457
          </a>
          <a
            href={REPO}
            target="_blank"
            rel="noopener noreferrer"
            className="text-ink-300 transition-colors hover:text-ink-100"
          >
            Source
          </a>
          <a
            href={`${REPO}/issues`}
            target="_blank"
            rel="noopener noreferrer"
            className="text-ink-300 transition-colors hover:text-ink-100"
          >
            Report a bug
          </a>
          <a
            href={`${REPO}#readme`}
            target="_blank"
            rel="noopener noreferrer"
            className="text-ink-300 transition-colors hover:text-ink-100"
          >
            Docs
          </a>
        </div>
      </div>
    </footer>
  );
}
