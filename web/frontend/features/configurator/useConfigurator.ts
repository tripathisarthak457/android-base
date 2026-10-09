"use client";

import { useCallback, useEffect, useMemo, useReducer, useRef, useState } from "react";
import { API_BASE, ApiError, BUILD, apiError } from "../../lib/api.ts";
import { canMakeKeystores, parseCatalogue } from "../catalogue/guard.ts";
import type { Catalogue } from "../catalogue/types.ts";
import { flushOnHide, reportErrors, sessionId, track, trackLanding } from "../telemetry/client.ts";
import { landed } from "../telemetry/events.ts";
import { DRAFT_KEY, draftToStore, parseDraft } from "./draft.ts";
import { derivePreset } from "./resolve.ts";
import { sessionHeaders, toSpecV2 } from "./request.ts";
import { type Action, type ConfigState, STEPS, type Step, findTarget, initialState, reducer } from "./state.ts";
import { type SpecErrors, validateSpec } from "./validate.ts";

/** Which step owns each spec field, so an error can send the visitor to where it is fixed. */
export function stepOf(field: string): Step {
  if (field === "platform" || field === "schema") return "platform";
  if (["app_name", "display_name", "package_name", "feature_modules", "languages"].includes(field)) return "identity";
  if (field === "features") return "features";
  if (["design_style", "accent_colour", "secondary_colour", "tertiary_colour", "font_name", "mono_font_name"].includes(field)) return "look";
  if (field === "motion_style" || field === "haptics_enabled") return "motion";
  return "build";
}

export type Outcome =
  | { kind: "done"; revision: number; filename: string; bytes: number; elapsedMs: number; keystoresSkipped: string[] }
  | { kind: "failed"; revision: number; message: string; field: string };

/** What only the server that answered can say: which targets it generates and whether it has keytool. */
export type Live = { enabled: Record<string, boolean> | null; keytool: Record<string, boolean> };

const GENERATE_TIMEOUT_MS = 120_000;

function readDraft(): string | null {
  try {
    return localStorage.getItem(DRAFT_KEY);
  } catch {
    return null;
  }
}

function writeDraft(text: string | null): void {
  try {
    if (text === null) localStorage.removeItem(DRAFT_KEY);
    else localStorage.setItem(DRAFT_KEY, text);
  } catch {
    // Not remembered, which costs the visitor a retyped name and nothing else.
  }
}

function getJson(path: string, signal: AbortSignal): Promise<unknown> {
  return fetch(`${API_BASE}${path}`, { signal }).then((response) => (response.ok ? response.json() : null));
}

/**
 * The targets this deployment generates, asked once on load (a few hundred bytes). Whether it can
 * make signing keys comes from the live catalogue, which is a hundred kilobytes, so that is asked
 * only once the visitor reaches the Build step, where the answer is shown.
 */
function useLive(catalogue: Catalogue, step: Step): Live {
  const [enabled, setEnabled] = useState<Record<string, boolean> | null>(null);
  const [keytool, setKeytool] = useState<Record<string, boolean>>({});
  const [wantsCatalogue, setWantsCatalogue] = useState(false);
  if (step === "build" && !wantsCatalogue) setWantsCatalogue(true);

  useEffect(() => {
    const controller = new AbortController();
    void getJson("/api/v1/targets", controller.signal)
      .then((value) => {
        if (!Array.isArray(value)) return;
        const answer: Record<string, boolean> = {};
        for (const entry of value as { key?: unknown; enabled?: unknown }[]) {
          if (typeof entry.key === "string") answer[entry.key] = entry.enabled === true;
        }
        if (Object.keys(answer).length > 0) setEnabled(answer);
      })
      .catch(() => undefined);
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!wantsCatalogue) return;
    const controller = new AbortController();
    void getJson("/api/v1/catalogue", controller.signal)
      .then((value) => {
        const live = parseCatalogue(value);
        setKeytool(Object.fromEntries(catalogue.targets.map((target) => [target.key, canMakeKeystores(live, target.key)])));
      })
      .catch(() => undefined);
    return () => controller.abort();
  }, [wantsCatalogue, catalogue]);

  return { enabled, keytool };
}

function filenameOf(response: Response): string {
  const match = /filename="([^"]+)"/.exec(response.headers.get("Content-Disposition") ?? "");
  return match?.[1] ?? "project.zip";
}

/**
 * Anchor-and-click rather than navigating, so the filename from Content-Disposition is used and
 * the page, with its summary, stays where it is.
 */
