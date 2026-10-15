"use client";

import { useMemo } from "react";
import type { Tokens } from "../catalogue/types.ts";
import { type PreviewTheme, previewTheme } from "./theme.ts";

/** Everything a preview is drawn from: the visitor's answers that change how the app looks. */
export type PreviewInput = {
  tokens: Tokens;
  designStyle: string;
  motionStyle: string;
  accent: string;
  secondary: string;
  tertiary: string;
  font: string;
  features: ReadonlySet<string>;
  modules: string[];
  haptics: boolean;
};

export function usePreviewTheme(input: PreviewInput, dark: boolean): PreviewTheme {
  const { tokens, designStyle, motionStyle, accent, secondary, tertiary } = input;
  return useMemo(
    () => previewTheme(tokens, { designStyle, motionStyle, dark, accent, secondary, tertiary }),
    [tokens, designStyle, motionStyle, dark, accent, secondary, tertiary],
  );
}
