import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Android base — a project generator",
  description:
    "A multi-module Jetpack Compose starter with no Material dependency, generated with your " +
    "package name, your accent colour and only the features you ticked. Downloads as a zip that " +
    "compiles.",
  metadataBase: new URL("https://android-base.vercel.app"),
  openGraph: {
    title: "Android base — a project generator",
    description:
      "Multi-module Compose, MVI, Hilt, Ktor. Pick your features, get a zip that builds.",
    type: "website",
  },
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: "#070a10",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head>
        {/*
          The two families the site is set in, loaded from Google Fonts the same way the generated
          app loads its own. `preconnect` because the render is blocked on them, and `display=swap`
          so the first paint is not a blank page on a slow connection.
        */}
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        <link
          href="https://fonts.googleapis.com/css2?family=DM+Sans:opsz,wght@9..40,400;9..40,500;9..40,700&family=JetBrains+Mono:wght@400;500&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="antialiased">{children}</body>
    </html>
  );
}
