"use client";

import { motion } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { Button } from "../../components/primitives.tsx";
import type { Catalogue } from "../catalogue/types.ts";
import type { PreviewView } from "../preview/LivePreview.tsx";
import { STEP_LABELS, STEPS } from "./state.ts";
import { BuildStep } from "./steps/BuildStep.tsx";
import { FeaturesStep } from "./steps/FeaturesStep.tsx";
import { IdentityStep } from "./steps/IdentityStep.tsx";
import { LookStep } from "./steps/LookStep.tsx";
import { MotionStep } from "./steps/MotionStep.tsx";
import { PlatformStep } from "./steps/PlatformStep.tsx";
import { ReviewStep } from "./steps/ReviewStep.tsx";
import type { ConfiguratorModel } from "./useConfigurator.ts";

export function Configurator({ catalogue, model }: { catalogue: Catalogue; model: ConfiguratorModel }) {
  const { step, stepIndex, goTo, identityValid } = model;
  const [preview, setPreview] = useState<PreviewView>({ dark: false, screen: null });
  const heading = useRef<HTMLHeadingElement>(null);
  const shown = useRef(step);

  const bar = useRef<HTMLElement>(null);
  const startOver = useRef<HTMLButtonElement>(null);

  // After a step change the focus moves to the new panel's heading, so a keyboard or screen reader
  // user lands on what changed rather than staying on the Next button. It compares with the step
  // last shown, not a first-run flag: Strict Mode runs the effect twice on mount, and a focus then
  // scrolls the page down to here on load. On a narrow screen the step bar scrolls sideways, so
  // the current step is brought to its middle.
  useEffect(() => {
    if (shown.current !== step) heading.current?.focus();
    shown.current = step;
    const nav = bar.current;
    const current = nav?.querySelector<HTMLElement>('[aria-current="step"]');
    if (nav && current) {
      const offset = current.getBoundingClientRect().left - nav.getBoundingClientRect().left + nav.scrollLeft;
      const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
      nav.scrollTo({ left: offset - (nav.clientWidth - current.clientWidth) / 2, behavior: reduce ? "auto" : "smooth" });
    }
  }, [step]);

  const reachable = (index: number) => index <= 1 || identityValid;
  const next = STEPS[stepIndex + 1];
  const previous = STEPS[stepIndex - 1];

  return (
    <section id="configure" aria-labelledby="configure-title" className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="max-w-2xl">
          <h2 id="configure-title" className="font-display text-4xl font-bold text-ink-100">
            Configure your project
          </h2>
          <p className="mt-3 text-ink-300">
            Every step has a working default, so you can jump to Review and download something sensible right now. Your
            answers stay in this browser until you start over.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <p aria-live="polite" className="text-sm text-ink-300">
            {model.canUndoStartOver && (
              <>
                Answers cleared.{" "}
                <button
                  type="button"
                  onClick={() => {
                    model.undoStartOver();
                    // The Undo button is about to go; a step change then moves focus to its heading.
                    startOver.current?.focus();
                  }}
                  className="font-semibold text-accent hover:text-accent-bright"
                >
                  Undo
                </button>
              </>
            )}
          </p>
          <Button ref={startOver} variant="ghost" size="small" onClick={model.startOver}>
            Start over
          </Button>
        </div>
      </div>

      <nav ref={bar} aria-label="Steps" className="no-scrollbar mt-8 overflow-x-auto pb-1">
        <ol className="flex gap-2">
          {STEPS.map((id, index) => {
            const active = id === step;
            const done = index < stepIndex;
            return (
              <li key={id} className="shrink-0">
                <button
                  type="button"
                  disabled={!reachable(index)}
                  aria-current={active ? "step" : undefined}
                  onClick={() => goTo(id)}
                  className={`relative rounded-lg border px-4 py-2.5 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-40 ${
                    active ? "border-accent/50 bg-accent-dim" : done ? "border-ink-600 bg-ink-850" : "border-ink-700 bg-ink-900"
                  }`}
                >
                  <span className="flex items-center gap-2">
                    <span
                      aria-hidden
                      className={`flex h-5 w-5 items-center justify-center rounded-full font-mono text-[11px] ${
                        done ? "bg-mint/20 text-mint" : active ? "bg-accent text-on-accent" : "bg-ink-700 text-ink-300"
                      }`}
                    >
                      {done ? "✓" : index + 1}
                    </span>
                    <span className={`text-sm font-medium ${active ? "text-ink-100" : "text-ink-300"}`}>{STEP_LABELS[id]}</span>
                    {/* The button is `relative` so this absolutely placed text stays inside the
                        scrolling step bar instead of widening the page. */}
                    {done && <span className="sr-only"> (done)</span>}
                  </span>
                </button>
              </li>
            );
          })}
        </ol>
      </nav>

      {/* overflow-clip rather than overflow-hidden: it clips the step slide without becoming the
          scroll container, which would stop the live preview from sticking. The step slides in
          without a fade: opacity below 1 would render the whole panel, live preview included,
          offscreen on every frame, and cut a glass bar's backdrop off from the page meanwhile. */}
      <div className="mt-6 overflow-clip rounded-xl border border-ink-700 bg-ink-850">
        <motion.div
          key={step}
          initial={{ x: 22 }}
          animate={{ x: 0 }}
          transition={{ duration: 0.24, ease: [0.16, 1, 0.3, 1] }}
          className="p-4 sm:p-6 md:p-8"
        >
          {/* Visible, so the focus a step change puts here shows where a keyboard user now is. */}
          <h3 ref={heading} tabIndex={-1} className="mb-6 w-fit font-display text-2xl font-bold text-ink-100">
            <span className="sr-only">{`Step ${stepIndex + 1} of ${STEPS.length}: `}</span>
            {STEP_LABELS[step]}
          </h3>
          {step === "platform" && <PlatformStep catalogue={catalogue} model={model} />}
          {step === "identity" && <IdentityStep catalogue={catalogue} model={model} />}
          {step === "features" && <FeaturesStep model={model} />}
          {step === "look" && <LookStep catalogue={catalogue} model={model} view={preview} onView={setPreview} />}
          {step === "motion" && <MotionStep catalogue={catalogue} model={model} view={preview} onView={setPreview} />}
          {step === "build" && <BuildStep catalogue={catalogue} model={model} />}
          {step === "review" && <ReviewStep catalogue={catalogue} model={model} dark={preview.dark} />}
        </motion.div>

        <div className="flex items-center justify-between border-t border-ink-700 bg-ink-900/60 px-4 py-4 sm:px-6">
          <Button variant="ghost" onClick={() => previous && goTo(previous)} disabled={!previous}>
            Back
          </Button>
          <span className="font-mono text-xs text-ink-400">
            {stepIndex + 1} / {STEPS.length}
          </span>
          {/* Kept, invisible, on the last step so the counter stays centred. */}
          <Button onClick={() => next && goTo(next)} disabled={!next || !reachable(stepIndex + 1)} className={next ? "" : "invisible"}>
            Next
          </Button>
        </div>
      </div>
    </section>
  );
}
