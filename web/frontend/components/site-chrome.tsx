import { licensedFeatures } from "../features/catalogue/features.ts";
import { ReportButton } from "../features/feedback/Feedback.tsx";
import { CATALOGUE, PRIVACY, REPO } from "../lib/catalogue.ts";
import { ThemeToggle } from "./theme-toggle.tsx";
import { Badge } from "./ui.tsx";

const NAV_LINK = "rounded-lg px-3 py-2 text-sm text-ink-300 transition-colors hover:bg-ink-800 hover:text-ink-100";

/** The GitHub mark from Octicons. */
function GitHubMark() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
      <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.06-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
    </svg>
  );
}

/**
 * Below `md` the actions are icons with their names kept for assistive tech, and the Beta badge
 * gives way on the narrowest phones: at 320 px the full row would scroll the page sideways. The
 * wordmark truncates rather than overflowing if a larger default font size still makes it too wide.
 */
export function Nav() {
  return (
    <nav aria-label="Site" className="sticky top-0 z-50 border-b border-ink-800 bg-ink-950">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 py-3.5 sm:px-6">
        <a href="#" className="flex min-w-0 items-center gap-2.5">
          <span aria-hidden className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-ink-100 font-display text-sm font-bold text-ink-950">
            a
          </span>
          <span className="truncate font-display font-bold text-ink-100">Android base</span>
          <span title="Everything works and is tested, but not by many people yet." className="shrink-0 max-[22.5rem]:hidden">
            <Badge tone="amber">Beta</Badge>
          </span>
        </a>
        <div className="flex shrink-0 items-center gap-1">
          <ReportButton />
          <ThemeToggle />
          <a href="#configure" className={`hidden sm:inline-block ${NAV_LINK}`}>
            Configure
          </a>
          <a
            href={REPO}
            target="_blank"
            rel="noopener noreferrer"
            title="GitHub"
            className={`flex h-9 items-center gap-2 max-md:w-9 max-md:justify-center max-md:px-0 ${NAV_LINK}`}
          >
            <GitHubMark />
            <span className="max-md:sr-only">GitHub</span>
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
