import type { Rules } from "../../catalogue/types.ts";
import { type Checker, type Json, codePoints, isObject, matches, withoutNulls } from "./check.ts";

// Signing keys: the shape of each entry in `android.keystores`, then what keytool would refuse.

/** Python's `str.isprintable()`: no control, format, private-use, unassigned or separator characters except the space. */
const NOT_PRINTABLE = /[\p{Cc}\p{Cf}\p{Cs}\p{Co}\p{Cn}\p{Zl}\p{Zp}]|(?! )\p{Zs}/u;

const KEYSTORE_TEXT = ["name", "alias", "store_password", "key_password", "existing_path", "common_name", "organisation", "country"];

export function checkKeystoreShape(check: Checker, raw: unknown, where: string): boolean {
  if (!isObject(raw)) return check.fail(where, `\`${where}\` must be an object.`);
  const data = withoutNulls(raw);
  for (const key of Object.keys(data)) {
    if (!KEYSTORE_TEXT.includes(key) && key !== "validity_days") {
      return check.fail(`${where}.${key}`, `Unknown field '${where}.${key}'.`);
    }
  }
  for (const key of KEYSTORE_TEXT) {
    if (key in data && typeof data[key] !== "string") return check.fail(where, `\`${where}\` must be a string.`);
  }
  if ("validity_days" in data && !Number.isInteger(data.validity_days)) {
    return check.fail(where, `\`${where}\` must be a whole number.`);
  }
  return true;
}

/** Why the generator would refuse to make this signing key, or undefined. */
export function keystoreProblem(keystore: Json, rules: Rules): string | undefined {
  const text = (key: string, fallback: string) =>
    typeof keystore[key] === "string" && keystore[key] !== "" ? (keystore[key] as string) : fallback;
  const name = text("name", "");
  if (!rules.keystore_names.includes(name)) {
    return `Unknown signing key '${name}'. Choose from: ${rules.keystore_names.join(", ")}.`;
  }
  if (!matches(rules.keystore_alias, text("alias", ""))) {
    return `The ${name} key alias must be letters, digits, dots, hyphens or underscores, and start with a letter or digit.`;
  }
  const passwords: [string, string][] = [["store", text("store_password", "")]];
  if (typeof keystore.existing_path === "string" && keystore.existing_path) passwords.push(["key", text("key_password", "")]);
  for (const [label, password] of passwords) {
    if (codePoints(password) < rules.keystore_password_min) {
      return `The ${name} ${label} password must be at least ${rules.keystore_password_min} characters: keytool refuses anything shorter.`;
    }
    if (NOT_PRINTABLE.test(password)) return `The ${name} ${label} password cannot contain control characters.`;
  }
  const subject: [string, string][] = [
    [`${name} common name`, text("common_name", "Unknown")],
    [`${name} organisation`, text("organisation", "Unknown")],
  ];
  for (const [label, value] of subject) {
    if (!value.trim()) return `The ${label} cannot be empty.`;
    if (NOT_PRINTABLE.test(value) || [...rules.keystore_dname_forbidden].some((character) => value.includes(character))) {
      return `The ${label} cannot contain any of ${[...rules.keystore_dname_forbidden].sort().join("")}: they separate the fields of the certificate's subject.`;
    }
  }
  if (!matches(rules.keystore_country, text("country", "US"))) return "The country must be a two-letter code, like US or IN.";
  const validity = Number.isInteger(keystore.validity_days) ? (keystore.validity_days as number) : 10_000;
  if (validity < 1 || validity > rules.keystore_validity_max_days) {
    return `The ${name} validity must be between 1 and ${rules.keystore_validity_max_days} days.`;
  }
  return undefined;
}
