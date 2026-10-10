// Static building blocks, safe to render on the server.

import type { ReactNode } from "react";

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-xl border border-ink-700 bg-ink-850 ${className}`}>{children}</div>;
}

export function Badge({ children, tone = "neutral" }: { children: ReactNode; tone?: "neutral" | "accent" | "mint" | "amber" | "rose" }) {
  const tones = {
    neutral: "bg-ink-800 text-ink-300 border-ink-600",
    accent: "bg-accent-dim text-accent-bright border-accent/30",
    mint: "bg-mint/10 text-mint border-mint/25",
    amber: "bg-amber/10 text-amber border-amber/25",
    rose: "bg-rose/10 text-rose border-rose/25",
  };
  return <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${tones[tone]}`}>{children}</span>;
}

export function Spinner({ className = "" }: { className?: string }) {
  return (
    <svg className={`animate-spin ${className}`} width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden>
      <circle cx="8" cy="8" r="6.5" stroke="currentColor" strokeOpacity="0.25" strokeWidth="2" />
      <path d="M14.5 8A6.5 6.5 0 0 0 8 1.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

/** A link that looks like a button, so it keeps middle-click, copy link and a link's semantics. */
export function LinkButton({ href, children, variant = "secondary" }: { href: string; children: ReactNode; variant?: "primary" | "secondary" }) {
  const external = href.startsWith("http");
  const look =
    variant === "primary"
      ? "bg-accent text-on-accent font-bold hover:bg-accent-bright"
      : "bg-ink-800 text-ink-100 border border-ink-600 hover:border-ink-500 hover:bg-ink-700";
  return (
    <a
      href={href}
      {...(external ? { target: "_blank", rel: "noopener noreferrer" } : {})}
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-[color,background-color,border-color,transform] h-13 px-7 text-base active:scale-[0.97] ${look}`}
    >
      {children}
    </a>
  );
}
