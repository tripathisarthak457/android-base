"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { API_BASE, ApiError, apiError } from "../../lib/api.ts";
import { saveFile } from "../../lib/download.ts";
import { parseCatalogue } from "../catalogue/guard.ts";

// The admin API's answers (`/api/v1/admin/*`). Every one starts with its window.

export type DateWindow = { from: string; to: string };

type Outcomes = Record<string, number>;

export type Overview = DateWindow & {
  generations: number;
  outcomes: Outcomes;
  median_ms: number;
  p95_ms: number;
  visitors: number;
  sessions: number;
  returning_share: number;
  open_errors: number;
  new_feedback: number;
};

export type Funnel = DateWindow & {
  landed: number;
  targets: { target: string; steps: { key: string; sessions: number }[]; also: Record<string, number> }[];
};

export type Series = DateWindow & { days: { day: string; outcomes: Outcomes; visitors: number; sessions: number }[] };

export type Count = { key: string; n: number };

export type Adoption = DateWindow & {
  targets: {
    target: string;
    total: number;
    features: { key: string; title: string; n: number; picked: number; share: number }[];
    dimensions: Record<string, Count[]>;
    weekly: { week: string; total: number; values: Record<string, Record<string, number>> }[];
  }[];
};

export type Rate = { n: number; failed: number };

export type Failures = DateWindow & {
  rejections: { field: string; reason: string; n: number }[];
  by_feature: { key: string; on: Rate; off: Rate }[];
  pairs: { a: string; b: string; n: number; failed: number }[];
  by_min_sdk: ({ key: string } & Rate)[];
  by_preset: ({ key: string } & Rate)[];
  by_commit: ({ key: string } & Rate)[];
};

type Bucket = { key: string; country?: string; sessions: number; generated: number };

export type Audience = DateWindow & {
  sessions: number;
  buckets: Record<string, Bucket[]>;
  campaigns: { source: string; medium: string; campaign: string; sessions: number; generated: number }[];
  clients: Count[];
};

type Timing = { n: number; p50_ms: number; p95_ms: number };

export type Durations = DateWindow & {
  config: Timing;
  config_buckets: { up_to_ms: number; n: number }[];
  generation_by_feature_count: ({ bucket: string } & Timing)[];
  queue: Timing;
  download: Timing;
};

export type Generation = {
  id: number;
  at: string;
  target: string;
  outcome: string;
  app_name: string;
  package_name: string;
  preset: string;
  features: string[];
  added: string[];
  removed: string[];
  feature_modules: string[];
  architecture: string;
  design_style: string;
  motion_style: string;
  languages: string[];
  min_sdk: number | null;
  target_sdk: number | null;
  glass: boolean | null;
  duration_ms: number;
  queue_ms: number | null;
  python_ms: number | null;
  stream_ms: number | null;
  config_ms: number | null;
  zip_bytes: number | null;
  cold_start: boolean | null;
  client: string;
  country: string;
  device_class: string;
  failure_reason: string;
  failure_field: string;
  generator_commit: string;
  // What the generator reported about the spec; empty for a failed run.
  notes: string[];
  ignored_fields: string[];
  warnings: string[];
  spec: unknown;
};

export type Generations = DateWindow & { items: Generation[]; next_before: number | null };

export type ErrorGroup = {
  id: number;
  fingerprint: string;
  kind: string;
  message: string;
  detail: string;
  path: string;
  occurrences: number;
  first_seen: string;
  last_seen: string;
  resolved: boolean;
};

export type Errors = DateWindow & { items: ErrorGroup[] };

/** Errors and reports come unpaged, at most this many in the API's order; only generations page. */
export const LIST_LIMIT = 100;

export type Report = {
  id: number;
  createdAt: string;
  target: string;
  kind: string;
  severity: string;
  area: string;
  title: string;
  body: string;
  steps: string;
  expected: string;
  actual: string;
  appName: string;
  packageName: string;
  features: string[];
  preset: string;
  minSdk: number;
  motionStyle: string;
  fontName: string;
  accentColour: string;
  contact: string;
  userAgent: string;
  pageUrl: string;
  appVersion: string;
  status: string;
  notes: string;
};

export type Reports = DateWindow & { items: Report[]; counts: { new: number; blocking: number; total: number; last7Days: number } };

export type Health = DateWindow & {
  routes: { route: string; requests: number; error_rate: number; median_ms: number; p95_ms: number }[];
  database: string;
  telemetry?: { queued: number; written: number; dropped: number; failed: number };
  uptime_s: number;
  targets: string[];
};

