import assert from "node:assert/strict";
import { test } from "node:test";
import { readJson } from "../catalogue/testing.ts";
import { BATCH_MAX_BYTES, BATCH_MAX_EVENTS, type ClientEvent, batches, landed, referrerOrigin, viewport } from "./events.ts";

type Accepted = {
  name: string;
  body: { session_id: string; events: Record<string, unknown>[] };
  stored: { visit: Record<string, unknown> | null };
};

const contract = readJson<{ accepted: Accepted[] }>("web", "api", "testdata", "contract", "events.json");

test("a landing from a campaign is stored as the API's contract says", () => {
  const entry = contract.accepted.find((candidate) => candidate.body.events.some((event) => event.name === "landed" && event.utm_source));
  assert.ok(entry?.stored.visit);
  const sent = entry.body.events.find((event) => event.name === "landed") ?? {};
  const event = landed({
    referrer: String(sent.referrer),
    origin: "https://android-base.vercel.app",
    search: `?utm_source=${sent.utm_source}&utm_medium=${sent.utm_medium}&utm_campaign=${sent.utm_campaign}&ref=x`,
    returning: sent.returning === true,
    width: 1280,
  });
  const visit = entry.stored.visit;
  assert.deepEqual(
    {
      referrer_origin: event.referrer ?? null,
      utm_source: event.utm_source ?? null,
      utm_medium: event.utm_medium ?? null,
      utm_campaign: event.utm_campaign ?? null,
      returning: event.returning,
      viewport: event.viewport,
    },
    visit,
  );
});

test("every other event the site sends has the shape of an accepted one", () => {
  const sent: ClientEvent[] = [
    { name: "step_viewed", target: "android", step: "identity" },
    { name: "platform_selected", target: "multiplatform" },
    { name: "preset_chosen", target: "android", preset: "everything" },
    { name: "generate_clicked", target: "android" },
    { name: "download_started", target: "android" },
  ];
  const accepted = contract.accepted.flatMap((entry) => entry.body.events);
  const fields = (event: object) => JSON.stringify(Object.keys(event).sort());
  for (const event of sent) {
    assert.ok(
      accepted.some((candidate) => candidate.name === event.name && fields(candidate) === fields(event)),
      `${event.name} has no accepted counterpart with the same fields`,
    );
  }
});

test("only the referrer's origin leaves the browser, and never the site's own", () => {
  assert.equal(referrerOrigin("https://news.ycombinator.com/item?id=1", "https://site.example"), "https://news.ycombinator.com");
  assert.equal(referrerOrigin("https://site.example/admin", "https://site.example"), undefined);
  assert.equal(referrerOrigin("", "https://site.example"), undefined);
  assert.equal(referrerOrigin("not a url", "https://site.example"), undefined);
});

test("a landing with nothing to say sends only the returning flag and viewport", () => {
  assert.deepEqual(landed({ referrer: "", origin: "https://s", search: "?utm_source=%20", returning: true, width: 390 }), {
    name: "landed",
    returning: true,
    viewport: "xs",
  });
});

test("viewport buckets match the API's", () => {
  assert.deepEqual([479, 480, 767, 768, 1023, 1024, 1439, 1440].map(viewport), ["xs", "sm", "sm", "md", "md", "lg", "lg", "xl"]);
});

test("a long queue is split into batches the API accepts", () => {
  const events: ClientEvent[] = Array.from({ length: 45 }, () => ({ name: "generate_clicked", target: "android" }));
  const bodies = batches("3f1c2a9e-8b7d-4c6e-9a1b-2d3e4f5a6b7c", events);
  assert.deepEqual(
    bodies.map((body) => (JSON.parse(body) as { events: unknown[] }).events.length),
    [BATCH_MAX_EVENTS, BATCH_MAX_EVENTS, 5],
  );
  assert.ok(bodies.every((body) => body.length <= BATCH_MAX_BYTES));
  assert.deepEqual(batches("x", []), []);
});
