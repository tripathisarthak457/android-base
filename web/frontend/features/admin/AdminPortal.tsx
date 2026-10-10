"use client";

import { useCallback, useEffect, useState } from "react";
import { Button, Field, TextInput } from "../../components/primitives.tsx";
import { Spinner } from "../../components/ui.tsx";
import { ApiError } from "../../lib/api.ts";
import { adminGet, useFilterChoices } from "./api.ts";
import { DEVICE_CLASSES, type Filters, OUTCOMES, type Range, defaultFilters, query, windowProblem } from "./filters.ts";
import { Adoption } from "./panels/Adoption.tsx";
import { Audience, Durations } from "./panels/Audience.tsx";
import { Errors, Failures } from "./panels/Failures.tsx";
import { Generations } from "./panels/Generations.tsx";
import { Daily, Funnel, Headline } from "./panels/Overview.tsx";
import { Health, Reports } from "./panels/Reports.tsx";

/** The token lives for the tab only: sessionStorage, never localStorage. */
const TOKEN_KEY = "android-base.admin-token";

const CONTROL = "h-8 rounded-md border border-ink-600 bg-ink-900 px-2 text-xs text-ink-100 placeholder:text-ink-500";

const RANGES: { value: Range; label: string }[] = [
  { value: "today", label: "Today" },
  { value: "7d", label: "7 days" },
  { value: "30d", label: "30 days" },
  { value: "90d", label: "90 days" },
  { value: "custom", label: "Custom" },
];

function readToken(): string {
  try {
    return sessionStorage.getItem(TOKEN_KEY) ?? "";
  } catch {
    return "";
  }
}

function writeToken(token: string | null): void {
  try {
    if (token === null) sessionStorage.removeItem(TOKEN_KEY);
    else sessionStorage.setItem(TOKEN_KEY, token);
  } catch {
    // The token then lasts as long as the page.
  }
}

export function AdminPortal() {
  const [token, setToken] = useState<string | null>(null);
  const [checked, setChecked] = useState(false);
  useEffect(() => {
    const saved = readToken();
    if (saved) setToken(saved);
    setChecked(true);
  }, []);

  const signOut = useCallback(() => {
    writeToken(null);
    setToken(null);
  }, []);

  if (!checked) return null;
  return token ? <Dashboard token={token} onSignOut={signOut} /> : <SignIn onSignedIn={setToken} />;
}

