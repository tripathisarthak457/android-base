"use client";

import { useMemo } from "react";
import type { Catalogue, Target } from "../catalogue/types.ts";
import type { PreviewInput } from "../preview/usePreviewTheme.ts";
import { type ConfigState, moduleNames, typeface } from "./state.ts";

/**
 * The answers a preview is drawn from. It lives with the configurator rather than the preview, so
 * the preview stays free of the configurator's state; each part is memoised, so a keystroke in a
 * field the preview does not show leaves every phone as it was.
 */
export function usePreviewInput(catalogue: Catalogue, target: Target, state: ConfigState): PreviewInput {
  const features = useMemo(() => new Set(state.features), [state.features]);
  const modules = useMemo(() => moduleNames(state.modules), [state.modules]);
  const font = typeface(state.fontName, target);
  return useMemo(
    () => ({
      tokens: catalogue.tokens,
      designStyle: state.designStyle,
      motionStyle: state.motionStyle,
      accent: state.accent,
      secondary: state.secondary,
      tertiary: state.tertiary,
      font,
      features,
      modules,
      haptics: state.hapticsEnabled,
    }),
    [
      catalogue.tokens,
      state.designStyle,
      state.motionStyle,
      state.accent,
      state.secondary,
      state.tertiary,
      font,
      features,
      modules,
      state.hapticsEnabled,
    ],
  );
}
