"use client";

/**
 * The handful of controls the site is built from.
 *
 * Small enough to live in one file, and kept there deliberately: a component library for a
 * five-screen site is a directory nobody navigates. Each of these has one job and one visual
 * state machine, and the press animation is shared so nothing on the page responds differently
 * from anything else.
 */

import { motion, type HTMLMotionProps } from "framer-motion";
import type { ReactNode } from "react";

/** The press feel, matching the `Bouncy` motion style the generated app ships with. */
export const press = {
  whileHover: { scale: 1.02 },
  whileTap: { scale: 0.97 },
  transition: { type: "spring" as const, stiffness: 520, damping: 22 },
};

export function Button({
  children,
  variant = "primary",
  size = "medium",
  className = "",
  ...rest
}: {
  children: ReactNode;
  variant?: "primary" | "secondary" | "ghost";
  size?: "small" | "medium" | "large";
} & HTMLMotionProps<"button">) {
  const variants = {
    primary:
      "bg-accent text-ink-950 font-bold hover:bg-accent-bright disabled:bg-ink-700 disabled:text-ink-400",
    secondary:
      "bg-ink-800 text-ink-100 border border-ink-600 hover:border-ink-500 hover:bg-ink-700",
    ghost: "text-ink-300 hover:text-ink-100 hover:bg-ink-800",
  };
  const sizes = {
    small: "h-9 px-3.5 text-sm",
    medium: "h-11 px-5 text-[15px]",
    large: "h-13 px-7 text-base",
  };

  return (
    <motion.button
      {...press}
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors disabled:cursor-not-allowed ${variants[variant]} ${sizes[size]} ${className}`}
      {...rest}
    >
      {children}
    </motion.button>
  );
}

export function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string;
  hint?: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-ink-200">{label}</span>
      {children}
      {/*
        The hint stays put when an error appears rather than being replaced by it. A field whose
        helper text vanishes the moment you get it wrong has taken away the instructions at
        exactly the point they became relevant.
      */}
      {hint && !error && <span className="mt-1.5 block text-xs text-ink-400">{hint}</span>}
      {error && <span className="mt-1.5 block text-xs text-rose">{error}</span>}
    </label>
  );
}

export function TextInput({
  invalid,
  className = "",
  ...rest
}: { invalid?: boolean } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={`h-11 w-full rounded-lg border bg-ink-900 px-3.5 text-[15px] text-ink-100 placeholder:text-ink-500 transition-colors focus:border-accent focus:outline-none ${
        invalid ? "border-rose" : "border-ink-600 hover:border-ink-500"
      } ${className}`}
      {...rest}
    />
  );
}

export function Toggle({
  checked,
  onChange,
  label,
  disabled,
}: {
  checked: boolean;
  onChange: (next: boolean) => void;
  label: string;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      onClick={() => onChange(!checked)}
      className={`relative h-6 w-11 shrink-0 rounded-full transition-colors disabled:opacity-40 ${
        checked ? "bg-accent" : "bg-ink-600"
      }`}
    >
      <motion.span
        layout
        transition={{ type: "spring", stiffness: 620, damping: 32 }}
        className="absolute top-0.5 h-5 w-5 rounded-full bg-ink-950 shadow"
        style={{ left: checked ? 22 : 2 }}
      />
    </button>
  );
}

export function Segmented<T extends string>({
  options,
  value,
  onChange,
}: {
  options: { value: T; label: string }[];
  value: T;
  onChange: (next: T) => void;
}) {
  return (
    <div className="inline-flex rounded-lg border border-ink-600 bg-ink-900 p-1">
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          onClick={() => onChange(option.value)}
          className="relative rounded-md px-3.5 py-1.5 text-sm font-medium transition-colors"
        >
          {/*
            One shared `layoutId` is what makes the pill slide between segments instead of
            fading out in one place and in at another.
          */}
          {value === option.value && (
            <motion.span
              layoutId="segment-pill"
              className="absolute inset-0 rounded-md bg-ink-700"
              transition={{ type: "spring", stiffness: 500, damping: 34 }}
            />
          )}
          <span
            className={`relative z-10 ${value === option.value ? "text-ink-100" : "text-ink-400"}`}
          >
            {option.label}
          </span>
        </button>
      ))}
    </div>
  );
}

export function Card({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={`rounded-xl border border-ink-700 bg-ink-850 ${className}`}>{children}</div>
  );
}

export function Badge({
  children,
  tone = "neutral",
}: {
  children: ReactNode;
  tone?: "neutral" | "accent" | "mint" | "amber";
}) {
  const tones = {
    neutral: "bg-ink-800 text-ink-300 border-ink-600",
    accent: "bg-accent-dim text-accent-bright border-accent/30",
    mint: "bg-mint/10 text-mint border-mint/25",
    amber: "bg-amber/10 text-amber border-amber/25",
  };
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${tones[tone]}`}
    >
      {children}
    </span>
  );
}

export function Spinner({ className = "" }: { className?: string }) {
  return (
    <svg
      className={`animate-spin ${className}`}
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden
    >
      <circle cx="8" cy="8" r="6.5" stroke="currentColor" strokeOpacity="0.25" strokeWidth="2" />
      <path
        d="M14.5 8A6.5 6.5 0 0 0 8 1.5"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  );
}
