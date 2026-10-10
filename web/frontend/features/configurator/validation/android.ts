import type { Field, Rules } from "../../catalogue/types.ts";
import { type Checker, type Json, isObject, matches } from "./check.ts";
import { keystoreProblem } from "./keystores.ts";

/** The `android` block: SDK levels, version, flavour URLs, deep links and signing keys, in the generator's order. */

function fieldDefault(fields: Field[], key: string): unknown {
  return fields.find((field) => field.key === key)?.default;
}

export function checkAndroid(check: Checker, data: Json, fields: Field[], rules: Rules): void {
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
