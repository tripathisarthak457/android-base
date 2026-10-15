const LIST = new Intl.ListFormat("en-GB", { type: "conjunction" });

/** `a, b and c`: how the site writes a list, and how the generator writes one in its messages. */
export function listed(items: readonly string[]): string {
  return LIST.format(items);
}

/** A length as Python's `len()` counts it, so a character outside the BMP is one, not two. */
export function codePoints(value: string): number {
  return [...value].length;
}
