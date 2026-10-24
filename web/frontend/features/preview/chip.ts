// The template's AppChip (`component/selection/AppChip.kt`), in dp: a filled pill with no outline.

export const CHIP = {
  height: 32,
  padding: 12,
  /** At the end beside the remove cross, which spaces itself as iOS's does. */
  paddingEndRemovable: 10,
  gap: 6,
  icon: 16,
  remove: 14,
  text: "subheadline",
} as const;

/** The colour roles of a chip's container and of its label and glyphs. */
export function chipRoles(selected: boolean): { container: string; content: string } {
  return selected ? { container: "accentSubtle", content: "accentContent" } : { container: "fill", content: "contentPrimary" };
}
