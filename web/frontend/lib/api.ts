/**
 * The API client, and the shapes it returns.
 *
 * Every type here mirrors what `genkit/catalogue.py` emits. They are hand-written rather than
 * generated because there are nine of them and a codegen step is a build dependency that has to
 * be installed on every machine that touches the site — but the *values* are never duplicated:
 * the feature list, the presets and the defaults all come down the wire.
 */

export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE?.replace(/\/$/, "") ?? "http://127.0.0.1:8080";

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

export type ApiLevel = {
  level: number;
  label: string;
  version: string;
  codename: string;
  needsDesugaring: boolean;
};

export type Catalogue = {
  features: Feature[];
  groups: Group[];
  presets: Preset[];
  motionStyles: MotionStyle[];
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
    hapticsEnabled: boolean;
    preset: string;
  };
  keystoreNames: string[];
  keystoresAvailable: boolean;
  fontSuggestions: string[];
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
  motion_style: string;
  haptics_enabled: boolean;
  preset?: string;
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

/**
 * Posts the spec and returns the zip as a Blob.
 *
 * The response is a file rather than a URL, so there is nothing to clean up on the server and no
 * window in which a generated project sits on disk addressable by anyone who guesses an id. The
 * cost is that the whole zip is held in memory here — about 400KB, which is fine.
 */
export async function generateProject(
  request: GenerateRequest,
  signal?: AbortSignal,
): Promise<{ blob: Blob; filename: string; elapsedMs: number }> {
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
  };
}

/**
 * Records one funnel step.
 *
 * `keepalive` so the "downloaded" ping survives the navigation the download triggers, and every
 * failure is swallowed: analytics must never be the reason a visitor sees an error.
 */
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
