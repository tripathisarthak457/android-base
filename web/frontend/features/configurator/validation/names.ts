import type { Rules } from "../../catalogue/types.ts";
import { type Checker, codePoints, matches } from "./check.ts";

// The names a visitor types on the identity step: app, display, package and feature modules.

/** What a display name may not contain: line breaks and controls, but format characters stay (Persian needs ZWNJ). */
const NOT_IN_DISPLAY_NAME = /[\p{Cc}\p{Cs}\p{Zl}\p{Zp}\uFFFE\uFFFF]/u;

function pascalCase(name: string): string {
  return name
    .split("_")
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join("");
}

function isReserved(word: string, rules: Rules): boolean {
  return Object.values(rules.reserved_words).some((words) => words.includes(word));
}

export function checkAppName(check: Checker, name: string, rules: Rules): void {
  if (!name) {
    check.fail("app_name", "An app name is required.");
  } else if (!matches(rules.app_name, name)) {
    check.fail(
      "app_name",
      `The app name must start with a letter and be at most ${rules.app_name_max} letters, digits, spaces, dots, hyphens or underscores. Put anything else in the display name.`,
    );
  }
}

export function checkDisplayName(check: Checker, name: string, rules: Rules): void {
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
  if (codePoints(name) > rules.package_max) return `The package name can be at most ${rules.package_max} characters.`;
  const segments = name.split(".");
  if (segments.length < rules.package_segments_min) {
    return `The package name needs at least ${rules.package_segments_min} segments, like com.example.myapp: Play rejects a single-segment application id.`;
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

export function checkPackage(check: Checker, name: string, rules: Rules): void {
  const problem = packageProblem(name, rules);
  if (problem) check.fail("package_name", problem);
}

export function checkModules(check: Checker, names: string[], rules: Rules, taken: string[]): void {
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
