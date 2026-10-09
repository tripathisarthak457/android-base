import type { StyleTokens } from "../catalogue/types.ts";

/** What a style changes, read from its tokens, so a style the generator adds describes itself. */
export function traits(style: StyleTokens): string[] {
  const words = (value: string) => value.replace(/([a-z])([A-Z])/g, "$1 $2").toLowerCase();
  return [
    `${words(style.surfaces)} surfaces, ${words(style.voice)} type`,
    `${style.buttonRadius === "pill" ? "pill" : style.buttonRadius <= 4 ? "square" : "rounded"} buttons, ${words(style.card)} cards`,
    `${words(style.field)} fields${style.uppercaseLabels ? ", uppercase labels" : ""}`,
    `${words(style.bar)} tab bar`,
  ].map((line) => line.charAt(0).toUpperCase() + line.slice(1));
}
