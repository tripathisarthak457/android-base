import { COMING_WITH_RELEASE, androidOnlyNote, disabledFeatures, fieldCopy, hiddenFields, isSetupOnly, uiComponentsOption } from "../catalogue/options.ts";
import type { Catalogue, Target } from "../catalogue/types.ts";
import { planTabs } from "../preview/tabs.ts";
import { type ConfigState, moduleNames } from "./state.ts";

// What the steps say about the "No UI components" box, worked out here so the wording is tested
// without rendering a step.

/** The box at the top of the Features step, the note that stands in for it, or nothing. */
export type UiComponentsBox =
  | {
      kind: "box";
      label: string;
      headline: string;
      description: string;
      ticked: boolean;
      /** Why the box cannot be changed on this deployment, or null when it can. */
      lockedNote: string | null;
    }
  | { kind: "note"; text: string }
  | { kind: "none" };

export function uiComponentsBox(catalogue: Catalogue, target: Target, uiComponents: boolean): UiComponentsBox {
  const option = uiComponentsOption(target);
  if (!option) {
    const text = androidOnlyNote(catalogue, target.key);
    return text ? { kind: "note", text } : { kind: "none" };
  }
  return {
    kind: "box",
    label: option.checkbox.label,
    headline: option.headline,
    description: option.description,
    ticked: uiComponents === option.checkbox.ticked_value,
    lockedNote: option.locked?.note ?? null,
  };
}

/** How one feature's card reads: open to tick, or greyed with the reason it cannot be had. */
export type FeatureCard =
  | { disabled: false }
  | {
      disabled: true;
      reason: string;
      /** On a setup-only deployment, the release that brings it; null elsewhere. */
      release: string | null;
    };

export function featureCard(catalogue: Catalogue, target: Target, uiComponents: boolean, key: string): FeatureCard {
  const reason = disabledFeatures(target, uiComponents).get(key);
  if (reason === undefined) return { disabled: false };
  return { disabled: true, reason, release: isSetupOnly(catalogue) ? COMING_WITH_RELEASE : null };
}

const FIELD_TITLES: Record<string, string> = { design_style: "Design style", motion_style: "Motion style" };

/** Review's "Skipped, and why": each feature the answers had that the project leaves out, then each field it does not ask. */
export function reviewSkipped(target: Target, state: ConfigState): { title: string; reason: string }[] {
  const disabled = disabledFeatures(target, state.uiComponents);
  const titleOf = new Map(target.features.map((feature) => [feature.key, feature.title]));
  const features = state.skipped.flatMap((key) => {
    const reason = disabled.get(key);
    return reason === undefined ? [] : [{ title: titleOf.get(key) ?? key, reason }];
  });
  const fields = [...hiddenFields(target, state.uiComponents)].map(([field, reason]) => ({
    title: FIELD_TITLES[field] ?? field,
    reason,
  }));
  return [...features, ...fields];
}

/** The haptics toggle's words where a project has controls that play a tick. */
const HAPTICS_HINT =
  "A light tick when a selection changes, a switch flips or a pull reaches its threshold, as iOS does; never on an ordinary tap. " +
  "The phone's own setting still applies on top.";

/** What the Motion step's haptics toggle sets: the option's words in a project with no control to play a tick. */
export function hapticsHint(target: Target, uiComponents: boolean): string {
  return fieldCopy(target, uiComponents, "haptics_enabled") ?? HAPTICS_HINT;
}

/**
 * The last line of the Project step's "What that produces": the tabs a project with UI components
 * gets, a scaffolded home module among them when nothing else gives one. A project without them
 * has no shell, so no tab bar and no home module; it opens on its blank start screen.
 */
export function producesNote(state: ConfigState): string {
  if (!state.uiComponents) return "The app opens on a blank start screen, for your design system to build on.";
  const plan = planTabs(new Set(state.features), moduleNames(state.modules));
  const tabs = `Tabs: ${plan.tabs.map((tab) => tab.label).join(" · ")}`;
  return plan.scaffoldedHome ? `${tabs}. Nothing you picked provides a tab, so the project gets a home module with an empty screen to build from.` : tabs;
}
