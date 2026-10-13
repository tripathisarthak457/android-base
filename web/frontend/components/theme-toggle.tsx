"use client";

import { useSyncExternalStore } from "react";

type ThemeChoice = "system" | "light" | "dark";

const ORDER: ThemeChoice[] = ["system", "light", "dark"];
const LABEL: Record<ThemeChoice, string> = { system: "Theme: follows your system", light: "Theme: light", dark: "Theme: dark" };

// The choice lives on <html data-theme>, where the layout's bootstrap script puts it before the
// first paint, so the toggle reads it from there rather than keeping a copy.
const listeners = new Set<() => void>();

function subscribe(listener: () => void): () => void {
  if (listeners.size === 0) window.addEventListener("storage", follow);
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
    if (listeners.size === 0) window.removeEventListener("storage", follow);
  };
}

function parseChoice(value: string | null | undefined): ThemeChoice {
  return value === "light" || value === "dark" ? value : "system";
}

function readChoice(): ThemeChoice {
  return parseChoice(document.documentElement.dataset.theme);
}

function show(choice: ThemeChoice) {
  const root = document.documentElement;
  if (choice === "system") delete root.dataset.theme;
  else root.dataset.theme = choice;
  for (const listener of listeners) listener();
}

// A choice made in another tab arrives as a storage event; a null key means storage was cleared.
function follow(event: StorageEvent) {
  if (event.key === "theme" || event.key === null) show(parseChoice(event.newValue));
}

/**
 * Light, dark, or whatever the OS says. The choice is a per-visitor convenience kept in
 * localStorage; the bootstrap script in the layout applies it before the first paint.
 */
export function ThemeToggle() {
  const choice = useSyncExternalStore(subscribe, readChoice, () => "system" as const);

  function apply(next: ThemeChoice) {
    show(next);
    try {
      if (next === "system") localStorage.removeItem("theme");
      else localStorage.setItem("theme", next);
    } catch {
      // Not remembered across visits, which is fine.
    }
  }

  const next = ORDER[(ORDER.indexOf(choice) + 1) % ORDER.length] ?? "system";

  return (
    <button
      type="button"
      onClick={() => apply(next)}
      aria-label={`${LABEL[choice]}. Switch to ${next}.`}
      title={LABEL[choice]}
      className="flex h-9 w-9 items-center justify-center rounded-lg text-ink-300 transition-colors hover:bg-ink-800 hover:text-ink-100 not-supports-[color:light-dark(#000,#fff)]:hidden"
    >
      <svg
        width="17"
        height="17"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden
      >
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
