import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Android base — a project generator",
  description:
    "A multi-module Jetpack Compose starter with no Material dependency, generated with your " +
    "package name, your colours, one of four design styles and only the features you ticked. " +
    "Downloads as a zip that compiles.",
  metadataBase: new URL("https://android-base.vercel.app"),
  openGraph: {
    title: "Android base — a project generator",
    description:
      "Multi-module Compose, MVI, Hilt, Ktor. Pick your features and a look, get a zip that builds.",
    type: "website",
  },
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f2ece2" },
    { media: "(prefers-color-scheme: dark)", color: "#141311" },
  ],
  width: "device-width",
  initialScale: 1,
};

/*
 * Runs before the first paint, so a visitor who chose dark does not see a flash of paper first.
 * Storage can be unavailable (private windows, blocked site data); the page then follows the OS.
 */
const THEME_BOOTSTRAP = `
try {
  var saved = localStorage.getItem("theme");
  if (saved === "light" || saved === "dark") document.documentElement.dataset.theme = saved;
} catch (e) {}
`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: THEME_BOOTSTRAP }} />
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        <link
          href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,500;12..96,700;12..96,800&family=Instrument+Sans:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="antialiased">{children}</body>
    </html>
  );
}
