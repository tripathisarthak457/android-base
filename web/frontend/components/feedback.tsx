"use client";

/**
 * The bug report form.
 *
 * Two things make a report worth having: enough context to reproduce it, and few enough questions
 * that somebody actually finishes it. Those pull in opposite directions, so the split here is —
 * the person types four things at most, and everything else is attached from state the page
 * already has: the exact feature set they configured, the package name, the motion style, the
 * browser, the page they were on.
 *
 * A reporter who has to retype what the wizard already knows is a reporter who closes the tab.
 */

import { AnimatePresence, motion } from "framer-motion";
import { useEffect, useState } from "react";
import { API_BASE } from "../lib/api";
import { Badge, Button, Field, Spinner, TextInput, press } from "./primitives";

export type ReportContext = {
  appName?: string;
  packageName?: string;
  features?: string[];
  preset?: string;
  minSdk?: number;
  motionStyle?: string;
  fontName?: string;
  accentColour?: string;
};

type Kind = "bug" | "idea" | "praise" | "question";
type Severity = "blocks" | "annoying" | "cosmetic";
type Area = "website" | "generated-project" | "cli" | "docs";

const KINDS: { value: Kind; label: string; blurb: string }[] = [
  { value: "bug", label: "Something is broken", blurb: "It did the wrong thing, or nothing" },
  { value: "idea", label: "Suggestion", blurb: "Something that would make this better" },
  { value: "question", label: "Question", blurb: "The docs did not answer it" },
  { value: "praise", label: "It worked", blurb: "Useful to know too" },
];

const SEVERITIES: { value: Severity; label: string; blurb: string }[] = [
  { value: "blocks", label: "Blocks me", blurb: "I cannot get a working project" },
  { value: "annoying", label: "Annoying", blurb: "There is a way round it" },
  { value: "cosmetic", label: "Cosmetic", blurb: "It looks wrong but works" },
];

const AREAS: { value: Area; label: string }[] = [
  { value: "website", label: "This website" },
  { value: "generated-project", label: "The project I downloaded" },
  { value: "cli", label: "The command line tool" },
  { value: "docs", label: "The documentation" },
];

export function FeedbackButton({ context }: { context: ReportContext }) {
  const [open, setOpen] = useState(false);

  // Escape closes it. A modal that traps you is worse than no modal, and this one is optional.
  useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open]);

  return (
    <>
      <motion.button
        {...press}
        type="button"
        onClick={() => setOpen(true)}
        className="fixed bottom-5 right-5 z-40 flex items-center gap-2 rounded-full border border-ink-600 bg-ink-850/95 px-4 py-2.5 text-sm font-medium text-ink-200 shadow-lg backdrop-blur transition-colors hover:border-accent/50 hover:text-ink-100"
      >
        <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden>
          <path
            d="M8 1.5 9.8 5.6l4.5.4-3.4 3 1 4.4L8 11.1l-3.9 2.3 1-4.4-3.4-3 4.5-.4L8 1.5Z"
            stroke="currentColor"
            strokeWidth="1.3"
            strokeLinejoin="round"
          />
        </svg>
        Report a bug
      </motion.button>

      <AnimatePresence>
        {open && <FeedbackModal context={context} onClose={() => setOpen(false)} />}
      </AnimatePresence>
    </>
  );
}

