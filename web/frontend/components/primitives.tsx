"use client";

// The handful of controls the site is built from.

import { motion, type HTMLMotionProps } from "framer-motion";
import { type ComponentProps, type ReactNode, useId, useState, useSyncExternalStore } from "react";

/** A press that answers like the template's Scale feedback: down under the finger, back on a spring. */
const press = {
  whileTap: { scale: 0.97 },
  transition: { type: "spring" as const, stiffness: 1400, damping: 60 },
};

export function Button({
  children,
  variant = "primary",
  size = "medium",
  disabled = false,
  focusableWhenDisabled = false,
  className = "",
  onClick,
  ...rest
}: {
  children: ReactNode;
  variant?: "primary" | "secondary" | "ghost";
  size?: "small" | "medium" | "large";
  /**
   * Keeps the button in the tab order while disabled, as `aria-disabled`, ignoring presses. For a
   * button that disables itself while its own action runs: a focused button that turns `disabled`
   * drops the focus to the page body, leaving a keyboard or screen reader user nowhere.
   */
  focusableWhenDisabled?: boolean;
} & HTMLMotionProps<"button">) {
  const variants = {
    primary: "bg-accent text-on-accent font-bold hover:bg-accent-bright",
    secondary: "bg-ink-800 text-ink-100 border border-ink-600 hover:border-ink-500 hover:bg-ink-700",
    ghost: "text-ink-300 hover:text-ink-100 hover:bg-ink-800",
  };
  const unavailable = {
    primary: "bg-ink-700 text-ink-400 font-bold",
    secondary: "bg-ink-800 text-ink-400 border border-ink-700",
    ghost: "text-ink-400",
  };
  const sizes = {
    small: "h-9 px-3.5 text-sm",
    medium: "h-11 px-5 text-[15px]",
    large: "h-13 px-7 text-base",
  };
  const inert = disabled && focusableWhenDisabled;

  return (
    <motion.button
      type="button"
      {...(disabled ? {} : press)}
      disabled={disabled && !focusableWhenDisabled}
      aria-disabled={inert || undefined}
      // Cancelling the click also cancels the form submission a submit button would start, by a
      // press or by Enter in one of the form's fields.
      onClick={inert ? (event) => event.preventDefault() : onClick}
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors ${
        disabled ? `cursor-not-allowed ${unavailable[variant]}` : variants[variant]
      } ${sizes[size]} ${className}`}
      {...rest}
    >
      {children}
    </motion.button>
  );
}

/**
 * Stops and restarts the looping previews beside it. They start on their own and run while in view,
 * so the page itself has to offer a way to stop them (WCAG 2.2.2); reduced motion only decides
 * whether they start. The label changes with the state rather than using `aria-pressed`, as the
 * APG carousel's rotation control does.
 */
export function PauseButton({ paused, onChange, label }: { paused: boolean; onChange: (paused: boolean) => void; label: string }) {
  return (
    <Button variant="secondary" size="small" onClick={() => onChange(!paused)}>
      <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor" aria-hidden>
        {paused ? <path d="M3 1.5v9l7.5-4.5z" /> : <path d="M2.5 1.5H5v9H2.5zM7 1.5h2.5v9H7z" />}
      </svg>
      {paused ? "Play" : "Pause"}
      <span className="sr-only"> {label}</span>
    </Button>
  );
}

const REDUCED_MOTION = "(prefers-reduced-motion: reduce)";

function subscribeToReducedMotion(onChange: () => void): () => void {
  const query = matchMedia(REDUCED_MOTION);
  query.addEventListener("change", onChange);
  return () => query.removeEventListener("change", onChange);
}

/**
 * The state behind a `PauseButton`: the visitor's choice once they have made one, until then their
 * reduced-motion setting. The server cannot know that setting, so hydration renders it as off and
 * React re-renders with the real value right after; reading `matchMedia` in the first client render
 * would make the button's markup differ from the server's and throw the whole page's HTML away.
 */
export function usePaused(): [boolean, (paused: boolean) => void] {
  const reduce = useSyncExternalStore(subscribeToReducedMotion, () => matchMedia(REDUCED_MOTION).matches, () => false);
  const [chosen, setChosen] = useState<boolean | null>(null);
  return [chosen ?? reduce, setChosen];
}

type ControlProps = { id: string; "aria-invalid"?: true; "aria-describedby"?: string };

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

export function TextInput({ className = "", ...rest }: ComponentProps<"input">) {
  return (
    <input
      className={`h-11 w-full rounded-lg border border-ink-500 bg-ink-900 px-3.5 text-[15px] text-ink-100 transition-colors placeholder:text-ink-500 hover:border-ink-400 aria-invalid:border-rose ${className}`}
      {...rest}
    />
  );
}

/**
 * The off track is ink-500, not a quieter step: the track's outline and the thumb's position are
 * what tell on from off, so each needs 3:1 against what is next to it (WCAG 1.4.11), on the panels
 * and under the thumb, in both themes.
 */
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
      className={`relative h-6 w-11 shrink-0 rounded-full transition-colors disabled:opacity-40 ${checked ? "bg-accent" : "bg-ink-500"}`}
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
 * selection and assistive tech announces the group and its state. The chosen pill is inverted:
 * a quieter fill on the track, or a darker label alone, stays under the 3:1 a state needs.
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
            value === option.value ? "bg-ink-100 text-ink-950" : "text-ink-400 hover:text-ink-200"
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
            ? "cursor-pointer border-accent bg-accent-dim/50 ring-1 ring-accent"
            : "cursor-pointer border-ink-700 bg-ink-900 hover:border-ink-600"
      } ${className}`}
    >
      <input type={type} name={name} checked={checked} disabled={disabled} onChange={onChange} className="sr-only" />
      {children}
    </label>
  );
}
