import type { Metadata, Viewport } from "next";
import { Bricolage_Grotesque, Instrument_Sans, JetBrains_Mono } from "next/font/google";
import { SiteMotion } from "../components/site-motion";
import "./globals.css";

// Self-hosted at build time: no render-blocking request to Google, no layout shift, and no
// visitor's address sent to a third party just to draw the page.
const display = Bricolage_Grotesque({ subsets: ["latin"], axes: ["opsz"], variable: "--font-bricolage", display: "swap" });
const sans = Instrument_Sans({ subsets: ["latin"], variable: "--font-instrument", display: "swap" });
const mono = JetBrains_Mono({ subsets: ["latin"], variable: "--font-jetbrains", display: "swap" });

export const metadata: Metadata = {
  title: "Android base — a project generator",
  description:
    "A multi-module Jetpack Compose starter with no Material components, generated with your package name, " +
    "your colours, a design style and only the features you ticked. Downloads as a zip that compiles.",
  metadataBase: new URL("https://android-base.vercel.app"),
  openGraph: {
    title: "Android base — a project generator",
    description: "Multi-module Compose, MVI, Navigation 3. Pick your platform, features and a look; get a zip that builds.",
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
    <html lang="en" suppressHydrationWarning className={`${display.variable} ${sans.variable} ${mono.variable}`}>
      <head>
        <script dangerouslySetInnerHTML={{ __html: THEME_BOOTSTRAP }} />
      </head>
      <body className="antialiased">
        <SiteMotion>{children}</SiteMotion>
      </body>
    </html>
  );
}
