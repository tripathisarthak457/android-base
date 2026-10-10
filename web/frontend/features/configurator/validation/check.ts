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

export type Json = Record<string, unknown>;

export function isObject(value: unknown): value is Json {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function isStringList(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === "string");
}

export function withoutNulls(value: Json): Json {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== null && item !== undefined));
}

export function matches(pattern: string, value: string): boolean {
  return new RegExp(pattern, "u").test(value);
}
