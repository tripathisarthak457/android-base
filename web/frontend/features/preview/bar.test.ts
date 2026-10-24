import assert from "node:assert/strict";
import { test } from "node:test";
import { loadCatalogue, readJson, readText } from "../catalogue/testing.ts";
import {
  BAR,
  DARK_OUTLINE_ALPHA,
  DRAWN_INDICATORS,
  DRAWN_TREATMENTS,
  SHELL_SPRINGS,
  TAB_REVEAL_SHIFT,
  TRANSLUCENT_ALPHA,
  barLook,
  isFloating,
  pillBox,
  pillSpan,
  pillWidth,
  pillCross,
  restingPills,
  revealFrom,
} from "./bar.ts";
import { dp } from "./theme.ts";

// The bar preview draws what `AppTabBar.kt` and `TabCells.kt` draw. Their enums, sizes and springs
// are read from the template here, so a treatment WP03 retires or a size it changes fails a test
// instead of leaving the site describing a bar the app no longer has.

const NAVIGATION = ["templates", "android", "core", "designsystem", "src", "main", "kotlin", "com", "base", "app", "core", "designsystem"];
const styleSource = readText(...NAVIGATION, "theme", "AppDesignStyle.kt");
const barSource = readText(...NAVIGATION, "component", "navigation", "AppTabBar.kt");
const cellsSource = readText(...NAVIGATION, "component", "navigation", "TabCells.kt");
const shellPath = ["templates", "android", "core", "navigation", "src", "main", "kotlin", "com", "base", "app", "core", "navigation"];

