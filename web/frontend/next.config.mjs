/** @type {import('next').NextConfig} */
const config = {
  reactStrictMode: true,
  // The API lives on another origin (the Vultr box); nothing is proxied through Next, so there
  // are no rewrites here. The browser talks to it directly, which is why the Go service has a
  // CORS allow-list rather than a wildcard.
  poweredByHeader: false,
};
export default config;
