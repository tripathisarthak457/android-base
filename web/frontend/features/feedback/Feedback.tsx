"use client";

import { useEffect, useId, useRef, useState } from "react";
import { Button, Field, TextInput } from "../../components/primitives.tsx";
import { Spinner } from "../../components/ui.tsx";
import { API_BASE, ApiError, BUILD, apiError, clientHeader } from "../../lib/api.ts";
import { REPO } from "../../lib/catalogue.ts";
import { type ReportContext, readReportContext } from "./context.ts";

// The bug report form. Two things make a report worth having: enough context to reproduce it, and
// few enough questions that somebody actually finishes it.

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

const TEXT_AREA = "w-full rounded-lg border border-ink-500 bg-ink-900 p-3 text-ink-100 placeholder:text-ink-500 aria-invalid:border-rose";

/** The API's minimums, which it counts in code points after trimming. */
const SUMMARY_MIN = 4;
const DETAILS_MIN = 10;

function tooShort(value: string, min: number): boolean {
  return [...value.trim()].length < min;
}

function Choices<T extends string>({ legend, options, value, onChange, columns }: {
  legend: string;
  options: { value: T; label: string; blurb?: string }[];
  value: T;
  onChange: (next: T) => void;
  columns: string;
}) {
  const name = useId();
  return (
    <fieldset>
      <legend className="mb-2 text-sm font-medium text-ink-200">{legend}</legend>
      <div className={`grid gap-2 ${columns}`}>
        {options.map((option) => (
          <label
            key={option.value}
            className={`cursor-pointer rounded-lg border p-3 text-left transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent ${
              value === option.value ? "border-accent bg-accent-dim/50 ring-1 ring-accent" : "border-ink-700 bg-ink-850 hover:border-ink-600"
            }`}
          >
            <input type="radio" name={name} checked={value === option.value} onChange={() => onChange(option.value)} className="sr-only" />
            <span className="block text-sm font-semibold text-ink-100">{option.label}</span>
            {option.blurb && <span className="mt-0.5 block text-xs text-ink-400">{option.blurb}</span>}
          </label>
        ))}
      </div>
    </fieldset>
  );
}

/**
 * The report button in the navigation, and its dialog. A native modal dialog: the browser traps
 * focus inside it, closes it on Escape, makes the page behind it inert, and returns focus to the
 * button that opened it. It sits in the navigation rather than floating, where it would cover the
 * configurator's own buttons on a phone.
 *
 * Closing keeps the form and what is typed in it; only closing after a report went out starts the
 * next one blank. A backdrop click closes it only when the press began on the backdrop too, so a
 * text selection dragged past the dialog's edge does not throw the report away.
 */
export function ReportButton() {
  const dialog = useRef<HTMLDialogElement>(null);
  const pressedBackdrop = useRef(false);
  const [open, setOpen] = useState(false);
  const [context, setContext] = useState<ReportContext | null>(null);
  const [form, setForm] = useState<{ key: number; sent: boolean } | null>(null);

  useEffect(() => {
    const element = dialog.current;
    if (!element) return;
    if (open && !element.open) element.showModal();
    if (!open && element.open) element.close();
  }, [open]);

  function close() {
    setOpen(false);
    setForm((current) => (current?.sent ? { key: current.key + 1, sent: false } : current));
  }

  return (
    <>
      <button
        type="button"
        onClick={() => {
          setContext(readReportContext());
          setForm((current) => current ?? { key: 0, sent: false });
          setOpen(true);
        }}
        title="Report a problem"
        className="flex h-9 items-center gap-2 rounded-lg px-3 text-sm text-ink-300 transition-colors hover:bg-ink-800 hover:text-ink-100 max-md:w-9 max-md:justify-center max-md:px-0"
      >
        <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden>
          <path d="M3 14V2.5M3 2.5h8.5l-1.5 3 1.5 3H3" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        <span className="max-md:sr-only">Report a problem</span>
      </button>
      <dialog
        ref={dialog}
        aria-labelledby="feedback-title"
        onClose={close}
        onPointerDown={(event) => {
          pressedBackdrop.current = event.target === dialog.current;
        }}
        onClick={(event) => {
          if (pressedBackdrop.current && event.target === dialog.current) close();
        }}
        className="m-auto w-[calc(100%-2rem)] max-w-2xl rounded-2xl border border-ink-700 bg-ink-900 p-0 text-ink-100 shadow-2xl"
      >
        {form && (
          <FeedbackForm
            key={form.key}
            context={context}
            onSent={() => setForm((current) => current && { ...current, sent: true })}
            onClose={close}
          />
        )}
      </dialog>
    </>
  );
}

