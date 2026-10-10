// The anonymous funnel events the site sends to `/api/v1/events`. The API accepts exactly these
// names and fields and refuses a batch with anything else, so this union is the whole vocabulary.

import type { Step } from "../configurator/state.ts";

export type Viewport = "xs" | "sm" | "md" | "lg" | "xl";

export type Landed = {
  name: "landed";
  referrer?: string;
  utm_source?: string;
  utm_medium?: string;
  utm_campaign?: string;
  returning: boolean;
  viewport: Viewport;
};

export type ClientEvent =
  | Landed
  | { name: "step_viewed"; target: string; step: Step }
  | { name: "platform_selected"; target: string }
  | { name: "preset_chosen"; target: string; preset: string }
  | { name: "generate_clicked"; target: string }
  | { name: "download_started"; target: string };

/** The API's limits on one batch. */
export const BATCH_MAX_EVENTS = 20;
export const BATCH_MAX_BYTES = 8 * 1024;

const UTM_MAX = 64;

export function viewport(width: number): Viewport {
  if (width < 480) return "xs";
  if (width < 768) return "sm";
  if (width < 1024) return "md";
  if (width < 1440) return "lg";
  return "xl";
}

/** Only the origin leaves the browser: the path and query of the page someone came from are theirs. */
export function referrerOrigin(referrer: string, ownOrigin: string): string | undefined {
  if (!referrer) return undefined;
  try {
    const origin = new URL(referrer).origin;
    return origin === "null" || origin === ownOrigin ? undefined : origin;
  } catch {
    return undefined;
  }
}

export function landed(page: {
  referrer: string;
  origin: string;
  search: string;
  returning: boolean;
  width: number;
}): Landed {
  const query = new URLSearchParams(page.search);
  const event: Landed = { name: "landed", returning: page.returning, viewport: viewport(page.width) };
  const referrer = referrerOrigin(page.referrer, page.origin);
  if (referrer) event.referrer = referrer;
  for (const key of ["utm_source", "utm_medium", "utm_campaign"] as const) {
    const value = query.get(key)?.trim().slice(0, UTM_MAX);
    if (value) event[key] = value;
  }
  return event;
}

/**
 * A random UUID v4, the form of session id the API stores. Built on `getRandomValues` because
 * `crypto.randomUUID` exists only in secure contexts, and a phone testing `next dev` over plain
 * http on the LAN is not one.
 */
export function randomSessionId(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6]! & 0x0f) | 0x40;
  bytes[8] = (bytes[8]! & 0x3f) | 0x80;
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

/** `events` split into bodies the API accepts: at most 20 events and 8 KB each. */
export function batches(sessionId: string, events: ClientEvent[]): string[] {
  const bodies: string[] = [];
  let current: ClientEvent[] = [];
  const body = (list: ClientEvent[]) => JSON.stringify({ session_id: sessionId, events: list });
  for (const event of events) {
    const next = [...current, event];
    if (current.length > 0 && (next.length > BATCH_MAX_EVENTS || body(next).length > BATCH_MAX_BYTES)) {
      bodies.push(body(current));
      current = [event];
    } else {
      current = next;
    }
  }
  if (current.length > 0) bodies.push(body(current));
  return bodies;
}
