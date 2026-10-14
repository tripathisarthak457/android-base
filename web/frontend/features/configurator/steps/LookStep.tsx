"use client";

import { memo, useDeferredValue, useId, useMemo, useState } from "react";
import { ChoiceCard, Field, TextInput } from "../../../components/primitives.tsx";
import type { Catalogue } from "../../catalogue/types.ts";
import { ScaledPreview } from "../../preview/AppPreview.tsx";
import { brandColours, parseHex, toHex } from "../../preview/brand.ts";
import { type FontStatus, useFamily, useSuggestionFonts } from "../../preview/fonts.ts";
import { LivePreview, type PreviewView } from "../../preview/LivePreview.tsx";
import { type PreviewTab, planTabs } from "../../preview/tabs.ts";
import { weightsFor } from "../../preview/theme.ts";
import { traits } from "../../preview/traits.ts";
import { type PreviewInput, usePreviewTheme } from "../../preview/usePreviewTheme.ts";
import type { ConfiguratorModel } from "../useConfigurator.ts";
import { usePreviewInput } from "../usePreviewInput.ts";

/** The screen that shows most of a style at once. */
function thumbnailScreen(tabs: PreviewTab[]): PreviewTab | undefined {
  return tabs.find((tab) => tab.screen === "profile") ?? tabs.find((tab) => tab.screen === "home") ?? tabs[0];
}

const StyleThumbnail = memo(function StyleThumbnail({ input, styleKey, dark }: { input: PreviewInput; styleKey: string; dark: boolean }) {
  const theme = usePreviewTheme({ ...input, designStyle: styleKey }, dark);
  const tabs = useMemo(() => planTabs(input.features, input.modules).tabs, [input.features, input.modules]);
  const screen = thumbnailScreen(tabs);
  const options = useMemo(() => ({ features: input.features, haptics: input.haptics }), [input.features, input.haptics]);
  if (!screen) return null;
  return <ScaledPreview theme={theme} font={input.font} tabs={tabs} screen={screen} options={options} scale={0.4} />;
});

