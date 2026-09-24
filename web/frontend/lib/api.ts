/**
 * The API client, and the shapes it returns. Every type here mirrors what `genkit/catalogue.py`
 * emits.
 */

/** Where the Go API lives. */
export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE?.replace(/\/$/, "") ??
  (process.env.NODE_ENV === "development" ? "http://127.0.0.1:8080" : "");

export type Feature = {
  key: string;
  title: string;
  headline: string;
  description: string;
  default: boolean;
  requires: string[];
  implies: string[];
  group: string;
};

export type Group = { name: string; caption: string };

export type Preset = {
  key: string;
  title: string;
  description: string;
  features: string[];
};

export type MotionStyle = { key: string; description: string };

export type DesignStyle = { key: string; description: string };

export type ApiLevel = {
  level: number;
  label: string;
  version: string;
  codename: string;
  needsDesugaring: boolean;
};

export type Language = {
  /** A BCP 47 tag, as the generator's spec takes it: "es", "pt-BR". */
  tag: string;
  /** The language's name for itself, which is how people look for their own. */
  name: string;
};

export type Catalogue = {
  features: Feature[];
  groups: Group[];
  presets: Preset[];
  motionStyles: MotionStyle[];
  designStyles: DesignStyle[];
  /** Translations the generator ships. English is always included and is not listed. */
  languages: Language[];
  apiLevels: ApiLevel[];
  defaults: {
    minSdk: number;
    targetSdk: number;
    compileSdk: number;
    versionName: string;
    versionCode: number;
    fontName: string;
    monoFontName: string;
    accentColour: string;
    motionStyle: string;
    designStyle: string;
    hapticsEnabled: boolean;
    preset: string;
  };
  keystoreNames: string[];
  /** Whether the server that answered has a JDK, and so can run `keytool` at all. */
  keystoresAvailable: boolean;
  minKeystorePassword: number;
  /**
   * Module names the template already occupies. Checked in the form, so a taken name is caught
   * before the round trip and the site never keeps its own copy of the list.
   */
  reservedModuleNames: string[];
  fontSuggestions: string[];
};

/**
 * One signing key to create. These carry passwords, which is why they are assembled at submit time
 * and never put in `localStorage`, in a URL, or in the funnel event the page sends alongside.
 */
export type Keystore = {
  name: string;
  alias: string;
  store_password: string;
  key_password: string;
  common_name: string;
  organisation: string;
  country: string;
};

export type GenerateRequest = {
  app_name: string;
  package_name: string;
  min_sdk: number;
  target_sdk: number;
  compile_sdk: number;
  version_name: string;
  version_code: number;
  features: string[];
  feature_modules: string[];
  api_base_urls?: Record<string, string>;
  web_socket_urls?: Record<string, string>;
  deeplink_scheme?: string;
  deeplink_host?: string;
  font_name: string;
  mono_font_name: string;
  accent_colour: string;
  /** Empty means the server derives it from the primary. */
  secondary_colour?: string;
  tertiary_colour?: string;
  motion_style: string;
  design_style: string;
  haptics_enabled: boolean;
  languages: string[];
  preset?: string;
  keystores?: Keystore[];
};

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function fetchCatalogue(signal?: AbortSignal): Promise<Catalogue> {
  const response = await fetch(`${API_BASE}/api/options`, { signal });
  if (!response.ok) {
    throw new ApiError("Could not load the options.", response.status);
  }
  return response.json();
}

/** Posts the spec and returns the zip as a Blob. */
export async function generateProject(
  request: GenerateRequest,
  signal?: AbortSignal,
): Promise<{ blob: Blob; filename: string; elapsedMs: number; keystoresSkipped: string[] }> {
  const response = await fetch(`${API_BASE}/api/generate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    signal,
  });

  if (!response.ok) {
    let message = "The generator failed.";
    try {
      const body = await response.json();
      if (typeof body?.error === "string") message = body.error;
    } catch {
      // A non-JSON error body means the request never reached the handler — a proxy, a 502.
      // The default message is closer to the truth than an empty one.
    }
    throw new ApiError(message, response.status);
  }

  const disposition = response.headers.get("Content-Disposition") ?? "";
  const match = /filename="([^"]+)"/.exec(disposition);

  return {
    blob: await response.blob(),
    filename: match?.[1] ?? "project.zip",
    elapsedMs: Number(response.headers.get("X-Generation-Ms") ?? 0),
    // Keys that were asked for and not produced. Empty on every request that asked for none.
    keystoresSkipped: (response.headers.get("X-Keystores-Skipped") ?? "")
      .split(",")
      .filter(Boolean),
  };
}

/** Records one funnel step. */
export function track(step: "landed" | "configured" | "downloaded"): void {
  try {
    void fetch(`${API_BASE}/api/track`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ step }),
      keepalive: true,
    }).catch(() => undefined);
  } catch {
    // Ignored on purpose.
  }
}