function SignIn({ onSignedIn }: { onSignedIn: (token: string) => void }) {
  const [value, setValue] = useState("");
  const [busy, setBusy] = useState(false);
  const [problem, setProblem] = useState<string>();

  async function submit() {
    setBusy(true);
    setProblem(undefined);
    try {
      await adminGet("health", value, "");
      writeToken(value);
      onSignedIn(value);
    } catch (error) {
      setProblem(
        error instanceof ApiError
          ? error.status === 401
            ? "That token was not accepted."
            : error.message
          : "Could not reach the API. Its admin routes exist only when DATABASE_URL is set.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <form
        className="w-full max-w-sm rounded-xl border border-ink-700 bg-ink-850 p-7"
        onSubmit={(event) => {
          event.preventDefault();
          void submit();
        }}
      >
        <h1 className="text-lg font-semibold text-ink-100">Admin</h1>
        <p className="mt-1.5 text-sm text-ink-400">
          The token is <code className="text-ink-300">ADMIN_TOKEN</code> from the API&apos;s environment.
        </p>
        <div className="mt-5">
          <Field label="Token" error={problem}>
            {(control) => (
              <TextInput
                {...control}
                type="password"
                value={value}
                onChange={(event) => setValue(event.target.value)}
                autoComplete="current-password"
                autoFocus
              />
            )}
          </Field>
        </div>
        <Button type="submit" className="mt-4 w-full" disabled={busy || value.length < 8}>
          {busy ? <Spinner /> : "Sign in"}
        </Button>
      </form>
    </main>
  );
}

function Dashboard({ token, onSignOut }: { token: string; onSignOut: () => void }) {
  const [filters, setFilters] = useState<Filters>(defaultFilters);
  const [applied, setApplied] = useState<Filters>(defaultFilters);
  // The window ends on the day of the last Apply or Refresh, so a panel does not refetch while a
  // date is half typed, and a tab left open past midnight UTC moves "Today" on when refreshed.
  const [asOf, setAsOf] = useState(() => new Date());
  const [version, setVersion] = useState(0);
  const problem = windowProblem(filters);
  const props = { token, query: query(applied, asOf), version, onUnauthorized: onSignOut };
  const set = (patch: Partial<Filters>) => setFilters((current) => ({ ...current, ...patch }));
  const { targets, presets } = useFilterChoices();

  function refresh() {
    setAsOf(new Date());
    setVersion((current) => current + 1);
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-ink-100">Generator admin</h1>
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="small" onClick={refresh}>
            Refresh
          </Button>
          <Button variant="ghost" size="small" onClick={onSignOut}>
            Sign out
          </Button>
        </div>
      </header>

      <form
        aria-label="Filters"
        className="sticky top-0 z-10 mt-6 flex flex-wrap items-end gap-3 rounded-xl border border-ink-700 bg-ink-900/95 p-4 backdrop-blur"
        onSubmit={(event) => {
          event.preventDefault();
          if (problem) return;
          setApplied(filters);
          setAsOf(new Date());
        }}
      >
        <fieldset className="flex flex-wrap gap-1">
          <legend className="sr-only">Window</legend>
          {RANGES.map((range) => (
            <label
              key={range.value}
              className={`cursor-pointer rounded-md px-2.5 py-1.5 text-xs font-medium has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent ${
                filters.range === range.value ? "bg-ink-700 text-ink-100" : "text-ink-300 hover:bg-ink-800"
              }`}
            >
              <input type="radio" name="range" className="sr-only" checked={filters.range === range.value} onChange={() => set({ range: range.value })} />
              {range.label}
            </label>
          ))}
        </fieldset>
        {filters.range === "custom" && (
          <>
            <input type="date" aria-label="From" value={filters.from} onChange={(event) => set({ from: event.target.value })} className={CONTROL} />
            <input type="date" aria-label="To" value={filters.to} onChange={(event) => set({ to: event.target.value })} className={CONTROL} />
          </>
        )}
        <Select label="Target" value={filters.target} options={targets} onChange={(value) => set({ target: value })} />
        <Select label="Preset" value={filters.preset} options={presets} onChange={(value) => set({ preset: value })} />
        <Select label="Outcome" value={filters.outcome} options={[...OUTCOMES]} onChange={(value) => set({ outcome: value })} />
        <Select label="Device" value={filters.device_class} options={[...DEVICE_CLASSES]} onChange={(value) => set({ device_class: value })} />
        <input
          aria-label="Country"
          placeholder="Country"
          maxLength={2}
          value={filters.country}
          onChange={(event) => set({ country: event.target.value })}
          className={`${CONTROL} w-20 uppercase placeholder:normal-case`}
        />
        <input
          aria-label="Source"
          placeholder="Source"
          value={filters.source}
          onChange={(event) => set({ source: event.target.value })}
          className={`${CONTROL} w-32`}
        />
        <Button type="submit" size="small" disabled={Boolean(problem)}>
          Apply
        </Button>
        {problem && <p className="w-full text-xs text-rose">{problem}</p>}
      </form>

      <div className="mt-6 space-y-6">
        <Headline {...props} />
        <Funnel {...props} />
        <Daily {...props} />
        <Adoption {...props} />
        <Failures {...props} />
        <Audience {...props} />
        <Durations {...props} />
        <Generations {...props} />
        <div className="grid gap-6 xl:grid-cols-2">
          <Reports {...props} />
          <Errors {...props} />
        </div>
        <Health {...props} />
      </div>
    </main>
  );
}

function Select({ label, value, options, onChange }: { label: string; value: string; options: string[]; onChange: (value: string) => void }) {
  return (
    <select aria-label={label} value={value} onChange={(event) => onChange(event.target.value)} className={CONTROL}>
      <option value="">{`Any ${label.toLowerCase()}`}</option>
      {options.map((option) => (
        <option key={option} value={option}>
          {option}
        </option>
      ))}
    </select>
  );
}
