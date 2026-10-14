import { licensedFeatures } from "../features/catalogue/features.ts";
import { CATALOGUE, PLATFORMS, PRIVACY } from "../lib/catalogue.ts";

function licenceAnswer(): string {
  const licensed = licensedFeatures(CATALOGUE);
  const base =
    "All of it. The generator is a Python script with no dependencies, the API is one Go binary, and this site is a Next.js app.";
  if (licensed.length === 0) return `${base} Everything is MIT.`;
  const named = licensed.map((feature) => `${feature.title} (${feature.licence?.name})`).join(", ");
  return `${base} The generator and the templates are MIT; optional features that bring third-party code keep its licence: ${named}. Each project's licence screen lists them.`;
}

function platformsAnswer(): string | null {
  const others = PLATFORMS.filter((platform) => platform.key !== "android");
  if (others.length === 0) return null;
  const offered = others.filter((platform) => platform.status !== "coming_soon");
  const soon = others.filter((platform) => platform.status === "coming_soon").map((platform) => platform.title);
  const lines = offered.map(
    (platform) =>
      `${platform.title} is ${platform.status === "beta" ? "in beta" : "available"}${platform.status_note ? `: ${platform.status_note.replace(/^Beta: /, "")}` : ""}.`,
  );
  if (soon.length > 0) {
    lines.push(`${new Intl.ListFormat("en-GB", { type: "conjunction" }).format(soon)} ${soon.length === 1 ? "is" : "are"} coming soon.`);
  }
  if (offered.length === 0) return `Android, as a native Jetpack Compose project. ${lines.join(" ")}`;
  return `Android Native is the most complete. ${lines.join(" ")} The platform step shows what each one leaves out.`;
}

export function Faq() {
  const platforms = platformsAnswer();
  const questions = [
    {
      q: "Is the project it gives me actually going to build?",
      a:
        "Yes, and that is checked rather than asserted. The template in the repository is a real Gradle project, and CI " +
        "generates every preset, plus a project with every feature and one with none, and builds each of them; between them " +
        "they run debug and shrunk release builds, unit tests, Android lint and the build's own rules. A change that breaks " +
        "one does not ship.",
    },
    ...(platforms ? [{ q: "Which platforms can it make?", a: platforms }] : []),
    {
      q: "Why no Material?",
      a:
        "Material is a brand as much as a toolkit, and a design system built on it inherits both. Owning the components means " +
        "the theme is the only thing that decides how anything looks, and the press feedback can be iOS-like rather than a " +
        "ripple. Only Material's adaptive layout libraries are used, which depend on no Material component.",
    },
    {
      q: "What if I want a feature I turned off?",
      a:
        "Turning one off deletes its modules rather than commenting them out, so adding it back means generating again or " +
        "copying it from the repository. Feature modules are different: add_feature.py scaffolds a new data and feature pair " +
        "into a project that already exists, and remove_feature.py takes one back out.",
    },
    {
      q: "Can it make my signing keys?",
      a:
        "Yes, if you ask; it is off by default. Keys made here are created on a server you do not control, which is fine for " +
        "dev and staging and a real trade for a Play upload key. The CLI makes them on your own machine, and every " +
        "project's README has the keytool commands.",
    },
    {
      q: "Does it work with AI coding agents?",
      a:
        "Every project ships an AGENTS.md, and a CLAUDE.md that points at it, describing the layout, the rules the build " +
        "enforces, how a feature is shaped and how to keep the app smooth. The repository has its own, explaining how an agent " +
        "can generate a project from a JSON spec without the wizard.",
    },
    {
      q: "Which JDK do I need?",
      a: "Whatever Android Studio already has. Gradle runs on its bundled runtime or any JDK 17 or newer, and nothing downloads a second one.",
    },
    {
      q: "Do you keep anything I typed?",
      a:
        "For each project generated, the server keeps what it was generated with: the platform, features, styles, SDK levels " +
        "and timings, and the app and package name for 90 days. For each visit it keeps a country, region and city, the " +
        "browser, system and device class, a screen-size bucket, where you came from, whether you have been here before, and " +
        "a daily-rotating salted hash instead of your IP address. A report you send keeps what you wrote, the email if you " +
        "give one, your browser, system and device class, and, if you leave them attached, your configurator answers and " +
        "the page it was sent from; an error the page runs into keeps its message, stack and page path. No IP, " +
        "no user agent string, no cookies, no third-party analytics. Your browser keeps a random id for this tab in " +
        "sessionStorage, your configurator answers in localStorage once you change one, so they are here when you come " +
        "back, and your light or dark choice if you change it. Start over clears the answers. The style previews and the " +
        "typeface you pick load from Google Fonts, which sees your IP address as any font request does; the site's own " +
        "fonts are served from here.",
      link: { href: PRIVACY, label: "The full list" },
    },
    {
      q: "Can I run this myself?",
      a: licenceAnswer(),
    },
  ];

  return (
    <section className="border-t border-ink-800 bg-ink-900/40">
      <div className="mx-auto max-w-4xl px-4 py-20 sm:px-6">
        <h2 className="font-display text-4xl font-bold text-ink-100">Questions</h2>
        <div className="mt-8 divide-y divide-ink-800">
          {questions.map((item) => (
            <details key={item.q} className="group py-5">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-4 [&::-webkit-details-marker]:hidden">
                <span className="font-medium text-ink-100">{item.q}</span>
                <span className="shrink-0 text-ink-400 transition-transform group-open:rotate-45" aria-hidden>
                  +
                </span>
              </summary>
              <p className="mt-3 text-sm leading-relaxed text-ink-300">
                {item.a}
                {"link" in item && item.link && (
                  <>
                    {" "}
                    <a href={item.link.href} className="text-accent underline underline-offset-2 hover:text-accent-bright">
                      {item.link.label}
                    </a>
                    .
                  </>
                )}
              </p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}