function headers(token: string): HeadersInit {
  return { Authorization: `Bearer ${token}` };
}

export async function adminGet<T>(path: string, token: string, query: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`${API_BASE}/api/v1/admin/${path}?${query}`, { headers: headers(token), signal });
  if (!response.ok) throw await apiError(response, `The API answered ${response.status}.`);
  return (await response.json()) as T;
}

export async function adminPost(path: string, token: string, body: unknown): Promise<void> {
  const response = await fetch(`${API_BASE}/api/v1/admin/${path}`, {
    method: "POST",
    headers: { ...headers(token), "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw await apiError(response, `The API answered ${response.status}.`);
}

/** A CSV export of the current filter, fetched with the token and saved under its own name. */
export async function downloadCsv(token: string, query: string): Promise<void> {
  const response = await fetch(`${API_BASE}/api/v1/admin/generations.csv?${query}`, { headers: headers(token) });
  if (!response.ok) throw await apiError(response, `The API answered ${response.status}.`);
  const range = new URLSearchParams(query);
  saveFile(await response.blob(), `generations-${range.get("from")}-${range.get("to")}.csv`);
}

// What a failed fetch says instead of the browser's own "Failed to fetch": the request got no answer.
const UNREACHABLE = "Could not reach the API.";

export type Query<T> = { data: T | null; error: ApiError | Error | null; loading: boolean };

/**
 * One admin read, refetched when its query or `version` changes. A failed refetch keeps the last
 * data on screen next to the error; only a 401 is passed up, because only a 401 means signed out.
 */
export function useAdminQuery<T>(path: string, token: string, query: string, version: number, onUnauthorized: () => void): Query<T> {
  const [state, setState] = useState<Query<T>>({ data: null, error: null, loading: true });
  useEffect(() => {
    const controller = new AbortController();
    setState((current) => ({ ...current, loading: true }));
    adminGet<T>(path, token, query, controller.signal)
      .then((data) => setState({ data, error: null, loading: false }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        if (error instanceof ApiError && error.status === 401) onUnauthorized();
        setState((current) => ({ ...current, error: error instanceof ApiError ? error : new Error(UNREACHABLE), loading: false }));
      });
    return () => controller.abort();
  }, [path, token, query, version, onUnauthorized]);
  return state;
}

type AdminAction = { busy: boolean; problem: string | null; run: (work: () => Promise<void>) => Promise<boolean> };

/**
 * One admin write or export at a time: a second `run` while one is in flight does nothing, and
 * `busy` lets the panel disable its controls meanwhile. A failure is kept to show beside the
 * panel, except a 401, which signs out, as it does for a read.
 */
export function useAdminAction(onUnauthorized: () => void): AdminAction {
  const [busy, setBusy] = useState(false);
  const [problem, setProblem] = useState<string | null>(null);
  const running = useRef(false);
  const run = useCallback(
    async (work: () => Promise<void>) => {
      if (running.current) return false;
      running.current = true;
      setBusy(true);
      setProblem(null);
      try {
        await work();
        return true;
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) onUnauthorized();
        else setProblem(error instanceof ApiError ? error.message : UNREACHABLE);
        return false;
      } finally {
        running.current = false;
        setBusy(false);
      }
    },
    [onUnauthorized],
  );
  return { busy, problem, run };
}

type FilterChoices = { targets: string[]; presets: string[] };

/**
 * The targets and presets to filter by, from the catalogue of the API being administered: its
 * generations were made with that one, and bundling the checked-in copy would add the whole
 * catalogue to this page for two lists of keys.
 */
export function useFilterChoices(): FilterChoices {
  const [choices, setChoices] = useState<FilterChoices>({ targets: [], presets: [] });
  useEffect(() => {
    const controller = new AbortController();
    fetch(`${API_BASE}/api/v1/catalogue`, { signal: controller.signal })
      .then((response) => (response.ok ? response.json() : null))
      .then((value: unknown) => {
        const catalogue = parseCatalogue(value);
        if (!catalogue) return;
        setChoices({
          targets: catalogue.targets.map((target) => target.key),
          presets: [...new Set(catalogue.targets.flatMap((target) => target.presets.map((preset) => preset.key)))],
        });
      })
      .catch(() => undefined);
    return () => controller.abort();
  }, []);
  return choices;
}