export function LookStep({
  catalogue,
  model,
  view,
  onView,
}: {
  catalogue: Catalogue;
  model: ConfiguratorModel;
  view: PreviewView;
  onView: (view: PreviewView) => void;
}) {
  const { state, dispatch, target, errors } = model;
  const input = usePreviewInput(catalogue, state);
  // Four thumbnails redraw on every colour-picker move or keystroke, so they wait, as the live preview does.
  const thumbnailInput = useDeferredValue(input);
  const styleName = useId();
  const styleError = errors.get("design_style");
  const featureTitle = new Map(target.features.map((feature) => [feature.key, feature.title]));
  const accent = parseHex(state.accent);
  const derived = accent ? brandColours(accent, null, null) : null;

  return (
    <div className="grid grid-cols-1 gap-10 lg:grid-cols-[minmax(0,1fr)_300px]">
      <div className="min-w-0 space-y-8">
        <fieldset aria-describedby={styleError ? `${styleName}-error` : undefined}>
          <legend className="text-sm font-medium text-ink-200">Design style</legend>
          <p className="mt-1 text-xs leading-relaxed text-ink-400">
            Corners, borders, fields, the tab bar and the type voice, chosen together. One argument to{" "}
            <code className="text-ink-300">AppTheme</code>, and the components are yours to reshape from there.
          </p>
          <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {target.design_styles.map((style) => {
              const tokens = catalogue.tokens.styles[style.key];
              return (
                <ChoiceCard
                  key={style.key}
                  type="radio"
                  name={styleName}
                  checked={state.designStyle === style.key}
                  onChange={() => dispatch({ type: "designStyle", value: style.key })}
                  className="flex gap-3 overflow-hidden p-3"
                >
                  <StyleThumbnail input={thumbnailInput} styleKey={style.key} dark={view.dark} />
                  <span className="min-w-0 py-1">
                    <span className="block font-display text-base font-bold text-ink-100">{style.title}</span>
                    <span className="mt-1 block text-xs leading-relaxed text-ink-400">{style.description}</span>
                    {tokens && (
                      <span className="mt-2 block space-y-0.5">
                        {traits(tokens).map((trait) => (
                          <span key={trait} className="flex items-center gap-1.5 text-[11px] text-ink-300">
                            <span aria-hidden className="h-1 w-1 shrink-0 rounded-full bg-accent" />
                            {trait}
                          </span>
                        ))}
                      </span>
                    )}
                    {style.requires.length > 0 && (
                      <span className="mt-2 block text-[11px] text-amber">Turns on {style.requires.map((key) => featureTitle.get(key) ?? key).join(", ")}</span>
                    )}
                  </span>
                </ChoiceCard>
              );
            })}
          </div>
          {styleError && (
            <p id={`${styleName}-error`} className="mt-2 text-xs text-rose">
              {styleError}
            </p>
          )}
        </fieldset>

        <fieldset>
          <legend className="text-sm font-medium text-ink-200">Brand colours</legend>
          <p className="mt-1 text-xs leading-relaxed text-ink-400">
            Each becomes a full ramp: pressed, subtle, the dark-theme shade, and whichever text reads on it. Leave the other two
            empty and they are worked out from the primary.
          </p>
          <div className="mt-3 space-y-2.5">
            <ColourRow
              label="Primary"
              value={state.accent}
              shown={state.accent}
              error={errors.get("accent_colour")}
              onCommit={(value) => dispatch({ type: "colour", which: "accent", value })}
            />
            <ColourRow
              label="Secondary"
              value={state.secondary}
              shown={state.secondary || (derived ? toHex(derived.secondary) : "")}
              error={errors.get("secondary_colour")}
              onCommit={(value) => dispatch({ type: "colour", which: "secondary", value })}
              derivable
            />
            <ColourRow
              label="Tertiary"
              value={state.tertiary}
              shown={state.tertiary || (derived ? toHex(derived.tertiary) : "")}
              error={errors.get("tertiary_colour")}
              onCommit={(value) => dispatch({ type: "colour", which: "tertiary", value })}
              derivable
            />
          </div>
        </fieldset>

        <FontPicker
          model={model}
          weights={weightsFor(catalogue.tokens, catalogue.tokens.styles[state.designStyle]?.voice ?? "Neutral")}
        />
      </div>

      <LivePreview input={input} view={view} onView={onView} />
    </div>
  );
}

/**
 * One brand colour. The text field keeps whatever is typed and commits only a full hex, so a
 * half-typed value never reaches the preview or the request. A supporting colour can go back to
 * being worked out from the primary.
 */
function ColourRow({
  label,
  value,
  shown,
  error,
  onCommit,
  derivable,
}: {
  label: string;
  value: string;
  shown: string;
  error?: string;
  onCommit: (value: string) => void;
  derivable?: boolean;
}) {
  const [draft, setDraft] = useState(value);
  // A value set from outside, by the picker or Derive, replaces whatever is half-typed.
  const [synced, setSynced] = useState(value);
  if (value !== synced) {
    setSynced(value);
    setDraft(value);
  }
  const valid = draft === "" ? Boolean(derivable) : parseHex(draft) !== null;
  const swatch = parseHex(shown) ? `#${shown.replace(/^#/, "")}` : "#000000";
  const errorId = useId();
  const problem = valid ? error : "A six-digit hex, like #2C6BED.";

  return (
    <div>
      <div className="flex items-center gap-2.5">
        <span className="w-[70px] shrink-0 text-sm text-ink-300">{label}</span>
        <input
          type="color"
          value={swatch.toLowerCase()}
          onChange={(event) => onCommit(event.target.value.toUpperCase())}
          className="h-10 w-12 shrink-0 cursor-pointer rounded-lg border border-ink-500 bg-ink-900 p-1"
          aria-label={`${label} colour`}
        />
        <TextInput
          value={draft}
          placeholder={derivable ? `${shown} (from primary)` : "#2C6BED"}
          aria-label={`${label} colour as hex`}
          aria-invalid={problem ? true : undefined}
          aria-describedby={problem ? errorId : undefined}
          onChange={(event) => {
            const next = event.target.value.trim();
            setDraft(next);
            if (next === "" ? derivable : parseHex(next)) onCommit(next === "" ? "" : `#${next.replace(/^#/, "").toUpperCase()}`);
          }}
          className="font-mono text-sm uppercase"
          spellCheck={false}
        />
        {derivable && (
          <button
            type="button"
            // aria-disabled, not disabled: pressing Derive disables it, and a disabled button
            // drops the focus to the page.
            onClick={() => value && onCommit("")}
            aria-disabled={!value || undefined}
            className="shrink-0 rounded-md border border-ink-600 px-2 py-1.5 text-xs text-ink-300 transition-colors hover:border-ink-500 hover:text-ink-100 aria-disabled:cursor-default aria-disabled:border-ink-700 aria-disabled:text-ink-400"
          >
            {value ? "Derive" : "Derived"}
          </button>
        )}
      </div>
      {problem && (
        <p id={errorId} className="mt-1 pl-[80px] text-xs text-rose">
          {problem}
        </p>
      )}
    </div>
  );
}

