/** Where the Go API lives: the same origin in production, where Vercel routes /api to it. */
export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE?.replace(/\/$/, "") ??
  (process.env.NODE_ENV === "development" ? "http://127.0.0.1:8080" : "");

/** The deployed commit, short. Sent as `X-Client` so the API can tell the site from scripts. */
export const BUILD = process.env.NEXT_PUBLIC_BUILD ?? "dev";

/** An error answer from the API: `{error, code, field}` (WP09), or a status with no body. */
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

export function postJson(path: string, body: unknown, init: RequestInit = {}): Promise<Response> {
  return fetch(`${API_BASE}${path}`, {
    ...init,
    method: "POST",
    headers: { "Content-Type": "application/json", ...init.headers },
    body: JSON.stringify(body),
  });
}
