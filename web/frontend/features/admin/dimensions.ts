// The breakdowns of the Adoption panel, keyed as the API's adoption answer names them.

/** Adoption's dimensions besides features, in the API's order, each with the panel's heading. */
export const ADOPTION_DIMENSIONS: readonly (readonly [key: string, label: string])[] = [
  ["presets", "Nearest preset"],
  ["ui_components", "UI components"],
  ["architectures", "Architecture"],
  ["design_styles", "Design style"],
  ["motion_styles", "Motion style"],
  ["languages", "Languages"],
];

/** What the weekly heatmap can show: features first, then every dimension. */
export const WEEKLY_DIMENSIONS: readonly (readonly [key: string, label: string])[] = [["features", "Features"], ...ADOPTION_DIMENSIONS];
