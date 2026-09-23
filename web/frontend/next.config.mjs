/**
 * Where the page may load things from.
 *
 * Scripts keep 'unsafe-inline' because Next.js hydrates through inline scripts, and nonces would
 * make every page render per request instead of being served static. What the policy does stop:
 * any script, style or font from an origin that is not this one or Google Fonts, plugins, the
 * page being framed, a <base> tag re-pointing relative URLs, and forms posting anywhere else.
 * `connect-src` names the API's origin when it is not this one — local development, or a split
 * deployment through NEXT_PUBLIC_API_BASE.
 */
const apiOrigin = (() => {
  const base = process.env.NEXT_PUBLIC_API_BASE;
  if (!base) return "";
  try {
    return new URL(base).origin;
  } catch {
    return "";
  }
})();

const contentSecurityPolicy = [
  "default-src 'self'",
  "script-src 'self' 'unsafe-inline'",
  "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
  "font-src 'self' https://fonts.gstatic.com",
  "img-src 'self' data: blob:",
  `connect-src 'self'${apiOrigin ? ` ${apiOrigin}` : ""}`,
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join("; ");

/** @type {import('next').NextConfig} */
const config = {
  reactStrictMode: true,
  poweredByHeader: false,
  async headers() {
    // Development runs React Refresh, which needs eval; the policy is for what ships.
    if (process.env.NODE_ENV !== "production") return [];
    return [
      {
        source: "/(.*)",
        headers: [{ key: "Content-Security-Policy", value: contentSecurityPolicy }],
      },
    ];
  },
};
export default config;
