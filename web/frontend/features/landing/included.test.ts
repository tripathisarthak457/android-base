import assert from "node:assert/strict";
import { test } from "node:test";
import { loadCatalogue, lockedCatalogue } from "../catalogue/testing.ts";
import { includedItems } from "./included.ts";

/** What a project without UI components does not do: its components and shell bring the motion. */
const COMPONENT_PROMISES = [/slide rather than fade/, /predictive back/i, /\btab bar\b/i, /\bfloats?\b/];

test("a setup-only site promises nothing unmarked that a project without UI components lacks", () => {
  for (const item of includedItems(lockedCatalogue())) {
    if (item.components) continue;
    for (const promise of COMPONENT_PROMISES) {
      assert.doesNotMatch(item.body, promise, item.title);
    }
  }
});

test("a site that makes both kinds keeps every promise of a project with UI components", () => {
  const bodies = includedItems(loadCatalogue()).map((item) => item.body).join("\n");
  assert.match(bodies, /screens slide rather than fade/);
  assert.match(bodies, /Predictive back follows the finger\./);
});

test("both sites list the same items", () => {
  const titles = (items: { title: string }[]) => items.map((item) => item.title);
  assert.deepEqual(titles(includedItems(lockedCatalogue())), titles(includedItems(loadCatalogue())));
});