function FeedbackModal({
  context,
  onClose,
}: {
  context: ReportContext;
  onClose: () => void;
}) {
  const [kind, setKind] = useState<Kind>("bug");
  const [severity, setSeverity] = useState<Severity>("annoying");
  const [area, setArea] = useState<Area>("website");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [steps, setSteps] = useState("");
  const [expected, setExpected] = useState("");
  const [actual, setActual] = useState("");
  const [contact, setContact] = useState("");
  const [attachContext, setAttachContext] = useState(true);

  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);
  const [sentId, setSentId] = useState<number | null>(null);

  const isBug = kind === "bug";
  const canSend = title.trim().length >= 4 && body.trim().length >= 10 && !busy;

  async function send() {
    setBusy(true);
    setFailure(null);

    try {
      const response = await fetch(`${API_BASE}/api/feedback`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          kind,
          severity: isBug ? severity : "",
          area,
          title: title.trim(),
          body: body.trim(),
          steps: isBug ? steps.trim() : "",
          expected: isBug ? expected.trim() : "",
          actual: isBug ? actual.trim() : "",
          contact: contact.trim(),
          pageUrl: window.location.href,
          appVersion: "beta",
          ...(attachContext
            ? {
                appName: context.appName ?? "",
                packageName: context.packageName ?? "",
                features: context.features ?? [],
                preset: context.preset ?? "",
                minSdk: context.minSdk ?? 0,
                motionStyle: context.motionStyle ?? "",
                fontName: context.fontName ?? "",
                accentColour: context.accentColour ?? "",
              }
            : { features: [] }),
        }),
      });

      if (!response.ok) {
        const problem = await response.json().catch(() => null);
        throw new Error(problem?.error ?? "That could not be sent.");
      }

      const result = await response.json();
      setSentId(result.id ?? 0);
    } catch (error) {
      setFailure(
        error instanceof Error
          ? error.message
          : "Could not reach the server. GitHub issues work even when this does not.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-ink-950/85 p-4 backdrop-blur-sm sm:p-8"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="Report a bug"
    >
      <motion.div
        initial={{ opacity: 0, y: 18, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 10, scale: 0.99 }}
        transition={{ type: "spring", stiffness: 420, damping: 32 }}
        onClick={(event) => event.stopPropagation()}
        className="my-auto w-full max-w-2xl rounded-2xl border border-ink-700 bg-ink-900 shadow-2xl"
      >
        {sentId !== null ? (
          <Sent id={sentId} onClose={onClose} />
        ) : (
          <>
            <header className="flex items-start justify-between gap-4 border-b border-ink-800 px-6 py-5">
              <div>
                <h2 className="text-lg font-semibold text-ink-100">Tell us what happened</h2>
                <p className="mt-1 text-sm text-ink-400">
                  This is beta. Reports go straight to the person who can fix it.
                </p>
              </div>
              <button
                type="button"
                onClick={onClose}
                aria-label="Close"
                className="rounded-lg px-2 py-1 text-ink-400 transition-colors hover:bg-ink-800 hover:text-ink-100"
              >
                ✕
              </button>
            </header>

            <div className="max-h-[65vh] space-y-5 overflow-y-auto px-6 py-5">
              <div>
                <p className="mb-2 text-sm font-medium text-ink-200">What kind of report is this?</p>
                <div className="grid gap-2 sm:grid-cols-2">
                  {KINDS.map((option) => (
                    <motion.button
                      key={option.value}
                      {...press}
                      type="button"
                      onClick={() => setKind(option.value)}
                      className={`rounded-lg border p-3 text-left transition-colors ${
                        kind === option.value
                          ? "border-accent/50 bg-accent-dim/50"
                          : "border-ink-700 bg-ink-850 hover:border-ink-600"
                      }`}
                    >
                      <p className="text-sm font-semibold text-ink-100">{option.label}</p>
                      <p className="mt-0.5 text-xs text-ink-400">{option.blurb}</p>
                    </motion.button>
                  ))}
                </div>
              </div>

              <div>
                <p className="mb-2 text-sm font-medium text-ink-200">Where?</p>
                <div className="flex flex-wrap gap-1.5">
                  {AREAS.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => setArea(option.value)}
                      className={`rounded-full border px-3 py-1.5 text-xs font-medium transition-colors ${
                        area === option.value
                          ? "border-accent bg-accent-dim text-accent-bright"
                          : "border-ink-600 text-ink-400 hover:border-ink-500 hover:text-ink-200"
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </div>

              <AnimatePresence initial={false}>
                {isBug && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    className="overflow-hidden"
                  >
                    <p className="mb-2 text-sm font-medium text-ink-200">How bad is it?</p>
                    <div className="grid gap-2 sm:grid-cols-3">
                      {SEVERITIES.map((option) => (
                        <button
                          key={option.value}
                          type="button"
                          onClick={() => setSeverity(option.value)}
                          className={`rounded-lg border p-2.5 text-left transition-colors ${
                            severity === option.value
                              ? "border-accent/50 bg-accent-dim/50"
                              : "border-ink-700 bg-ink-850 hover:border-ink-600"
                          }`}
                        >
                          <p className="text-xs font-semibold text-ink-100">{option.label}</p>
                          <p className="mt-0.5 text-[11px] text-ink-400">{option.blurb}</p>
                        </button>
                      ))}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              <Field label="One-line summary">
                <TextInput
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                  placeholder={
                    isBug ? "The zip has no gradlew" : "Let me pick a starting screen"
                  }
                  maxLength={200}
                  autoFocus
                />
              </Field>

              <Field
                label={isBug ? "What happened?" : "Tell us more"}
                hint="As much detail as you have. Error messages verbatim are gold."
              >
                <textarea
                  value={body}
                  onChange={(event) => setBody(event.target.value)}
                  rows={4}
                  maxLength={8000}
                  className="w-full rounded-lg border border-ink-600 bg-ink-900 p-3 text-sm text-ink-100 placeholder:text-ink-500 focus:border-accent focus:outline-none"
                  placeholder={
                    isBug
                      ? "I picked the standard preset, downloaded it, ran ./gradlew build and got…"
                      : "It would help if…"
                  }
                />
              </Field>

              <AnimatePresence initial={false}>
                {isBug && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    className="space-y-4 overflow-hidden"
                  >
                    <Field
                      label="Steps to reproduce (optional)"
                      hint="Numbered is easiest. Skip it if it happens every time."
                    >
                      <textarea
                        value={steps}
                        onChange={(event) => setSteps(event.target.value)}
                        rows={3}
                        maxLength={4000}
                        className="w-full rounded-lg border border-ink-600 bg-ink-900 p-3 font-mono text-xs text-ink-100 placeholder:text-ink-500 focus:border-accent focus:outline-none"
                        placeholder={"1. Pick the lean preset\n2. Click generate\n3. Unzip and run ./gradlew build"}
                      />
                    </Field>

                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field label="What you expected (optional)">
                        <TextInput
                          value={expected}
                          onChange={(event) => setExpected(event.target.value)}
                          placeholder="BUILD SUCCESSFUL"
                          maxLength={2000}
                        />
                      </Field>
                      <Field label="What you got (optional)">
                        <TextInput
                          value={actual}
                          onChange={(event) => setActual(event.target.value)}
                          placeholder="Unresolved reference 'Foo'"
                          maxLength={2000}
                        />
                      </Field>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              <Field
                label="Email (optional)"
                hint="Only used to ask a follow-up question. Nothing else, ever."
              >
                <TextInput
                  value={contact}
                  onChange={(event) => setContact(event.target.value)}
                  placeholder="you@example.com"
                  type="email"
                  maxLength={200}
                />
              </Field>

              <ContextPanel
                context={context}
                attached={attachContext}
                onToggle={() => setAttachContext((current) => !current)}
              />

              {failure && (
                <p className="rounded-lg border border-rose/30 bg-rose/10 p-3 text-xs text-rose">
                  {failure}
                </p>
              )}
            </div>

            <footer className="flex items-center justify-between gap-4 border-t border-ink-800 px-6 py-4">
              <p className="text-xs text-ink-500">
                No account, no cookie. Your IP is never stored.
              </p>
              <div className="flex gap-2">
                <Button variant="ghost" onClick={onClose}>
                  Cancel
                </Button>
                <Button onClick={send} disabled={!canSend}>
                  {busy ? <Spinner /> : "Send report"}
                </Button>
              </div>
            </footer>
          </>
        )}
      </motion.div>
    </motion.div>
  );
}

/**
 * Shows exactly what is being attached, and lets it be turned off.
 *
 * A form that silently uploads the reporter's configuration is a form that gets a reputation. It
 * is all things they typed into a public page and none of it identifies them, but showing it is
 * cheap and the alternative is asking people to trust a claim.
 */
function ContextPanel({
  context,
  attached,
  onToggle,
}: {
  context: ReportContext;
  attached: boolean;
  onToggle: () => void;
}) {
  const rows: [string, string][] = [
    ["App name", context.appName || "—"],
    ["Package", context.packageName || "—"],
    ["Preset", context.preset || "—"],
    ["Features", context.features?.length ? `${context.features.length} selected` : "—"],
    ["Min SDK", context.minSdk ? String(context.minSdk) : "—"],
    ["Typeface", context.fontName || "—"],
    ["Motion", context.motionStyle || "—"],
    ["Accent", context.accentColour || "—"],
  ];

  return (
    <div className="rounded-lg border border-ink-700 bg-ink-850 p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-ink-200">Attach what you configured</p>
          <p className="mt-0.5 text-xs text-ink-400">
            Plus your browser and this page&apos;s URL. It is usually the difference between a bug
            that gets fixed today and one that needs three emails first.
          </p>
        </div>
        <button
          type="button"
          onClick={onToggle}
          className={`shrink-0 rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
            attached
              ? "border-accent bg-accent-dim text-accent-bright"
              : "border-ink-600 text-ink-400"
          }`}
        >
          {attached ? "Attached" : "Not attached"}
        </button>
      </div>

      <AnimatePresence initial={false}>
        {attached && (
          <motion.dl
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            className="mt-3 grid gap-x-4 gap-y-1 overflow-hidden border-t border-ink-700 pt-3 text-xs sm:grid-cols-2"
          >
            {rows.map(([label, value]) => (
              <div key={label} className="flex justify-between gap-3">
                <dt className="text-ink-500">{label}</dt>
                <dd className="truncate font-mono text-ink-300">{value}</dd>
              </div>
            ))}
          </motion.dl>
        )}
      </AnimatePresence>
    </div>
  );
}

function Sent({ id, onClose }: { id: number; onClose: () => void }) {
  return (
    <div className="px-6 py-10 text-center">
      <motion.div
        initial={{ scale: 0.7, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ type: "spring", stiffness: 380, damping: 18 }}
        className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-mint/15 text-2xl text-mint"
      >
        ✓
      </motion.div>
      <h2 className="mt-5 text-lg font-semibold text-ink-100">Got it</h2>
      <p className="mx-auto mt-2 max-w-sm text-sm text-ink-400">
        Report <span className="font-mono text-ink-300">#{id}</span>. If you left an email you will
        hear back when there is something to say — not before, and not otherwise.
      </p>
      <Button variant="secondary" className="mt-6" onClick={onClose}>
        Close
      </Button>
    </div>
  );
}

/** The beta mark. Small, everywhere, and honest about what it means. */
export function BetaBadge({ className = "" }: { className?: string }) {
  return (
    <span className={className} title="Everything works and has been tested, but not by many people yet.">
      <Badge tone="amber">Beta</Badge>
    </span>
  );
}