function FeedbackForm({ context, onSent, onClose }: { context: ReportContext | null; onSent: () => void; onClose: () => void }) {
  const [kind, setKind] = useState<Kind>("bug");
  const [severity, setSeverity] = useState<Severity>("annoying");
  const [area, setArea] = useState<Area>("website");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [steps, setSteps] = useState("");
  const [contact, setContact] = useState("");
  const [attach, setAttach] = useState(context !== null);
  const [tried, setTried] = useState(false);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<{ message: string; unreachable: boolean } | null>(null);
  const [sentId, setSentId] = useState<number | null>(null);
  const titleInput = useRef<HTMLInputElement>(null);
  const bodyInput = useRef<HTMLTextAreaElement>(null);

  const bug = kind === "bug";
  // Send stays enabled and says what is missing when pressed, rather than staying grey with no clue why.
  const titleProblem = tried && tooShort(title, SUMMARY_MIN) ? `Write at least ${SUMMARY_MIN} characters.` : undefined;
  const bodyProblem = tried && tooShort(body, DETAILS_MIN) ? `Write at least ${DETAILS_MIN} characters.` : undefined;

  function submit() {
    if (busy) return;
    setTried(true);
    if (tooShort(title, SUMMARY_MIN)) titleInput.current?.focus();
    else if (tooShort(body, DETAILS_MIN)) bodyInput.current?.focus();
    else void send();
  }

  async function send() {
    setBusy(true);
    setFailure(null);
    try {
      const response = await fetch(`${API_BASE}/api/v1/feedback`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Client": clientHeader(BUILD) },
        body: JSON.stringify({
          kind,
          severity: bug ? severity : "",
          area,
          title: title.trim(),
          body: body.trim(),
          steps: bug ? steps.trim() : "",
          contact: contact.trim(),
          appVersion: BUILD,
          ...(attach && context ? { ...context, pageUrl: `${location.origin}${location.pathname}` } : { features: [] }),
        }),
      });
      if (!response.ok) throw await apiError(response, "That could not be sent.");
      const result = (await response.json()) as { id?: number };
      setSentId(result.id ?? 0);
      onSent();
    } catch (error) {
      // fetch rejects with a TypeError when the request never got an answer; its message is the
      // browser's ("Failed to fetch"), which tells the visitor nothing they can act on.
      setFailure(error instanceof ApiError ? { message: error.message, unreachable: false } : { message: "Could not reach the server.", unreachable: true });
    } finally {
      setBusy(false);
    }
  }

  if (sentId !== null) return <Sent id={sentId} onClose={onClose} />;

  const attached: [string, string][] = context
    ? [
        ["Platform", context.target],
        ["App name", context.appName],
        ["Package", context.packageName],
        ["Preset", context.preset],
        ["Features", `${context.features.length} selected`],
        ["Min SDK", context.minSdk ? String(context.minSdk) : "—"],
        ["Typeface", context.fontName],
        ["Accent", context.accentColour],
      ]
    : [];

  return (
    <form
      method="dialog"
      onSubmit={(event) => {
        event.preventDefault();
        submit();
      }}
    >
      <div className="flex items-start justify-between gap-4 border-b border-ink-800 px-6 py-5">
        <div>
          <h2 id="feedback-title" className="text-lg font-semibold text-ink-100">
            Tell us what happened
          </h2>
          <p className="mt-1 text-sm text-ink-400">Reports go straight to the person who can fix it.</p>
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Close"
          className="rounded-lg px-2 py-1 text-ink-400 transition-colors hover:bg-ink-800 hover:text-ink-100"
        >
          ✕
        </button>
      </div>

      <div className="max-h-[65vh] space-y-5 overflow-y-auto px-6 py-5">
        <Choices legend="What kind of report is this?" options={KINDS} value={kind} onChange={setKind} columns="sm:grid-cols-2" />
        <Choices legend="Where?" options={AREAS} value={area} onChange={setArea} columns="grid-cols-2" />
        {bug && <Choices legend="How bad is it?" options={SEVERITIES} value={severity} onChange={setSeverity} columns="sm:grid-cols-3" />}

        <Field label="One-line summary" hint={`At least ${SUMMARY_MIN} characters.`} error={titleProblem}>
          {(control) => (
            <TextInput
              {...control}
              ref={titleInput}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder={bug ? "The zip has no gradlew" : "Let me pick a starting screen"}
              maxLength={200}
            />
          )}
        </Field>

        <Field
          label={bug ? "What happened?" : "Tell us more"}
          hint={`At least ${DETAILS_MIN} characters, and as much detail as you have. Error messages word for word help most.`}
          error={bodyProblem}
        >
          {(control) => (
            <textarea
              {...control}
              ref={bodyInput}
              value={body}
              onChange={(event) => setBody(event.target.value)}
              rows={4}
              maxLength={8000}
              className={`${TEXT_AREA} text-sm`}
              placeholder={bug ? "I picked the standard preset, ran ./gradlew build and got…" : "It would help if…"}
            />
          )}
        </Field>

        {bug && (
          <Field label="Steps to reproduce (optional)">
            {(control) => (
              <textarea
                {...control}
                value={steps}
                onChange={(event) => setSteps(event.target.value)}
                rows={3}
                maxLength={4000}
                className={`${TEXT_AREA} font-mono text-xs`}
                placeholder={"1. Pick the lean preset\n2. Generate\n3. Run ./gradlew build"}
              />
            )}
          </Field>
        )}

        <Field label="Email (optional)" hint="Only used to ask a follow-up question.">
          {(control) => (
            <TextInput
              {...control}
              value={contact}
              onChange={(event) => setContact(event.target.value)}
              placeholder="you@example.com"
              type="email"
              maxLength={200}
            />
          )}
        </Field>

        {context && (
          <div className="rounded-lg border border-ink-700 bg-ink-850 p-4">
            <label className="flex items-start justify-between gap-3">
              <span>
                <span className="block text-sm font-medium text-ink-200">Attach what you configured</span>
                <span className="mt-0.5 block text-xs text-ink-400">And this page&apos;s address. It usually saves three emails.</span>
              </span>
              <input
                type="checkbox"
                checked={attach}
                onChange={(event) => setAttach(event.target.checked)}
                className="mt-1 h-4 w-4 accent-[var(--color-accent)]"
              />
            </label>
            {attach && (
              <dl className="mt-3 grid gap-x-4 gap-y-1 border-t border-ink-700 pt-3 text-xs sm:grid-cols-2">
                {attached.map(([label, value]) => (
                  <div key={label} className="flex justify-between gap-3">
                    <dt className="text-ink-400">{label}</dt>
                    <dd className="truncate font-mono text-ink-300">{value || "—"}</dd>
                  </div>
                ))}
              </dl>
            )}
          </div>
        )}

        {failure && (
          <p role="alert" className="rounded-lg border border-rose/30 bg-rose/10 p-3 text-xs text-rose">
            {failure.message}
            {failure.unreachable && (
              <>
                {" "}
                <a href={`${REPO}/issues`} target="_blank" rel="noopener noreferrer" className="font-medium underline underline-offset-2">
                  GitHub issues
                </a>{" "}
                work even when this does not.
              </>
            )}
          </p>
        )}
      </div>

      <div className="flex items-center justify-between gap-4 border-t border-ink-800 px-6 py-4">
        <p className="text-xs text-ink-400">No account, no cookie. Your IP is never stored.</p>
        <div className="flex gap-2">
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={busy} focusableWhenDisabled>
            {busy ? (
              <>
                <Spinner /> Sending…
              </>
            ) : (
              "Send report"
            )}
          </Button>
        </div>
      </div>
    </form>
  );
}

/**
 * What the dialog shows once a report is in. It takes the focus: the button that sent the report
 * went with the form, and a status region that mounts already holding its text is not announced.
 */
function Sent({ id, onClose }: { id: number; onClose: () => void }) {
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => heading.current?.focus(), []);
  return (
    <div className="px-6 py-10 text-center">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-mint/15 text-2xl text-mint" aria-hidden>
        ✓
      </div>
      <h2 ref={heading} id="feedback-title" tabIndex={-1} className="mx-auto mt-5 w-fit text-lg font-semibold text-ink-100">
        Report sent
      </h2>
      <p className="mx-auto mt-2 max-w-sm text-sm text-ink-400">
        Report <span className="font-mono text-ink-300">#{id}</span>. If you left an email you will hear back when there is something to say.
      </p>
      <Button variant="secondary" className="mt-6" onClick={onClose}>
        Close
      </Button>
    </div>
  );
}
