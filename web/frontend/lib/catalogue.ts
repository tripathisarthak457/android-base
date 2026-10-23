import { type Announced, offerOnly, platformsOf } from "../features/catalogue/platforms.ts";
import type { Catalogue, Target } from "../features/catalogue/types.ts";
import data from "./catalogue.json";

/**
 * The targets a visitor can generate here. The generator may already have others; they read as
 * coming soon until their key is added, so the site offers a platform only once it is verified.
 */
const OFFERED = new Set(["android"]);

/** Named as coming soon while the generator has no target for them. */
const ANNOUNCED: Announced[] = [
  { key: "multiplatform", title: "Compose Multiplatform", summary: "One Compose UI for Android and iOS: Navigation 3, Koin, Ktor, Room." },
  { key: "ios", title: "Swift", summary: "SwiftUI with @Observable stores, local Swift packages and XcodeGen. iOS 17 and later." },
];

/**
 * The catalogue this build was made with, as this site offers it. It is emitted by the generator
 * and checked in, so the page, its counts and its copy deploy together with the generator they
 * describe.
 */
export const CATALOGUE = offerOnly(data as unknown as Catalogue, OFFERED);

/** Every platform the site names, offered or coming soon, in the order it shows them. */
export const PLATFORMS = platformsOf(CATALOGUE, ANNOUNCED);

export { PRIVACY, REPO } from "./links.ts";

export function android(): Target {
  const target = CATALOGUE.targets.find((candidate) => candidate.key === "android") ?? CATALOGUE.targets[0];
  if (!target) throw new Error("The checked-in catalogue has no targets.");
  return target;
}
