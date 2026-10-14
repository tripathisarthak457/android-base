import type { Json } from "../../../lib/json.ts";

/** What every rule module writes into: the first problem per field, in the order they are found. */
export class Checker {
  readonly errors = new Map<string, string>();

  fail(field: string, message: string): false {
    if (!this.errors.has(field)) this.errors.set(field, message);
    return false;
  }

  ok(field: string): boolean {
    return !this.errors.has(field);
  }
}

export function withoutNulls(value: Json): Json {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== null && item !== undefined));
}

// validateSpec runs on every keystroke and colour drag, and the catalogue's patterns never change
// while the page is open, so each is compiled once.
const compiled = new Map<string, RegExp>();

export function matches(pattern: string, value: string): boolean {
  let regex = compiled.get(pattern);
  if (!regex) {
    regex = new RegExp(pattern, "u");
    compiled.set(pattern, regex);
  }
  return regex.test(value);
}
