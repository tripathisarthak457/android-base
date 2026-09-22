"use client";

import { useEffect, useState } from "react";

/**
 * Loads a Google Fonts family into the page, once, and reports whether Google knows it.
 *
 * The same service the generated app downloads its typeface from, so a family that fails here
 * would also fail on the phone — where it degrades quietly to the system font. Better to find out
 * while the name can still be corrected.
 */
const requested = new Map<string, Promise<boolean>>();

export function loadGoogleFont(family: string): Promise<boolean> {
  const name = family.trim();
  if (!name) return Promise.resolve(false);
  const known = requested.get(name);
  if (known) return known;

  const promise = new Promise<boolean>((resolve) => {
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = `https://fonts.googleapis.com/css2?family=${encodeURIComponent(name).replace(/%20/g, "+")}:wght@400;600;700&display=swap`;
    link.onload = () => resolve(true);
    // Google answers an unknown family with a 400, which lands here.
    link.onerror = () => resolve(false);
    document.head.appendChild(link);
  });
  requested.set(name, promise);
  return promise;
}

type Status = "checking" | "ok" | "missing";

export function FontPicker({
  value,
  onChange,
  suggestions,
}: {
  value: string;
  onChange: (next: string) => void;
  suggestions: string[];
}) {
  const [status, setStatus] = useState<Status>("checking");

  useEffect(() => {
    suggestions.forEach((family) => void loadGoogleFont(family));
  }, [suggestions]);

  // Debounced, so typing "Plus Jakarta Sans" does not request sixteen families on the way.
  useEffect(() => {
    let current = true;
    setStatus("checking");
    const timer = setTimeout(() => {
      void loadGoogleFont(value).then((ok) => {
        if (current) setStatus(ok ? "ok" : "missing");
      });
    }, 450);
    return () => {
      current = false;
      clearTimeout(timer);
    };
  }, [value]);

  const family = `"${value}", var(--font-sans)`;

  return (
    <div>
      <label className="block">
        <span className="mb-1.5 block text-sm font-medium text-ink-200">Typeface</span>
        <input
          value={value}
          onChange={(event) => onChange(event.target.value)}
          spellCheck={false}
          className={`h-11 w-full rounded-lg border bg-ink-900 px-3.5 text-[15px] text-ink-100 transition-colors focus:border-accent focus:outline-none ${
            status === "missing" ? "border-rose" : "border-ink-600 hover:border-ink-500"
          }`}
        />
      </label>
      <p className={`mt-1.5 text-xs ${status === "missing" ? "text-rose" : "text-ink-400"}`}>
        {status === "missing"
          ? "Google Fonts does not know this family. Check the spelling on fonts.google.com — the app would fall back to the system font."
          : "Any family from fonts.google.com, spelled as it is on the family's page. Written into the one constant every text style reads."}
      </p>

      <div className="mt-3 flex flex-wrap gap-1.5">
        {suggestions.map((suggestion) => (
          <button
            key={suggestion}
            type="button"
            onClick={() => onChange(suggestion)}
            style={{ fontFamily: `"${suggestion}", var(--font-sans)` }}
            className={`rounded-full border px-3 py-1 text-[13px] transition-colors ${
              value === suggestion
                ? "border-accent bg-accent-dim text-accent-bright"
                : "border-ink-600 text-ink-300 hover:border-ink-500 hover:text-ink-100"
            }`}
          >
            {suggestion}
          </button>
        ))}
      </div>

      {/* The sizes and weights the template's type scale actually uses. */}
      <div
        className="mt-4 overflow-hidden rounded-xl border border-ink-700 bg-ink-900 p-5"
        style={{ fontFamily: family }}
      >
        <div className="flex items-end justify-between gap-4">
          <span className="text-[44px] font-bold leading-none text-ink-100">Aa</span>
          <span className="font-mono text-[11px] text-ink-400">
            {status === "checking" ? "loading…" : status === "ok" ? value : "not found"}
          </span>
        </div>
        <p className="mt-4 text-[24px] font-bold leading-tight tracking-tight text-ink-100">
          Welcome back
        </p>
        <p className="mt-1 text-[16px] font-semibold text-ink-200">Sunt aut facere repellat</p>
        <p className="mt-1 text-[14px] leading-relaxed text-ink-300">
          Quia et suscipit recusandae consequuntur expedita et cum reprehenderit molestiae.
        </p>
        <p className="mt-3 text-[11px] font-semibold uppercase tracking-[0.08em] text-ink-400">
          0123456789 · Settings · Sign in
        </p>
      </div>
    </div>
  );
}
