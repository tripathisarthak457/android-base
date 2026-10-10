import { API_BASE, BUILD, clientHeader } from "../../lib/api.ts";
import { type ClientEvent, batches } from "./events.ts";

/**
 * The browser half of the funnel: one random session id per tab, events queued and sent in
 * batches, and uncaught errors reported. Nothing here outlives the tab: the id lives in
 * `sessionStorage`, which the browser clears when the tab closes.
 */

const SESSION_KEY = "android-base.session";
const FLUSH_DELAY_MS = 1500;
const ERROR_REPORTS_PER_PAGE = 5;

type Session = { id: string; landed: boolean };

let memory: Session | null = null;
const queue: ClientEvent[] = [];
let timer: ReturnType<typeof setTimeout> | null = null;

function readSession(): Session {
  if (memory) return memory;
  try {
    const saved = JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? "null") as Partial<Session> | null;
    if (saved && typeof saved.id === "string") memory = { id: saved.id, landed: saved.landed === true };
  } catch {
    // Storage blocked or the entry unreadable: a fresh id for this page is the honest fallback.
  }
  memory ??= { id: crypto.randomUUID(), landed: false };
  writeSession(memory);
  return memory;
}

function writeSession(session: Session): void {
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } catch {
    // The id then lasts as long as the page, which is all the funnel needs.
  }
}

export function sessionId(): string {
  return readSession().id;
}

/** Records `landed` once per tab, however often the page reloads. */
export function trackLanding(event: Extract<ClientEvent, { name: "landed" }>): void {
  const session = readSession();
  if (session.landed) return;
  memory = { ...session, landed: true };
  writeSession(memory);
  track(event);
}

export function track(event: ClientEvent): void {
  queue.push(event);
  timer ??= setTimeout(flush, FLUSH_DELAY_MS);
}

/** Sends what is queued. `keepalive` lets the last batch outlive the page it was sent from. */
function flush(): void {
  if (timer) clearTimeout(timer);
  timer = null;
  if (queue.length === 0) return;
  const bodies = batches(sessionId(), queue.splice(0));
  for (const body of bodies) {
    void fetch(`${API_BASE}/api/v1/events`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Client": clientHeader(BUILD) },
      body,
      keepalive: true,
    }).catch(() => undefined);
  }
}

/** Flushes when the page is hidden, which is the last moment a browser reliably runs script. */
export function flushOnHide(): void {
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "hidden") flush();
  });
  window.addEventListener("pagehide", flush);
}

/**
 * Sends uncaught errors and rejections to `/api/v1/client-error`, where they are grouped by
 * fingerprint, so a crash shows up in the admin within minutes instead of in a bug report.
 */
export function reportErrors(): void {
  const sent = new Set<string>();
  const report = (message: string, stack: string) => {
    if (!message || sent.has(message) || sent.size >= ERROR_REPORTS_PER_PAGE) return;
    sent.add(message);
    void fetch(`${API_BASE}/api/v1/client-error`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Client": clientHeader(BUILD) },
      body: JSON.stringify({
        message: message.slice(0, 1000),
        stack: stack.slice(0, 8000),
        // The API keeps only the path; the query and fragment never leave the page.
        url: `${location.origin}${location.pathname}`,
        release: BUILD,
      }),
      keepalive: true,
    }).catch(() => undefined);
  };
  window.addEventListener("error", (event) => report(event.message, event.error instanceof Error ? (event.error.stack ?? "") : ""));
  window.addEventListener("unhandledrejection", (event) => {
    const reason: unknown = event.reason;
    if (reason instanceof Error) report(reason.message, reason.stack ?? "");
    else report(String(reason), "");
  });
}
