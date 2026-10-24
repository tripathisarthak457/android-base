// Reads the template's icon set (`core/designsystem/.../icon/glyph/*.kt`, built with the small DSL
// in `GlyphBuilder.kt`) into SVG paths, so the preview draws the app's own glyphs. `npm run glyphs`
// writes `glyphs.ts` with it, and `glyphs.test.ts` checks that file against the template.

export type GlyphPath = { kind: "stroke" | "fill"; d: string; width?: number; evenOdd?: boolean };

export type Glyph = { paths: GlyphPath[]; mirrored: boolean };

/** `StrokeWidth`, `BoldStrokeWidth` and `DotWidth` in `GlyphBuilder.kt`. */
export function strokeWidths(builder: string): Record<string, number> {
  const widths: Record<string, number> = {};
  for (const [, name = "", value = ""] of builder.matchAll(/const val (\w+) = ([\d.]+)f/g)) widths[name] = Number(value);
  return widths;
}

/** The text between the brace at `open` and the one that closes it. */
function block(source: string, open: number): string {
  let depth = 0;
  for (let index = open; index < source.length; index++) {
    if (source[index] === "{") depth++;
    if (source[index] === "}" && --depth === 0) return source.slice(open + 1, index);
  }
  throw new Error(`unbalanced braces after ${source.slice(open, open + 40)}`);
}

const number = (value: string): string => {
  const parsed = Number(value.trim().replace(/f$/, ""));
  if (Number.isNaN(parsed)) throw new Error(`not a number: ${value}`);
  return String(parsed);
};

/** One `PathBuilder` block as SVG path data. */
function pathData(body: string): string {
  const commands: string[] = [];
  for (const [, call = "", args = ""] of body.matchAll(/(\w+)\(([^()]*)\)/g)) {
    const values = args.split(",").filter((value) => value.trim() !== "");
    switch (call) {
      case "moveTo":
        commands.push(`M${values.map(number).join(" ")}`);
        break;
      case "lineTo":
        commands.push(`L${values.map(number).join(" ")}`);
        break;
      case "curveTo":
        commands.push(`C${values.map(number).join(" ")}`);
        break;
      case "arcTo": {
        const [rx = "", ry = "", rotation = "", large = "", sweep = "", x = "", y = ""] = values.map((value) => value.trim());
        commands.push(`A${number(rx)} ${number(ry)} ${number(rotation)} ${large === "true" ? 1 : 0} ${sweep === "true" ? 1 : 0} ${number(x)} ${number(y)}`);
        break;
      }
      case "close":
        commands.push("Z");
        break;
      case "circle": {
        const [cx = 0, cy = 0, r = 0] = values.map((value) => Number(number(value)));
        commands.push(`M${cx - r} ${cy}A${r} ${r} 0 1 1 ${cx + r} ${cy}A${r} ${r} 0 1 1 ${cx - r} ${cy}Z`);
        break;
      }
      case "dot": {
        const [x = 0, y = 0] = values.map((value) => Number(number(value)));
        commands.push(`M${x} ${y}L${Math.round((x + 0.01) * 1000) / 1000} ${y}`);
        break;
      }
      default:
        throw new Error(`the glyph DSL has a call this reader does not know: ${call}`);
    }
  }
  return commands.join("");
}

/** Every glyph a glyph file declares, by its `AppIcons` name. */
export function parseGlyphs(source: string, widths: Record<string, number>): Record<string, Glyph> {
  const kotlin = source.replace(/\/\*[\s\S]*?\*\//g, "").replace(/\/\/.*$/gm, "");
  const glyphs: Record<string, Glyph> = {};
  for (const match of kotlin.matchAll(/val (\w+) by icon\("\w+"(, autoMirror = true)?\) \{/g)) {
    const [header = "", name = "", mirror] = match;
    const body = block(kotlin, (match.index ?? 0) + header.length - 1);
    const paths: GlyphPath[] = [];
    for (const part of body.matchAll(/\b(stroke|fill)(?:\(([^)]*)\))?\s*\{/g)) {
      const [opening = "", kind = "", args = ""] = part;
      const d = pathData(block(body, (part.index ?? 0) + opening.length - 1));
      if (kind === "stroke") {
        const width = args.trim() === "" ? widths.StrokeWidth : widths[args.trim()] ?? Number(number(args));
        if (width === undefined || Number.isNaN(width)) throw new Error(`${name}: unknown stroke width ${args}`);
        paths.push({ kind: "stroke", d, width });
      } else {
        paths.push({ kind: "fill", d, ...(/evenOdd = true/.test(args) ? { evenOdd: true } : {}) });
      }
    }
    glyphs[name] = { paths, mirrored: mirror !== undefined };
  }
  return glyphs;
}

/** Where the template keeps its glyphs, from the repository root. */
export const GLYPH_DIRECTORY = "templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/icon/glyph";

/** Every glyph of the template, from the files of `GLYPH_DIRECTORY` by name. */
export function readTemplateGlyphs(files: Record<string, string>): Record<string, Glyph> {
  const builder = files["GlyphBuilder.kt"];
  if (!builder) throw new Error(`GlyphBuilder.kt is not in ${GLYPH_DIRECTORY}`);
  const widths = strokeWidths(builder);
  const glyphs: Record<string, Glyph> = {};
  for (const [name, source] of Object.entries(files)) {
    if (name !== "GlyphBuilder.kt") Object.assign(glyphs, parseGlyphs(source, widths));
  }
  return glyphs;
}
