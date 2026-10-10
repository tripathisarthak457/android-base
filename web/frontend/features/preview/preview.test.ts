import assert from "node:assert/strict";
import { test } from "node:test";
import { loadCatalogue } from "../catalogue/testing.ts";
import { MAX_TABS, moduleTitle, planTabs, screensFor } from "./tabs.ts";
import { css, dp, previewTheme, weightsFor } from "./theme.ts";
import { traits } from "./traits.ts";

const { tokens } = loadCatalogue();

const choices = { designStyle: "Social", motionStyle: "Bouncy", dark: false, accent: "#E4405F", secondary: "", tertiary: "" };

test("roles come from the style's palette, brand roles from the visitor's colour", () => {
  const theme = previewTheme(tokens, choices);
  assert.equal(theme.colour("background"), css(String(tokens.palette.Social?.light.background)));
  assert.equal(theme.colour("accent"), "#E4405F");
  assert.notEqual(theme.colour("accentContent"), css(String(tokens.palette.Social?.light.accentContent)));
  assert.equal(theme.colour("noSuchRole"), "transparent");
});

test("a style or motion style this build does not know falls back instead of throwing", () => {
  const theme = previewTheme(tokens, { ...choices, designStyle: "Brutal", motionStyle: "Wobbly" });
  assert.equal(theme.styleKey, tokens.defaults.designStyle);
  assert.ok(theme.spring("screen").stiffness > 0);
  assert.equal(theme.type("noSuchRole").fontSize, dp(17));
});

test("a new style added to the tokens renders with its own tokens", () => {
  const glass = { ...tokens.styles.Utility!, bar: "Glass", surfaces: "Cool" };
  const withGlass = { ...tokens, styles: { ...tokens.styles, Glass: glass }, palette: { ...tokens.palette, Glass: tokens.palette.Utility! } };
  const theme = previewTheme(withGlass, { ...choices, designStyle: "Glass" });
  assert.equal(theme.style.bar, "Glass");
});

test("an unfinished hex leaves the template's brand roles in place", () => {
  const theme = previewTheme(tokens, { ...choices, accent: "#E44" });
  assert.equal(theme.colour("accent"), css(String(tokens.palette.Social?.light.accent)));
});

test("type takes the voice's weight and the scale's size", () => {
  const editorial = previewTheme(tokens, { ...choices, designStyle: "Editorial" });
  const largeTitle = tokens.type.roles.largeTitle!;
  assert.equal(editorial.type("largeTitle").fontWeight, largeTitle.weight.Editorial);
  assert.equal(editorial.type("largeTitle").fontSize, dp(largeTitle.size));
  assert.deepEqual(weightsFor(tokens, "Rounded").at(-1), 900);
});

test("springs carry the token's stiffness and damping ratio", () => {
  const spring = previewTheme(tokens, choices).spring("selection");
  const token = tokens.motion.styles.Bouncy!.springs.selection;
  assert.equal(spring.stiffness, token.stiffness);
  assert.ok(Math.abs(spring.damping - token.dampingRatio * 2 * Math.sqrt(token.stiffness)) < 1e-9);
});

test("colours with alpha are written the CSS way", () => {
  assert.equal(css("#FF2C6BED"), "#2c6bed");
  assert.equal(css("#735B8DEF"), "#5b8def73");
});

test("tabs follow the template's order, then modules, at most five", () => {
  const plan = planTabs(new Set(["sample", "paging", "search", "profile", "settings"]), ["orders", "wallet"]);
  assert.deepEqual(plan.tabs.map((tab) => tab.label), ["Home", "Feed", "Search", "Profile", "Settings"]);
  assert.deepEqual(plan.overflow, ["Orders", "Wallet"]);
  assert.equal(plan.tabs.length, MAX_TABS);
});

test("scaffolded modules get distinct icons", () => {
  const plan = planTabs(new Set(), ["orders", "wallet", "trips"]);
  assert.deepEqual(plan.tabs.map((tab) => tab.label), ["Orders", "Wallet", "Trips"]);
  assert.equal(new Set(plan.tabs.map((tab) => tab.icon)).size, 3);
});

test("with no tab source the generator's home module is the one tab", () => {
  const plan = planTabs(new Set(["network", "auth"]), []);
  assert.equal(plan.scaffoldedHome, true);
  assert.deepEqual(plan.tabs.map((tab) => [tab.screen, tab.label]), [["module", "Home"]]);
  assert.deepEqual(screensFor(new Set(["auth"]), []).map((tab) => tab.screen), ["signin", "module"]);
});

test("module titles read like the scaffolded screen's", () => {
  assert.equal(moduleTitle("order_history"), "Order history");
});

test("a style describes itself from its tokens, so a new one needs no copy", () => {
  const style = {
    ...tokens.styles.Utility!,
    surfaces: "BrandTint",
    voice: "Bold",
    buttonRadius: "pill" as const,
    card: "Raised",
    field: "Filled",
    uppercaseLabels: false,
    bar: "Floating",
  };
  assert.deepEqual(traits(style), ["Brand tint surfaces, bold type", "Pill buttons, raised cards", "Filled fields", "Floating tab bar"]);
});

test("buttons read square up to 4 dp of radius and rounded above it", () => {
  const base = { ...tokens.styles.Utility!, uppercaseLabels: true };
  assert.match(traits({ ...base, buttonRadius: 4 })[1] ?? "", /^Square buttons/);
  assert.match(traits({ ...base, buttonRadius: 5 })[1] ?? "", /^Rounded buttons/);
  assert.match(traits(base)[2] ?? "", /, uppercase labels$/);
});
