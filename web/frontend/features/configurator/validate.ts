import type { Catalogue, Field, Rules, Target } from "../catalogue/types.ts";
import { closeOver } from "./resolve.ts";

/**
 * The generator's spec rules, run in the browser so a mistake is caught where it was typed rather
 * than as a 400 after the round trip. They read their patterns and limits from the catalogue, and
 * a test runs every case of `generator/tests/fixtures/validation_cases.json` through both sides.
 *
 * The checks run in the generator's order and stop at the first problem in each field, so the
 * first error reported is the one the generator would report.
 */

export type SpecErrors = Map<string, string>;

type Json = Record<string, unknown>;

const SCHEMA = 2;

/** Python's `str.isprintable()`: no control, format, private-use, unassigned or separator characters except the space. */
const NOT_PRINTABLE = /[\p{Cc}\p{Cf}\p{Cs}\p{Co}\p{Cn}\p{Zl}\p{Zp}]|(?! )\p{Zs}/u;

/** What a display name may not contain: line breaks and controls, but format characters stay (Persian needs ZWNJ). */
const NOT_IN_DISPLAY_NAME = /[\p{Cc}\p{Cs}\p{Zl}\p{Zp}\uFFFE\uFFFF]/u;

class Checker {
  readonly errors: SpecErrors = new Map();

  fail(field: string, message: string): false {
    if (!this.errors.has(field)) this.errors.set(field, message);
    return false;
  }

  ok(field: string): boolean {
    return !this.errors.has(field);
  }
}

function isObject(value: unknown): value is Json {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isStringList(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === "string");
}

function withoutNulls(value: Json): Json {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== null && item !== undefined));
}

function matches(pattern: string, value: string): boolean {
  return new RegExp(pattern, "u").test(value);
}

function codePoints(value: string): number {
  return [...value].length;
}

function pascalCase(name: string): string {
  return name
    .split("_")
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join("");
}

function joinNames(names: string[]): string {
  const quoted = names.map((name) => `'${name}'`);
  const last = quoted.pop() ?? "";
  return quoted.length > 0 ? `${quoted.join(", ")} and ${last}` : last;
}

function isReserved(word: string, rules: Rules): boolean {
  return Object.values(rules.reserved_words).some((words) => words.includes(word));
}

/** The settings blocks a target reads, from the dotted field keys it declares. */
function blocksOf(target: Target): Map<string, Field[]> {
  const blocks = new Map<string, Field[]>();
  for (const field of target.fields) {
    const dot = field.key.indexOf(".");
    if (dot < 0) continue;
    const block = field.key.slice(0, dot);
    blocks.set(block, [...(blocks.get(block) ?? []), field]);
  }
  return blocks;
}

function checkType(check: Checker, field: Field, value: unknown, where: string): boolean {
  switch (field.type) {
    case "int":
      return Number.isInteger(value) || check.fail(where, `\`${where}\` must be a whole number.`);
    case "bool":
      return typeof value === "boolean" || check.fail(where, `\`${where}\` must be true or false.`);
    case "list":
    case "features":
      return isStringList(value) || check.fail(where, `\`${where}\` must be a list of strings.`);
    case "map":
      return (
        (isObject(value) && Object.values(value).every((item) => typeof item === "string")) ||
        check.fail(where, `\`${where}\` must be an object of strings.`)
      );
    case "keystores":
      return Array.isArray(value) || check.fail(where, `\`${where}\` must be a list.`);
    default:
      return typeof value === "string" || check.fail(where, `\`${where}\` must be a string.`);
  }
}

/**
 * Every problem with `raw` as a spec for `targetKey`, by dotted field, in the order the generator
 * finds them. Empty when the generator would accept it.
 */
