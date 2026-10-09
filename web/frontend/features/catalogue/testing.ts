import { readFileSync } from "node:fs";
import { join } from "node:path";
import type { Catalogue, Target } from "./types.ts";

/** The repository root, for the fixtures the generator and the API share with these tests. */
export const REPOSITORY = join(import.meta.dirname, "..", "..", "..", "..");

export function readJson<T>(...path: string[]): T {
  return JSON.parse(readFileSync(join(REPOSITORY, ...path), "utf8")) as T;
}

export function loadCatalogue(): Catalogue {
  return readJson<Catalogue>("web", "frontend", "lib", "catalogue.json");
}

export function androidTarget(catalogue: Catalogue = loadCatalogue()): Target {
  const target = catalogue.targets.find((candidate) => candidate.key === "android");
  if (!target) throw new Error("The catalogue has no android target.");
  return target;
}
