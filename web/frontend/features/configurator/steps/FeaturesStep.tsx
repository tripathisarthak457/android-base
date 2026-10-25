"use client";

import { motion } from "framer-motion";
import { useId, useRef } from "react";
import { ChoiceCard } from "../../../components/primitives.tsx";
import { Badge } from "../../../components/ui.tsx";
import { selectableFeatures } from "../../catalogue/features.ts";
import { disabledFeatures, featureCopy, presetDescription } from "../../catalogue/options.ts";
import type { Catalogue, Target } from "../../catalogue/types.ts";
import { dependentsOf } from "../resolve.ts";
import type { ToggleHint } from "../state.ts";
import { type UiComponentsBox, featureCard, uiComponentsBox } from "../uiComponents.ts";
import type { ConfiguratorModel } from "../useConfigurator.ts";

function titles(keys: string[], byKey: Map<string, string>): string {
  return keys.map((key) => byKey.get(key) ?? key).join(", ");
}

function hintText(hint: ToggleHint, designStyle: string, target: Target, visible: (keys: string[]) => string[]): string {
  const titleOf = new Map(target.features.map((feature) => [feature.key, feature.title]));
  const styleTitle = (key: string) => target.design_styles.find((style) => style.key === key)?.title ?? key;
  const name = titleOf.get(hint.key) ?? hint.key;
  if (hint.on) return `Turning on ${name} also turned on what it needs: ${titles(visible(hint.alsoAdded), titleOf)}.`;
  const removed = visible(hint.alsoRemoved);
  const sentences = [removed.length > 0 ? `Turning off ${name} also turned off what needs it: ${titles(removed, titleOf)}.` : `${name} is off.`];
  if (hint.previousStyle !== designStyle) {
    sentences.push(`The ${styleTitle(hint.previousStyle)} look needs it, so the look is now ${styleTitle(designStyle)}.`);
  }
  return sentences.join(" ");
}

function Tick({ on }: { on: boolean }) {
  return (
    <span
      aria-hidden
      className={`mt-0.5 flex h-[18px] w-[18px] shrink-0 items-center justify-center rounded border text-[10px] ${
        on ? "border-accent bg-accent text-on-accent" : "border-ink-500"
      }`}
    >
      {on ? "✓" : ""}
    </span>
  );
}

/** The box drawn like a feature card, since it is one more choice about what the project holds. */
function UiComponentsChoice({ box, onToggle }: { box: UiComponentsBox; onToggle: () => void }) {
  const noteId = useId();
  if (box.kind === "none") return null;
  if (box.kind === "note") return <p className="mb-6 max-w-2xl text-sm text-ink-400">{box.text}</p>;
  return (
    <ChoiceCard
      type="checkbox"
      checked={box.ticked}
      unavailable={box.lockedNote !== null}
      describedBy={box.lockedNote !== null ? noteId : undefined}
      onChange={onToggle}
      className="mb-7 p-4"
    >
      <span className="flex items-start gap-3">
        <Tick on={box.ticked} />
        <span className="min-w-0">
          <span className="block text-sm font-semibold text-ink-100">{box.label}</span>
          <span className="mt-1 block text-sm text-ink-300">{box.headline}</span>
          <span className="mt-1.5 block text-xs leading-relaxed text-ink-400">{box.description}</span>
          {box.lockedNote !== null && (
            <span id={noteId} className="mt-2 block text-xs leading-relaxed text-amber">
              {box.lockedNote}
            </span>
          )}
        </span>
      </span>
    </ChoiceCard>
  );
}