function enumEntries(source: string, name: string): string[] {
  const body = new RegExp(`enum class ${name} \\{([\\s\\S]*?)\\n\\}`).exec(source)?.[1];
  assert.ok(body, `enum class ${name} is not in the template`);
  const code = body.replace(/\/\*[\s\S]*?\*\//g, "").replace(/\/\/.*$/gm, "");
  return [...code.matchAll(/^\s*([A-Z]\w*)\s*,?\s*$/gm)].map(([, entry]) => entry ?? "");
}

function dpConstant(source: string, name: string): number {
  const value = new RegExp(`\\bval ${name} = ([\\d.]+)\\.dp\\b`).exec(source)?.[1];
  assert.ok(value, `${name} is not a dp constant in the template`);
  return Number(value);
}

function spring(source: string, name: string): { stiffness: number; dampingRatio: number } {
  const match = new RegExp(`\\bval ${name}\\b[^=]*= spring\\(dampingRatio = ([\\d.]+)f, stiffness = ([\\d.]+)f`).exec(source);
  assert.ok(match, `${name} is not a fixed spring in the template`);
  return { dampingRatio: Number(match[1]), stiffness: Number(match[2]) };
}

const colour = (role: string) => `<${role}>`;

test("the preview draws every bar treatment and indicator the template has, and no retired one", () => {
  const treatments = enumEntries(styleSource, "BarTreatment");
  const indicators = enumEntries(styleSource, "BarIndicator");
  assert.ok(treatments.length >= 3 && indicators.length >= 2, `${treatments} / ${indicators}`);
  for (const treatment of treatments) assert.ok(DRAWN_TREATMENTS.has(treatment), `the preview cannot draw ${treatment}`);
  // Glass is WP05's treatment, fenced behind liquid glass in the template it brings.
  assert.deepEqual(
    [...DRAWN_TREATMENTS].filter((treatment) => !treatments.includes(treatment)),
    treatments.includes("Glass") ? [] : ["Glass"],
  );
  assert.deepEqual([...DRAWN_INDICATORS].sort(), [...indicators].sort());
});

test("every design style in the catalogue uses a treatment and an indicator the preview draws", () => {
  const { tokens } = loadCatalogue();
  for (const [name, style] of Object.entries(tokens.styles)) {
    assert.ok(DRAWN_TREATMENTS.has(style.bar), `${name}: ${style.bar}`);
    assert.ok(DRAWN_INDICATORS.has(style.indicator), `${name}: ${style.indicator}`);
  }
});

test("the bar's sizes are the template's", () => {
  assert.equal(BAR.height, dpConstant(barSource, "BarHeight"));
  assert.equal(BAR.margin, dpConstant(barSource, "BarMargin"));
  assert.equal(BAR.lift, dpConstant(barSource, "BarLift"));
  assert.equal(BAR.dockedInset, dpConstant(barSource, "DockedInset"));
  assert.equal(BAR.pillHeight, dpConstant(barSource, "PillHeight"));
  assert.equal(BAR.minPill, dpConstant(barSource, "MinPillWidth"));
  assert.equal(BAR.maxPill, dpConstant(barSource, "MaxPillWidth"));
  assert.equal(BAR.slotGap, dpConstant(barSource, "SlotGap"));
  assert.equal(BAR.labelPadding, dpConstant(barSource, "LabelPadding"));
  assert.equal(BAR.labelPaddingMin, dpConstant(barSource, "LabelPaddingMin"));
  assert.equal(BAR.maxStretch, Number(/const val MAX_STRETCH = ([\d.]+)f/.exec(cellsSource)?.[1]));
  assert.equal(BAR.squash, Number(/const val SQUASH = ([\d.]+)f/.exec(cellsSource)?.[1]));
  assert.equal(BAR.bounceScale, Number(/const val BOUNCE_DOWN = ([\d.]+)f/.exec(cellsSource)?.[1]));
  assert.equal(BAR.minLabelShrink, Number(/const val MIN_LABEL_SHRINK = ([\d.]+)f/.exec(cellsSource)?.[1]));
});

test("the push and the tab bar move on the shell's own springs, whatever the motion style", () => {
  // When the template reads these from AppTheme.springs, as MOTION-PLAN rule 1 asks, the preview
  // and the Motion step go back to the motion style's springs and this test goes with them.
  assert.deepEqual(SHELL_SPRINGS.indicatorLead, spring(cellsSource, "LeadSpring"));
  assert.deepEqual(SHELL_SPRINGS.indicatorTrail, spring(cellsSource, "TrailSpring"));
  assert.deepEqual(SHELL_SPRINGS.iconBounceDown, spring(cellsSource, "BounceDown"));
  assert.deepEqual(SHELL_SPRINGS.iconBounceUp, spring(cellsSource, "BounceUp"));
  const transitions = readText(...shellPath, "transition", "NavTransitions.kt");
  const shell = readText(...shellPath, "AppShell.kt");
  assert.match(shell, /ScreenMotion\(response = screenResponse \?: ScreenMotion\.DEFAULT_RESPONSE\)/);
  const response = Number(/const val DEFAULT_RESPONSE = ([\d.]+)f/.exec(transitions)?.[1]);
  assert.equal(SHELL_SPRINGS.screen.dampingRatio, 1);
  assert.ok(Math.abs(SHELL_SPRINGS.screen.stiffness - ((2 * Math.PI) / response) ** 2) < 1e-6);
});

test("a tab arriving after a tap is nudged in from the side it comes from, on the shell's reveal spring", () => {
  const reveal = readText(...shellPath, "shell", "tabs", "TabReveal.kt");
  assert.deepEqual(SHELL_SPRINGS.tabReveal, spring(reveal, "RevealSpring"));
  assert.equal(TAB_REVEAL_SHIFT, dpConstant(reveal, "SHIFT"));
  assert.match(reveal, /direction = sign\(\(position\(selected\) - position\(shown\)\)\.toFloat\(\)\)/);
  // Only a switch the person makes is revealed, and not under reduced motion; any other is instant.
  const kept = readText(...shellPath, "shell", "tabs", "KeptTabs.kt");
  assert.match(kept, /animate = revealSwitch\(\) && !reduceMotion/);
  assert.equal(revealFrom(0, 2), 12);
  assert.equal(revealFrom(3, 1), -12);
  assert.equal(revealFrom(1, 1), 0);
});

test("Floating and Translucent are capsules, Docked sits on the edge, and an unknown treatment floats", () => {
  assert.equal(isFloating("Floating"), true);
  assert.equal(isFloating("Translucent"), true);
  assert.equal(isFloating("Glass"), true);
  assert.equal(isFloating("Docked"), false);
  assert.equal(isFloating("SomethingNew"), true);
});

test("a Pill indicator is a solid accent pill with onAccent content; Subtle tints it", () => {
  const pill = barLook({ bar: "Floating", indicator: "Pill", borderWidth: 1 }, colour, false);
  assert.equal(pill.indicator, "<accent>");
  assert.equal(pill.selectedContent, "<onAccent>");
  assert.equal(pill.content, "<contentSecondary>");
  const subtle = barLook({ bar: "Floating", indicator: "Subtle", borderWidth: 1 }, colour, false);
  assert.equal(subtle.indicator, "<accentSubtle>");
  assert.equal(subtle.selectedContent, "<accentContent>");
  // A style naming an indicator this build does not know gets the quieter one.
  assert.deepEqual(barLook({ bar: "Floating", indicator: "Underline", borderWidth: 1 }, colour, false), subtle);
});

test("barIndicator, which a Pill is filled with, is the accent in every palette", () => {
  const palette = readJson<unknown>("tokens", "palette.json");
  const found: unknown[] = [];
  const visit = (node: unknown) => {
    if (!node || typeof node !== "object") return;
    for (const [key, value] of Object.entries(node)) {
      if (key === "barIndicator") found.push(value);
      else visit(value);
    }
  };
  visit(palette);
  assert.ok(found.length >= 2);
  assert.deepEqual(new Set(found), new Set(["@accent"]));
});

test("a capsule is the surface with a hairline and a soft shadow in light theme, raised and unshadowed in dark", () => {
  assert.equal(TRANSLUCENT_ALPHA, Number(/\bTranslucentAlpha = ([\d.]+)f/.exec(barSource)?.[1]));
  assert.equal(DARK_OUTLINE_ALPHA, Number(/\bDarkOutline = Color\.White\.copy\(alpha = ([\d.]+)f\)/.exec(barSource)?.[1]));
  const light = barLook({ bar: "Floating", indicator: "Subtle", borderWidth: 2 }, colour, false);
  assert.deepEqual([light.floating, light.surface, light.border, light.borderWidth, light.shadow], [true, "<surface>", "<separator>", 2, true]);
  const dark = barLook({ bar: "Floating", indicator: "Subtle", borderWidth: 2 }, colour, true);
  assert.equal(dark.surface, "<surfaceElevated>");
  assert.equal(dark.border, `rgba(255, 255, 255, ${DARK_OUTLINE_ALPHA})`);
  assert.equal(dark.shadow, false);
  const translucent = barLook({ bar: "Translucent", indicator: "Subtle", borderWidth: 1 }, colour, false);
  assert.equal(translucent.surface, `color-mix(in srgb, <surface> ${Math.round(TRANSLUCENT_ALPHA * 100)}%, transparent)`);
  const docked = barLook({ bar: "Docked", indicator: "Pill", borderWidth: 2 }, colour, true);
  assert.deepEqual([docked.floating, docked.surface, docked.border, docked.borderWidth, docked.shadow], [false, "<surface>", "<separator>", 1, false]);
});

test("the pill fits its label with room to spare, between 52 and 72 dp, never past its slot", () => {
  assert.equal(pillWidth(120, 24), 52);
  assert.equal(pillWidth(120, 40), 60);
  assert.equal(pillWidth(120, 70), 78);
  assert.equal(pillWidth(120, 60), 72);
  assert.equal(pillWidth(64, 50), 56);
  assert.equal(pillWidth(56, 24), 48);
});

test("the pill stretches toward its target by at most half a slot, and thins as it does", () => {
  assert.deepEqual(pillSpan(1, 1, true), [1, 1]);
  assert.deepEqual(pillSpan(0.2, 0.6, true), [0.2, 0.6]);
  assert.deepEqual(pillSpan(0, 2, true), [1.5, 2]);
  assert.deepEqual(pillSpan(0, 2, false), [0, 0.5]);
  const resting = pillCross(10, 54, 1);
  assert.deepEqual(resting, [10, 54]);
  const [top, bottom] = pillCross(10, 54, 1.5);
  assert.ok(top > 10 && bottom < 54);
  assert.ok(Math.abs(bottom - top - 44 * (1 - BAR.squash * 0.5)) < 1e-9);
});

test("the pill is centred across the row the cells centre their content in, whatever its height", () => {
  // A capsule's hairline drawn as a CSS border takes its width from the row on each side, and the
  // cells centre their glyph and label in what is left. The template's border takes nothing
  // (TabRow keeps BarHeight), but the pill must follow the row either way, as PillSlots does.
  for (const height of [dp(BAR.height), dp(BAR.height) - 2 * 2, dp(BAR.height) - 2 * 0.75, dp(56)]) {
    const row = { width: dp(320), height, contents: [dp(24), dp(40), dp(31), dp(24)] };
    const resting = restingPills(row);
    const still = pillBox(row, resting, 1, 1, true);
    const inset = (height - dp(BAR.pillHeight)) / 2;
    assert.ok(Math.abs(still.top - inset) < 1e-9, `at ${height} px the pill's top is ${still.top}, not ${inset}`);
    assert.ok(Math.abs(height - still.bottom - inset) < 1e-9, `at ${height} px the pill's bottom inset is ${height - still.bottom}, not ${inset}`);
    for (const [start, end, forward] of [[1, 2.2, true], [2.6, 0.4, false], [0, 3, true]] as const) {
      const moving = pillBox(row, resting, start, end, forward);
      assert.ok(Math.abs((moving.top + moving.bottom) / 2 - height / 2) < 1e-9, `a stretched pill leaves the row's middle at ${height} px`);
    }
  }
});

test("each pill rests in the middle of its slot, as wide as pillWidth gives", () => {
  const row = { width: dp(320), height: dp(BAR.height), contents: [dp(24), dp(40), dp(31), dp(24)] };
  const { starts, ends } = restingPills(row);
  const slot = row.width / 4;
  row.contents.forEach((content, index) => {
    assert.ok(Math.abs((starts[index]! + ends[index]!) / 2 - (index + 0.5) * slot) < 1e-9);
    assert.ok(Math.abs(ends[index]! - starts[index]! - dp(pillWidth(slot / dp(1), content / dp(1)))) < 1e-9);
  });
  const halfway = pillBox(row, { starts, ends }, 1, 1.4, true);
  assert.ok(Math.abs(halfway.left - starts[1]!) < 1e-9);
  assert.ok(Math.abs(halfway.right - (ends[1]! + (ends[2]! - ends[1]!) * 0.4)) < 1e-9);
});