export function validateSpec(raw: unknown, catalogue: Catalogue, targetKey: string): SpecErrors {
  const check = new Checker();
  const target = catalogue.targets.find((candidate) => candidate.key === targetKey);
  if (!target) {
    check.fail("platform", `Unknown platform '${targetKey}'.`);
    return check.errors;
  }
  if (!isObject(raw)) {
    check.fail("", "The spec must be a JSON object.");
    return check.errors;
  }
  const rules = catalogue.rules;
  const data = withoutNulls(raw);

  if (data.schema !== SCHEMA) {
    check.fail("schema", `The website writes schema ${SCHEMA} specs; this one says ${JSON.stringify(data.schema)}.`);
    return check.errors;
  }
  const platform = data.platform ?? target.key;

  const blocks = blocksOf(target);
  const topLevel = new Map(target.fields.filter((field) => !field.key.includes(".")).map((field) => [field.key, field]));
  const android = blocks.get("android");
  if (android && (platform === "android" || platform === "multiplatform")) {
    const flat = android.map((field) => field.key.slice("android.".length)).filter((key) => key in data);
    if (flat.length > 0) {
      const block = data.android ?? {};
      if (!isObject(block)) {
        check.fail("android", "`android` must be an object.");
        return check.errors;
      }
      for (const key of flat) {
        if (key in block) {
          check.fail(`android.${key}`, `'${key}' is given both at the top level and under "android"; keep one.`);
          return check.errors;
        }
      }
      data.android = { ...block, ...Object.fromEntries(flat.map((key) => [key, data[key]])) };
      for (const key of flat) delete data[key];
    }
  }

  for (const key of Object.keys(data)) {
    if (key !== "schema" && key !== "platform" && !topLevel.has(key) && !blocks.has(key)) {
      check.fail(key, `Unknown field '${key}'.`);
      return check.errors;
    }
  }
  if (typeof platform !== "string") {
    check.fail("platform", "`platform` must be a string.");
    return check.errors;
  }
  if (platform !== target.key) {
    check.fail("platform", `This spec is for '${platform}', but it was given to the '${target.key}' generator.`);
    return check.errors;
  }

  const features = data.features ?? [];
  if (!isStringList(features)) {
    check.fail("features", "`features` must be a list of strings.");
    return check.errors;
  }
  if (features.length > rules.features_max) {
    check.fail("features", `At most ${rules.features_max} features can be listed.`);
    return check.errors;
  }
  const known = new Set(target.features.map((feature) => feature.key));
  const unknown = features.filter((key) => !known.has(key));
  if (unknown.length > 0) {
    check.fail("features", `Unknown feature(s): ${[...new Set(unknown)].sort().join(", ")}.`);
    return check.errors;
  }

  for (const [key, field] of topLevel) {
    if (key === "features" || !(key in data)) continue;
    if (!checkType(check, field, data[key], key)) return check.errors;
  }
  for (const [block, fields] of blocks) {
    if (!(block in data)) continue;
    if (!checkBlock(check, data[block], block, fields, rules)) return check.errors;
  }

  const text = (key: string, fallback = "") => (typeof data[key] === "string" ? (data[key] as string) : fallback);
  const list = (key: string) => (isStringList(data[key]) ? (data[key] as string[]) : []);

  checkAppName(check, text("app_name"), rules);
  checkDisplayName(check, text("display_name"), rules);
  checkPackage(check, text("package_name"), rules);

  const architecture = text("architecture", target.defaults.architecture);
  const architectures = target.architectures.map((option) => option.key);
  if (!architectures.includes(architecture)) {
    check.fail("architecture", `Unknown architecture '${architecture}'. Choose one of: ${architectures.join(", ")}.`);
  }

  checkModules(check, list("feature_modules"), rules, rules.template_module_names[target.key] ?? []);

  const languages = list("languages");
  if (languages.length > rules.languages_max) {
    check.fail("languages", `At most ${rules.languages_max} languages can be added.`);
  } else if (new Set(languages).size !== languages.length) {
    check.fail("languages", "Each language can only be listed once.");
  }

  const designStyle = text("design_style", target.defaults.design_style);
  const style = target.design_styles.find((option) => option.key === designStyle);
  if (!style) {
    check.fail(
      "design_style",
      `Unknown design style '${designStyle}'. Choose one of: ${target.design_styles.map((option) => option.key).join(", ")}.`,
    );
  } else {
    const resolved = closeOver(features, target);
    const missing = style.requires.filter((key) => !resolved.has(key));
    if (missing.length > 0) check.fail("design_style", `The ${designStyle} design style needs ${joinNames(missing)}.`);
  }
  const motionStyle = text("motion_style", target.defaults.motion_style);
  if (!target.motion_styles.some((option) => option.key === motionStyle)) {
    check.fail(
      "motion_style",
      `Unknown motion style '${motionStyle}'. Choose one of: ${target.motion_styles.map((option) => option.key).join(", ")}.`,
    );
  }

  for (const [label, key, fallback] of [
    ["typeface", "font_name", target.defaults.font_name],
    ["monospace typeface", "mono_font_name", target.defaults.mono_font_name],
  ] as const) {
    const value = text(key, fallback) || fallback;
    if (!matches(rules.font, value)) {
      check.fail(
        key,
        `The ${label} '${value}' is not a Google Fonts family name: letters, digits, spaces and hyphens, as fonts.google.com spells it.`,
      );
    }
  }
  for (const [label, key] of [
    ["accent", "accent_colour"],
    ["secondary", "secondary_colour"],
    ["tertiary", "tertiary_colour"],
  ] as const) {
    const value = text(key);
    if (value && !matches(rules.hex, value)) check.fail(key, `The ${label} colour must be a six-digit hex, like #2C6BED.`);
  }

  if (blocks.has("android")) checkAndroid(check, isObject(data.android) ? data.android : {}, blocks.get("android") ?? [], rules);

  const offered = new Set(target.languages.map((language) => language.tag));
  if (check.ok("languages")) {
    const missing = languages.find((tag) => !offered.has(tag));
    if (missing !== undefined) {
      check.fail(
        "languages",
        `No translation for '${missing}'. Choose from: ${[...offered].join(", ")}. English is always included.`,
      );
    }
  }
  if (check.ok("architecture") && target.architectures.some((option) => option.key === architecture && !option.available)) {
    check.fail("architecture", `${architecture} is not available yet; generate ${target.defaults.architecture} for now.`);
  }
  return check.errors;
}

