/** A parsed JSON object whose values have not been checked yet. */
export type Json = Record<string, unknown>;

/** A JSON object, not an array, null or a primitive. */
export function isObject(value: unknown): value is Json {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function isStringList(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === "string");
}
