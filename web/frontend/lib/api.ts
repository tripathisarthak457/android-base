/** Where the Go API lives: the same origin in production, where Vercel routes /api to it. */
export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE?.replace(/\/$/, "") ??
  (process.env.NODE_ENV === "development" ? "http://127.0.0.1:8080" : "");

/**
 * The deployed commit, short: in `X-Client`, so the API can tell the site from scripts, and as the
 * release a client error or a report is grouped under. Vercel hands its builds the commit; a build
 * made anywhere else names itself with NEXT_PUBLIC_BUILD.
 */
export const BUILD = process.env.NEXT_PUBLIC_BUILD || process.env.NEXT_PUBLIC_VERCEL_GIT_COMMIT_SHA?.slice(0, 7) || "dev";

/** `X-Client` is at most 64 printable ASCII characters. */
const CLIENT_MAX = 64;

export const CLIENT_PREFIX = "android-base-web/";

export function clientHeader(build: string): string {
  const safe = build.replace(/[^\x21-\x7e]/g, "") || "dev";
  return `${CLIENT_PREFIX}${safe}`.slice(0, CLIENT_MAX);
}

/** An error answer from the API: `{error, code, field}`, or a status with no body. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly field: string;

  constructor(message: string, status: number, code = "", field = "") {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.field = field;
  }
}

export async function apiError(response: Response, fallback: string): Promise<ApiError> {
  try {
    const body = (await response.json()) as { error?: unknown; code?: unknown; field?: unknown };
    return new ApiError(
      typeof body.error === "string" ? body.error : fallback,
      response.status,
      typeof body.code === "string" ? body.code : "",
      typeof body.field === "string" ? body.field : "",
    );
  } catch {
    // No JSON body means the request never reached a handler: a proxy, a cold start, a 502.
    return new ApiError(fallback, response.status);
  }
}
