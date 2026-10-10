// The admin's one date range and filters, applied to every panel.

export type Range = "today" | "7d" | "30d" | "90d" | "custom";

export type Filters = {
  range: Range;
  /** Inclusive UTC dates, `YYYY-MM-DD`; read only when `range` is custom. */
  from: string;
  to: string;
  target: string;
  preset: string;
  outcome: string;
  country: string;
  device_class: string;
  source: string;
};

export const OUTCOMES = [
  "ok",
  "download_interrupted",
  "invalid",
  "internal_error",
  "timeout",
  "abandoned_queue",
  "abandoned_running",
  "rate_limited",
] as const;

export const DEVICE_CLASSES = ["mobile", "tablet", "desktop", "unknown"] as const;

/** The API's longest window. */
const MAX_DAYS = 400;

const DAYS: Record<Exclude<Range, "custom">, number> = { today: 1, "7d": 7, "30d": 30, "90d": 90 };

function utcDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function defaultFilters(): Filters {
  return { range: "30d", from: "", to: "", target: "", preset: "", outcome: "", country: "", device_class: "", source: "" };
}

/** The inclusive window a range covers, ending today (UTC) unless it is custom. */
export function windowOf(filters: Filters, now: Date): { from: string; to: string } {
  if (filters.range === "custom") return { from: filters.from, to: filters.to };
  const to = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));
  const from = new Date(to);
  from.setUTCDate(from.getUTCDate() - (DAYS[filters.range] - 1));
  return { from: utcDate(from), to: utcDate(to) };
}

/** Why a custom window cannot be asked for, or null. The API refuses the same things. */
export function windowProblem(filters: Filters): string | null {
  if (filters.range !== "custom") return null;
  const pattern = /^\d{4}-\d{2}-\d{2}$/;
  if (!pattern.test(filters.from) || !pattern.test(filters.to)) return "Pick both dates.";
  const from = Date.parse(`${filters.from}T00:00:00Z`);
  const to = Date.parse(`${filters.to}T00:00:00Z`);
  if (from > to) return "The start is after the end.";
  if ((to - from) / 86_400_000 + 1 > MAX_DAYS) return `The window can be at most ${MAX_DAYS} days.`;
  return null;
}

/** The query string every admin read takes: the window plus whichever filters are set. */
export function query(filters: Filters, now: Date, extra: Record<string, string | number | boolean> = {}): string {
  const params = new URLSearchParams(windowOf(filters, now));
  for (const key of ["target", "preset", "outcome", "country", "device_class", "source"] as const) {
    const value = filters[key].trim();
    if (value) params.set(key, key === "country" ? value.toUpperCase() : value);
  }
  for (const [key, value] of Object.entries(extra)) params.set(key, String(value));
  return params.toString();
}
