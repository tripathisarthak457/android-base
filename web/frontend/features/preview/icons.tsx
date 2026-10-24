import { GLYPHS, type GlyphName } from "./glyphs.ts";

export type IconName = GlyphName;

/** One of the template's `AppIcons`, drawn from its own paths and tinted whole, as `AppIcon` tints it. */
export function Icon({ name, size, colour }: { name: IconName; size: number; colour: string }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden style={{ flexShrink: 0 }}>
      {GLYPHS[name].paths.map((path, index) =>
        path.kind === "stroke" ? (
          <path key={index} d={path.d} fill="none" stroke={colour} strokeWidth={path.width} strokeLinecap="round" strokeLinejoin="round" />
        ) : (
          <path key={index} d={path.d} fill={colour} fillRule={"evenOdd" in path && path.evenOdd ? "evenodd" : "nonzero"} />
        ),
      )}
    </svg>
  );
}
