import assert from "node:assert/strict";
import { test } from "node:test";
import { defaultFilters, query, windowOf, windowProblem } from "./filters.ts";

const now = new Date("2026-10-09T23:30:00+05:30");

test("a preset range ends today in UTC and includes both ends", () => {
  assert.deepEqual(windowOf({ ...defaultFilters(), range: "today" }, now), { from: "2026-10-09", to: "2026-10-09" });
  assert.deepEqual(windowOf({ ...defaultFilters(), range: "7d" }, now), { from: "2026-10-03", to: "2026-10-09" });
  assert.deepEqual(windowOf({ ...defaultFilters(), range: "30d" }, now), { from: "2026-09-10", to: "2026-10-09" });
});

test("only the filters that are set go into the query, the country uppercased", () => {
  const filters = { ...defaultFilters(), range: "7d" as const, target: "android", country: "in", source: " " };
  assert.equal(query(filters, now, { limit: 50 }), "from=2026-10-03&to=2026-10-09&target=android&country=IN&limit=50");
});

test("a custom window is checked the way the API checks it", () => {
  const custom = { ...defaultFilters(), range: "custom" as const };
  assert.equal(windowProblem({ ...custom, from: "", to: "2026-10-01" }), "Pick both dates.");
  assert.equal(windowProblem({ ...custom, from: "2026-10-02", to: "2026-10-01" }), "The start is after the end.");
  assert.match(windowProblem({ ...custom, from: "2025-01-01", to: "2026-10-01" }) ?? "", /400 days/);
  assert.equal(windowProblem({ ...custom, from: "2026-01-01", to: "2026-10-01" }), null);
  assert.equal(windowProblem(defaultFilters()), null);
});