function checkBlock(check: Checker, raw: unknown, block: string, fields: Field[], rules: Rules): boolean {
  if (!isObject(raw)) return check.fail(block, `\`${block}\` must be an object.`);
  const data = withoutNulls(raw);
  const byKey = new Map(fields.map((field) => [field.key.slice(block.length + 1), field]));
  for (const key of Object.keys(data)) {
    if (!byKey.has(key)) return check.fail(`${block}.${key}`, `Unknown field '${block}.${key}'.`);
  }
  for (const [key, field] of byKey) {
    if (!(key in data)) continue;
    const where = `${block}.${key}`;
    if (!checkType(check, field, data[key], where)) return false;
    if (field.type === "keystores") {
      const keystores = data[key] as unknown[];
      if (keystores.length > rules.keystores_max) {
        return check.fail(where, `At most ${rules.keystores_max} signing keys can be described.`);
      }
      for (const entry of keystores) {
        if (!checkKeystoreShape(check, entry, where)) return false;
      }
    }
  }
  return true;
}

const KEYSTORE_TEXT = ["name", "alias", "store_password", "key_password", "existing_path", "common_name", "organisation", "country"];

function checkKeystoreShape(check: Checker, raw: unknown, where: string): boolean {
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

function checkAppName(check: Checker, name: string, rules: Rules): void {
  if (!name) {
    check.fail("app_name", "An app name is required.");
  } else if (!matches(rules.app_name, name)) {
    check.fail(
      "app_name",
      `The app name must start with a letter and be at most ${rules.app_name_max} letters, digits, spaces, dots, hyphens or underscores. Put anything else in the display name.`,
    );
  }
}

function checkDisplayName(check: Checker, name: string, rules: Rules): void {
  if (codePoints(name) > rules.display_name_max) {
    check.fail(
      "display_name",
      `The display name can be at most ${rules.display_name_max} characters: launchers cut it off well before that.`,
    );
  } else if (NOT_IN_DISPLAY_NAME.test(name)) {
    check.fail("display_name", "The display name cannot contain control characters or line breaks.");
  }
}

/** Problems with a package name, or undefined when the generator would accept it. */
function packageProblem(name: string, rules: Rules): string | undefined {
  if (!name) return "A package name is required, like com.example.myapp.";
  if (name.length > rules.package_max) return `The package name can be at most ${rules.package_max} characters.`;
  const segments = name.split(".");
  if (segments.length < rules.package_segments_min) {
    return "The package name needs at least two segments, like com.example.myapp: Play rejects a single-segment application id.";
  }
  if (segments.length > rules.package_segments_max) {
    return `The package name can have at most ${rules.package_segments_max} segments.`;
  }
  const first = segments[0] ?? "";
  if (rules.package_first_segment_forbidden.includes(first)) {
    return `A package cannot start with '${first}.': the runtime refuses to load an app's classes from it.`;
  }
  for (const segment of segments) {
    if (!matches(rules.segment, segment)) {
      return `Package segment '${segment}' is invalid: lowercase letters, digits and underscores only, starting with a letter.`;
    }
    if (segment.length > rules.segment_max) {
      return `Package segment '${segment}' is longer than ${rules.segment_max} characters.`;
    }
    if (isReserved(segment, rules)) {
      return `Package segment '${segment}' is a Kotlin or Java keyword, so the generated sources would not compile.`;
    }
  }
  return undefined;
}

function checkPackage(check: Checker, name: string, rules: Rules): void {
  const problem = packageProblem(name, rules);
  if (problem) check.fail("package_name", problem);
}

function checkModules(check: Checker, names: string[], rules: Rules, taken: string[]): void {
  const where = "feature_modules";
  if (names.length > rules.feature_modules_max) {
    check.fail(
      where,
      `At most ${rules.feature_modules_max} feature modules can be scaffolded at once; add more later with add_feature.py.`,
    );
    return;
  }
  for (const name of names) {
    if (!matches(rules.module_name, name)) {
      check.fail(
        where,
        `Module name '${name}' is invalid: lowercase letters and digits, words joined by single underscores, starting with a letter. It becomes a Gradle path, a package segment and a class name.`,
      );
      return;
    }
    if (name.length > rules.module_name_max) {
      check.fail(where, `Module name '${name}' is longer than ${rules.module_name_max} characters.`);
      return;
    }
    if (isReserved(name, rules)) {
      check.fail(where, `Module name '${name}' is a Kotlin or Java keyword, so its package would not compile.`);
      return;
    }
    if (taken.includes(name)) {
      check.fail(
        where,
        `Module name '${name}' is taken: the template already ships a module or directory called that. Pick another name.`,
      );
      return;
    }
  }
  if (new Set(names).size !== names.length) {
    check.fail(where, "Feature module names must be unique.");
    return;
  }
  const seen = new Map<string, string>();
  for (const name of names) {
    const pascal = pascalCase(name);
    const other = seen.get(pascal);
    if (other !== undefined) {
      check.fail(where, `Modules '${other}' and '${name}' would both generate classes named ${pascal}…; rename one.`);
      return;
    }
    seen.set(pascal, name);
  }
}

function fieldDefault(fields: Field[], key: string): unknown {
  return fields.find((field) => field.key === key)?.default;
}

function checkAndroid(check: Checker, data: Json, fields: Field[], rules: Rules): void {
  const levels = (fields.find((field) => field.key === "android.min_sdk")?.choices ?? []).map(
    (choice) => choice.value as number,
  );
  const number = (key: string) => (Number.isInteger(data[key]) ? (data[key] as number) : (fieldDefault(fields, `android.${key}`) as number));
  const minSdk = number("min_sdk");
  const targetSdk = number("target_sdk");
  const compileSdk = number("compile_sdk");
  const newest = Math.max(...levels);

  if (!levels.includes(minSdk)) {
    check.fail("android.min_sdk", `minSdk ${minSdk} is outside the supported range (${Math.min(...levels)}–${newest}).`);
    return;
  }
  if (targetSdk < minSdk) {
    check.fail("android.target_sdk", "targetSdk cannot be lower than minSdk.");
    return;
  }
  for (const [label, key, level] of [
    ["targetSdk", "target_sdk", targetSdk],
    ["compileSdk", "compile_sdk", compileSdk],
  ] as const) {
    if (level > newest) {
      check.fail(`android.${key}`, `${label} ${level} is past API ${newest}, the newest level this generator knows.`);
      return;
    }
  }
  if (compileSdk < targetSdk) {
    check.fail("android.compile_sdk", "compileSdk cannot be lower than targetSdk.");
    return;
  }
  if (compileSdk < rules.min_compile_sdk) {
    check.fail(
      "android.compile_sdk",
      `compileSdk must be at least ${rules.min_compile_sdk}: the template's libraries are built against it, and an older one fails the project's first build.`,
    );
    return;
  }
  const versionCode = number("version_code");
  if (versionCode < 1 || versionCode > rules.version_code_max) {
    check.fail("android.version_code", `versionCode must be between 1 and ${rules.version_code_max}.`);
    return;
  }
  const versionName =
    typeof data.version_name === "string" && data.version_name ? data.version_name : String(fieldDefault(fields, "android.version_name"));
  if (!matches(rules.version_name, versionName)) {
    check.fail("android.version_name", "versionName must look like 1.0.0.");
    return;
  }

  for (const [label, key, pattern, example] of [
    ["API base URL", "api_base_urls", rules.http_url, "https://api.example.com/"],
    ["WebSocket URL", "web_socket_urls", rules.socket_url, "wss://api.example.com/ws"],
  ] as const) {
    const urls = isObject(data[key]) ? (data[key] as Record<string, string>) : {};
    for (const [flavour, url] of Object.entries(urls)) {
      if (!rules.flavours.includes(flavour)) {
        check.fail(`android.${key}`, `Unknown flavour '${flavour}' for an ${label}. Choose from: ${rules.flavours.join(", ")}.`);
        return;
      }
      if (!matches(pattern, url)) {
        check.fail(
          `android.${key}`,
          `The ${flavour} ${label} must look like ${example}, without quotes, spaces, backslashes or $.`,
        );
        return;
      }
    }
  }

  const scheme = typeof data.deeplink_scheme === "string" ? data.deeplink_scheme : "";
  if (scheme && !matches(rules.scheme, scheme)) {
    check.fail(
      "android.deeplink_scheme",
      "The deep-link scheme must start with a lowercase letter and contain only lowercase letters, digits, +, . and -.",
    );
    return;
  }
  const host = typeof data.deeplink_host === "string" ? data.deeplink_host : "";
  if (host && !matches(rules.host, host)) {
    check.fail("android.deeplink_host", "The deep-link host must be a domain name, like example.com.");
    return;
  }

  const keystores = Array.isArray(data.keystores) ? (data.keystores as Json[]) : [];
  for (const keystore of keystores) {
    const problem = keystoreProblem(keystore, rules);
    if (problem) {
      check.fail("android.keystores", problem);
      return;
    }
  }
  const names = keystores.map((keystore) => keystore.name);
  if (new Set(names).size !== names.length) check.fail("android.keystores", "Each signing key can only be described once.");
}

/** Why the generator would refuse to make this signing key, or undefined. */
function keystoreProblem(keystore: Json, rules: Rules): string | undefined {
  const text = (key: string, fallback: string) =>
    typeof keystore[key] === "string" && keystore[key] !== "" ? (keystore[key] as string) : fallback;
  const name = text("name", "");
  if (!rules.keystore_names.includes(name)) {
    return `Unknown signing key '${name}'. The build has four: ${rules.keystore_names.join(", ")}.`;
  }
  if (!matches(rules.keystore_alias, text("alias", ""))) {
    return `The ${name} key alias must be letters, digits, dots, hyphens or underscores, and start with a letter or digit.`;
  }
  const passwords: [string, string][] = [["store", text("store_password", "")]];
  if (typeof keystore.existing_path === "string" && keystore.existing_path) passwords.push(["key", text("key_password", "")]);
  for (const [label, password] of passwords) {
    if (password.length < rules.keystore_password_min) {
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
