"use client";

/** The handful of controls the site is built from. */

import { motion, type HTMLMotionProps } from "framer-motion";
import { type InputHTMLAttributes, type ReactNode, useId } from "react";

/** A press that answers like the template's Scale feedback: down under the finger, back on a spring. */
export const press = {
  whileTap: { scale: 0.97 },
  transition: { type: "spring" as const, stiffness: 1400, damping: 60 },
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
    primary: "bg-accent text-on-accent font-bold hover:bg-accent-bright disabled:bg-ink-700 disabled:text-ink-400",
    secondary: "bg-ink-800 text-ink-100 border border-ink-600 hover:border-ink-500 hover:bg-ink-700",
    ghost: "text-ink-300 hover:text-ink-100 hover:bg-ink-800",
  };
  const sizes = {
    small: "h-9 px-3.5 text-sm",
    medium: "h-11 px-5 text-[15px]",
    large: "h-13 px-7 text-base",
  };

  return (
    <motion.button
      type="button"
      {...press}
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors disabled:cursor-not-allowed ${variants[variant]} ${sizes[size]} ${className}`}
      {...rest}
    >
      {children}
    </motion.button>
  );
}

export type ControlProps = { id: string; "aria-invalid"?: true; "aria-describedby"?: string };

/**
 * A label, its control, and the hint and error under it. The hint stays when an error appears:
 * a field that loses its instructions the moment you get it wrong has removed them exactly when
 * they became relevant. The control is wired to both through `aria-describedby`.
 */
export function Field({
  label,
  hint,
  error,
  children,
}: {
  label: ReactNode;
  hint?: ReactNode;
  error?: string;
  children: (control: ControlProps) => ReactNode;
}) {
  const id = useId();
  const described = [hint ? `${id}-hint` : "", error ? `${id}-error` : ""].filter(Boolean).join(" ");
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-ink-200">
        {label}
      </label>
      {children({ id, ...(error ? { "aria-invalid": true } : {}), ...(described ? { "aria-describedby": described } : {}) })}
      {hint && (
        <p id={`${id}-hint`} className="mt-1.5 text-xs text-ink-400">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="mt-1 text-xs text-rose">
          {error}
        </p>
      )}
    </div>
  );
}

export function TextInput({ className = "", ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={`h-11 w-full rounded-lg border border-ink-600 bg-ink-900 px-3.5 text-[15px] text-ink-100 transition-colors placeholder:text-ink-500 hover:border-ink-500 focus:border-accent focus:outline-none aria-invalid:border-rose ${className}`}
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
      className={`relative h-6 w-11 shrink-0 rounded-full transition-colors disabled:opacity-40 ${checked ? "bg-accent" : "bg-ink-600"}`}
    >
      <motion.span
        animate={{ x: checked ? 20 : 0 }}
        transition={{ type: "spring", stiffness: 620, damping: 40 }}
        className="absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-ink-850 shadow"
      />
    </button>
  );
}

/**
 * A radio group drawn as one pill per option. Native radios underneath, so arrow keys move the
 * selection and assistive tech announces the group and its state.
 */
export function Segmented<T extends string>({
  label,
  options,
  value,
  onChange,
}: {
  label: string;
  options: { value: T; label: string }[];
  value: T;
  onChange: (next: T) => void;
}) {
  const name = useId();
  return (
    <div role="radiogroup" aria-label={label} className="inline-flex rounded-lg border border-ink-600 bg-ink-900 p-1">
      {options.map((option) => (
        <label
          key={option.value}
          className={`relative cursor-pointer rounded-md px-3.5 py-1.5 text-sm font-medium transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent ${
            value === option.value ? "bg-ink-700 text-ink-100" : "text-ink-400 hover:text-ink-200"
          }`}
        >
          <input
            type="radio"
            name={name}
            value={option.value}
            checked={value === option.value}
            onChange={() => onChange(option.value)}
            className="sr-only"
          />
          {option.label}
        </label>
      ))}
    </div>
  );
}

/** A selectable card: a native radio or checkbox under a styled label. */
export function ChoiceCard({
  type,
  name,
  checked,
  disabled,
  onChange,
  className = "",
  children,
}: {
  type: "radio" | "checkbox";
  name?: string;
  checked: boolean;
  disabled?: boolean;
  onChange: () => void;
  className?: string;
  children: ReactNode;
}) {
  return (
    <label
      className={`block rounded-xl border text-left transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-accent ${
        disabled
          ? "cursor-not-allowed border-ink-700 bg-ink-900 opacity-60"
          : checked
            ? "cursor-pointer border-accent/60 bg-accent-dim/50"
            : "cursor-pointer border-ink-700 bg-ink-900 hover:border-ink-600"
      } ${className}`}
    >
      <input type={type} name={name} checked={checked} disabled={disabled} onChange={onChange} className="sr-only" />
      {children}
    </label>
  );
}