function FontPicker({ model, weights }: { model: ConfiguratorModel; weights: number[] }) {
  const { state, dispatch, target, errors } = model;
  const suggestions = target.fields.find((field) => field.key === "font_name")?.suggestions ?? [];
  const names = useMemo(() => suggestions.map((suggestion) => suggestion.value), [suggestions]);
  useSuggestionFonts(names);
  const status: FontStatus = useFamily(state.fontName, weights);
  const groupName = useId();
  const family = `"${state.fontName}", var(--font-sans)`;
  const problem =
    errors.get("font_name") ??
    (status === "missing"
      ? "Google Fonts does not know this family. Check the spelling on fonts.google.com; the app would fall back to the system font."
      : undefined);

  return (
    <div>
      <Field label="Typeface" hint="Any family from fonts.google.com, spelled as on its page. Every text style reads it." error={problem}>
        {(control) => (
          <TextInput {...control} value={state.fontName} onChange={(event) => dispatch({ type: "fontName", value: event.target.value })} spellCheck={false} />
        )}
      </Field>
      <div role="radiogroup" aria-label="Suggested typefaces" className="mt-3 flex flex-wrap gap-1.5">
        {suggestions.map((suggestion) => {
          const on = state.fontName === suggestion.value;
          return (
            <label
              key={suggestion.value}
              title={suggestion.note}
              style={{ fontFamily: `"${suggestion.value}", var(--font-sans)` }}
              className={`cursor-pointer rounded-full border px-3 py-1 text-[13px] transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent ${
                on ? "border-accent bg-accent-dim text-accent-bright ring-1 ring-accent" : "border-ink-600 text-ink-300 hover:border-ink-500 hover:text-ink-100"
              }`}
            >
              <input type="radio" name={groupName} checked={on} onChange={() => dispatch({ type: "fontName", value: suggestion.value })} className="sr-only" />
              {suggestion.value}
            </label>
          );
        })}
      </div>
      <div className="mt-4 overflow-hidden rounded-xl border border-ink-700 bg-ink-900 p-5" style={{ fontFamily: family }}>
        <div className="flex items-end justify-between gap-4">
          <span className="text-[44px] font-bold leading-none text-ink-100">Aa</span>
          <span className="font-mono text-[11px] text-ink-400" aria-live="polite">
            {status === "checking" ? "loading…" : status === "ok" ? state.fontName : "not found"}
          </span>
        </div>
        <p className="mt-4 text-[24px] font-bold leading-tight tracking-tight text-ink-100">Welcome back</p>
        <p className="mt-1 text-[16px] font-semibold text-ink-200">Sunt aut facere repellat</p>
        <p className="mt-1 text-[14px] leading-relaxed text-ink-300">Quia et suscipit recusandae consequuntur expedita et cum reprehenderit molestiae.</p>
      </div>
    </div>
  );
}
