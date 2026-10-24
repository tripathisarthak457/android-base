import { type Json, isObject, isStringList } from "../../../lib/json.ts";
import { codePoints, listed } from "../../../lib/text.ts";
import type { Catalogue, Field, Rules, Target } from "../../catalogue/types.ts";
import { closeOver } from "../resolve.ts";
import { checkAndroid } from "./android.ts";
import { Checker, matches, withoutNulls } from "./check.ts";
import { checkKeystoreShape } from "./keystores.ts";
import { checkAppName, checkDisplayName, checkModules, checkPackage } from "./names.ts";

// The generator's spec rules, run in the browser so a mistake is caught where it was typed rather
// than as a 400 after the round trip. They read their patterns and limits from the catalogue, and
// a test runs every case of `generator/tests/fixtures/validation_cases.json` through both sides.
//
// The checks run in the generator's order and stop at the first problem in each field, so the
// first error reported is the one the generator would report.

export type SpecErrors = Map<string, string>;

const SCHEMA = 2;

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
  // Each option is a top-level true or false; a target without one has no such field.
  for (const option of target.options) {
    if (!topLevel.has(option.key)) topLevel.set(option.key, { key: option.key, type: "bool", default: option.default });
  }
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
    if (missing.length > 0) check.fail("design_style", `The ${designStyle} design style needs ${listed(missing.map((name) => `'${name}'`))}.`);
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

  for (const [block, fields] of blocks) {
    const values = isObject(data[block]) ? data[block] : {};
    if (block === "android") checkAndroid(check, values, fields, rules);
    else checkByRule(check, values, block, fields, rules);
  }

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

/**
 * A settings block with no rule module of its own, such as `ios`: each text field is held to its
 * `max` and to the pattern its `rule` names, which is how the catalogue states the generator's
 * checks for it.
 */
function checkByRule(check: Checker, data: Json, block: string, fields: Field[], rules: Rules): void {
  const patterns: Record<string, unknown> = rules;
  for (const field of fields) {
    const value = data[field.key.slice(block.length + 1)];
    if (typeof value !== "string" || value === "") continue;
    const pattern = field.rule === undefined ? undefined : patterns[field.rule];
    if (field.max !== undefined && codePoints(value) > field.max) {
      check.fail(field.key, `\`${field.key}\` can be at most ${field.max} characters.`);
    } else if (typeof pattern === "string" && !matches(pattern, value)) {
      check.fail(field.key, `\`${field.key}\` is not in the form the generator accepts.`);
    }
  }
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