function save(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

export function useConfigurator(catalogue: Catalogue) {
  const [state, dispatchRaw] = useReducer(
    (current: ConfigState, action: Action) => reducer(current, action, catalogue),
    catalogue,
    (source) => initialState(source),
  );
  const [step, setStep] = useState<Step>("platform");
  const [busy, setBusy] = useState(false);
  const [outcome, setOutcome] = useState<Outcome | null>(null);
  const [restored, setRestored] = useState(false);
  const identityAt = useRef<number | null>(null);
  const live = useLive(catalogue, step);
  const target = findTarget(catalogue, state.target);

  useEffect(() => {
    const text = readDraft();
    const draft = parseDraft(text, catalogue);
    if (draft) dispatchRaw({ type: "restore", state: draft });
    setRestored(true);
    trackLanding(
      landed({
        referrer: document.referrer,
        origin: location.origin,
        search: location.search,
        returning: Boolean(text),
        width: window.innerWidth,
      }),
    );
    const stopFlushing = flushOnHide();
    const stopReporting = reportErrors();
    return () => {
      stopFlushing();
      stopReporting();
    };
  }, [catalogue]);

  useEffect(() => {
    if (!restored) return;
    const timer = setTimeout(() => writeDraft(draftToStore(state, catalogue)), 300);
    return () => clearTimeout(timer);
  }, [state, restored, catalogue]);

  const lastViewed = useRef<string | null>(null);
  useEffect(() => {
    const key = `${state.target}:${step}`;
    if (lastViewed.current === key) return;
    lastViewed.current = key;
    if (step === "identity" && identityAt.current === null) identityAt.current = performance.now();
    track({ name: "step_viewed", target: state.target, step });
  }, [step, state.target]);

  const dispatch = useCallback((action: Action) => {
    if (action.type === "preset") track({ name: "preset_chosen", target: state.target, preset: action.key });
    dispatchRaw(action);
  }, [state.target]);

  const spec = useMemo(() => toSpecV2(state, catalogue), [state, catalogue]);
  const errors: SpecErrors = useMemo(() => validateSpec(spec, catalogue, state.target), [spec, catalogue, state.target]);
  const preset = useMemo(() => derivePreset(new Set(state.features), target), [state.features, target]);

  const goTo = useCallback(
    (next: Step) => {
      if (step === "platform" && next !== "platform") track({ name: "platform_selected", target: state.target });
      setStep(next);
    },
    [step, state.target],
  );

  const startOver = useCallback(() => {
    writeDraft(null);
    dispatchRaw({ type: "reset", catalogue });
    setOutcome(null);
    setStep("platform");
  }, [catalogue]);

  const generate = useCallback(async () => {
    if (errors.size > 0 || busy) return;
    const revision = state.revision;
    setBusy(true);
    setOutcome(null);
    track({ name: "generate_clicked", target: state.target });
    const started = identityAt.current;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), GENERATE_TIMEOUT_MS);
    try {
      const response = await fetch(`${API_BASE}/api/v1/${state.target}/generate`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...sessionHeaders({ id: sessionId(), configMs: started === null ? null : performance.now() - started, build: BUILD }),
        },
        body: JSON.stringify(spec),
        signal: controller.signal,
      });
      if (!response.ok) throw await apiError(response, "The generator failed.");
      track({ name: "download_started", target: state.target });
      const blob = await response.blob();
      const filename = filenameOf(response);
      save(blob, filename);
      setOutcome({
        kind: "done",
        revision,
        filename,
        bytes: blob.size,
        elapsedMs: Number(response.headers.get("X-Generation-Ms") ?? 0),
        keystoresSkipped: (response.headers.get("X-Keystores-Skipped") ?? "").split(",").filter(Boolean),
      });
    } catch (error) {
      const message =
        error instanceof ApiError
          ? error.message
          : controller.signal.aborted
            ? "The generator took too long to answer. Try again; the command line tool does the same job offline."
            : "Could not reach the generator. It may be restarting; the command line tool in the repository does the same job.";
      setOutcome({ kind: "failed", revision, message, field: error instanceof ApiError ? error.field : "" });
    } finally {
      clearTimeout(timeout);
      setBusy(false);
    }
  }, [errors, busy, state.revision, state.target, spec]);

  const stepIndex = STEPS.indexOf(step);
  const identityValid = !["app_name", "display_name", "package_name", "feature_modules", "languages"].some((field) => errors.has(field));

  return {
    state,
    dispatch,
    target,
    step,
    stepIndex,
    goTo,
    identityValid,
    errors,
    preset,
    live,
    busy,
    // A result describes the answers it was made from; after any edit it no longer applies.
    outcome: outcome && outcome.revision === state.revision ? outcome : null,
    generate,
    startOver,
  };
}

export type Configurator = ReturnType<typeof useConfigurator>;
