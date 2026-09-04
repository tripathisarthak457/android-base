"use client";

import { useEffect, useRef, useState } from "react";
import { Configurator } from "../components/configurator";
import { Hero } from "../components/hero";
import { Included } from "../components/included";
import { Button, Card, Spinner } from "../components/primitives";
import { type Catalogue, fetchCatalogue, track } from "../lib/api";

const REPO = "https://github.com/tripathisarthak457/android-base";

export default function Home() {
  const [catalogue, setCatalogue] = useState<Catalogue | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const configureRef = useRef<HTMLDivElement>(null);

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
      <Hero onStart={scrollToConfigure} />
      <Included />

      <div ref={configureRef}>
        {catalogue ? (
          <Configurator catalogue={catalogue} />
        ) : (
          <section className="mx-auto max-w-6xl px-6 py-20">
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
      <Footer />
    </main>
  );
}

function Nav() {
  return (
    <nav className="sticky top-0 z-50 border-b border-ink-800 bg-ink-950/85 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-3.5">
        <a href="#" className="flex items-center gap-2.5">
          <span className="flex h-7 w-7 items-center justify-center rounded-md bg-accent font-mono text-sm font-bold text-ink-950">
            A
          </span>
          <span className="font-semibold text-ink-100">Android base</span>
        </a>
        <div className="flex items-center gap-1">
          <a
            href="#configure"
            className="rounded-lg px-3 py-2 text-sm text-ink-300 transition-colors hover:bg-ink-800 hover:text-ink-100"
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

const QUESTIONS = [
  {
    q: "Is the project it gives me actually going to build?",
    a:
      "Yes, and that is checked rather than asserted. The template in the repository is a real " +
      "Gradle project that this repository's own build compiles, tests and lints on every change " +
      "— and both extremes of the generator, everything on and everything off, are built the same " +
      "way. If a combination broke, the repository would be red before you got here.",
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
      "exists, and makes the three edits people forget.",
  },
  {
    q: "Why don't I get signing keys?",
    a:
      "Because they would have been generated on a server you do not control and sent back to you " +
      "over the wire. That is fine for a throwaway debug key and not fine for a Play upload key, " +
      "which is the one credential in Android whose loss cannot be undone. The CLI generates all " +
      "four, on your machine, with keytool.",
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
        <h2 className="text-3xl font-bold tracking-tight text-ink-100">Questions</h2>
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
