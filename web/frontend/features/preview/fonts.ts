"use client";

import { useEffect, useState } from "react";

/**
 * Google Fonts for the previews: one stylesheet per family the visitor picks, with only the
 * weights its type voice uses, and one small subset request for the suggestion chips.
 */

const GOOGLE = "https://fonts.googleapis.com/css2";

const loaded = new Map<string, Promise<boolean>>();

function family(name: string): string {
  return encodeURIComponent(name.trim()).replace(/%20/g, "+");
}

function stylesheet(href: string): Promise<boolean> {
  const known = loaded.get(href);
  if (known) return known;
  const promise = new Promise<boolean>((resolve) => {
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = href;
    link.onload = () => resolve(true);
    // Google answers an unknown family, or a weight a static family lacks, with a 400.
    link.onerror = () => {
      link.remove();
      resolve(false);
    };
    document.head.appendChild(link);
  });
  loaded.set(href, promise);
  return promise;
}

/**
 * Loads `name` at `weights`. A static family without one of them is retried at the two weights
 * every family has, and then at its default, so a rare face still previews instead of falling back.
 */
export async function loadFamily(name: string, weights: number[]): Promise<boolean> {
  if (!name.trim()) return false;
  const base = `${GOOGLE}?family=${family(name)}`;
  for (const href of [`${base}:wght@${weights.join(";")}&display=swap`, `${base}:wght@400;700&display=swap`, `${base}&display=swap`]) {
    if (await stylesheet(href)) return true;
  }
  return false;
}

export type FontStatus = "checking" | "ok" | "missing";

/** Loads the chosen family after typing settles, and says whether Google knows it. */
export function useFamily(name: string, weights: number[]): FontStatus {
  const [status, setStatus] = useState<FontStatus>("checking");
  const key = weights.join(";");
  useEffect(() => {
    let current = true;
    setStatus("checking");
    const timer = setTimeout(() => {
      void loadFamily(name, key.split(";").map(Number)).then((ok) => {
        if (current) setStatus(ok ? "ok" : "missing");
      });
    }, 400);
    return () => {
      current = false;
      clearTimeout(timer);
    };
  }, [name, key]);
  return status;
}

/** The suggestion chips: every family in one request, subset to the glyphs of their own names. */
export function useSuggestionFonts(names: string[]): void {
  const key = names.join("|");
  useEffect(() => {
    if (!key) return;
    const list = key.split("|");
    const glyphs = [...new Set(list.join(""))].join("");
    void stylesheet(`${GOOGLE}?${list.map((name) => `family=${family(name)}`).join("&")}&text=${encodeURIComponent(glyphs)}&display=swap`);
  }, [key]);
}
