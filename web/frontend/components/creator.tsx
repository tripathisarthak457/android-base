"use client";

/**
 * Who wrote this, and how to reach them.
 *
 * Deliberately at the bottom and deliberately small. Someone who has just downloaded a project
 * and hit a problem needs a person to ask before they need a biography, so this is three links
 * and a sentence rather than an about page.
 */

import { motion } from "framer-motion";
import { Card, press } from "./primitives";

const GITHUB_USER = "tripathisarthak457";
const EMAIL = "tripathisarthak457@gmail.com";
const LINKEDIN_USER = "ts15";

const LINKS = [
  {
    label: "GitHub",
    handle: `@${GITHUB_USER}`,
    href: `https://github.com/${GITHUB_USER}`,
    icon: (
      <path d="M8 0C3.58 0 0 3.58 0 8a8 8 0 0 0 5.47 7.59c.4.07.55-.17.55-.38l-.01-1.49c-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.4 7.4 0 0 1 4 0c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48l-.01 2.2c0 .21.15.46.55.38A8 8 0 0 0 16 8c0-4.42-3.58-8-8-8Z" />
    ),
  },
  {
    label: "LinkedIn",
    handle: `in/${LINKEDIN_USER}`,
    href: `https://www.linkedin.com/in/${LINKEDIN_USER}`,
    icon: (
      <path d="M13.6 0H2.4A2.4 2.4 0 0 0 0 2.4v11.2A2.4 2.4 0 0 0 2.4 16h11.2a2.4 2.4 0 0 0 2.4-2.4V2.4A2.4 2.4 0 0 0 13.6 0ZM5 13.2H2.8V6.2H5v7ZM3.9 5.2a1.3 1.3 0 1 1 0-2.6 1.3 1.3 0 0 1 0 2.6Zm9.3 8h-2.2V9.5c0-.9-.02-2-1.24-2-1.25 0-1.44.96-1.44 1.95v3.75H6.1v-7h2.13v.96h.03c.3-.56 1.02-1.15 2.1-1.15 2.25 0 2.66 1.48 2.66 3.4v3.79Z" />
    ),
  },
  {
    label: "Email",
    handle: EMAIL,
    href: `mailto:${EMAIL}`,
    icon: (
      <path d="M1.5 2h13A1.5 1.5 0 0 1 16 3.5v9a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 0 12.5v-9A1.5 1.5 0 0 1 1.5 2Zm.36 1.5L8 8.06 14.14 3.5H1.86ZM14.5 12.5v-7.1L8.3 9.98a.5.5 0 0 1-.6 0L1.5 5.4v7.1h13Z" />
    ),
  },
];

export function Creator() {
  return (
    <section className="border-t border-ink-800">
      <div className="mx-auto max-w-4xl px-6 py-16">
        <p className="text-xs font-medium uppercase tracking-widest text-ink-500">
          Who made this
        </p>

        <Card className="mt-4 p-6">
          <div className="flex flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
            <div className="max-w-md">
              <h2 className="text-xl font-semibold text-ink-100">Sarthak Tripathi</h2>
              <p className="mt-2 text-sm leading-relaxed text-ink-300">
                Android developer. This started as the setup work at the front of every new
                project — the modules, the variants, the design system — done once properly
                instead of badly every time. It is all open source; issues, pull requests and
                arguments about the architecture are welcome.
              </p>
            </div>

            <div className="flex shrink-0 flex-col gap-2">
              {LINKS.map((link) => (
                <motion.a
                  key={link.label}
                  href={link.href}
                  {...(link.href.startsWith("mailto:")
                    ? {}
                    : { target: "_blank", rel: "noopener noreferrer" })}
                  {...press}
                  className="flex items-center gap-2.5 rounded-lg border border-ink-700 bg-ink-850 px-3.5 py-2.5 text-sm text-ink-200 transition-colors hover:border-ink-600 hover:text-ink-100"
                >
                  <svg
                    width="15"
                    height="15"
                    viewBox="0 0 16 16"
                    fill="currentColor"
                    className="shrink-0 text-ink-400"
                    aria-hidden
                  >
                    {link.icon}
                  </svg>
                  <span className="font-medium">{link.label}</span>
                  <span className="truncate font-mono text-xs text-ink-500">{link.handle}</span>
                </motion.a>
              ))}
            </div>
          </div>
        </Card>
      </div>
    </section>
  );
}
