import assert from "node:assert/strict";
import { readdirSync } from "node:fs";
import { test } from "node:test";
import { readText, repositoryPath } from "./testing.ts";

// What the hero, the included cards and the FAQ say about the template in their own words, each
// beside the code that makes it true. A template change that makes a sentence false fails here and
// names the sentence to reword; rewording a sentence means changing its row.

const PACKAGE = ["src", "main", "kotlin", "com", "base", "app"];
const template = (...path: string[]) => readText("templates", "android", ...path);
const guard = (file: string) => template("build-logic", "convention", ...PACKAGE, "buildlogic", "guard", file);

/** Every main Kotlin source of a template module, joined. */
function moduleSources(...module: string[]): string {
  const root = repositoryPath("templates", "android", ...module, "src", "main", "kotlin");
  return readdirSync(root, { recursive: true, encoding: "utf8" })
    .filter((path) => path.endsWith(".kt"))
    .map((path) => readText("templates", "android", ...module, "src", "main", "kotlin", ...path.split(/[\\/]/)))
    .join("\n");
}

/** A component's copy with its string concatenations joined and its whitespace collapsed. */
function copyOf(file: string): string {
  return readText("web", "frontend", "components", file)
    .replace(/["`]\s*\+\s*["`]/g, "")
    .replace(/\s+/g, " ");
}

type Claim = { file: string; says: string; holds: () => void };

const refusesMaterial = () => {
  const versions = template("gradle", "libs.versions.toml");
  assert.doesNotMatch(versions, /(group = "|module = ")androidx\.compose\.material3?[":]/, "a Material component library is a dependency");
  assert.match(guard("ComposeRules.kt"), /id = "material",/, "the build no longer refuses Material imports");
};

const CLAIMS: Claim[] = [
  { file: "faq.tsx", says: "No Material component library is a dependency and the build refuses their imports", holds: refusesMaterial },
  { file: "hero.tsx", says: "the build refuses Material imports", holds: refusesMaterial },
  { file: "included.tsx", says: "The build refuses Material component imports and the ripple", holds: refusesMaterial },
  {
    file: "faq.tsx",
    says: "window size classes come from androidx.window",
    holds: () =>
      assert.match(
        template("core", "designsystem", ...PACKAGE, "core", "designsystem", "foundation", "adaptive", "SizeClass.kt"),
        /^import androidx\.window\.core\.layout\.computeWindowSizeClass$/m,
      ),
  },
  {
    file: "included.tsx",
    says: "the tabs out of sight pause",
    holds: () =>
      assert.match(
        template("core", "navigation", ...PACKAGE, "core", "navigation", "shell", "tabs", "KeptTabs.kt"),
        /owner\.cap = Lifecycle\.State\.CREATED/,
        "a hidden tab's lifecycle is no longer held at CREATED",
      ),
  },
  {
    file: "included.tsx",
    says: "The tab bar becomes a rail on wider windows and a sidebar on large ones",
    holds: () => {
      const policy = template("core", "navigation", ...PACKAGE, "core", "navigation", "shell", "ShellChromePolicy.kt");
      assert.match(policy, /width == WindowWidth\.Medium -> NavigationChrome\.Rail/);
      assert.match(policy, /\(width >= WindowWidth\.Large\) -> NavigationChrome\.Sidebar/);
    },
  },
  {
    file: "included.tsx",
    says: "Locking an activity to one orientation fails the build",
    holds: () => assert.match(guard("ManifestRules.kt"), /LOCKS = listOf\("screenOrientation"/),
  },
  {
    file: "included.tsx",
    says: "motion styles set how presses spring back and how sheets, dialogs and layout changes move",
    holds: () => {
      const designSystem = moduleSources("core", "designsystem");
      for (const spring of ["press", "release", "sheet", "dialog", "layout"]) {
        assert.match(designSystem, new RegExp(`springs\\.${spring}\\b`), `the design system no longer reads springs.${spring}`);
      }
    },
  },
  {
    file: "hero.tsx",
    says: "Spring presets for presses, sheets and dialogs",
    holds: () => {
      const designSystem = moduleSources("core", "designsystem");
      for (const spring of ["press", "sheet", "dialog"]) assert.match(designSystem, new RegExp(`springs\\.${spring}\\b`));
    },
  },
];

test("each sentence checked here is still on the site, word for word", () => {
  for (const { file, says } of CLAIMS) {
    assert.ok(copyOf(file).includes(says), `components/${file} no longer says "${says}": reword its row in claims.test.ts`);
  }
});

test("each sentence the site says about the template is true of the template", () => {
  for (const { file, says, holds } of CLAIMS) {
    try {
      holds();
    } catch (error) {
      assert.fail(`components/${file} says "${says}", which the template no longer makes true: ${(error as Error).message}`);
    }
  }
});
