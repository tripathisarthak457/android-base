import assert from "node:assert/strict";
import { test } from "node:test";
import { readText } from "../catalogue/testing.ts";
import { GLYPHS } from "./glyphs.ts";
import { FALLBACK_GLYPHS, NAMED_GLYPHS, TEMPLATE_TABS, planTabs } from "./tabs.ts";

// The bar's glyphs as the generator chooses them (`generator/genkit/targets/android/tabs.py`) and
// as the template declares its own tabs (`app/.../ui/AppDestinations.kt`).

const tabsPy = readText("generator", "genkit", "targets", "android", "tabs.py");
const destinations = readText(..."templates/android/app/src/main/kotlin/com/base/app/ui/AppDestinations.kt".split("/"));

const quoted = (text: string) => [...text.matchAll(/"(\w+)"/g)].map(([, word]) => word ?? "");

test("the glyph tables are the generator's", () => {
  const named = /_GLYPHS[^=]*= \(([\s\S]*?)\n\)/.exec(tabsPy)?.[1] ?? "";
  const rows = [...named.matchAll(/^\s*\(\(([^)]*)\),\s*\(([^)]*)\)\),/gm)].map(([, keys = "", options = ""]) => [quoted(keys), quoted(options)]);
  assert.ok(rows.length > 5);
  assert.deepEqual(NAMED_GLYPHS, rows);
  assert.deepEqual(FALLBACK_GLYPHS, quoted(/_FALLBACK = \(([^)]*)\)/.exec(tabsPy)?.[1] ?? ""));
  const own = /^_LEADING = [\s\S]*?^_TRAILING = \([\s\S]*?^\)/m.exec(tabsPy)?.[0] ?? "";
  const template = [...own.matchAll(/\("(\w+)", "(\w+)", "(\w+)"\)/g)].map(([, feature, label, glyph]) => ({ feature, label, glyph }));
  assert.deepEqual(
    TEMPLATE_TABS.map(({ feature, tab }) => ({ feature, label: tab.label, glyph: tab.icon })),
    template,
  );
});

test("the template's own tabs are drawn with the glyphs AppDestinations gives them", () => {
  const declared = [...destinations.matchAll(/ShellTab\(key = \w+, label = R\.string\.tab_(\w+), icon = AppIcons\.(\w+)(?:, selectedIcon = AppIcons\.(\w+))?\)/g)];
  assert.equal(declared.length, TEMPLATE_TABS.length);
  for (const [, key, icon, selected] of declared) {
    const tab = TEMPLATE_TABS.find((candidate) => candidate.tab.key === key)?.tab;
    assert.ok(tab, `the preview has no ${key} tab`);
    assert.equal(tab.icon, icon);
    assert.equal(tab.selectedIcon ?? tab.icon, selected ?? icon);
  }
});

test("a module's glyph follows its name, filled when selected if the icon set has a filled form", () => {
  const plan = planTabs(new Set(["sample"]), ["orders", "wallet", "team_chat"]);
  assert.deepEqual(
    plan.tabs.slice(1).map((tab) => [tab.icon, tab.selectedIcon]),
    [
      ["Package", "PackageFilled"],
      ["CreditCard", "CreditCardFilled"],
      ["Message", "MessageFilled"],
    ],
  );
});

test("a name that matches nothing takes the next free fallback, and no two tabs share a glyph", () => {
  const plan = planTabs(new Set(["sample", "search"]), ["reports", "stats", "favourites", "notes"]);
  const icons = plan.all.map((tab) => tab.icon);
  assert.deepEqual(icons, ["Home", "Grid", "Tag", "Heart", "Bookmark", "Search"]);
  assert.equal(new Set(icons).size, icons.length);
});

test("past every fallback the glyphs run on through the icon set's outlines, as the generator's do", () => {
  const names = Array.from({ length: 12 }, (_, index) => `area${index}`);
  const icons = planTabs(new Set(), names).all.map((tab) => tab.icon);
  assert.deepEqual(icons.slice(0, 8), [...FALLBACK_GLYPHS]);
  const outlines = Object.keys(GLYPHS)
    .filter((name) => !name.endsWith("Filled"))
    .sort();
  assert.deepEqual(icons.slice(8), outlines.filter((name) => !FALLBACK_GLYPHS.includes(name)).slice(0, 4));
});

test("the scaffolded home module and More take the generator's and the shell's glyphs", () => {
  assert.deepEqual(planTabs(new Set(["network"]), []).tabs.map((tab) => [tab.icon, tab.selectedIcon]), [["Grid", "GridFilled"]]);
  const more = planTabs(new Set(["sample", "paging", "search", "profile", "settings"]), ["orders"]).tabs.at(-1);
  assert.equal(more?.icon, "MoreHorizontal");
});
