import { licensedFeatures } from "../features/catalogue/features.ts";
import { ReportButton } from "../features/feedback/Feedback.tsx";
import { CATALOGUE, PRIVACY, REPO } from "../lib/catalogue.ts";
import { ThemeToggle } from "./theme-toggle.tsx";
import { Badge } from "./ui.tsx";

const NAV_LINK = "rounded-lg px-3 py-2 text-sm text-ink-300 transition-colors hover:bg-ink-800 hover:text-ink-100";

export function Nav() {
  return (
    <nav aria-label="Site" className="sticky top-0 z-50 border-b border-ink-800 bg-ink-950/85 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3.5 sm:px-6">
        <a href="#" className="flex items-center gap-2.5">
          <span aria-hidden className="flex h-7 w-7 items-center justify-center rounded-md bg-ink-100 font-display text-sm font-bold text-ink-950">
            a
          </span>
          <span className="whitespace-nowrap font-display font-bold text-ink-100">Android base</span>
          <span title="Everything works and is tested, but not by many people yet.">
            <Badge tone="amber">Beta</Badge>
          </span>
        </a>
        <div className="flex items-center gap-1">
          <ReportButton />
          <ThemeToggle />
          <a href="#configure" className={`hidden sm:inline-block ${NAV_LINK}`}>
            Configure
          </a>
          <a href={REPO} target="_blank" rel="noopener noreferrer" className={NAV_LINK}>
            GitHub
          </a>
        </div>
      </div>
    </nav>
  );
}

export function Footer() {
  const licence = licensedFeatures(CATALOGUE).length === 0 ? "MIT licensed." : "MIT licensed, except the third-party code some optional features bring.";
  const links = [
    { href: "https://github.com/tripathisarthak457", label: "@tripathisarthak457" },
    { href: REPO, label: "Source" },
    { href: `${REPO}/issues`, label: "Issues" },
    { href: `${REPO}#readme`, label: "Docs" },
    { href: PRIVACY, label: "Privacy" },
  ];
  return (
    <footer className="border-t border-ink-800">
      <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-10 sm:flex-row sm:items-center sm:justify-between sm:px-6">
        <p className="text-sm text-ink-400">{licence} Built because setting this up by hand every month stopped being funny.</p>
        <ul className="flex flex-wrap gap-x-5 gap-y-2 text-sm">
          {links.map((link) => (
            <li key={link.label}>
              <a href={link.href} target="_blank" rel="noopener noreferrer" className="text-ink-300 transition-colors hover:text-ink-100">
                {link.label}
              </a>
            </li>
          ))}
        </ul>
      </div>
    </footer>
  );
}