export function FeaturesStep({ catalogue, model }: { catalogue: Catalogue; model: ConfiguratorModel }) {
  const { state, dispatch, target, preset, errors } = model;
  const presetName = useId();
  const reasonId = useId();
  const featuresError = errors.get("features");
  const featuresErrorId = useId();
  const selected = new Set(state.features);
  const titleOf = new Map(target.features.map((feature) => [feature.key, feature.title]));
  const offered = selectableFeatures(target);
  const current = target.presets.find((option) => option.key === preset);
  const shown = new Set(offered.map((feature) => feature.key));
  // Firebase and anything else nobody can tick arrive and leave with the features that need them,
  // so naming or counting them would only describe what the visitor cannot change. Review counts
  // the same way.
  const visible = (keys: string[]) => keys.filter((key) => shown.has(key));
  const hint =
    state.hint &&
    (visible(state.hint.alsoAdded).length > 0 || visible(state.hint.alsoRemoved).length > 0 || state.hint.previousStyle !== state.designStyle)
      ? state.hint
      : null;
  const style = target.design_styles.find((option) => option.key === state.designStyle);
  const toggled = useRef<HTMLInputElement>(null);
  // A preset keeps its list, so its count leaves out what this project cannot have, as the selection does.
  const disabled = disabledFeatures(target, state.uiComponents);
  const counted = (keys: string[]) => visible(keys.filter((key) => !disabled.has(key)));

  return (
    <div>
      <UiComponentsChoice
        box={uiComponentsBox(catalogue, target, state.uiComponents)}
        onToggle={() => dispatch({ type: "uiComponents", value: !state.uiComponents })}
      />
      {/* A fieldset rather than role=radiogroup, which may hold only radios: the group also shows
          its lead-in and the "Your own" badge. */}
      <fieldset className="flex flex-wrap items-center gap-2">
        <legend className="sr-only">Start from a preset</legend>
        <span aria-hidden className="mr-1 text-sm text-ink-300">
          Start from
        </span>
        {target.presets.map((option) => {
          const on = preset === option.key;
          return (
            <label
              key={option.key}
              title={presetDescription(target, state.uiComponents, option)}
              className={`cursor-pointer rounded-full border px-4 py-1.5 text-sm font-medium transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent ${
                on ? "border-accent bg-accent-dim text-accent-bright ring-1 ring-accent" : "border-ink-600 bg-ink-900 text-ink-300 hover:border-ink-500"
              }`}
            >
              <input type="radio" name={presetName} checked={on} onChange={() => dispatch({ type: "preset", key: option.key })} className="sr-only" />
              {option.title}
              <span className="ml-1.5 font-mono text-xs">{counted(option.features).length}</span>
            </label>
          );
        })}
        {!current && <Badge tone="accent">Your own · {visible(state.features).length}</Badge>}
      </fieldset>
      <p className="mt-3 text-sm text-ink-400">
        {current ? presetDescription(target, state.uiComponents, current) : "Your own selection. Ticking a feature also ticks what it needs."}
      </p>

      {/* Re-keyed on each toggle so a new hint replaces the last one instead of stacking under it. */}
      <div aria-live="polite">
        {hint && (
          <motion.p
            key={`${hint.key}-${hint.on}-${state.revision}`}
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-4 flex flex-wrap items-center gap-x-3 gap-y-1 rounded-lg border border-ink-600 bg-ink-900 px-4 py-2.5 text-sm text-ink-200"
          >
            <span>{hintText(hint, state.designStyle, target, visible)}</span>
            <button
              type="button"
              onClick={() => {
                // Undo takes the hint, and this button, away; the feature it changes back keeps the focus.
                toggled.current?.focus();
                dispatch({ type: "undoToggle" });
              }}
              className="font-semibold text-accent hover:text-accent-bright"
            >
              Undo
            </button>
          </motion.p>
        )}
      </div>
      {featuresError && (
        <p id={featuresErrorId} className="mt-3 text-sm text-rose">
          {featuresError}
        </p>
      )}

      <div className="mt-7 space-y-8">
        {target.groups.map((group) => {
          const inGroup = offered.filter((feature) => feature.group === group.key);
          if (inGroup.length === 0) return null;
          return (
            <fieldset key={group.key} aria-describedby={featuresError ? featuresErrorId : undefined}>
              <legend className="mb-3 text-sm font-semibold uppercase tracking-wide text-ink-200">{group.title}</legend>
              <div className="grid grid-cols-1 gap-2.5 md:grid-cols-2">
                {inGroup.map((feature) => {
                  const on = selected.has(feature.key);
                  const needed = on ? visible(dependentsOf(selected, feature.key, target)).map((key) => titleOf.get(key) ?? key) : [];
                  if (on && style?.requires.includes(feature.key)) needed.push(`the ${style.title} look`);
                  const card = featureCard(catalogue, target, state.uiComponents, feature.key);
                  const copy = featureCopy(target, state.uiComponents, feature);
                  const why = `${reasonId}-${feature.key}`;
                  return (
                    <ChoiceCard
                      key={feature.key}
                      type="checkbox"
                      checked={on}
                      unavailable={card.disabled}
                      describedBy={card.disabled ? why : undefined}
                      onChange={() => dispatch({ type: "feature", key: feature.key })}
                      inputRef={feature.key === hint?.key ? toggled : undefined}
                      className="p-4"
                    >
                      <span className="flex items-start gap-3">
                        <Tick on={on} />
                        <span className="min-w-0">
                          <span className="block text-sm font-semibold text-ink-100">{copy.headline}</span>
                          <span className="mt-1 block text-xs leading-relaxed text-ink-400">{copy.description}</span>
                          {feature.requires.length > 0 && (
                            <span className="mt-2 block font-mono text-[11px] text-ink-400">needs {titles(feature.requires, titleOf)}</span>
                          )}
                          {needed.length > 0 && <span className="mt-1 block font-mono text-[11px] text-amber">needed by {needed.join(", ")}</span>}
                          {feature.licence && <span className="mt-1 block font-mono text-[11px] text-ink-400">{feature.licence.name}</span>}
                          {card.disabled && (
                            <span id={why} className="mt-2 block text-xs leading-relaxed text-ink-300">
                              {card.reason}
                              {card.release && <span className="mt-1 block font-mono text-[11px] text-amber">{card.release}</span>}
                            </span>
                          )}
                        </span>
                      </span>
                    </ChoiceCard>
                  );
                })}
              </div>
            </fieldset>
          );
        })}
      </div>
    </div>
  );
}
