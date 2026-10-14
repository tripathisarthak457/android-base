"use client";

import { useCallback, useEffect, useMemo, useReducer, useRef, useState } from "react";
import { API_BASE, ApiError, BUILD, apiError } from "../../lib/api.ts";
import { canMakeKeystores, parseCatalogue } from "../catalogue/guard.ts";
import type { Catalogue } from "../catalogue/types.ts";
import { sessionId, track, trackLanding } from "../telemetry/client.ts";
import { landed } from "../telemetry/events.ts";
import { DRAFT_KEY, draftToStore, parseDraft } from "./draft.ts";
import { derivePreset } from "./resolve.ts";
import { sessionHeaders, toSpecV2 } from "./request.ts";
import {
  type Action,
  type ConfigState,
  IDENTITY_FIELDS,
  STEPS,
  type Step,
  enabledTarget,
  findTarget,
  initialState,
  reducer,
  snapshotForUndo,
  type StartOverUndo,
} from "./state.ts";
import { type SpecErrors, validateSpec } from "./validation/validate.ts";

export type Outcome =
  | { kind: "done"; revision: number; filename: string; bytes: number; elapsedMs: number | null; keystoresSkipped: string[] }
  | { kind: "failed"; revision: number; message: string; field: string };

/** What only the server that answered can say: which targets it generates and whether it has keytool. */
type Live = { enabled: Record<string, boolean> | null; keytool: Record<string, boolean> };

const GENERATE_TIMEOUT_MS = 120_000;

/**
 * The zip's own budget once the generator has answered, so a slow download is not blamed on the
 * generator and a stalled one does not leave the button busy for good.
 */
const DOWNLOAD_TIMEOUT_MS = 120_000;

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
 * The targets this deployment generates, a small answer asked once on load. Whether it can make
 * signing keys comes from the live catalogue, which is large, so that is asked only once the
 * visitor reaches the Build step, where the answer is shown.
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

/** The generator's time from `X-Generation-Ms`, or null when the answer carries no whole number of milliseconds. */
function generationMs(response: Response): number | null {
  const header = response.headers.get("X-Generation-Ms") ?? "";
  return /^\d+$/.test(header) ? Number(header) : null;
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
  // A restored draft for a target this server has turned off moves to one it generates, rather than
  // leaving the visitor on a card they cannot select.
  const usable = enabledTarget(catalogue, state.target, live.enabled);
  if (usable !== state.target) dispatchRaw({ type: "target", target: usable });
  // Signing turned on before the live catalogue said this server has no keytool goes off again;
  // left on, the spec would still ask for keys and Review would say they are made here.
  if (state.signing.enabled && live.keytool[state.target] === false) {
    dispatchRaw({ type: "signing", value: { enabled: false, passwords: {} } });
  }
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
  }, [catalogue]);

  // The write waits for a pause in typing. A tab closed or left within that wait writes at once,
  // since hiding is the last moment a browser reliably runs script.
  useEffect(() => {
    if (!restored) return;
    let pending = true;
    const write = () => {
      if (!pending) return;
      pending = false;
      writeDraft(draftToStore(state, catalogue));
    };
    const writeIfHidden = () => {
      if (document.visibilityState === "hidden") write();
    };
    const timer = setTimeout(write, 300);
    document.addEventListener("visibilitychange", writeIfHidden);
    window.addEventListener("pagehide", write);
    return () => {
      clearTimeout(timer);
      document.removeEventListener("visibilitychange", writeIfHidden);
      window.removeEventListener("pagehide", write);
    };
  }, [state, restored, catalogue]);

  // Keyed on the step alone: picking another platform card, or a restored draft for another
  // target, is not another view of the step. The first view waits for the draft, so it names the
  // target the visitor is shown.
  const lastViewed = useRef<Step | null>(null);
  useEffect(() => {
    if (!restored || lastViewed.current === step) return;
    lastViewed.current = step;
    if (step === "identity" && identityAt.current === null) identityAt.current = performance.now();
    track({ name: "step_viewed", target: state.target, step });
  }, [restored, step, state.target]);

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

  // Start over is one small button beside the answers it erases, so the answers it replaced are
  // kept in memory and offered back until the next edit.
  const [beforeReset, setBeforeReset] = useState<StartOverUndo | null>(null);

  const startOver = useCallback(() => {
    const current = { state, step, identityAt: identityAt.current };
    const untouched = draftToStore(state, catalogue) === null;
    setBeforeReset((kept) => snapshotForUndo(kept, current, untouched));
    writeDraft(null);
    dispatchRaw({ type: "reset" });
    setOutcome(null);
    setStep("platform");
    identityAt.current = null;
  }, [state, step, catalogue]);

  const canUndoStartOver = beforeReset !== null && beforeReset.revision === state.revision;
  const undoStartOver = useCallback(() => {
    if (!beforeReset) return;
    dispatchRaw({ type: "restore", state: beforeReset.state });
    setStep(beforeReset.step);
    identityAt.current = beforeReset.identityAt;
    setBeforeReset(null);
  }, [beforeReset]);

  const generate = useCallback(async () => {
    if (errors.size > 0 || busy) return;
    const revision = state.revision;
    setBusy(true);
    setOutcome(null);
    track({ name: "generate_clicked", target: state.target });
    const started = identityAt.current;
    const controller = new AbortController();
    let timeout = setTimeout(() => controller.abort(), GENERATE_TIMEOUT_MS);
    let downloading = false;
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
      clearTimeout(timeout);
      downloading = true;
      timeout = setTimeout(() => controller.abort(), DOWNLOAD_TIMEOUT_MS);
      const blob = await response.blob();
      const filename = filenameOf(response);
      save(blob, filename);
      setOutcome({
        kind: "done",
        revision,
        filename,
        bytes: blob.size,
        elapsedMs: generationMs(response),
        keystoresSkipped: (response.headers.get("X-Keystores-Skipped") ?? "").split(",").filter(Boolean),
      });
    } catch (error) {
      const message =
        error instanceof ApiError
          ? error.message
          : downloading
            ? "The download stopped before the zip arrived. Try again."
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
  const identityValid = !IDENTITY_FIELDS.some((field) => errors.has(field));

  return {
    state,
    dispatch,
    target,
    spec,
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
    canUndoStartOver,
    undoStartOver,
  };
}

export type ConfiguratorModel = ReturnType<typeof useConfigurator>;
