// Catalogue v2, as `python3 -m genkit.core.catalogue` emits it. The site imports the checked-in
// copy (`lib/catalogue.json`) and reads the live one from `/api/v1/catalogue` only for what differs
// per deployment.

export type TargetStatus = "available" | "beta" | "coming_soon" | "hidden";

export type Feature = {
  key: string;
  title: string;
  headline: string;
  description: string;
  group: string;
  default: boolean;
  /** Direct dependencies. */
  requires: string[];
  /** Everything `requires` pulls in, transitively. */
  implies: string[];
  /** Reached only through another feature; never offered on its own. */
  implied_only: boolean;
  licence: { name: string; spdx: string; url: string } | null;
};

export type Group = { key: string; title: string };

export type Preset = {
  key: string;
  title: string;
  description: string;
  /** Already closed over `requires`. */
  features: string[];
  default: boolean;
};

export type Choice = { value: number | string; label: string; needs_desugaring?: boolean };

export type Field = {
  key: string;
  type: string;
  required?: boolean;
  rule?: string;
  default?: unknown;
  min?: number;
  max?: number;
  /** Shown only while every one of these features is on. */
  requires?: string[];
  /** The fixed keys of a map field, such as the four build flavours. */
  keys?: string[];
  /** The fixed names of a keystores field. */
  names?: string[];
  choices?: Choice[];
  suggestions?: { value: string; note: string }[];
};

export type Architecture = { key: string; title: string; description: string; available: boolean };

export type DesignStyle = { key: string; title: string; description: string; requires: string[] };

export type MotionStyle = { key: string; title: string; description: string };

export type Language = {
  /** A BCP 47 tag, as the spec takes it: "es", "pt-BR". */
  tag: string;
  /** The language's name for itself, which is how people look for their own. */
  name: string;
};

export type TargetDefaults = {
  preset: string;
  architecture: string;
  design_style: string;
  motion_style: string;
  font_name: string;
  mono_font_name: string;
  accent_colour: string;
  haptics_enabled: boolean;
  languages: string[];
  android?: Record<string, unknown>;
  ios?: Record<string, unknown>;
};

/**
 * A deployment that generates only one value of an option, with the sentence that says why. The
 * API refuses any other value, so the site shows the box set and fixed.
 */
export type OptionLock = { value: boolean; note: string };

/**
 * A spec-level switch the template declares, such as `ui_components`. The spec field is the key; a
 * spec that leaves it out gets `default`.
 */
export type Option = {
  key: string;
  title: string;
  /** The box the site and the wizard show, and the value a ticked box stands for. */
  checkbox: { label: string; ticked_value: boolean };
  headline: string;
  description: string;
  default: boolean;
  /** Features a project cannot have while the option is not at its default, in catalogue order. */
  disables: { feature: string; reason: string }[];
  /** Spec fields that mean nothing while the option is not at its default. */
  hides: { field: string; reason: string }[];
  /**
   * How kept features read while the option is not at its default, for those whose own copy names
   * what such a project lacks (a tab, a component, the harness); the rest keep their own.
   */
  feature_copy: { feature: string; headline: string; description: string }[];
  /** How presets read while the option is not at its default, likewise. */
  preset_copy: { preset: string; description: string }[];
  locked: OptionLock | null;
};

export type Target = {
  key: string;
  title: string;
  status: TargetStatus;
  status_note: string;
  summary: string;
  groups: Group[];
  features: Feature[];
  presets: Preset[];
  defaults: TargetDefaults;
  fields: Field[];
  architectures: Architecture[];
  design_styles: DesignStyle[];
  motion_styles: MotionStyle[];
  languages: Language[];
  /** Empty for a target that has none. */
  options: Option[];
};

export type Rules = {
  app_name: string;
  app_name_max: number;
  display_name_max: number;
  package_max: number;
  package_segments_min: number;
  package_segments_max: number;
  package_first_segment_forbidden: string[];
  segment: string;
  segment_max: number;
  module_name: string;
  module_name_max: number;
  feature_modules_max: number;
  features_max: number;
  languages_max: number;
  keystores_max: number;
  spec_bytes_max: number;
  reserved_words: Record<string, string[]>;
  template_module_names: Record<string, string[]>;
  hex: string;
  font: string;
  version_name: string;
  version_code_max: number;
  min_compile_sdk: number;
  http_url: string;
  socket_url: string;
  scheme: string;
  host: string;
  flavours: string[];
  keystore_names: string[];
  keystore_alias: string;
  keystore_country: string;
  keystore_dname_forbidden: string;
  keystore_password_min: number;
  keystore_generated_password: string;
  keystore_validity_max_days: number;
};

/** A colour as the tokens write it: `#AARRGGBB`. */
export type ArgbHex = string;

export type Status = { base: ArgbHex; content: ArgbHex; subtle: ArgbHex; border: ArgbHex; on: ArgbHex };

/** Every semantic colour role of one theme. Status roles are nested; the rest are flat. */
export type Roles = Record<string, ArgbHex | Status>;

export type Spring = { response: number; bounce: number; stiffness: number; dampingRatio: number };

export type SpringName =
  | "press"
  | "release"
  | "thumb"
  | "indicatorLead"
  | "indicatorTrail"
  | "screen"
  | "sheet"
  | "dialog"
  | "layout"
  | "emphasis"
  | "interactive"
  | "bounceBack"
  | "iconBounceUp"
  | "iconBounceDown";

export type TypeRole = {
  size: number;
  lineHeight: number;
  tracking: number;
  weight: Record<string, number>;
};

export type StyleTokens = {
  bar: string;
  card: string;
  field: string;
  borderWidth: number;
  borderWidthStrong: number;
  buttonRadius: number | "pill";
  chipRadius: number | "pill";
  offsetShadow: number;
  radii: Record<"xs" | "sm" | "md" | "lg" | "xl" | "sheet", number>;
  requires: string[];
  surfaces: string;
  uppercaseLabels: boolean;
  voice: string;
};

export type BrandField =
  | "base"
  | "pressed"
  | "subtleLight"
  | "dark"
  | "darkPressed"
  | "subtleDark"
  | "onLight"
  | "onDark";

export type Tokens = {
  spacing: Record<string, number>;
  radii: Record<string, number>;
  sizes: Record<string, number>;
  borders: { hairlinePx: number; width: number; widthStrong: number };
  elevation: Record<string, number>;
  type: { voices: string[]; roles: Record<string, TypeRole> };
  motion: {
    styles: Record<string, { bounce: number; speed: number; springs: Record<SpringName, Spring> }>;
    fades: { shortMs: number; longMs: number };
    navigation: { parallax: number; edgeShadow: number; scrimAlpha: { light: number; dark: number } };
    press: { scale: number; scaleLarge: number; scaleIcon: number; dimAlpha: number; highlightFadeOutMs: number };
    stagger: { stepMs: number; maxItems: number };
  };
  ramps: Record<string, Record<string, ArgbHex>>;
  /** The template's own brand ramps, which the palettes below were resolved with. */
  brand: Record<"accent" | "secondary" | "tertiary", Record<BrandField, ArgbHex>>;
  /** Each design style's roles, light and dark. */
  palette: Record<string, { light: Roles; dark: Roles }>;
  styles: Record<string, StyleTokens>;
  defaults: Record<string, string | boolean>;
};

export type Catalogue = {
  version: 2;
  generator_commit: string;
  targets: Target[];
  rules: Rules;
  tokens: Tokens;
  /** Only in a catalogue served live: what that deployment's machine can do. */
  environment?: Record<string, { keytool?: boolean }>;
};
