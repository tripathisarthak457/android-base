"use client";

import { useEffect, useRef } from "react";
import { Configurator } from "../../features/configurator/Configurator.tsx";
import { typeface } from "../../features/configurator/state.ts";
import { useConfigurator } from "../../features/configurator/useConfigurator.ts";
import { provideReportContext } from "../../features/feedback/context.ts";
import { CATALOGUE } from "../../lib/catalogue.ts";

/**
 * The configurator's answers live in this island, so a keystroke re-renders the configurator and
 * nothing else on the landing page.
 */
export function ConfigureIsland() {
  const model = useConfigurator(CATALOGUE);
  const latest = useRef(model);
  useEffect(() => {
    latest.current = model;
  });

  // Read when the report dialog opens, so the report button never re-renders with the answers.
  useEffect(
    () =>
      provideReportContext(() => {
        const { state, target, preset } = latest.current;
        const minSdk = state.settings["android.min_sdk"];
        return {
          target: state.target,
          appName: state.appName,
          packageName: state.packageName,
          features: state.features,
          preset: preset ?? "custom",
          minSdk: typeof minSdk === "number" ? minSdk : 0,
          motionStyle: state.motionStyle,
          fontName: typeface(state.fontName, target),
          accentColour: state.accent,
        };
      }),
    [],
  );

  return <Configurator catalogue={CATALOGUE} model={model} />;
}
