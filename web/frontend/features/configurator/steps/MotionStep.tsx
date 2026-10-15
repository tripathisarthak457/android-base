"use client";

import { useId } from "react";
import { ChoiceCard, PauseButton, Toggle, usePaused } from "../../../components/primitives.tsx";
import type { Catalogue, Spring } from "../../catalogue/types.ts";
import { LivePreview, type PreviewView } from "../../preview/LivePreview.tsx";
import { MotionPreview } from "../../preview/MotionPreview.tsx";
import { type PreviewInput, usePreviewTheme } from "../../preview/usePreviewTheme.ts";
import type { ConfiguratorModel } from "../useConfigurator.ts";
import { usePreviewInput } from "../usePreviewInput.ts";

function describe(spring: Spring | undefined): string {
  if (!spring) return "";
  return `${spring.response.toFixed(2)} s${spring.bounce > 0 ? ` · bounce ${spring.bounce.toFixed(2)}` : ""}`;
}

function MotionCard({ input, model, styleKey, title, description, name, dark, paused }: {
  input: PreviewInput;
  model: ConfiguratorModel;
  styleKey: string;
  title: string;
  description: string;
  name: string;
  dark: boolean;
  paused: boolean;
}) {
  const theme = usePreviewTheme({ ...input, motionStyle: styleKey }, dark);
  const springs = input.tokens.motion.styles[styleKey]?.springs;
  const rows: [string, string][] = [
    ["press", describe(springs?.press)],
    ["release", describe(springs?.release)],
    ["push and pop", describe(springs?.screen)],
    ["selection", describe(springs?.selection)],
  ];
  return (
    <ChoiceCard
      type="radio"
      name={name}
      checked={model.state.motionStyle === styleKey}
      onChange={() => model.dispatch({ type: "motionStyle", value: styleKey })}
      className="flex gap-4 p-4"
    >
      <MotionPreview theme={theme} tokens={input.tokens} font={input.font} paused={paused} />
      <span className="min-w-0">
        <span className="block font-display text-base font-bold text-ink-100">{title}</span>
        <span className="mt-1 block text-xs leading-relaxed text-ink-400">{description}</span>
        <span className="mt-3 block space-y-0.5 font-mono text-[10.5px]">
          {rows.map(([label, value]) => (
            <span key={label} className="flex gap-2">
              <span className="w-[86px] shrink-0 text-ink-400">{label}</span>
              <span className="text-ink-300">{value}</span>
            </span>
          ))}
        </span>
      </span>
    </ChoiceCard>
  );
}

export function MotionStep({
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
  const { state, dispatch, target } = model;
  const input = usePreviewInput(catalogue, target, state);
  const name = useId();
  const [paused, setPaused] = usePaused();
  return (
    <div className="grid grid-cols-1 gap-10 lg:grid-cols-[minmax(0,1fr)_300px]">
      <div className="min-w-0 space-y-6">
        <div className="flex flex-col items-start gap-3 sm:flex-row sm:justify-between sm:gap-4">
          <div>
            <p className="text-sm font-medium text-ink-200">How it moves</p>
            <p className="mt-1 text-xs leading-relaxed text-ink-400">
              Each loop runs the style&apos;s own springs from the design tokens: a press and its release, a screen pushed and popped,
              a tab switch. Hold a phone&apos;s button down with the mouse or a finger to feel the press. Every spring is interruptible
              and keeps its velocity in the app.
            </p>
          </div>
          <PauseButton paused={paused} onChange={setPaused} label="the motion previews" />
        </div>
        <fieldset className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <legend className="sr-only">Motion style</legend>
          {target.motion_styles.map((style) => (
            <MotionCard
              key={style.key}
              input={input}
              model={model}
              styleKey={style.key}
              title={style.title}
              description={style.description}
              name={name}
              dark={view.dark}
              paused={paused}
            />
          ))}
        </fieldset>

        <div className="flex items-start gap-3 rounded-xl border border-ink-700 bg-ink-900 p-4">
          <Toggle checked={state.hapticsEnabled} onChange={(value) => dispatch({ type: "haptics", value })} label="Haptics on by default" />
          <div>
            <p className="text-sm font-medium text-ink-100">Haptics on by default</p>
            <p className="mt-0.5 text-xs leading-relaxed text-ink-400">
              A light tick when a selection changes, a switch flips or a pull reaches its threshold, as iOS does; never on an
              ordinary tap. The phone&apos;s own setting still applies on top.
            </p>
          </div>
        </div>
      </div>

      <LivePreview input={input} view={view} onView={onView} />
    </div>
  );
}
