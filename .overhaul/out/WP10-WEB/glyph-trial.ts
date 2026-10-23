import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { parseGlyphs, strokeWidths } from "file:///D:/projects/tools/.overhaul/wt/sw-WP10/web/frontend/features/preview/glyphSource.ts";
const dir = "D:/projects/tools/.overhaul/wt/sw-WP10/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/icon/glyph";
const widths = strokeWidths(readFileSync(join(dir, "GlyphBuilder.kt"), "utf8"));
let all: Record<string, unknown> = {};
for (const f of readdirSync(dir)) if (f !== "GlyphBuilder.kt") all = { ...all, ...parseGlyphs(readFileSync(join(dir, f), "utf8"), widths) };
console.log(Object.keys(all).length, JSON.stringify(all).length, widths);
console.log(JSON.stringify((all as any).Home), JSON.stringify((all as any).ListView));
