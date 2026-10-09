"use client";

import { useCallback, useEffect, useRef } from "react";
import { Configurator } from "../../features/configurator/Configurator.tsx";
import { useConfigurator } from "../../features/configurator/useConfigurator.ts";
import { FeedbackButton, type ReportContext } from "../../features/feedback/Feedback.tsx";
import { CATALOGUE } from "../../lib/catalogue.ts";

/**
 * The page's one stateful island. The configurator's answers live here, so a keystroke re-renders
 * the configurator and nothing else on the landing page.
 */
export function ConfigureIsland() {
  const model = useConfigurator(CATALOGUE);
  const latest = useRef(model);
  useEffect(() => {
    latest.current = model;
  });

  // Read when the report dialog opens, so the report button never re-renders with the answers.
  const context = useCallback((): ReportContext => {
    const { state, preset } = latest.current;
    const minSdk = state.settings["android.min_sdk"];
    return {
      target: state.target,
      appName: state.appName,
      packageName: state.packageName,
      features: state.features,
      preset: preset ?? "custom",
      minSdk: typeof minSdk === "number" ? minSdk : 0,
      motionStyle: state.motionStyle,
      fontName: state.fontName,
      accentColour: state.accent,
    };
  }, []);

  return (
    <>
      <Configurator catalogue={CATALOGUE} model={model} />
      <FeedbackButton context={context} />
    </>
  );
}
